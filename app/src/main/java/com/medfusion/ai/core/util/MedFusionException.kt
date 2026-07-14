package com.medfusion.ai.core.util

/**
 * Thrown by data sources when they want to surface a specific, already-classified
 * [AppError] (e.g. a validation or not-found case) rather than relying on generic
 * exception mapping. [toAppError] unwraps this first, preserving the message.
 */
class MedFusionException(val appError: AppError) : Exception(appError.userMessage)

/** Shorthand for throwing a classified error from inside a `resourceOf { }` block. */
fun fail(error: AppError): Nothing = throw MedFusionException(error)
