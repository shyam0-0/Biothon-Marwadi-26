package com.medfusion.ai.core.util

/**
 * Canonical screen state used by view models across the app. Every screen can
 * therefore render the same set of states — Loading / Success / Empty / Error —
 * which the reusable [com.medfusion.ai.ui.components.StateContainer] consumes.
 */
sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data object Empty : UiState<Nothing>
    data class Error(val error: AppError) : UiState<Nothing>
}
