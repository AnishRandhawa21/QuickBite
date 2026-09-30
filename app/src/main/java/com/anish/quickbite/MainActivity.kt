package com.anish.quickbite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.anish.quickbite.navigation.AppNavigation
import com.anish.quickbite.ui.theme.QuickBiteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            QuickBiteTheme {
                MainScreen()
            }
        }
    }
}
