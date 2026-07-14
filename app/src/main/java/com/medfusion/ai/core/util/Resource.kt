package com.medfusion.ai.core.util

/**
 * A generic wrapper for the result of any operation that can fail — used by the
 * repository layer so view models can react to success/error uniformly without
 * try/catch leaking into the presentation layer.
 */
sealed interface Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>
    data class Error(val error: AppError) : Resource<Nothing>
}

/** Convenience for building a successful [Resource]. */
fun <T> T.asSuccess(): Resource<T> = Resource.Success(this)
