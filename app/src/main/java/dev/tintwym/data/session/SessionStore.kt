package dev.tintwym.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore("cg_session")

class SessionStore(private val context: Context) {
  private val keyToken = stringPreferencesKey("token")
  private val keyId = stringPreferencesKey("id")
  private val keyEmail = stringPreferencesKey("email")
  private val keyName = stringPreferencesKey("name")
  private val keyRole = stringPreferencesKey("role")
  private val keyOrgId = stringPreferencesKey("org_id")
  private val keyOrgName = stringPreferencesKey("org_name")
  private val keyPhone = stringPreferencesKey("phone")

  val sessionFlow: Flow<SessionUser?> = context.sessionDataStore.data.map { prefs ->
    val token = prefs[keyToken] ?: return@map null
    val id = prefs[keyId] ?: return@map null
    val email = prefs[keyEmail] ?: return@map null
    val name = prefs[keyName] ?: return@map null
    val role = prefs[keyRole] ?: return@map null
    SessionUser(
      id = id,
      email = email,
      fullName = name,
      role = role,
      organizationId = prefs[keyOrgId],
      organizationName = prefs[keyOrgName],
      phone = prefs[keyPhone],
      accessToken = token,
    )
  }

  suspend fun current(): SessionUser? = sessionFlow.first()

  suspend fun save(user: SessionUser) {
    context.sessionDataStore.edit { prefs ->
      prefs[keyToken] = user.accessToken
      prefs[keyId] = user.id
      prefs[keyEmail] = user.email
      prefs[keyName] = user.fullName
      prefs[keyRole] = user.role
      user.organizationId?.let { prefs[keyOrgId] = it } ?: prefs.remove(keyOrgId)
      user.organizationName?.let { prefs[keyOrgName] = it } ?: prefs.remove(keyOrgName)
      user.phone?.let { prefs[keyPhone] = it } ?: prefs.remove(keyPhone)
    }
  }

  suspend fun clear() {
    context.sessionDataStore.edit { it.clear() }
  }
}
