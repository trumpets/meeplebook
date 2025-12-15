package app.meeplebook.feature.plays

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PlaysViewModel @Inject constructor(
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaysUiState())
    val uiState: StateFlow<PlaysUiState> = _uiState.asStateFlow()
}