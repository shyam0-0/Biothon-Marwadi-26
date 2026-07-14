package com.medfusion.ai.core.util

/**
 * Domain-level error taxonomy. Repositories translate framework exceptions
 * (IOException, FirebaseAuthException, HttpException…) into one of these so the
 * UI can render a meaningful, localized message and decide whether to offer a
 * retry — satisfying the "handle every failure" requirement.
 */
sealed class AppError(val userMessage: String, val cause: Throwable? = null) {

    class Network(cause: Throwable? = null) :
        AppError("No internet connection. Please check your network and try again.", cause)

    class Timeout(cause: Throwable? = null) :
        AppError("The request took too long. Please try again.", cause)

    class Unauthorized(cause: Throwable? = null) :
        AppError("Your session has expired. Please sign in again.", cause)

    class PermissionDenied(message: String = "You don't have permission to do this.", cause: Throwable? = null) :
        AppError(message, cause)

    class NotFound(message: String = "The requested information could not be found.", cause: Throwable? = null) :
        AppError(message, cause)

    class Validation(message: String) : AppError(message)

    class Server(message: String = "Our servers are having trouble. Please try again shortly.", cause: Throwable? = null) :
        AppError(message, cause)

    class Unknown(cause: Throwable? = null) :
        AppError("Something went wrong. Please try again.", cause)

    /** Whether a retry action is worth offering the user for this class of error. */
    val isRetryable: Boolean
        get() = this is Network || this is Timeout || this is Server || this is Unknown
}
