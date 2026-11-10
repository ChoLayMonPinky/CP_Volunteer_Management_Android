package dev.tintwym

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.tintwym.navigation.AppRoot
import dev.tintwym.notify.LocalNotifier
import dev.tintwym.ui.theme.MyApplicationTheme
import dev.tintwym.ui.theme.SleekBackground

class MainActivity : ComponentActivity() {
  private var deepLinkEventId by mutableStateOf<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    deepLinkEventId = extractEventId(intent)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = SleekBackground) {
          AppRoot(
            deepLinkEventId = deepLinkEventId,
            onDeepLinkConsumed = { deepLinkEventId = null },
          )
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    deepLinkEventId = extractEventId(intent)
  }

  private fun extractEventId(intent: Intent?): String? {
    if (intent == null) return null
    intent.getStringExtra(LocalNotifier.EXTRA_EVENT_ID)?.takeIf { it.isNotBlank() }?.let { return it }
    val data = intent.data ?: return null
    if (data.scheme == "commonground" && data.host == "event") {
      return data.lastPathSegment?.takeIf { it.isNotBlank() }
    }
    return null
  }
}
