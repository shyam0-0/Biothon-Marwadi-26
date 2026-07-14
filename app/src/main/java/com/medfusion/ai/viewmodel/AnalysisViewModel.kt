package com.medfusion.ai.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.core.util.UiState
import com.medfusion.ai.domain.model.Case
import com.medfusion.ai.domain.repository.CaseRepository
import com.medfusion.ai.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Runs fusion analysis automatically when the analysis screen opens (right after
 * upload completes). Symptoms and report URLs come from the stored case, so the
 * patient never re-enters anything.
 */
@HiltViewModel
class AnalysisViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val caseRepository: CaseRepository,
) : ViewModel() {

    private val caseId: String = checkNotNull(savedStateHandle[Routes.Args.CASE_ID]) {
        "AnalysisViewModel requires a caseId argument"
    }

    private val _state = MutableStateFlow<UiState<Case>>(UiState.Loading)
    val state: StateFlow<UiState<Case>> = _state.asStateFlow()

    init {
        analyze()
    }

    fun analyze() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = when (val result = caseRepository.runAnalysis(caseId)) {
                is Resource.Success -> UiState.Success(result.data)
                is Resource.Error -> UiState.Error(result.error)
            }
        }
    }
}
