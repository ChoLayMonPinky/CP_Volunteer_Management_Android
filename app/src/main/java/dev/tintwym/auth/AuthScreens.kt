package dev.tintwym.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.tintwym.R
import dev.tintwym.appGraph
import dev.tintwym.data.repo.ApiResult
import dev.tintwym.ui.components.PrimaryAction
import dev.tintwym.ui.theme.SleekBorder
import dev.tintwym.ui.theme.SleekPrimary
import dev.tintwym.ui.theme.SleekSurface
import dev.tintwym.ui.theme.SleekTextPrimary
import dev.tintwym.ui.theme.SleekTextSecondary
import dev.tintwym.ui.theme.Space
import kotlinx.coroutines.launch

enum class AuthRoute { Login, Register, Recovery }

@Composable
fun AuthGate() {
  var route by remember { mutableStateOf(AuthRoute.Login) }
  when (route) {
    AuthRoute.Login -> LoginScreen(
      onRegister = { route = AuthRoute.Register },
      onForgot = { route = AuthRoute.Recovery },
    )
    AuthRoute.Register -> RegisterScreen(onBack = { route = AuthRoute.Login })
    AuthRoute.Recovery -> PasswordRecoveryScreen(onBack = { route = AuthRoute.Login })
  }
}

@Composable
private fun AuthShell(content: @Composable () -> Unit) {
  val wash = remember {
    Brush.verticalGradient(listOf(Color(0xFFD1FAE5), Color(0xFFF4F7F5), Color(0xFFF4F7F5)))
  }
  Box(Modifier.fillMaxSize().background(wash)) {
    Column(
      Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = Space.xl, vertical = 28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(Modifier.height(20.dp))
      Image(
        painter = painterResource(R.drawable.ic_brand_logo),
        contentDescription = "CommonGround",
        modifier = Modifier.size(64.dp),
      )
      Spacer(Modifier.height(14.dp))
      Text("CommonGround", style = MaterialTheme.typography.displaySmall, color = SleekTextPrimary)
      Spacer(Modifier.height(4.dp))
      Text(
        "Volunteer programmes, organised.",
        style = MaterialTheme.typography.bodyLarge,
        color = SleekTextSecondary,
      )
      Spacer(Modifier.height(28.dp))
      content()
    }
  }
}

@Composable
fun LoginScreen(onRegister: () -> Unit, onForgot: () -> Unit) {
  val graph = LocalContext.current.appGraph()
  val scope = rememberCoroutineScope()
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var error by remember { mutableStateOf<String?>(null) }
  var loading by remember { mutableStateOf(false) }

  fun submit() {
    if (loading) return
    scope.launch {
      loading = true
      error = null
      when (val res = graph.auth.login(email, password)) {
        is ApiResult.Ok -> Unit
        is ApiResult.Err -> error = res.message
      }
      loading = false
    }
  }

  AuthShell {
    AuthPanel {
      Text("Sign in", style = MaterialTheme.typography.headlineSmall, color = SleekTextPrimary)
      Spacer(Modifier.height(4.dp))
      Text(
        "Use your organisation account email and password.",
        style = MaterialTheme.typography.bodyMedium,
        color = SleekTextSecondary,
      )
      Spacer(Modifier.height(18.dp))
      AuthField(email, { email = it }, "Email", KeyboardType.Email)
      Spacer(Modifier.height(10.dp))
      AuthField(password, { password = it }, "Password", KeyboardType.Password, isPassword = true)
      if (error != null) {
        Spacer(Modifier.height(8.dp))
        Text(error!!, color = Color(0xFFB91C1C), style = MaterialTheme.typography.bodySmall)
      }
      Spacer(Modifier.height(16.dp))
      if (loading) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = SleekPrimary)
        }
      } else {
        PrimaryAction("Sign in", onClick = ::submit)
      }
      TextButton(onClick = onForgot, modifier = Modifier.align(Alignment.End)) {
        Text("Forgot password?", color = SleekPrimary)
      }
      Spacer(Modifier.height(6.dp))
      TextButton(onClick = onRegister) {
        Text("Create organisation account", color = SleekPrimary, fontWeight = FontWeight.SemiBold)
      }
    }
  }
}

@Composable
fun RegisterScreen(onBack: () -> Unit) {
  val graph = LocalContext.current.appGraph()
  val scope = rememberCoroutineScope()
  var name by remember { mutableStateOf("") }
  var org by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var error by remember { mutableStateOf<String?>(null) }
  var loading by remember { mutableStateOf(false) }

  AuthShell {
    AuthPanel {
      Text("Create organisation", style = MaterialTheme.typography.headlineSmall)
      Spacer(Modifier.height(6.dp))
      Text(
        "Signup creates an organiser account for a new organisation.",
        style = MaterialTheme.typography.bodyMedium,
        color = SleekTextSecondary,
      )
      Spacer(Modifier.height(16.dp))
      AuthField(name, { name = it }, "Full name")
      Spacer(Modifier.height(10.dp))
      AuthField(org, { org = it }, "Organisation name")
      Spacer(Modifier.height(10.dp))
      AuthField(email, { email = it }, "Email", KeyboardType.Email)
      Spacer(Modifier.height(10.dp))
      AuthField(password, { password = it }, "Password (min 8)", KeyboardType.Password, isPassword = true)
      if (error != null) {
        Spacer(Modifier.height(8.dp))
        Text(error!!, color = Color(0xFFB91C1C), style = MaterialTheme.typography.bodySmall)
      }
      Spacer(Modifier.height(16.dp))
      if (loading) {
        CircularProgressIndicator(color = SleekPrimary, modifier = Modifier.align(Alignment.CenterHorizontally))
      } else {
        PrimaryAction("Create account") {
          if (loading) return@PrimaryAction
          scope.launch {
            loading = true
            error = null
            when (val res = graph.auth.signup(name, email, password, org)) {
              is ApiResult.Ok -> Unit
              is ApiResult.Err -> error = res.message
            }
            loading = false
          }
        }
      }
      TextButton(onClick = onBack) { Text("Back to sign in", color = SleekPrimary) }
    }
  }
}

@Composable
fun PasswordRecoveryScreen(onBack: () -> Unit) {
  val graph = LocalContext.current.appGraph()
  val scope = rememberCoroutineScope()
  var email by remember { mutableStateOf("") }
  var message by remember { mutableStateOf<String?>(null) }
  var error by remember { mutableStateOf<String?>(null) }
  var loading by remember { mutableStateOf(false) }

  AuthShell {
    AuthPanel {
      Text("Reset password", style = MaterialTheme.typography.headlineSmall)
      Spacer(Modifier.height(6.dp))
      Text(
        "We’ll email a reset link if the account exists.",
        style = MaterialTheme.typography.bodyMedium,
        color = SleekTextSecondary,
      )
      Spacer(Modifier.height(16.dp))
      AuthField(email, { email = it }, "Email", KeyboardType.Email)
      if (error != null) {
        Spacer(Modifier.height(8.dp))
        Text(error!!, color = Color(0xFFB91C1C), style = MaterialTheme.typography.bodySmall)
      }
      if (message != null) {
        Spacer(Modifier.height(8.dp))
        Text(message!!, color = SleekPrimary, style = MaterialTheme.typography.bodySmall)
      }
      Spacer(Modifier.height(16.dp))
      PrimaryAction(if (loading) "Sending…" else "Send reset link") {
        if (loading) return@PrimaryAction
        scope.launch {
          loading = true
          error = null
          message = null
          when (val res = graph.auth.forgotPassword(email)) {
            is ApiResult.Ok -> message = res.value
            is ApiResult.Err -> error = res.message
          }
          loading = false
        }
      }
      TextButton(onClick = onBack) { Text("Back to sign in", color = SleekPrimary) }
    }
  }
}

@Composable
private fun AuthPanel(content: @Composable ColumnScope.() -> Unit) {
  Column(
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(SleekSurface)
      .border(1.dp, SleekBorder, RoundedCornerShape(20.dp))
      .padding(Space.xl),
    content = content,
  )
}

@Composable
private fun AuthField(
  value: String,
  onChange: (String) -> Unit,
  label: String,
  keyboardType: KeyboardType = KeyboardType.Text,
  isPassword: Boolean = false,
) {
  OutlinedTextField(
    value = value,
    onValueChange = onChange,
    label = { Text(label) },
    singleLine = true,
    modifier = Modifier.fillMaxWidth(),
    visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    shape = RoundedCornerShape(Space.radiusMd),
    colors = OutlinedTextFieldDefaults.colors(
      focusedBorderColor = SleekPrimary,
      unfocusedBorderColor = SleekBorder,
      focusedContainerColor = Color.Transparent,
      unfocusedContainerColor = Color.Transparent,
    ),
  )
}
