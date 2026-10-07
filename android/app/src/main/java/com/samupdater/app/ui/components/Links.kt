package com.samupdater.app.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent

/** Opens a link in a Custom Tab. Only http(s) links are allowed. */
fun openLink(context: Context, url: String?) {
    val uri = url?.let(Uri::parse)
    if (uri == null || uri.scheme !in setOf("https", "http")) {
        Toast.makeText(context, "This link can't be opened.", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, uri)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No browser found to open this link.", Toast.LENGTH_SHORT).show()
    }
}
