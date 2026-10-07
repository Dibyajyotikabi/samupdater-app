package com.samupdater.app.ui.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samupdater.app.data.FirmwareRepository
import com.samupdater.app.domain.Article
import com.samupdater.app.ui.Loadable
import com.samupdater.app.ui.toLoadable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NewsViewModel(private val repository: FirmwareRepository) : ViewModel() {

    private val _articles = MutableStateFlow<Loadable<List<Article>>>(Loadable.Loading)
    val articles: StateFlow<Loadable<List<Article>>> = _articles.asStateFlow()

    init {
        load(force = false)
    }

    fun refresh() = load(force = true)

    private fun load(force: Boolean) {
        _articles.value = Loadable.Loading
        viewModelScope.launch { _articles.value = repository.articles(force).toLoadable() }
    }
}
