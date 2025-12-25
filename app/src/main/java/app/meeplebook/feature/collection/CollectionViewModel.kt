package app.meeplebook.feature.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel @Inject constructor(
) : ViewModel() {

    private val _uiState = MutableStateFlow<CollectionUiState>(CollectionUiState.Loading)
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<CollectionUiEffects>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: CollectionEvent) {
        when (event) {

            is CollectionEvent.JumpToLetter -> {
                emitEffect(CollectionUiEffects.ScrollToLetter(event.letter))
            }

            is CollectionEvent.GameClicked -> {
                emitEffect(CollectionUiEffects.NavigateToGame(event.gameId))
            }

            CollectionEvent.OpenSortSheet -> {
                emitEffect(CollectionUiEffects.OpenSortSheet)
            }

            CollectionEvent.DismissSortSheet -> {
                emitEffect(CollectionUiEffects.DismissSortSheet)
            }

            else -> reduceState(event)
        }
    }

    private fun reduceState(event: CollectionEvent) {
        _uiState.update { state ->
            when (event) {
                is CollectionEvent.SearchChanged ->
                    // TODO debounce search input
                    (state as CollectionUiState.Content)
                        .copy(searchQuery = event.query)

                is CollectionEvent.SortSelected ->
                    (state as CollectionUiState.Content)
                        .copy(sort = event.sort)

                else -> state
            }
        }
    }

    private fun emitEffect(effect: CollectionUiEffects) {
        viewModelScope.launch {
            _uiEffect.emit(effect)
        }
    }

    private fun buildSectionIndices(
        sections: List<CollectionSection>
    ): Map<Char, Int> {
        var index = 0
        val result = LinkedHashMap<Char, Int>(sections.size)

        sections.forEach { section ->
            result[section.key] = index
            index += 1 // sticky header
            index += section.games.size
        }

        return result
    }
}