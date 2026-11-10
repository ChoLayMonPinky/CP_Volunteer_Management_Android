package dev.tintwym.data.offline

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class PendingCheckIn(
  val eventId: String,
  val eventName: String? = null,
  val code: String,
  val queuedAt: Long = System.currentTimeMillis(),
)

private val Context.checkInStore by preferencesDataStore("cg_offline_checkins")

class OfflineCheckInQueue(private val context: Context) {
  private val key = stringPreferencesKey("pending_json")
  private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
  private val type = Types.newParameterizedType(List::class.java, PendingCheckIn::class.java)
  private val adapter = moshi.adapter<List<PendingCheckIn>>(type)

  val pendingFlow: Flow<List<PendingCheckIn>> = context.checkInStore.data.map { prefs ->
    decode(prefs[key])
  }

  suspend fun pending(): List<PendingCheckIn> = pendingFlow.first()

  suspend fun enqueue(item: PendingCheckIn) {
    context.checkInStore.edit { prefs ->
      val next = decode(prefs[key]).filterNot { it.eventId == item.eventId } + item
      prefs[key] = adapter.toJson(next)
    }
  }

  suspend fun remove(eventId: String) {
    context.checkInStore.edit { prefs ->
      val next = decode(prefs[key]).filterNot { it.eventId == eventId }
      if (next.isEmpty()) prefs.remove(key) else prefs[key] = adapter.toJson(next)
    }
  }

  suspend fun replaceAll(items: List<PendingCheckIn>) {
    context.checkInStore.edit { prefs ->
      if (items.isEmpty()) prefs.remove(key) else prefs[key] = adapter.toJson(items)
    }
  }

  private fun decode(raw: String?): List<PendingCheckIn> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching { adapter.fromJson(raw).orEmpty() }.getOrDefault(emptyList())
  }
}
