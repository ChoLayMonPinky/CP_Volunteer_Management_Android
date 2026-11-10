package dev.tintwym.notify

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.messaging.FirebaseMessaging
import dev.tintwym.data.repo.ApiResult
import dev.tintwym.data.repo.MeRepository
import dev.tintwym.data.session.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val Context.pushTokenDataStore by preferencesDataStore("cg_push_token")

/**
 * Registers the FCM device token with `POST /api/me/devices` after login.
 * Safe to call when Firebase is misconfigured — failures are logged only.
 */
class PushTokenRegistrar(
  context: Context,
  private val sessionStore: SessionStore,
  private val me: MeRepository,
) {
  private val appContext = context.applicationContext
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val keyToken = stringPreferencesKey("fcm_token")

  fun registerAfterAuth() {
    scope.launch { registerCurrentToken() }
  }

  fun onNewToken(token: String) {
    scope.launch { upload(token) }
  }

  /** Best-effort unregister before session clear. */
  suspend fun unregisterBeforeLogout() {
    val token = cachedToken()
      ?: runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
      ?: return
    me.unregisterDevice(token)
    appContext.pushTokenDataStore.edit { it.remove(keyToken) }
  }

  fun unregisterBeforeLogoutAsync() {
    scope.launch {
      runCatching { unregisterBeforeLogout() }
        .onFailure { Log.w(TAG, "Device token unregister failed", it) }
    }
  }

  private suspend fun registerCurrentToken() {
    if (sessionStore.current() == null) return
    val token = runCatching {
      FirebaseMessaging.getInstance().token.await()
    }.onFailure { err ->
      Log.w(TAG, "FCM token unavailable (check google-services.json)", err)
    }.getOrNull() ?: return
    upload(token)
  }

  private suspend fun upload(token: String) {
    if (token.isBlank()) return
    if (sessionStore.current() == null) return
    when (val result = me.registerDevice("ANDROID", token)) {
      is ApiResult.Ok -> {
        appContext.pushTokenDataStore.edit { it[keyToken] = token }
        Log.d(TAG, "Registered FCM device token")
      }
      is ApiResult.Err ->
        Log.w(TAG, "Device token registration failed: ${result.message}")
    }
  }

  private suspend fun cachedToken(): String? =
    appContext.pushTokenDataStore.data.map { it[keyToken] }.first()

  companion object {
    private const val TAG = "PushTokenRegistrar"
  }
}
