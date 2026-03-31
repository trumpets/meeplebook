package app.meeplebook.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.meeplebook.core.auth.AuthRepository
import app.meeplebook.core.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ProfileUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    init {
        combine(
            authRepository.observeCurrentUser(),
            preferencesRepository.getPreferences()
        ) { user, prefs ->
            _uiState.update { state ->
                state.copy(
                    username = user?.username ?: "",
                    startingScreen = prefs.startingScreen,
                    collectionViewMode = prefs.collectionViewMode,
                    collectionAlphabetJumpVisible = prefs.collectionAlphabetJumpVisible
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.StartingScreenSelected -> {
                viewModelScope.launch {
                    preferencesRepository.setStartingScreen(event.screen)
                }
            }

            is ProfileEvent.CollectionViewModeSelected -> {
                viewModelScope.launch {
                    preferencesRepository.setCollectionViewMode(event.viewMode)
                }
            }

            is ProfileEvent.CollectionAlphabetJumpVisibilityChanged -> {
                viewModelScope.launch {
                    preferencesRepository.setCollectionAlphabetJumpVisible(event.visible)
                }
            }

            ProfileEvent.OpenSourceLicensesClicked -> {
                viewModelScope.launch {
                    _uiEffect.emit(ProfileUiEffect.OpenSourceLicenses)
                }
            }

            ProfileEvent.LogoutClicked -> {
                _uiState.update { it.copy(isLogoutConfirmVisible = true) }
            }

            ProfileEvent.LogoutDismissed -> {
                _uiState.update { it.copy(isLogoutConfirmVisible = false) }
            }

            ProfileEvent.LogoutConfirmed -> {
                _uiState.update { it.copy(isLogoutConfirmVisible = false, isLoading = true) }
                viewModelScope.launch {
                    authRepository.logout()
                    _uiEffect.emit(ProfileUiEffect.NavigateToLogin)
                }
            }
        }
    }
}
