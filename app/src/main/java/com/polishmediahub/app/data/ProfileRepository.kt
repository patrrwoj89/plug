package com.polishmediahub.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.polishmediahub.app.data.local.ProfileDao
import com.polishmediahub.app.data.local.ProfileEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.profileDataStore by preferencesDataStore(name = "profile_settings")

@Singleton
class ProfileRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val profileDao: ProfileDao
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dataStore = context.profileDataStore

    private val _currentProfile = MutableStateFlow<ProfileEntity?>(null)
    val currentProfile: StateFlow<ProfileEntity?> = _currentProfile.asStateFlow()

    val profiles: Flow<List<ProfileEntity>> = profileDao.observeAll()

    init {
        scope.launch { loadCurrentProfile() }
    }

    private suspend fun loadCurrentProfile() {
        val savedId = dataStore.data.map { it[KEY_CURRENT_PROFILE] }.first()
        val profile = if (!savedId.isNullOrBlank()) {
            profileDao.getById(savedId)
        } else {
            null
        } ?: ensureDefaultProfile()
        _currentProfile.value = profile
    }

    private suspend fun ensureDefaultProfile(): ProfileEntity {
        val existing = profileDao.getById(DEFAULT_PROFILE_ID)
        if (existing != null) return existing
        val default = ProfileEntity(
            id = DEFAULT_PROFILE_ID,
            name = "Default",
            avatarUrl = null,
            isPinLocked = false,
            pinCode = null,
            maxAgeRating = null,
            allowNsfw = false
        )
        profileDao.upsert(default)
        return default
    }

    suspend fun selectProfile(profile: ProfileEntity) = withContext(Dispatchers.IO) {
        dataStore.edit { it[KEY_CURRENT_PROFILE] = profile.id }
        _currentProfile.value = profile
    }

    suspend fun createProfile(name: String, avatarUrl: String? = null, pinCode: String? = null): ProfileEntity =
        withContext(Dispatchers.IO) {
            val id = "profile_${System.currentTimeMillis()}"
            val profile = ProfileEntity(
                id = id,
                name = name,
                avatarUrl = avatarUrl,
                isPinLocked = !pinCode.isNullOrBlank(),
                pinCode = pinCode?.takeIf { it.isNotBlank() }?.let { PinSecurity.hash(it) },
                maxAgeRating = null,
                allowNsfw = false
            )
            profileDao.upsert(profile)
            if (_currentProfile.value == null) {
                selectProfile(profile)
            }
            profile
        }

    suspend fun updateProfile(profile: ProfileEntity) = withContext(Dispatchers.IO) {
        val secured = profile.copy(pinCode = securePin(profile.pinCode))
        profileDao.upsert(secured)
        if (_currentProfile.value?.id == secured.id) {
            _currentProfile.value = secured
        }
    }

    private fun securePin(pinCode: String?): String? {
        val raw = pinCode?.takeIf { it.isNotBlank() } ?: return null
        return if (PinSecurity.isHashed(raw)) raw else PinSecurity.hash(raw)
    }

    suspend fun updateParentalControls(
        profileId: String,
        maxAgeRating: String?,
        allowNsfw: Boolean
    ) = withContext(Dispatchers.IO) {
        val profile = profileDao.getById(profileId)
            ?: return@withContext
        val updated = profile.copy(maxAgeRating = maxAgeRating, allowNsfw = allowNsfw)
        profileDao.upsert(updated)
        if (_currentProfile.value?.id == profileId) {
            _currentProfile.value = updated
        }
    }

    suspend fun deleteProfile(id: String) = withContext(Dispatchers.IO) {
        profileDao.delete(id)
        if (_currentProfile.value?.id == id) {
            val next = profileDao.observeAll().first().firstOrNull() ?: ensureDefaultProfile()
            selectProfile(next)
        }
    }

    fun verifyPin(profile: ProfileEntity, code: String): Boolean {
        return PinSecurity.verify(code, profile.pinCode)
    }

    companion object {
        const val DEFAULT_PROFILE_ID = "default_profile"
        private val KEY_CURRENT_PROFILE = stringPreferencesKey("current_profile_id")
    }
}
