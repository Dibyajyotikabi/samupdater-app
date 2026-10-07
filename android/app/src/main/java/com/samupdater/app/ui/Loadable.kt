package com.samupdater.app.ui

import com.samupdater.app.data.AppResult

sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>
    data class Ready<T>(val value: T) : Loadable<T>
    data class Failed(val message: String) : Loadable<Nothing>
}

fun <T> AppResult<T>.toLoadable(): Loadable<T> = when (this) {
    is AppResult.Success -> Loadable.Ready(value)
    is AppResult.Failure -> Loadable.Failed(message)
}
