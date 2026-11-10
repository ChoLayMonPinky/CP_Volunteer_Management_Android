package dev.tintwym.data.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dev.tintwym.data.repo.ApiResult
import dev.tintwym.data.repo.EventRepository
import dev.tintwym.notify.LocalNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

sealed class CheckInOutcome {
  data object Success : CheckInOutcome()
  data class Queued(val message: String) : CheckInOutcome()
  data class Failed(val message: String) : CheckInOutcome()
}

class CheckInCoordinator(
  context: Context,
  private val events: EventRepository,
  private val queue: OfflineCheckInQueue,
  private val notifier: LocalNotifier,
  private val scope: CoroutineScope,
) {
  private val appContext = context.applicationContext
  private val _pendingCount = MutableStateFlow(0)
  val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()
  private val _flushing = MutableStateFlow(false)
  val flushing: StateFlow<Boolean> = _flushing.asStateFlow()
  private val _syncedEventIds = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val syncedEventIds: SharedFlow<String> = _syncedEventIds.asSharedFlow()
  private val flushMutex = Mutex()
  private var connectivityFlushJob: Job? = null
  private var needsReflush = false

  init {
    scope.launch {
      queue.pendingFlow.collect { _pendingCount.value = it.size }
    }
    registerConnectivity()
    scope.launch { flush() }
  }

  suspend fun selfCheckIn(
    eventId: String,
    code: String,
    eventName: String?,
  ): CheckInOutcome {
    val trimmed = code.trim()
    if (trimmed.isBlank()) return CheckInOutcome.Failed("Enter or scan a check-in code.")
    return when (val res = events.selfCheckIn(eventId, trimmed)) {
      is ApiResult.Ok -> {
        queue.remove(eventId)
        CheckInOutcome.Success
      }
      is ApiResult.Err -> {
        if (isAlreadyCheckedIn(res.message)) {
          queue.remove(eventId)
          CheckInOutcome.Success
        } else if (isTransientFailure(res)) {
          // Only transient/network failures queue — never permanent 4xx when VALIDATED lags.
          queue.enqueue(PendingCheckIn(eventId, eventName, trimmed))
          CheckInOutcome.Queued("You’re offline — check-in saved and will sync when you’re back online.")
        } else {
          CheckInOutcome.Failed(res.message)
        }
      }
    }
  }

  fun requestFlush() {
    scope.launch { flush() }
  }

  suspend fun flush() {
    if (!flushMutex.tryLock()) {
      needsReflush = true
      return
    }
    try {
      _flushing.value = true
      do {
        needsReflush = false
        if (!isOnline()) break
        val pending = queue.pending()
        if (pending.isEmpty()) break
        var keptTransient = false
        for (item in pending) {
          when (val res = events.selfCheckIn(item.eventId, item.code)) {
            is ApiResult.Ok -> {
              queue.remove(item.eventId)
              notifier.notifyCheckInSynced(item.eventName ?: "Event", item.eventId)
              _syncedEventIds.tryEmit(item.eventId)
            }
            is ApiResult.Err -> {
              when {
                isAlreadyCheckedIn(res.message) -> {
                  queue.remove(item.eventId)
                  notifier.notifyCheckInSynced(item.eventName ?: "Event", item.eventId)
                  _syncedEventIds.tryEmit(item.eventId)
                }
                isTransientFailure(res) -> {
                  keptTransient = true
                }
                else -> {
                  queue.remove(item.eventId)
                  notifier.notifyMessage(
                    title = "Check-in couldn’t sync",
                    body = "${item.eventName ?: "Event"}: ${res.message}",
                    eventId = item.eventId,
                  )
                }
              }
            }
          }
          delay(150)
        }
        if (keptTransient && isOnline()) {
          delay(800)
          needsReflush = true
        }
      } while (needsReflush && queue.pending().isNotEmpty())
    } finally {
      _flushing.value = false
      flushMutex.unlock()
      if (needsReflush) {
        needsReflush = false
        scope.launch { flush() }
      }
    }
  }

  private fun isTransientFailure(res: ApiResult.Err): Boolean {
    val code = res.httpCode
    if (code != null && code >= 500) return true
    if (code == null) {
      // Likely IOException mapped to "Network error…"
      val msg = res.message
      return msg.contains("Network", ignoreCase = true) ||
        msg.contains("connection", ignoreCase = true) ||
        msg.contains("timeout", ignoreCase = true) ||
        msg.contains("Unable to resolve", ignoreCase = true)
    }
    val msg = res.message
    return msg.contains("Server error", ignoreCase = true)
  }

  private fun isAlreadyCheckedIn(message: String): Boolean {
    val m = message.lowercase()
    return m.contains("already checked") || m.contains("already check-in")
  }

  private fun isOnline(): Boolean {
    val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
      caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
  }

  private fun registerConnectivity() {
    val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val request = NetworkRequest.Builder()
      .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      .build()
    runCatching {
      cm.registerNetworkCallback(
        request,
        object : ConnectivityManager.NetworkCallback() {
          override fun onAvailable(network: Network) {
            connectivityFlushJob?.cancel()
            connectivityFlushJob = scope.launch(Dispatchers.IO) {
              delay(500)
              flush()
            }
          }

          override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
              connectivityFlushJob?.cancel()
              connectivityFlushJob = scope.launch(Dispatchers.IO) {
                delay(300)
                flush()
              }
            }
          }
        },
      )
    }
  }
}
