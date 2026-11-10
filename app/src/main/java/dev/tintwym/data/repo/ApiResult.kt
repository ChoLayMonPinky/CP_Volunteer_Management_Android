package dev.tintwym.data.repo

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

sealed class ApiResult<out T> {
  data class Ok<T>(val value: T) : ApiResult<T>()
  data class Err(
    val message: String,
    val unauthorized: Boolean = false,
    val httpCode: Int? = null,
  ) : ApiResult<Nothing>()
}

object SessionEvents {
  private val _unauthorized = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
  val unauthorized: SharedFlow<Unit> = _unauthorized.asSharedFlow()

  fun emitUnauthorized() {
    _unauthorized.tryEmit(Unit)
  }
}

suspend fun <T> apiCall(block: suspend () -> T): ApiResult<T> = try {
  ApiResult.Ok(block())
} catch (e: HttpException) {
  val body = e.response()?.errorBody()?.string().orEmpty()
  val parsed = runCatching {
    JSONObject(body).optString("message").ifBlank {
      JSONObject(body).optString("error")
    }
  }.getOrDefault("")
  val unauthorized = e.code() == 401
  if (unauthorized) SessionEvents.emitUnauthorized()
  val msg = when {
    parsed.isNotBlank() -> parsed
    unauthorized -> "Session expired. Please sign in again."
    e.code() == 403 -> "You don’t have permission for that."
    e.code() in 400..499 -> "Request failed (${e.code()})."
    else -> "Server error (${e.code()})."
  }
  ApiResult.Err(msg.take(200), unauthorized = unauthorized, httpCode = e.code())
} catch (_: IOException) {
  ApiResult.Err("Network error. Check your connection and retry.")
} catch (e: Exception) {
  ApiResult.Err(e.message ?: "Something went wrong.")
}
