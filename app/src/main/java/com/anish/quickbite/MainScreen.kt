package com.anish.quickbite

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import com.anish.quickbite.navigation.AppNavigation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("QuickBite")
                }
            )
        }
    ) {
        paddingValues ->
        AppNavigation()
    }
}
