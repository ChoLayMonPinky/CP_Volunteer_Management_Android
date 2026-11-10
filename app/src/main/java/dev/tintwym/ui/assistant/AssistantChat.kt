package dev.tintwym.ui.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.tintwym.appGraph
import dev.tintwym.data.remote.ChatMessageDto
import dev.tintwym.data.repo.ApiResult
import dev.tintwym.ui.theme.SleekBackground
import dev.tintwym.ui.theme.SleekBorder
import dev.tintwym.ui.theme.SleekPrimary
import dev.tintwym.ui.theme.SleekPrimaryStrong
import dev.tintwym.ui.theme.SleekSurface
import dev.tintwym.ui.theme.SleekSurfaceMuted
import dev.tintwym.ui.theme.SleekTextPrimary
import dev.tintwym.ui.theme.SleekTextSecondary
import kotlinx.coroutines.launch

private const val WELCOME =
  "Hi — I’m your CommonGround assistant. Ask about your hours, upcoming events, tasks, or how to use the app."

private val SUGGESTIONS = listOf(
  "How many hours do I have?",
  "What’s my next event?",
  "Any open tasks?",
)

private data class ChatTurn(val id: String, val role: String, val content: String)

@Composable
fun AssistantChatHost(modifier: Modifier = Modifier) {
  var open by remember { mutableStateOf(false) }

  Box(modifier = modifier.fillMaxSize()) {
    FloatingActionButton(
      onClick = { open = true },
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .navigationBarsPadding()
        .padding(end = 16.dp, bottom = 88.dp),
      containerColor = SleekPrimary,
      contentColor = Color.White,
      shape = CircleShape,
    ) {
      Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Open assistant")
    }

    if (open) {
      Dialog(
        onDismissRequest = { open = false },
        properties = DialogProperties(usePlatformDefaultWidth = false),
      ) {
        Surface(
          modifier = Modifier
            .fillMaxWidth(0.94f)
            .fillMaxHeight(0.78f)
            .imePadding(),
          shape = RoundedCornerShape(20.dp),
          color = SleekSurface,
          tonalElevation = 0.dp,
          shadowElevation = 8.dp,
        ) {
          AssistantChatPanel(onClose = { open = false })
        }
      }
    }
  }
}

@Composable
private fun AssistantChatPanel(onClose: () -> Unit) {
  val graph = LocalContext.current.appGraph()
  val scope = rememberCoroutineScope()
  val focus = LocalFocusManager.current
  val welcomeId = remember { "welcome" }
  val messages = remember {
    mutableStateListOf(ChatTurn(welcomeId, "assistant", WELCOME))
  }
  var draft by remember { mutableStateOf("") }
  var sending by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }
  val listState = rememberLazyListState()

  LaunchedEffect(messages.size, sending) {
    val last = (messages.size - 1).coerceAtLeast(0)
    listState.animateScrollToItem(last)
  }

  val showSuggestions = !sending && messages.size <= 1 && messages.all { it.role == "assistant" }

  fun sendMessage(textRaw: String) {
    val text = textRaw.trim()
    if (text.isEmpty() || sending) return
    draft = ""
    error = null
    messages.add(ChatTurn("u-${System.currentTimeMillis()}", "user", text))
    sending = true
    scope.launch {
      val history = messages
        .filter { it.id != welcomeId }
        .dropLast(1)
        .takeLast(12)
        .map { ChatMessageDto(role = it.role, content = it.content) }
      when (val result = graph.me.chat(text, history)) {
        is ApiResult.Ok -> {
          messages.add(ChatTurn("a-${System.currentTimeMillis()}", "assistant", result.value.reply))
        }
        is ApiResult.Err -> {
          error = result.message
          messages.add(
            ChatTurn(
              "err-${System.currentTimeMillis()}",
              "assistant",
              "Sorry — I couldn’t answer that just now. Please try again in a moment.",
            ),
          )
        }
      }
      sending = false
    }
  }

  Column(Modifier.fillMaxSize()) {
    Row(
      Modifier
        .fillMaxWidth()
        .background(SleekPrimaryStrong)
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Color.White)
      Column(Modifier.weight(1f).padding(start = 10.dp)) {
        Text(
          "CommonGround Assistant",
          color = Color.White,
          style = MaterialTheme.typography.titleSmall,
        )
        Text(
          "Hours · events · tasks",
          color = Color.White.copy(alpha = 0.85f),
          fontSize = 11.sp,
        )
      }
      IconButton(onClick = onClose) {
        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color.White)
      }
    }

    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .background(SleekBackground)
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      items(messages, key = { it.id }) { msg ->
        MessageBubble(msg)
      }
      if (showSuggestions) {
        item {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SUGGESTIONS.forEach { prompt ->
              TextButton(
                onClick = { sendMessage(prompt) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                  .clip(RoundedCornerShape(999.dp))
                  .border(1.dp, SleekBorder, RoundedCornerShape(999.dp))
                  .background(SleekSurface),
              ) {
                Text(prompt, color = SleekTextSecondary, fontSize = 12.sp)
              }
            }
          }
        }
      }
      if (sending) {
        item {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
              .clip(RoundedCornerShape(16.dp))
              .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
              .background(SleekSurface)
              .padding(horizontal = 12.dp, vertical = 8.dp),
          ) {
            CircularProgressIndicator(Modifier.size(14.dp), color = SleekPrimary, strokeWidth = 2.dp)
            Text("Thinking…", color = SleekTextSecondary, fontSize = 12.sp)
          }
        }
      }
    }

    error?.let {
      Text(
        it,
        color = SleekPrimaryStrong,
        fontSize = 12.sp,
        modifier = Modifier
          .fillMaxWidth()
          .background(SleekSurfaceMuted)
          .padding(horizontal = 12.dp, vertical = 8.dp),
      )
    }

    Row(
      Modifier
        .fillMaxWidth()
        .background(SleekSurface)
        .padding(12.dp),
      verticalAlignment = Alignment.Bottom,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      OutlinedTextField(
        value = draft,
        onValueChange = { draft = it },
        modifier = Modifier.weight(1f),
        placeholder = { Text("Ask about hours, events…", color = SleekTextSecondary) },
        enabled = !sending,
        maxLines = 4,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = {
          sendMessage(draft)
          focus.clearFocus()
        }),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = SleekPrimary,
          unfocusedBorderColor = SleekBorder,
          focusedContainerColor = SleekSurfaceMuted,
          unfocusedContainerColor = SleekSurfaceMuted,
          cursorColor = SleekPrimary,
          focusedTextColor = SleekTextPrimary,
          unfocusedTextColor = SleekTextPrimary,
        ),
        shape = RoundedCornerShape(14.dp),
      )
      IconButton(
        onClick = {
          sendMessage(draft)
          focus.clearFocus()
        },
        enabled = !sending && draft.isNotBlank(),
        modifier = Modifier
          .size(44.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(if (!sending && draft.isNotBlank()) SleekPrimary else SleekBorder),
      ) {
        if (sending) {
          CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
          Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Send", tint = Color.White)
        }
      }
    }
  }
}

@Composable
private fun MessageBubble(msg: ChatTurn) {
  val isUser = msg.role == "user"
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
  ) {
    Text(
      msg.content,
      color = if (isUser) Color.White else SleekTextPrimary,
      fontSize = 14.sp,
      modifier = Modifier
        .widthIn(max = 280.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(if (isUser) SleekPrimary else SleekSurface)
        .then(if (isUser) Modifier else Modifier.border(1.dp, SleekBorder, RoundedCornerShape(16.dp)))
        .padding(horizontal = 12.dp, vertical = 10.dp),
    )
  }
}
