package com.samupdater.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.samupdater.app.ui.nav.AppNavigation
import com.samupdater.app.ui.theme.SamUpdaterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { SamUpdaterTheme { AppNavigation() } }
    }
}
