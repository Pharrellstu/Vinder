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
import com.example.vinted.data.dto.AccountFollowingEntity
import com.example.vinted.data.dto.DialogueMessageEntity
import com.example.vinted.data.dto.ItemOfferEntity
import com.example.vinted.data.dto.PurchaseFullEntity
import com.example.vinted.ui.initialisers.SupabaseClientInitialiser
import com.example.vinted.ui.models.NotificationType
import io.github.jan.supabase.SupabaseClient
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Subscribes to Supabase Realtime inserts on the activity tables while the user is signed in and
 * posts a local notification for each event addressed to them:
 *  - `dialogue_message` → a new message,
 *  - `item_offer`       → a new offer on one of their items,
 *  - `purchase`         → one of their items sold,
 *  - `account_following`→ a new follower.
 *
 * Each handler decides "is this for me?" purely from the Realtime record (plus RLS, which already
 * scopes which rows are streamed) and never makes a follow-up Postgrest read — from the background
 * service those reads do not carry the auth token, so they would return empty and drop the event.
 *
 * Hosted by [MessageListenerService], a foreground service that keeps the app process (and this
 * socket) alive while backgrounded or locked. It does not survive the app being swiped away or long
 * Doze periods — true app-killed push would require FCM, which this setup does not provide.
 *
 * Note: every table above must be in the `supabase_realtime` publication for its inserts to stream
 * (see migrations 003 and 009).
 */
object MessageNotificationController {

    // Settable rather than a hardcoded singleton reference so tests can swap in a fake client;
    // this is an `object` (process-wide realtime listener) so constructor injection isn't
    // possible — a mutable field is the seam instead.
    internal var client: SupabaseClient = SupabaseClientInitialiser.client

    private const val TAG = "MsgNotifications"
    private const val RETRY_DELAY_MS = 5_000L
    private const val SESSION_RESTORE_TIMEOUT_MS = 5_000L
    private const val CHANNEL_NAME = "vinder-notifications"

    // Notification ids share one namespace, so offset each event type to keep a message, offer,
    // sale, and follow with the same row id from overwriting one another.
    private const val OFFER_ID_OFFSET = 1_000_000
    private const val SALE_ID_OFFSET = 2_000_000
    private const val FOLLOW_ID_OFFSET = 3_000_000

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
                    // All postgresChangeFlow bindings must be created before subscribe(): the
                    // library forbids adding them once the channel has joined.
                    val messages = channel.insertsOn("dialogue_message")
                    val offers = channel.insertsOn("item_offer")
                    val sales = channel.insertsOn("purchase")
                    val follows = channel.insertsOn("account_following")
                    channel.subscribe()
                    // Collect every stream concurrently; coroutineScope keeps the channel alive
                    // until one collector fails (e.g. dropped socket), which then triggers a retry.
                    coroutineScope {
                        launch { messages.collect { it.decode<DialogueMessageEntity>()?.let { m -> handleMessage(appContext, m) } } }
                        launch { offers.collect { it.decode<ItemOfferEntity>()?.let { o -> handleOffer(appContext, o) } } }
                        launch { sales.collect { it.decode<PurchaseFullEntity>()?.let { p -> handleSale(appContext, p) } } }
                        launch { follows.collect { it.decode<AccountFollowingEntity>()?.let { f -> handleFollow(appContext, f) } } }
                    }
                }.onFailure { Log.w(TAG, "Realtime notification listener dropped, retrying: ${it.message}") }
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
                    runCatching { client.realtime.removeChannel(channel) }
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

    private fun RealtimeChannel.insertsOn(tableName: String) =
        postgresChangeFlow<PostgresAction.Insert>(schema = "public") { table = tableName }

    private inline fun <reified T> PostgresAction.Insert.decode(): T? = decodeRecordOrNull()

    private suspend fun handleMessage(context: Context, message: DialogueMessageEntity) {
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

    private fun handleOffer(context: Context, offer: ItemOfferEntity) {
        val me = SessionManager.currentAccountId
        // item_offer RLS streams to the offer creator AND the item's seller. Only the seller should
        // be notified, so skip offers we made ourselves.
        if (me == SessionManager.NO_ACCOUNT_ID || offer.offerCreatorId == me) return

        VinderNotifications.notify(
            context = context,
            type = NotificationType.OFFERS,
            title = "New offer",
            body = "Someone offered €${formatPrice(offer.offerPrice)} on your item",
            id = OFFER_ID_OFFSET + offer.itemOfferId,
        )
    }

    private fun handleSale(context: Context, purchase: PurchaseFullEntity) {
        val me = SessionManager.currentAccountId
        // purchase RLS streams to both buyer and seller; "item sold" only concerns the seller.
        if (me == SessionManager.NO_ACCOUNT_ID || purchase.sellerId != me) return

        VinderNotifications.notify(
            context = context,
            type = NotificationType.ITEM_SOLD,
            title = "Item sold",
            body = "Your item sold for €${formatPrice(purchase.totalAmount)}",
            id = SALE_ID_OFFSET + purchase.purchaseId,
        )
    }

    private fun handleFollow(context: Context, follow: AccountFollowingEntity) {
        val me = SessionManager.currentAccountId
        // account_following is world-readable, so Realtime streams every follow row. Notify only
        // when we are the one being followed, and never for our own follow actions.
        if (me == SessionManager.NO_ACCOUNT_ID || follow.followingId != me || follow.followerId == me) return

        VinderNotifications.notify(
            context = context,
            type = NotificationType.NEW_FOLLOWERS,
            title = "New follower",
            body = "Someone started following you",
            id = FOLLOW_ID_OFFSET + follow.accountFollowingId,
        )
    }

    /** Trims a whole-number price to "35" and keeps cents otherwise ("12.50"). */
    private fun formatPrice(price: Double): String =
        if (price % 1.0 == 0.0) price.toInt().toString() else "%.2f".format(price)

    /**
     * Resolves a sender's display name, caching successful lookups. A failed lookup is NOT cached
     * (so a transient error doesn't permanently pin the sender to the generic fallback), and a
     * generic title is used instead — the notification still fires.
     */
    private suspend fun resolveSenderName(senderId: Int): String {
        senderNameCache[senderId]?.let { return it }
        val name = runCatching {
            client.from("account")
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
