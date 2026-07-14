package com.medfusion.ai.core.util

import kotlinx.coroutines.CancellationException

/**
 * Coroutine-safe variant of [runCatchingResource]: runs a suspending [block] and
 * wraps the outcome in a [Resource], but rethrows [CancellationException] so
 * structured-concurrency cancellation is never swallowed.
 */
suspend inline fun <T> resourceOf(crossinline block: suspend () -> T): Resource<T> = try {
    Resource.Success(block())
} catch (c: CancellationException) {
    throw c
} catch (t: Throwable) {
    Resource.Error(t.toAppError())
}
