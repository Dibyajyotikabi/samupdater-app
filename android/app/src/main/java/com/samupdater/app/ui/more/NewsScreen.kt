package com.samupdater.app.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samupdater.app.domain.Article
import com.samupdater.app.ui.appViewModel
import com.samupdater.app.ui.components.EmptyText
import com.samupdater.app.ui.components.LoadableContent
import com.samupdater.app.ui.components.openLink
import com.samupdater.app.ui.components.OneUiCard
import com.samupdater.app.ui.components.OneUiPage
import androidx.compose.foundation.layout.Column

@Composable
fun NewsScreen(onBack: () -> Unit, vm: NewsViewModel = viewModel(factory = appViewModel { NewsViewModel(it.repository) })) {
    val articles by vm.articles.collectAsStateWithLifecycle()
    val context = LocalContext.current
    OneUiPage(title = "News and guides", subtitle = "Fresh from samupdater.com.", onBack = onBack) {
        item {
            LoadableContent(articles, onRetry = vm::refresh) { list ->
                if (list.isEmpty()) {
                    EmptyText("No posts yet. Check back soon.")
                } else {
                    OneUiCard {
                        Column(Modifier.padding(vertical = 8.dp)) {
                            list.forEachIndexed { index, article ->
                                if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 20.dp))
                                ArticleRow(article) { openLink(context, article.link) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleRow(article: Article, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        overlineContent = { Text(article.date.take(DATE_LENGTH)) },
        headlineContent = { Text(article.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(article.excerpt, maxLines = EXCERPT_LINES, overflow = TextOverflow.Ellipsis) },
    )
}

private const val DATE_LENGTH = 10
private const val EXCERPT_LINES = 3
