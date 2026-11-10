package dev.tintwym

import android.app.Application
import dev.tintwym.data.remote.BackendClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CommonGroundApp : Application() {
  lateinit var graph: AppGraph
    private set

  private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onCreate() {
    super.onCreate()
    graph = AppGraph(this)
    // Wake Render while splash/auth UI loads (free tier cold start ~45–60s).
    BackendClient.warmup()
    appScope.launch {
      graph.auth.restoreToken()
      if (graph.sessionStore.current() != null) {
        graph.pushTokens.registerAfterAuth()
      }
    }
  }
}
