package dev.tintwym.ui.leader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.tintwym.appGraph
import dev.tintwym.data.remote.EventDto
import dev.tintwym.data.remote.RegistrationDto
import dev.tintwym.data.repo.ApiResult
import dev.tintwym.ui.checkin.CheckInQrImage
import dev.tintwym.ui.components.EventCover
import dev.tintwym.ui.components.EventRow
import dev.tintwym.ui.components.FadeSwap
import dev.tintwym.ui.components.PrimaryAction
import dev.tintwym.ui.components.QuietPanel
import dev.tintwym.ui.components.ScreenScaffold
import dev.tintwym.ui.components.SecondaryAction
import dev.tintwym.ui.components.SegmentTabs
import dev.tintwym.ui.components.StatusChip
import dev.tintwym.ui.components.SurfaceBlock
import dev.tintwym.ui.components.capacityLine
import dev.tintwym.ui.components.subtitleLine
import dev.tintwym.ui.state.EmptyBlock
import dev.tintwym.ui.state.ErrorBlock
import dev.tintwym.ui.state.LoadState
import dev.tintwym.ui.state.LoadingBlock
import dev.tintwym.ui.theme.SleekPrimary
import dev.tintwym.ui.theme.SleekTextPrimary
import dev.tintwym.ui.theme.SleekTextSecondary
import kotlinx.coroutines.launch

@Composable
fun LeaderEventDetailScreen(
  eventId: String,
  onBack: () -> Unit,
  organiserView: Boolean = false,
) {
  val graph = LocalContext.current.appGraph()
  var eventState by remember { mutableStateOf<LoadState<EventDto>>(LoadState.Loading) }
  var roster by remember { mutableStateOf<LoadState<List<RegistrationDto>>>(LoadState.Loading) }
  var tab by remember { mutableStateOf("Briefing") }
  var incidentTitle by remember { mutableStateOf("") }
  var incidentBody by remember { mutableStateOf("") }
  var notice by remember { mutableStateOf<String?>(null) }
  var busy by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  val tabs = listOf("Briefing", "Roster", "Attendance", "Incident")

  fun reloadEvent() {
    scope.launch {
      eventState = when (val r = graph.events.get(eventId)) {
        is ApiResult.Ok -> LoadState.Ready(r.value)
        is ApiResult.Err -> if (eventState is LoadState.Ready) eventState else LoadState.Failed(r.message)
      }
    }
  }

  fun reloadRoster() {
    scope.launch {
      roster = when (val r = graph.events.registrations(eventId)) {
        is ApiResult.Ok -> LoadState.Ready(r.value)
        is ApiResult.Err -> if (roster is LoadState.Ready) roster else LoadState.Failed(r.message)
      }
    }
  }

  fun runStaff(block: suspend () -> Unit) {
    if (busy) return
    scope.launch {
      busy = true
      block()
      busy = false
    }
  }

  LaunchedEffect(eventId) {
    reloadEvent()
    reloadRoster()
  }

  when (val s = eventState) {
    LoadState.Loading -> ScreenScaffold(title = "Event", onBack = onBack) { item { LoadingBlock() } }
    is LoadState.Failed -> ScreenScaffold(title = "Event", onBack = onBack) {
      item { ErrorBlock(s.message, onRetry = { reloadEvent() }) }
    }
    is LoadState.Ready -> {
      val event = s.data
      val checked = (roster as? LoadState.Ready)?.data?.count { it.status == "CHECKED_IN" } ?: 0
      val total = (roster as? LoadState.Ready)?.data?.size ?: 0
      ScreenScaffold(
        title = event.name,
        subtitle = if (organiserView) "Organiser overview" else "Leader ops",
        onBack = onBack,
      ) {
        item { EventCover(event.imageUrl, event.category, tall = true) }
        item {
          QuietPanel {
            Text(event.subtitleLine(), style = MaterialTheme.typography.bodyLarge, color = SleekTextSecondary)
            Text(event.capacityLine(), color = SleekTextSecondary)
            Text("On site: $checked / $total checked in", color = SleekTextSecondary)
            Spacer(Modifier.height(10.dp))
            StatusChip(event.status)
          }
        }
        if (!event.checkInCode.isNullOrBlank()) {
          item {
            SurfaceBlock {
              Text("Volunteer check-in", style = MaterialTheme.typography.titleMedium, color = SleekTextPrimary)
              Spacer(Modifier.height(6.dp))
              Text(
                "Show this QR at the venue, or read the code aloud.",
                style = MaterialTheme.typography.bodyMedium,
                color = SleekTextSecondary,
              )
              Spacer(Modifier.height(14.dp))
              Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                CheckInQrImage(code = event.checkInCode)
                Spacer(Modifier.height(12.dp))
                Text(event.checkInCode, style = MaterialTheme.typography.headlineSmall, color = SleekPrimary)
              }
              if (organiserView) {
                Spacer(Modifier.height(14.dp))
                SecondaryAction(if (busy) "Working…" else "Regenerate check-in code") {
                  runStaff {
                    when (val r = graph.events.regenerateCode(eventId)) {
                      is ApiResult.Ok -> {
                        notice = "New code: ${r.value.checkInCode}"
                        reloadEvent()
                      }
                      is ApiResult.Err -> notice = r.message
                    }
                  }
                }
              }
            }
          }
        } else {
          item {
            QuietPanel {
              Text("No check-in code yet", color = SleekTextSecondary)
              if (organiserView) {
                Spacer(Modifier.height(12.dp))
                SecondaryAction(if (busy) "Working…" else "Generate check-in code") {
                  runStaff {
                    when (val r = graph.events.regenerateCode(eventId)) {
                      is ApiResult.Ok -> {
                        notice = "Code: ${r.value.checkInCode}"
                        reloadEvent()
                      }
                      is ApiResult.Err -> notice = r.message
                    }
                  }
                }
              }
            }
          }
        }
        if (notice != null) item { Text(notice!!, color = SleekPrimary) }
        item { SegmentTabs(tabs, tab) { tab = it } }
        item {
          FadeSwap(tab) { current ->
            when (current) {
              "Briefing" -> QuietPanel {
                Text("Briefing", style = MaterialTheme.typography.titleMedium, color = SleekTextPrimary)
                Spacer(Modifier.height(8.dp))
                Text(event.description ?: "No briefing yet.", style = MaterialTheme.typography.bodyLarge)
              }
              "Roster", "Attendance" -> QuietPanel {
                when (val r = roster) {
                  LoadState.Loading -> LoadingBlock()
                  is LoadState.Failed -> ErrorBlock(r.message, onRetry = { reloadRoster() })
                  is LoadState.Ready -> {
                    if (r.data.isEmpty()) EmptyBlock("No registrations yet.")
                    r.data.forEach { reg ->
                      EventRow(
                        title = reg.userName ?: reg.userEmail ?: "Volunteer",
                        subtitle = reg.status.replace('_', ' '),
                      )
                      if (current == "Attendance") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                          when (reg.status) {
                            "CHECKED_IN" -> {
                              SecondaryAction(
                                if (busy) "…" else "Check out",
                                modifier = Modifier.weight(1f),
                              ) {
                                runStaff {
                                  when (val res = graph.events.staffCheckOut(eventId, reg.id)) {
                                    is ApiResult.Ok -> {
                                      notice = "Checked out"
                                      reloadRoster()
                                    }
                                    is ApiResult.Err -> notice = res.message
                                  }
                                }
                              }
                            }
                            "CONFIRMED", "NO_SHOW" -> {
                              PrimaryAction(
                                if (busy) "…" else "Check in",
                                modifier = Modifier.weight(1f),
                              ) {
                                runStaff {
                                  when (val res = graph.events.staffCheckIn(eventId, reg.id)) {
                                    is ApiResult.Ok -> {
                                      notice = "Checked in"
                                      reloadRoster()
                                    }
                                    is ApiResult.Err -> notice = res.message
                                  }
                                }
                              }
                              SecondaryAction(
                                if (busy) "…" else "No-show",
                                modifier = Modifier.weight(1f),
                              ) {
                                runStaff {
                                  when (val res = graph.events.noShow(eventId, reg.id)) {
                                    is ApiResult.Ok -> {
                                      notice = "Marked no-show"
                                      reloadRoster()
                                    }
                                    is ApiResult.Err -> notice = res.message
                                  }
                                }
                              }
                            }
                            "WAITLIST" -> {
                              Text(
                                "Waitlisted — confirm before check-in",
                                style = MaterialTheme.typography.bodySmall,
                                color = SleekTextSecondary,
                              )
                            }
                            else -> Unit
                          }
                        }
                        Spacer(Modifier.height(8.dp))
                      }
                    }
                  }
                }
              }
              "Incident" -> QuietPanel {
                Text("Report incident", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                  incidentTitle,
                  { incidentTitle = it },
                  label = { Text("Title") },
                  modifier = Modifier.fillMaxWidth(),
                  enabled = !busy,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                  incidentBody,
                  { incidentBody = it },
                  label = { Text("Description") },
                  modifier = Modifier.fillMaxWidth(),
                  enabled = !busy,
                )
                Spacer(Modifier.height(12.dp))
                PrimaryAction(if (busy) "Working…" else "Submit incident") {
                  runStaff {
                    when (
                      val res = graph.work.createIncident(
                        title = incidentTitle.ifBlank { "Incident" },
                        description = incidentBody.ifBlank { "Reported from mobile" },
                        severity = "MEDIUM",
                        eventId = eventId,
                      )
                    ) {
                      is ApiResult.Ok -> {
                        notice = "Incident reported"
                        incidentTitle = ""
                        incidentBody = ""
                      }
                      is ApiResult.Err -> notice = res.message
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
