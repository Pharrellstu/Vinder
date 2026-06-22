package com.example.vinted.notifications

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.vinted.data.AccountPreferences
import com.example.vinted.data.SessionManager
import com.example.vinted.data.dto.AccountEntity
import com.example.vinted.data.dto.DialogueMessageEntity
import com.example.vinted.ui.initialisers.SupabaseClientInitialiser
import com.example.vinted.ui.models.NotificationType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecordOrNull
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Subscribes to Supabase Realtime inserts on `dialogue_message` while the user is signed in
 * and posts a local notification for each incoming message addressed to them.
 *
 * This is hosted by [MessageListenerService], a foreground service that keeps the app process
 * (and therefore this Realtime socket) alive while the app is backgrounded or the screen is
 * locked. It does not survive the app being swiped away from recents or long Doze periods —
 * true app-killed push would require a backend such as FCM, which this setup does not provide.
 *
 * Note: the `dialogue_message` table must be part of the `supabase_realtime` publication on
 * the server for inserts to be streamed.
 */
object MessageNotificationController {

    private const val TAG = "MsgNotifications"
    private const val RETRY_DELAY_MS = 5_000L
    private const val SESSION_RESTORE_TIMEOUT_MS = 5_000L
    private const val CHANNEL_NAME = "vinder-messages"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private val senderNameCache = mutableMapOf<Int, String>()

    // The channel currently being collected. Held so a network-regain event can tear it down and
    // force an immediate re-subscribe — a stale (half-open) socket never errors on its own.
    @Volatile
    private var activeChannel: RealtimeChannel? = null

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    fun start(context: Context) {
        if (job?.isActive == true) return
        val appContext = context.applicationContext
        val client = SupabaseClientInitialiser.client

        registerNetworkCallback(appContext)

        job = scope.launch {
            // The service can be (re)started by the OS without the Activity ever running — e.g.
            // START_STICKY after the process is reclaimed, or BootReceiver after a reboot — in
            // which case the in-memory SessionManager is empty and every message would be dropped.
            // Restore it from disk before listening.
            if (!ensureSession()) {
                Log.w(TAG, "No signed-in session; stopping message listener.")
                MessageListenerService.stop(appContext)
                return@launch
            }

            // Keep (re)subscribing: a single subscribe()/collect failure (e.g. Realtime not yet
            // enabled for the table, or a dropped socket) would otherwise kill the listener
            // permanently and require an app restart. Retry with a fixed backoff instead.
            while (isActive) {
                val channel = client.channel(CHANNEL_NAME)
                activeChannel = channel
                runCatching {
                    val inserts = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                        table = "dialogue_message"
                    }
                    channel.subscribe()
                    inserts.collect { insert ->
                        val message = insert.decodeRecordOrNull<DialogueMessageEntity>() ?: return@collect
                        handleIncoming(appContext, message)
                    }
                }.onFailure { Log.w(TAG, "Realtime message listener dropped, retrying: ${it.message}") }
                // Always tear the channel down before retrying so repeated subscribes don't leak it.
                runCatching { client.realtime.removeChannel(channel) }
                if (!isActive) break
                delay(RETRY_DELAY_MS)
            }
        }
    }

    /**
     * Ensures [SessionManager] is populated. Returns true if a signed-in account is available.
     * Waits for the persisted Supabase auth session to settle, then restores the app account row
     * id from [AccountPreferences] (mirrors `SplashViewModel.restoreSession`).
     */
    private suspend fun ensureSession(): Boolean {
        if (SessionManager.currentAccountId != SessionManager.NO_ACCOUNT_ID) return true

        val client = SupabaseClientInitialiser.client
        val status = withTimeoutOrNull(SESSION_RESTORE_TIMEOUT_MS) {
            client.auth.sessionStatus.first { it !is SessionStatus.Initializing }
        }
        if (status !is SessionStatus.Authenticated) return false

        val saved = AccountPreferences.load() ?: return false
        SessionManager.currentAccountId = saved.first
        SessionManager.currentEmail = saved.second
        return true
    }

    private fun registerNetworkCallback(context: Context) {
        if (networkCallback != null) return
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Connectivity returned. Drop the current channel so the collect loop completes
                // and re-subscribes on a fresh socket instead of waiting on a possibly stale one.
                val channel = activeChannel ?: return
                scope.launch {
                    runCatching { SupabaseClientInitialiser.client.realtime.removeChannel(channel) }
                }
            }
        }
        runCatching {
            cm.registerNetworkCallback(
                NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build(),
                callback,
            )
        }.onSuccess {
            connectivityManager = cm
            networkCallback = callback
        }
    }

    private fun unregisterNetworkCallback() {
        val cm = connectivityManager
        val callback = networkCallback
        if (cm != null && callback != null) {
            runCatching { cm.unregisterNetworkCallback(callback) }
        }
        connectivityManager = null
        networkCallback = null
    }

    private suspend fun handleIncoming(context: Context, message: DialogueMessageEntity) {
        val me = SessionManager.currentAccountId
        // Realtime already enforced the `dialogue_message` participant RLS policy before delivering
        // this insert, so receiving it already proves we are a participant of the dialogue. We must
        // NOT re-verify with a Postgrest read of `dialogue` here: from the background service that
        // request does not carry our auth token reliably, so RLS returns an empty row and the
        // notification gets silently dropped. Just skip our own messages.
        if (me == SessionManager.NO_ACCOUNT_ID || message.senderId == me) return

        val senderName = resolveSenderName(message.senderId)

        VinderNotifications.notify(
            context = context,
            type = NotificationType.NEW_MESSAGES,
            title = senderName,
            body = message.text,
            id = message.messageId,
        )
    }

    /**
     * Resolves a sender's display name, caching successful lookups. A failed lookup is NOT cached
     * (so a transient error doesn't permanently pin the sender to the generic fallback), and a
     * generic title is used instead — the notification still fires.
     */
    private suspend fun resolveSenderName(senderId: Int): String {
        senderNameCache[senderId]?.let { return it }
        val name = runCatching {
            SupabaseClientInitialiser.client.from("account")
                .select { filter { eq("account_id", senderId) } }
                .decodeList<AccountEntity>()
                .firstOrNull()
                ?.accountName
        }.getOrNull()
        if (name != null) senderNameCache[senderId] = name
        return name ?: "New message"
    }

    fun stop() {
        senderNameCache.clear()
        unregisterNetworkCallback()
        activeChannel = null
        job?.cancel()
        job = null
    }
}
