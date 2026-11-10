package dev.tintwym.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.tintwym.MainActivity
import dev.tintwym.R
import java.util.concurrent.atomic.AtomicInteger

class LocalNotifier(context: Context) {
  private val app = context.applicationContext

  init {
    ensureChannels()
  }

  fun notifyCheckInSynced(eventName: String, eventId: String? = null) {
    show(
      id = notificationId("sync", eventId ?: eventName),
      title = "Check-in synced",
      body = "You’re checked in for $eventName.",
      channel = CHANNEL_SYNC,
      eventId = eventId,
    )
  }

  fun notifyMessage(title: String, body: String, eventId: String? = null) {
    show(
      id = notificationId("msg", eventId) + nextSeq.getAndIncrement(),
      title = title,
      body = body,
      channel = CHANNEL_UPDATES,
      eventId = eventId,
    )
  }

  private fun show(
    id: Int,
    title: String,
    body: String,
    channel: String,
    eventId: String? = null,
  ) {
    if (!canPost()) return
    val intent = Intent(app, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      if (!eventId.isNullOrBlank()) putExtra(EXTRA_EVENT_ID, eventId)
    }
    val pending = PendingIntent.getActivity(
      app,
      id,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val notification = NotificationCompat.Builder(app, channel)
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setAutoCancel(true)
      .setContentIntent(pending)
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .build()
    NotificationManagerCompat.from(app).notify(id, notification)
  }

  private fun canPost(): Boolean {
    if (Build.VERSION.SDK_INT < 33) return true
    return ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) ==
      PackageManager.PERMISSION_GRANTED
  }

  private fun ensureChannels() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = app.getSystemService(NotificationManager::class.java) ?: return
    manager.createNotificationChannel(
      NotificationChannel(CHANNEL_SYNC, "Check-in sync", NotificationManager.IMPORTANCE_DEFAULT).apply {
        description = "Confirms offline check-ins after they sync"
      },
    )
    manager.createNotificationChannel(
      NotificationChannel(CHANNEL_UPDATES, "Event updates", NotificationManager.IMPORTANCE_DEFAULT).apply {
        description = "Messages and reminders about your events"
      },
    )
  }

  companion object {
    const val EXTRA_EVENT_ID = "deep_link_event_id"
    const val CHANNEL_SYNC = "cg_checkin_sync"
    const val CHANNEL_UPDATES = "cg_event_updates"
    private val nextSeq = AtomicInteger(1)

    private fun notificationId(prefix: String, key: String?): Int {
      val seed = "$prefix:${key.orEmpty()}"
      return seed.hashCode()
    }
  }
}
