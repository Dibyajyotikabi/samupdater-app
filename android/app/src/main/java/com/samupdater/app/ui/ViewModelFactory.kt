package com.samupdater.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.samupdater.app.AppContainer
import com.samupdater.app.SamUpdaterApp

/** Builds a ViewModel from the app container: viewModel(factory = appViewModel { MyViewModel(it.repository) }). */
inline fun <reified VM : ViewModel> appViewModel(crossinline create: (AppContainer) -> VM) =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val app = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as SamUpdaterApp
            return create(app.container) as T
        }
    }
