package dev.tintwym.data.repo

import dev.tintwym.data.remote.AuthResponseDto
import dev.tintwym.data.remote.AuthUserDto
import dev.tintwym.data.remote.BackendClient
import dev.tintwym.data.remote.ChatMessageDto
import dev.tintwym.data.remote.ChatRequestDto
import dev.tintwym.data.remote.ChatResponseDto
import dev.tintwym.data.remote.DeviceTokenRequest
import dev.tintwym.data.remote.DeviceTokenResponse
import dev.tintwym.data.remote.EventCreateRequest
import dev.tintwym.data.remote.EventDto
import dev.tintwym.data.remote.ForgotPasswordRequest
import dev.tintwym.data.remote.HourLogDto
import dev.tintwym.data.remote.HourReviewRequest
import dev.tintwym.data.remote.IncidentCreateRequest
import dev.tintwym.data.remote.IncidentDto
import dev.tintwym.data.remote.LoginRequest
import dev.tintwym.data.remote.MemberDto
import dev.tintwym.data.remote.MessageDto
import dev.tintwym.data.remote.MessagePostRequest
import dev.tintwym.data.remote.MySummaryDto
import dev.tintwym.data.remote.NotificationListDto
import dev.tintwym.data.remote.OrganizerSummaryDto
import dev.tintwym.data.remote.RegistrationDto
import dev.tintwym.data.remote.SelfCheckInRequest
import dev.tintwym.data.remote.SignupRequest
import dev.tintwym.data.remote.TaskDto
import dev.tintwym.data.remote.TaskStatusRequest
import dev.tintwym.data.session.SessionStore
import dev.tintwym.data.session.SessionUser

class AuthRepository(private val sessionStore: SessionStore) {
  private val api get() = BackendClient.api

  suspend fun restoreToken() {
    BackendClient.setToken(sessionStore.current()?.accessToken)
  }

  suspend fun login(email: String, password: String): ApiResult<SessionUser> = apiCall {
    val res = api.login(LoginRequest(email.trim(), password))
    persist(res)
  }

  suspend fun signup(
    fullName: String,
    email: String,
    password: String,
    organizationName: String,
  ): ApiResult<SessionUser> = apiCall {
    val res = api.signup(
      SignupRequest(
        fullName = fullName.trim(),
        email = email.trim(),
        password = password,
        organizationName = organizationName.trim(),
      ),
    )
    persist(res)
  }

  suspend fun forgotPassword(email: String): ApiResult<String> = apiCall {
    api.forgotPassword(ForgotPasswordRequest(email.trim())).message
      ?: "If an account exists, a reset link was sent."
  }

  suspend fun refreshMe(): ApiResult<SessionUser> = apiCall {
    val current = sessionStore.current() ?: error("Not signed in")
    val me = api.me()
    val user = me.toSession(current.accessToken)
    sessionStore.save(user)
    user
  }

  suspend fun logout() {
    // Caller should unregister push token first while JWT is still valid.
    sessionStore.clear()
    BackendClient.setToken(null)
  }

  private suspend fun persist(res: AuthResponseDto): SessionUser {
    val user = res.user.toSession(res.accessToken)
    BackendClient.setToken(user.accessToken)
    sessionStore.save(user)
    return user
  }
}

private fun AuthUserDto.toSession(token: String) = SessionUser(
  id = id,
  email = email,
  fullName = fullName,
  role = role,
  organizationId = organizationId,
  organizationName = organizationName,
  phone = phone,
  accessToken = token,
)

class EventRepository {
  private val api get() = BackendClient.api

  suspend fun list(upcoming: Boolean = false): ApiResult<List<EventDto>> =
    apiCall { api.listEvents(upcoming) }

  suspend fun get(id: String): ApiResult<EventDto> = apiCall { api.getEvent(id) }

  suspend fun create(body: EventCreateRequest): ApiResult<EventDto> =
    apiCall { api.createEvent(body) }

  suspend fun regenerateCode(id: String): ApiResult<EventDto> =
    apiCall { api.regenerateCheckInCode(id) }

  suspend fun register(id: String): ApiResult<RegistrationDto> = apiCall { api.register(id) }

  suspend fun cancel(id: String): ApiResult<Unit> = apiCall { api.cancelRegistration(id) }

  suspend fun selfCheckIn(id: String, code: String): ApiResult<RegistrationDto> =
    apiCall { api.selfCheckIn(id, SelfCheckInRequest(code.trim())) }

  suspend fun selfCheckOut(id: String): ApiResult<RegistrationDto> =
    apiCall { api.selfCheckOut(id) }

  suspend fun registrations(id: String): ApiResult<List<RegistrationDto>> =
    apiCall { api.registrations(id) }

  suspend fun staffCheckIn(eventId: String, regId: String): ApiResult<RegistrationDto> =
    apiCall { api.staffCheckIn(eventId, regId) }

  suspend fun staffCheckOut(eventId: String, regId: String): ApiResult<RegistrationDto> =
    apiCall { api.staffCheckOut(eventId, regId) }

  suspend fun noShow(eventId: String, regId: String): ApiResult<RegistrationDto> =
    apiCall { api.noShow(eventId, regId) }
}

class MeRepository {
  private val api get() = BackendClient.api

  suspend fun summary(): ApiResult<MySummaryDto> = apiCall { api.mySummary() }

  suspend fun registrations(): ApiResult<List<RegistrationDto>> =
    apiCall { api.myRegistrations() }

  suspend fun notifications(): ApiResult<NotificationListDto> =
    apiCall { api.notifications() }

  suspend fun markRead(id: String): ApiResult<Unit> =
    apiCall { api.markNotificationRead(id); Unit }

  suspend fun markAllRead(): ApiResult<Unit> = apiCall { api.markAllNotificationsRead() }

  suspend fun hours(): ApiResult<List<HourLogDto>> = apiCall { api.myHours() }

  suspend fun tasks(): ApiResult<List<TaskDto>> = apiCall { api.myTasks() }

  suspend fun registerDevice(platform: String, token: String): ApiResult<DeviceTokenResponse> =
    apiCall { api.registerDevice(DeviceTokenRequest(platform, token)) }

  suspend fun unregisterDevice(token: String): ApiResult<Unit> =
    apiCall { api.unregisterDevice(token); Unit }

  suspend fun chat(message: String, history: List<ChatMessageDto>): ApiResult<ChatResponseDto> =
    apiCall { api.chat(ChatRequestDto(message = message, history = history)) }
}

class MemberRepository {
  private val api get() = BackendClient.api

  suspend fun list(): ApiResult<List<MemberDto>> = apiCall { api.members() }

  suspend fun dashboard(): ApiResult<OrganizerSummaryDto> =
    apiCall { api.organizationDashboard() }

  suspend fun pendingHours(): ApiResult<List<HourLogDto>> =
    apiCall { api.listHours("PENDING") }

  suspend fun reviewHour(id: String, approve: Boolean): ApiResult<HourLogDto> =
    apiCall { api.reviewHour(id, HourReviewRequest(approve = approve)) }
}

class WorkRepository {
  private val api get() = BackendClient.api

  suspend fun messages(channel: String): ApiResult<List<MessageDto>> =
    apiCall { api.messages(channel) }

  suspend fun postMessage(channel: String, content: String, urgent: Boolean = false): ApiResult<MessageDto> =
    apiCall { api.postMessage(channel, MessagePostRequest(content, urgent)) }

  suspend fun createIncident(
    title: String,
    description: String,
    severity: String,
    eventId: String?,
  ): ApiResult<IncidentDto> = apiCall {
    api.createIncident(
      IncidentCreateRequest(title, description, severity, eventId),
    )
  }

  suspend fun incidents(): ApiResult<List<IncidentDto>> = apiCall { api.incidents() }

  suspend fun tasks(): ApiResult<List<TaskDto>> = apiCall { api.listTasks() }

  suspend fun setTaskStatus(id: String, status: String): ApiResult<TaskDto> =
    apiCall { api.setTaskStatus(id, TaskStatusRequest(status)) }
}
