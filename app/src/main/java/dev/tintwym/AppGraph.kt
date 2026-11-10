package dev.tintwym

import android.content.Context
import dev.tintwym.data.offline.CheckInCoordinator
import dev.tintwym.data.offline.OfflineCheckInQueue
import dev.tintwym.data.repo.AuthRepository
import dev.tintwym.data.repo.EventRepository
import dev.tintwym.data.repo.MeRepository
import dev.tintwym.data.repo.MemberRepository
import dev.tintwym.data.repo.WorkRepository
import dev.tintwym.data.session.SessionStore
import dev.tintwym.notify.LocalNotifier
import dev.tintwym.notify.PushTokenRegistrar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppGraph(context: Context) {
  private val appContext = context.applicationContext
  private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  val sessionStore = SessionStore(appContext)
  val auth = AuthRepository(sessionStore)
  val events = EventRepository()
  val me = MeRepository()
  val members = MemberRepository()
  val work = WorkRepository()
  val notifier = LocalNotifier(appContext)
  val pushTokens = PushTokenRegistrar(appContext, sessionStore, me)
  val offlineQueue = OfflineCheckInQueue(appContext)
  val checkIns = CheckInCoordinator(
    context = appContext,
    events = events,
    queue = offlineQueue,
    notifier = notifier,
    scope = appScope,
  )
}

fun Context.appGraph(): AppGraph = (applicationContext as CommonGroundApp).graph
