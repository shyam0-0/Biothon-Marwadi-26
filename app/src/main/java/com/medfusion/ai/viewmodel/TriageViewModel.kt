package com.medfusion.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medfusion.ai.core.util.AppError
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.core.util.UiState
import com.medfusion.ai.domain.model.Case
import com.medfusion.ai.domain.repository.CaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the symptom triage screen. Exposes a [UiState] of the resulting [Case];
 * Idle before submission, Loading during the call, Success with the case, or Error.
 */
@HiltViewModel
class TriageViewModel @Inject constructor(
    private val caseRepository: CaseRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<Case>>(UiState.Idle)
    val state: StateFlow<UiState<Case>> = _state.asStateFlow()

    fun submit(symptomsText: String) {
        val trimmed = symptomsText.trim()
        if (trimmed.length < 10) {
            _state.value = UiState.Error(
                AppError.Validation("Please describe your symptoms in a little more detail.")
            )
            return
        }
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = when (val result = caseRepository.runTriage(trimmed)) {
                is Resource.Success -> UiState.Success(result.data)
                is Resource.Error -> UiState.Error(result.error)
            }
        }
    }

    /** Returns to the input state so the user can edit and resubmit. */
    fun reset() {
        _state.value = UiState.Idle
    }
}
