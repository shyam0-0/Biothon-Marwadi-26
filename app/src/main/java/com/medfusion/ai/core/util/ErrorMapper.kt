package com.medfusion.ai.core.util

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestoreException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Central translation of low-level exceptions into [AppError]. Keeping this in
 * one place means every repository maps failures consistently.
 */
fun Throwable.toAppError(): AppError = when (this) {
    is MedFusionException -> appError
    is SocketTimeoutException -> AppError.Timeout(this)
    is FirebaseNetworkException, is IOException -> AppError.Network(this)

    is HttpException -> when (code()) {
        401, 403 -> AppError.Unauthorized(this)
        404 -> AppError.NotFound(cause = this)
        in 500..599 -> AppError.Server(cause = this)
        else -> AppError.Unknown(this)
    }

    is FirebaseAuthInvalidUserException ->
        AppError.Validation("No account found with these details.")
    is FirebaseAuthInvalidCredentialsException ->
        AppError.Validation("Incorrect email or password.")
    is FirebaseAuthException -> AppError.Unauthorized(this)

    is FirebaseFirestoreException -> when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> AppError.PermissionDenied(cause = this)
        FirebaseFirestoreException.Code.NOT_FOUND -> AppError.NotFound(cause = this)
        FirebaseFirestoreException.Code.UNAVAILABLE -> AppError.Network(this)
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> AppError.Timeout(this)
        else -> AppError.Server(cause = this)
    }

    else -> AppError.Unknown(this)
}

/**
 * Runs [block] and wraps its result in a [Resource], mapping any thrown
 * exception through [toAppError]. CancellationException is intentionally not
 * caught here — callers using this on suspend functions should let structured
 * concurrency cancellation propagate (see repository implementations).
 */
inline fun <T> runCatchingResource(block: () -> T): Resource<T> = try {
    Resource.Success(block())
} catch (t: Throwable) {
    Resource.Error(t.toAppError())
}
