package app.meeplebook.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.meeplebook.R
import app.meeplebook.core.auth.AuthRepository
import app.meeplebook.core.collection.domain.GetCollectionUseCase
import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.plays.domain.GetPlaysUseCase
import app.meeplebook.core.plays.model.PlaysError
import app.meeplebook.core.result.fold
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Home screen that manages collection and plays data.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val getCollectionUseCase: GetCollectionUseCase,
    private val getPlaysUseCase: GetPlaysUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    /**
     * Loads initial data (collection and first page of plays).
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            val credentials = authRepository.currentUser().first()
            val username = credentials?.username

            if (username.isNullOrBlank()) {
                _uiState.update {
                    it.copy(errorMessageResId = R.string.msg_not_logged_in)
                }
                return@launch
            }

            _uiState.update { it.copy(username = username) }

            // Launch both fetches concurrently
            launch { loadCollection(username) }
            launch { loadPlays(username, page = 1) }
        }
    }

    /**
     * Refreshes all data.
     */
    fun refresh() {
        val username = _uiState.value.username
        if (username.isBlank()) {
            loadInitialData()
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(errorMessageResId = null) }
            launch { loadCollection(username) }
            launch { loadPlays(username, page = 1) }
        }
    }

    /**
     * Loads more plays (next page).
     */
    fun loadMorePlays() {
        val currentState = _uiState.value
        if (!currentState.hasMorePlays || currentState.isLoadingPlays) {
            return
        }

        val username = currentState.username
        if (username.isBlank()) return

        viewModelScope.launch {
            loadPlays(username, page = currentState.playsCurrentPage + 1)
        }
    }

    private suspend fun loadCollection(username: String) {
        _uiState.update { it.copy(isLoadingCollection = true, errorMessageResId = null) }

        getCollectionUseCase(username).fold(
            onSuccess = { items ->
                _uiState.update {
                    it.copy(
                        isLoadingCollection = false,
                        collection = items
                    )
                }
            },
            onFailure = { error ->
                val resId = when (error) {
                    is CollectionError.NetworkError -> R.string.msg_network_error
                    is CollectionError.NotLoggedIn -> R.string.msg_not_logged_in
                    is CollectionError.Timeout -> R.string.msg_collection_timeout
                    is CollectionError.Unknown -> R.string.msg_unknown_error
                }
                _uiState.update {
                    it.copy(
                        isLoadingCollection = false,
                        errorMessageResId = resId
                    )
                }
            }
        )
    }

    private suspend fun loadPlays(username: String, page: Int) {
        _uiState.update { it.copy(isLoadingPlays = true, errorMessageResId = null) }

        getPlaysUseCase(username, page).fold(
            onSuccess = { response ->
                _uiState.update { currentState ->
                    val newPlays = if (page == 1) {
                        response.plays
                    } else {
                        currentState.recentPlays + response.plays
                    }
                    currentState.copy(
                        isLoadingPlays = false,
                        recentPlays = newPlays,
                        totalPlays = response.totalPlays,
                        playsCurrentPage = response.currentPage,
                        hasMorePlays = response.hasMorePages
                    )
                }
            },
            onFailure = { error ->
                val resId = when (error) {
                    is PlaysError.NetworkError -> R.string.msg_network_error
                    is PlaysError.NotLoggedIn -> R.string.msg_not_logged_in
                    is PlaysError.Unknown -> R.string.msg_unknown_error
                }
                _uiState.update {
                    it.copy(
                        isLoadingPlays = false,
                        errorMessageResId = resId
                    )
                }
            }
        )
    }
}
