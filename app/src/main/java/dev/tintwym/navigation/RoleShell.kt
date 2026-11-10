package dev.tintwym.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.tintwym.ui.assistant.AssistantChatHost
import dev.tintwym.ui.leader.LeaderEventDetailScreen
import dev.tintwym.ui.leader.LeaderHomeScreen
import dev.tintwym.ui.leader.LeaderMessagesScreen
import dev.tintwym.ui.leader.LeaderMyEventsScreen
import dev.tintwym.ui.leader.LeaderMyTeamScreen
import dev.tintwym.ui.leader.LeaderProfileScreen
import dev.tintwym.ui.organiser.OrganiserDashboardScreen
import dev.tintwym.ui.organiser.OrganiserEventsScreen
import dev.tintwym.ui.organiser.OrganiserProfileScreen
import dev.tintwym.ui.organiser.OrganiserReportsScreen
import dev.tintwym.ui.organiser.OrganiserVolunteersScreen
import dev.tintwym.ui.theme.SleekBorder
import dev.tintwym.ui.theme.SleekPrimary
import dev.tintwym.ui.theme.SleekSurface
import dev.tintwym.ui.theme.SleekTextSecondary
import dev.tintwym.ui.volunteer.VolunteerEventDetailScreen
import dev.tintwym.ui.volunteer.VolunteerExploreScreen
import dev.tintwym.ui.volunteer.VolunteerHomeScreen
import dev.tintwym.ui.volunteer.VolunteerMyEventsScreen
import dev.tintwym.ui.volunteer.VolunteerNotificationsScreen
import dev.tintwym.ui.volunteer.VolunteerProfileScreen

@Composable
fun RoleShell(
  persona: AppPersona,
  onLogout: () -> Unit,
  deepLinkEventId: String? = null,
  onDeepLinkConsumed: () -> Unit = {},
) {
  val tabs = tabsFor(persona)
  val navController = rememberNavController()
  val backStack by navController.currentBackStackEntryAsState()
  val currentRoute = backStack?.destination?.route
  val hideBar = currentRoute?.startsWith("event_detail") == true

  LaunchedEffect(deepLinkEventId) {
    val id = deepLinkEventId ?: return@LaunchedEffect
    navController.navigate(AppDestination.eventDetail(id)) {
      launchSingleTop = true
      popUpTo(tabs.first().route) { saveState = true }
    }
    onDeepLinkConsumed()
  }

  Box(Modifier.fillMaxSize()) {
  Scaffold(
    bottomBar = {
      if (!hideBar) {
        NavigationBar(
          containerColor = SleekSurface,
          contentColor = SleekPrimary,
          tonalElevation = 0.dp,
        ) {
          tabs.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
              selected = selected,
              onClick = {
                navController.navigate(tab.route) {
                  popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                  launchSingleTop = true
                  restoreState = true
                }
              },
              icon = { Icon(tab.icon, contentDescription = tab.label) },
              label = {
                Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp)
              },
              colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SleekPrimary,
                selectedTextColor = SleekPrimary,
                unselectedIconColor = SleekTextSecondary,
                unselectedTextColor = SleekTextSecondary,
                indicatorColor = SleekBorder.copy(alpha = 0.45f),
              ),
            )
          }
        }
      }
    },
  ) { padding ->
    NavHost(
      navController = navController,
      startDestination = tabs.first().route,
      modifier = Modifier.padding(padding),
    ) {
      when (persona) {
        AppPersona.ORGANISER -> {
          composable(AppDestination.ORG_DASHBOARD) {
            OrganiserDashboardScreen(onOpenEvent = { navController.navigate(AppDestination.eventDetail(it)) })
          }
          composable(AppDestination.ORG_EVENTS) {
            OrganiserEventsScreen(onOpenEvent = { navController.navigate(AppDestination.eventDetail(it)) })
          }
          composable(AppDestination.ORG_VOLUNTEERS) { OrganiserVolunteersScreen() }
          composable(AppDestination.ORG_REPORTS) { OrganiserReportsScreen() }
          composable(AppDestination.ORG_PROFILE) { OrganiserProfileScreen(onLogout = onLogout) }
        }
        AppPersona.LEADER -> {
          composable(AppDestination.LDR_HOME) {
            LeaderHomeScreen(onOpenEvent = { navController.navigate(AppDestination.eventDetail(it)) })
          }
          composable(AppDestination.LDR_EVENTS) {
            LeaderMyEventsScreen(onOpenEvent = { navController.navigate(AppDestination.eventDetail(it)) })
          }
          composable(AppDestination.LDR_TEAM) { LeaderMyTeamScreen() }
          composable(AppDestination.LDR_MESSAGES) { LeaderMessagesScreen() }
          composable(AppDestination.LDR_PROFILE) { LeaderProfileScreen(onLogout = onLogout) }
        }
        AppPersona.VOLUNTEER -> {
          composable(AppDestination.VOL_HOME) {
            VolunteerHomeScreen(onOpenEvent = { navController.navigate(AppDestination.eventDetail(it)) })
          }
          composable(AppDestination.VOL_EXPLORE) {
            VolunteerExploreScreen(onOpenEvent = { navController.navigate(AppDestination.eventDetail(it)) })
          }
          composable(AppDestination.VOL_EVENTS) {
            VolunteerMyEventsScreen(onOpenEvent = { navController.navigate(AppDestination.eventDetail(it)) })
          }
          composable(AppDestination.VOL_NOTIFICATIONS) { VolunteerNotificationsScreen() }
          composable(AppDestination.VOL_PROFILE) { VolunteerProfileScreen(onLogout = onLogout) }
        }
      }

      composable(
        route = AppDestination.EVENT_DETAIL,
        arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
      ) { entry ->
        val eventId = entry.arguments?.getString("eventId").orEmpty()
        when (persona) {
          AppPersona.VOLUNTEER -> VolunteerEventDetailScreen(
            eventId = eventId,
            onBack = { navController.popBackStack() },
          )
          AppPersona.LEADER -> LeaderEventDetailScreen(
            eventId = eventId,
            onBack = { navController.popBackStack() },
          )
          AppPersona.ORGANISER -> LeaderEventDetailScreen(
            eventId = eventId,
            onBack = { navController.popBackStack() },
            organiserView = true,
          )
        }
      }
    }
  }
    if (!hideBar) {
      AssistantChatHost()
    }
  }
}
