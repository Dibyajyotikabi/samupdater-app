package com.samupdater.app.data

/** Small success/failure wrapper so screens can show friendly errors instead of crashing. */
sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val message: String, val cause: Throwable? = null) : AppResult<Nothing>
}

inline fun <T> appCatching(errorMessage: String, block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: kotlinx.coroutines.CancellationException) {
    throw e
} catch (e: Exception) {
    android.util.Log.w("SamUpdater", errorMessage, e)
    AppResult.Failure(errorMessage, e)
}

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.value
