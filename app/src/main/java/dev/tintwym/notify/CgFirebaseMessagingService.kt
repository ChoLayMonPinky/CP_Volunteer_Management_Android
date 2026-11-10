package dev.tintwym.notify

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dev.tintwym.appGraph

class CgFirebaseMessagingService : FirebaseMessagingService() {
  @Deprecated("Deprecated in Firebase Messaging")
  override fun onNewToken(token: String) {
    runCatching {
      applicationContext.appGraph().pushTokens.onNewToken(token)
    }
  }

  override fun onMessageReceived(message: RemoteMessage) {
    val title = message.notification?.title
      ?: message.data["title"]
      ?: "CommonGround"
    val body = message.notification?.body
      ?: message.data["body"]
      ?: message.data["message"]
      ?: return
    val eventId = message.data["eventId"] ?: message.data["event_id"]
    runCatching {
      applicationContext.appGraph().notifier.notifyMessage(title, body, eventId)
    }
  }
}
