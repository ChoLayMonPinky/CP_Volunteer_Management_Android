package dev.tintwym.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppPersona {
  ORGANISER,
  LEADER,
  VOLUNTEER,
}

data class TabDest(
  val route: String,
  val label: String,
  val icon: ImageVector,
)

object AppDestination {
  const val ORG_DASHBOARD = "org_dashboard"
  const val ORG_EVENTS = "org_events"
  const val ORG_VOLUNTEERS = "org_volunteers"
  const val ORG_REPORTS = "org_reports"
  const val ORG_PROFILE = "org_profile"

  const val LDR_HOME = "ldr_home"
  const val LDR_EVENTS = "ldr_events"
  const val LDR_TEAM = "ldr_team"
  const val LDR_MESSAGES = "ldr_messages"
  const val LDR_PROFILE = "ldr_profile"

  const val VOL_HOME = "vol_home"
  const val VOL_EXPLORE = "vol_explore"
  const val VOL_EVENTS = "vol_events"
  const val VOL_NOTIFICATIONS = "vol_notifications"
  const val VOL_PROFILE = "vol_profile"

  const val EVENT_DETAIL = "event_detail/{eventId}"
  fun eventDetail(eventId: String) = "event_detail/$eventId"
}

fun tabsFor(persona: AppPersona): List<TabDest> = when (persona) {
  AppPersona.ORGANISER -> listOf(
    TabDest(AppDestination.ORG_DASHBOARD, "Dashboard", Icons.Outlined.Dashboard),
    TabDest(AppDestination.ORG_EVENTS, "Events", Icons.Outlined.Event),
    TabDest(AppDestination.ORG_VOLUNTEERS, "Volunteers", Icons.Outlined.Groups),
    TabDest(AppDestination.ORG_REPORTS, "Reports", Icons.Outlined.Assessment),
    TabDest(AppDestination.ORG_PROFILE, "Profile", Icons.Outlined.Person),
  )
  AppPersona.LEADER -> listOf(
    TabDest(AppDestination.LDR_HOME, "Home", Icons.Outlined.Home),
    TabDest(AppDestination.LDR_EVENTS, "My Events", Icons.Outlined.Event),
    TabDest(AppDestination.LDR_TEAM, "My Team", Icons.Outlined.Groups),
    TabDest(AppDestination.LDR_MESSAGES, "Messages", Icons.Outlined.MailOutline),
    TabDest(AppDestination.LDR_PROFILE, "Profile", Icons.Outlined.Person),
  )
  AppPersona.VOLUNTEER -> listOf(
    TabDest(AppDestination.VOL_HOME, "Home", Icons.Outlined.Home),
    TabDest(AppDestination.VOL_EXPLORE, "Explore", Icons.Outlined.Explore),
    TabDest(AppDestination.VOL_EVENTS, "My Events", Icons.Outlined.Event),
    TabDest(AppDestination.VOL_NOTIFICATIONS, "Notifications", Icons.Outlined.Notifications),
    TabDest(AppDestination.VOL_PROFILE, "Profile", Icons.Outlined.Person),
  )
}
