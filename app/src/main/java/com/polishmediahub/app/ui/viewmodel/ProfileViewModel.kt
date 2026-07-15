package com.polishmediahub.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polishmediahub.app.data.ProfileRepository
import com.polishmediahub.app.data.local.ProfileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    val currentProfile: StateFlow<ProfileEntity?> = profileRepository.currentProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val profiles: StateFlow<List<ProfileEntity>> = profileRepository.profiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectProfile(profile: ProfileEntity) = viewModelScope.launch {
        profileRepository.selectProfile(profile)
    }

    fun verifyPin(profile: ProfileEntity, code: String): Boolean {
        return profileRepository.verifyPin(profile, code)
    }

    fun createProfile(name: String, avatarUrl: String? = null, pinCode: String? = null) = viewModelScope.launch {
        profileRepository.createProfile(name, avatarUrl, pinCode)
    }

    fun updateParentalControls(profileId: String, maxAgeRating: String?, allowNsfw: Boolean) = viewModelScope.launch {
        profileRepository.updateParentalControls(profileId, maxAgeRating, allowNsfw)
    }

    fun deleteProfile(id: String) = viewModelScope.launch {
        profileRepository.deleteProfile(id)
    }
}
