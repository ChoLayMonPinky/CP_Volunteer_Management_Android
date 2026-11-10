package dev.tintwym.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface CommonGroundApi {
  @POST("api/auth/login")
  suspend fun login(@Body body: LoginRequest): AuthResponseDto

  @POST("api/auth/signup")
  suspend fun signup(@Body body: SignupRequest): AuthResponseDto

  @POST("api/auth/forgot-password")
  suspend fun forgotPassword(@Body body: ForgotPasswordRequest): MessageOnlyResponse

  @GET("api/auth/me")
  suspend fun me(): AuthUserDto

  @GET("api/events")
  suspend fun listEvents(@Query("upcoming") upcoming: Boolean = false): List<EventDto>

  @GET("api/events/{id}")
  suspend fun getEvent(@Path("id") id: String): EventDto

  @POST("api/events")
  suspend fun createEvent(@Body body: EventCreateRequest): EventDto

  @POST("api/events/{id}/check-in-code")
  suspend fun regenerateCheckInCode(@Path("id") id: String): EventDto

  @POST("api/events/{id}/register")
  suspend fun register(@Path("id") id: String): RegistrationDto

  @DELETE("api/events/{id}/register")
  suspend fun cancelRegistration(@Path("id") id: String)

  @POST("api/events/{id}/check-in")
  suspend fun selfCheckIn(@Path("id") id: String, @Body body: SelfCheckInRequest): RegistrationDto

  @POST("api/events/{id}/check-out")
  suspend fun selfCheckOut(@Path("id") id: String): RegistrationDto

  @GET("api/events/{id}/registrations")
  suspend fun registrations(@Path("id") id: String): List<RegistrationDto>

  @POST("api/events/{id}/registrations/{registrationId}/check-in")
  suspend fun staffCheckIn(
    @Path("id") id: String,
    @Path("registrationId") registrationId: String,
  ): RegistrationDto

  @POST("api/events/{id}/registrations/{registrationId}/check-out")
  suspend fun staffCheckOut(
    @Path("id") id: String,
    @Path("registrationId") registrationId: String,
  ): RegistrationDto

  @POST("api/events/{id}/registrations/{registrationId}/no-show")
  suspend fun noShow(
    @Path("id") id: String,
    @Path("registrationId") registrationId: String,
  ): RegistrationDto

  @GET("api/me/summary")
  suspend fun mySummary(): MySummaryDto

  @GET("api/me/registrations")
  suspend fun myRegistrations(): List<RegistrationDto>

  @GET("api/me/notifications")
  suspend fun notifications(@Query("limit") limit: Int = 50): NotificationListDto

  @POST("api/me/notifications/{id}/read")
  suspend fun markNotificationRead(@Path("id") id: String): NotificationDto

  @POST("api/me/notifications/read-all")
  suspend fun markAllNotificationsRead()

  @POST("api/me/devices")
  suspend fun registerDevice(@Body body: DeviceTokenRequest): DeviceTokenResponse

  @DELETE("api/me/devices")
  suspend fun unregisterDevice(@Query("token") token: String)

  @GET("api/hours/mine")
  suspend fun myHours(): List<HourLogDto>

  @GET("api/hours")
  suspend fun listHours(@Query("status") status: String? = null): List<HourLogDto>

  @POST("api/hours/{id}/review")
  suspend fun reviewHour(@Path("id") id: String, @Body body: HourReviewRequest): HourLogDto

  @GET("api/tasks")
  suspend fun listTasks(): List<TaskDto>

  @GET("api/me/tasks")
  suspend fun myTasks(): List<TaskDto>

  @PATCH("api/tasks/{id}/status")
  suspend fun setTaskStatus(@Path("id") id: String, @Body body: TaskStatusRequest): TaskDto

  @GET("api/members")
  suspend fun members(): List<MemberDto>

  @GET("api/dashboard/organization")
  suspend fun organizationDashboard(): OrganizerSummaryDto

  @GET("api/messages/{channel}")
  suspend fun messages(
    @Path("channel") channel: String,
    @Query("limit") limit: Int = 100,
  ): List<MessageDto>

  @POST("api/messages/{channel}")
  suspend fun postMessage(
    @Path("channel") channel: String,
    @Body body: MessagePostRequest,
  ): MessageDto

  @GET("api/incidents")
  suspend fun incidents(): List<IncidentDto>

  @POST("api/incidents")
  suspend fun createIncident(@Body body: IncidentCreateRequest): IncidentDto

  @POST("api/chat")
  suspend fun chat(@Body body: ChatRequestDto): ChatResponseDto
}
