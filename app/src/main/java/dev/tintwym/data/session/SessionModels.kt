package dev.tintwym.data.session

import dev.tintwym.navigation.AppPersona

data class SessionUser(
  val id: String,
  val email: String,
  val fullName: String,
  val role: String,
  val organizationId: String? = null,
  val organizationName: String? = null,
  val phone: String? = null,
  val accessToken: String,
)

fun SessionUser.toPersona(): AppPersona = when (role.uppercase()) {
  "ORGANIZER", "ORGANISER" -> AppPersona.ORGANISER
  "VOLUNTEER_LEADER" -> AppPersona.LEADER
  else -> AppPersona.VOLUNTEER
}
