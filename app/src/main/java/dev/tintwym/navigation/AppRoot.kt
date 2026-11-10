package dev.tintwym.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.tintwym.appGraph
import dev.tintwym.auth.AuthGate
import dev.tintwym.data.repo.SessionEvents
import dev.tintwym.data.session.toPersona
import dev.tintwym.ui.theme.SleekPrimary
import kotlinx.coroutines.launch

@Composable
fun AppRoot(
  deepLinkEventId: String? = null,
  onDeepLinkConsumed: () -> Unit = {},
) {
  val graph = LocalContext.current.appGraph()
  val session by graph.sessionStore.sessionFlow.collectAsState(initial = null)
  var booting by remember { mutableStateOf(true) }
  val scope = rememberCoroutineScope()

  LaunchedEffect(Unit) {
    graph.auth.restoreToken()
    booting = false
  }

  LaunchedEffect(session?.accessToken) {
    if (session != null) {
      graph.pushTokens.registerAfterAuth()
    }
  }

  LaunchedEffect(Unit) {
    SessionEvents.unauthorized.collect {
      runCatching { graph.pushTokens.unregisterBeforeLogout() }
      graph.auth.logout()
    }
  }

  when {
    booting -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator(color = SleekPrimary)
    }
    session == null -> AuthGate()
    else -> RoleShell(
      persona = session!!.toPersona(),
      onLogout = {
        scope.launch {
          runCatching { graph.pushTokens.unregisterBeforeLogout() }
          graph.auth.logout()
        }
      },
      deepLinkEventId = deepLinkEventId,
      onDeepLinkConsumed = onDeepLinkConsumed,
    )
  }
}
