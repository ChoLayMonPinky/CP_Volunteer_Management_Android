package dev.tintwym.data.remote

/** Moshi DTOs — dates/times as ISO strings matching Spring JSON. */

data class LoginRequest(val email: String, val password: String)

data class SignupRequest(
  val fullName: String,
  val email: String,
  val password: String,
  val organizationName: String,
)

data class ForgotPasswordRequest(val email: String)

data class MessageOnlyResponse(val message: String? = null)

data class AuthUserDto(
  val id: String,
  val email: String,
  val fullName: String,
  val role: String,
  val organizationId: String? = null,
  val organizationName: String? = null,
  val phone: String? = null,
)

data class AuthResponseDto(
  val accessToken: String,
  val tokenType: String? = null,
  val user: AuthUserDto,
)

data class EventDto(
  val id: String,
  val name: String,
  val description: String? = null,
  val category: String? = null,
  val venue: String? = null,
  val address: String? = null,
  val remote: Boolean = false,
  val meetingLink: String? = null,
  val date: String,
  val startTime: String,
  val endTime: String,
  val durationHours: Double = 0.0,
  val capacity: Int = 0,
  val registeredCount: Long = 0,
  val waitlistCount: Long = 0,
  val status: String = "OPEN",
  val imageUrl: String? = null,
  val checkInCode: String? = null,
  val myRegistrationStatus: String? = null,
  val createdAt: String? = null,
)

data class EventCreateRequest(
  val name: String,
  val description: String? = null,
  val category: String? = null,
  val venue: String? = null,
  val address: String? = null,
  val remote: Boolean = false,
  val meetingLink: String? = null,
  val date: String,
  val startTime: String,
  val endTime: String,
  val capacity: Int,
  val status: String? = "OPEN",
  val imageUrl: String? = null,
)

data class RegistrationDto(
  val id: String,
  val eventId: String,
  val eventName: String? = null,
  val eventDate: String? = null,
  val startTime: String? = null,
  val endTime: String? = null,
  val venue: String? = null,
  val userId: String,
  val userName: String? = null,
  val userEmail: String? = null,
  val status: String,
  val checkedInAt: String? = null,
  val checkedOutAt: String? = null,
  val hours: Double? = null,
  val notes: String? = null,
)

data class SelfCheckInRequest(val code: String)

data class MemberDto(
  val id: String,
  val email: String,
  val fullName: String,
  val phone: String? = null,
  val role: String,
  val active: Boolean = true,
  val totalHours: Double? = null,
  val joinedAt: String? = null,
)

data class OrganizerSummaryDto(
  val totalApprovedHours: Double? = null,
  val activeVolunteers: Long = 0,
  val activeLeaders: Long = 0,
  val upcomingEvents: Long = 0,
  val completedEvents: Long = 0,
  val pendingHourApprovals: Long = 0,
  val openIncidents: Long = 0,
  val openTasks: Long = 0,
)

data class BadgeDto(
  val hours: Int = 0,
  val title: String? = null,
  val tier: String? = null,
  val earned: Boolean = false,
  val earnedDate: String? = null,
  val progress: Double? = null,
)

data class MySummaryDto(
  val totalHours: Double? = null,
  val pendingHourLogs: Long = 0,
  val upcomingRegistrations: Long = 0,
  val completedEvents: Long = 0,
  val openTasks: Long = 0,
  val unreadNotifications: Long = 0,
  val currentBadge: BadgeDto? = null,
  val nextBadge: BadgeDto? = null,
)

data class NotificationDto(
  val id: String,
  val type: String? = null,
  val title: String,
  val body: String? = null,
  val link: String? = null,
  val read: Boolean = false,
  val createdAt: String? = null,
)

data class NotificationListDto(
  val unreadCount: Long = 0,
  val items: List<NotificationDto> = emptyList(),
)

data class HourLogDto(
  val id: String,
  val userId: String? = null,
  val userName: String? = null,
  val eventId: String? = null,
  val eventName: String? = null,
  val workDate: String? = null,
  val hours: Double? = null,
  val description: String? = null,
  val status: String? = null,
  val source: String? = null,
  val reviewedByName: String? = null,
  val reviewedAt: String? = null,
  val reviewNote: String? = null,
  val createdAt: String? = null,
)

data class MessageDto(
  val id: String,
  val channel: String? = null,
  val senderId: String? = null,
  val senderName: String? = null,
  val senderRole: String? = null,
  val content: String,
  val urgent: Boolean = false,
  val createdAt: String? = null,
)

data class MessagePostRequest(val content: String, val urgent: Boolean = false)

data class IncidentCreateRequest(
  val title: String,
  val description: String,
  val severity: String,
  val eventId: String? = null,
)

data class IncidentDto(
  val id: String,
  val title: String,
  val description: String? = null,
  val severity: String? = null,
  val status: String? = null,
  val eventId: String? = null,
  val eventName: String? = null,
  val reportedByName: String? = null,
  val createdAt: String? = null,
)

data class HourReviewRequest(
  val approve: Boolean,
  val note: String? = null,
  val hours: Double? = null,
)

data class TaskDto(
  val id: String,
  val title: String,
  val description: String? = null,
  val priority: String? = null,
  val status: String? = null,
  val assigneeId: String? = null,
  val assigneeName: String? = null,
  val dueDate: String? = null,
  val eventId: String? = null,
  val eventName: String? = null,
  val createdByName: String? = null,
  val createdAt: String? = null,
)

data class TaskStatusRequest(val status: String)

data class DeviceTokenRequest(val platform: String, val token: String)

data class DeviceTokenResponse(
  val id: String? = null,
  val platform: String? = null,
  val updatedAt: String? = null,
)

data class ChatMessageDto(val role: String, val content: String)

data class ChatRequestDto(
  val message: String,
  val history: List<ChatMessageDto>? = null,
)

data class ChatResponseDto(val reply: String)
