package io.github.barqallayl.burkan.feature.connection

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import dev.zacsweers.metro.Inject
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.model.messageRes
import io.github.barqallayl.burkan.feature.connection.data.PairingStatus

/**
 * The pairing notification. The pairing dialog closes as soon as the user leaves Settings, so the code is typed into
 * this notification's reply field instead of into the app.
 */
@Inject
class PairingNotifications(private val application: Application) {

    private val manager = application.getSystemService(NotificationManager::class.java)

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            application.getString(R.string.pairing_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = application.getString(R.string.pairing_channel_description) }
        manager.createNotificationChannel(channel)
    }

    /** The ongoing notification for [status]; a failure keeps the reply field so the code can be typed again. */
    fun forStatus(status: PairingStatus): Notification {
        val builder = base().setOngoing(true)
        when (status) {
            PairingStatus.Idle, PairingStatus.WaitingForCode -> builder
                .setContentTitle(application.getString(R.string.pairing_waiting_title))
                .setStyledText(application.getString(R.string.pairing_waiting_text))
                .addAction(replyAction())
            PairingStatus.Paired -> builder
                .setContentTitle(application.getString(R.string.pairing_done_title))
                .setStyledText(application.getString(R.string.pairing_done_text))
            PairingStatus.Pairing -> builder
                .setContentTitle(application.getString(R.string.pairing_in_progress_title))
                .setProgress(0, 0, true)
            is PairingStatus.Failed -> builder
                .setContentTitle(application.getString(R.string.pairing_failed_title))
                .setStyledText(application.getString(status.error.messageRes()))
                .addAction(replyAction())
        }
        return builder.build()
    }

    /** Replaces the ongoing notification while the service is in the foreground. */
    fun update(status: PairingStatus) {
        manager.notify(ONGOING_ID, forStatus(status))
    }

    /** The notification left behind once the session is over: paired, or why not. */
    fun showOutcome(status: PairingStatus) {
        val builder = base().setAutoCancel(true)
        if (status == PairingStatus.Paired) {
            builder.setContentTitle(application.getString(R.string.pairing_done_title))
                .setStyledText(application.getString(R.string.pairing_done_text))
        } else {
            val text = (status as? PairingStatus.Failed)?.error?.messageRes() ?: R.string.error_pairing_failed
            builder.setContentTitle(application.getString(R.string.pairing_failed_title))
                .setStyledText(application.getString(text))
        }
        manager.notify(OUTCOME_ID, builder.build())
    }

    fun cancelOutcome() {
        manager.cancel(OUTCOME_ID)
    }

    private fun base(): NotificationCompat.Builder = NotificationCompat.Builder(application, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setOnlyAlertOnce(true)
        .setContentIntent(openApp())

    private fun NotificationCompat.Builder.setStyledText(text: String): NotificationCompat.Builder =
        setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text))

    private fun replyAction(): NotificationCompat.Action {
        val input = RemoteInput.Builder(KEY_CODE)
            .setLabel(application.getString(R.string.pairing_reply_label))
            .build()
        val intent = Intent(application, PairingService::class.java).setAction(PairingService.ACTION_REPLY)
        // Mutable, so the system can add the typed text to it.
        val pending = PendingIntent.getService(
            application,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        return NotificationCompat.Action.Builder(
            R.drawable.ic_notification,
            application.getString(R.string.pairing_reply_action),
            pending,
        ).addRemoteInput(input).setAllowGeneratedReplies(false).build()
    }

    private fun openApp(): PendingIntent? {
        val launch = application.packageManager.getLaunchIntentForPackage(application.packageName) ?: return null
        return PendingIntent.getActivity(application, 0, launch, PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        const val CHANNEL_ID = "pairing"
        const val ONGOING_ID = 1
        const val OUTCOME_ID = 2
        const val KEY_CODE = "code"

        /** What the user typed into the reply field, if [intent] carries a reply. */
        fun codeFrom(intent: Intent): String? =
            RemoteInput.getResultsFromIntent(intent)?.getCharSequence(KEY_CODE)?.toString()
    }
}
