package com.example.vinted.notifications

import android.content.Context
import android.util.Log
import com.example.vinted.data.SessionManager
import com.example.vinted.data.dto.AccountEntity
import com.example.vinted.data.dto.DialogueEntity
import com.example.vinted.data.dto.DialogueMessageEntity
import com.example.vinted.ui.initialisers.SupabaseClientInitialiser
import com.example.vinted.ui.models.NotificationType
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecordOrNull
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private val senderNameCache = mutableMapOf<Int, String>()

    fun start(context: Context) {
        if (job?.isActive == true) return
        val appContext = context.applicationContext
        val client = SupabaseClientInitialiser.client

        job = scope.launch {
            // Keep (re)subscribing: a single subscribe()/collect failure (e.g. Realtime not
            // yet enabled for the table, or a dropped socket) would otherwise kill the listener
            // permanently and require an app restart. Retry with a fixed backoff instead.
            while (isActive) {
                runCatching {
                    val channel = client.channel("vinder-messages")
                    val inserts = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                        table = "dialogue_message"
                    }
                    channel.subscribe()
                    inserts.collect { insert ->
                        val message = insert.decodeRecordOrNull<DialogueMessageEntity>() ?: return@collect
                        handleIncoming(appContext, message)
                    }
                }.onFailure { Log.w(TAG, "Realtime message listener dropped, retrying: ${it.message}") }
                if (!isActive) break
                delay(RETRY_DELAY_MS)
            }
        }
    }

    private suspend fun handleIncoming(context: Context, message: DialogueMessageEntity) {
        val me = SessionManager.currentAccountId
        if (me == -1 || message.senderId == me) return

        val client = SupabaseClientInitialiser.client

        // Only notify when the current user is a participant in this dialogue.
        val dialogue = runCatching {
            client.from("dialogue")
                .select { filter { eq("dialogue_id", message.dialogueId) } }
                .decodeList<DialogueEntity>()
                .firstOrNull()
        }.getOrNull() ?: return
        if (dialogue.creatorId != me && dialogue.receiverId != me) return

        val senderName = senderNameCache.getOrPut(message.senderId) {
            runCatching {
                client.from("account")
                    .select { filter { eq("account_id", message.senderId) } }
                    .decodeList<AccountEntity>()
                    .firstOrNull()
                    ?.accountName
            }.getOrNull() ?: "New message"
        }

        VinderNotifications.notify(
            context = context,
            type = NotificationType.NEW_MESSAGES,
            title = senderName,
            body = message.text,
            id = message.messageId,
        )
    }

    fun stop() {
        senderNameCache.clear()
        job?.cancel()
        job = null
    }
}
