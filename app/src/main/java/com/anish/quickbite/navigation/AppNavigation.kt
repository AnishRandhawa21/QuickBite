package com.anish.quickbite.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.anish.quickbite.data.model.UserRole
import com.anish.quickbite.ui.screens.admin.AdminScreen
import com.anish.quickbite.ui.screens.auth.LoginScreen
import com.anish.quickbite.ui.screens.home.HomeScreen
import com.anish.quickbite.viewmodel.AuthState
import com.anish.quickbite.viewmodel.AuthViewModel

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel()
) {
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Authenticated -> {
                when (state.userProfile.role) {
                    UserRole.STUDENT -> {
                        navController.navigate(Routes.STUDENT_HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                    UserRole.ADMIN -> {
                        navController.navigate(Routes.ADMIN_DASHBOARD) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                }
            }
            is AuthState.NotAuthenticated -> {
                if (navController.currentBackStackEntry?.destination?.route != Routes.LOGIN) {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            else -> {}
        }
    }

    if (authState is AuthState.Initializing) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        NavHost(
            navController = navController,
            startDestination = Routes.LOGIN
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    authState = authState,
                    onGoogleSignInClicked = { context -> authViewModel.signInWithGoogleNative(context) },
                    onDismissError = { authViewModel.resetErrorState() }
                )
            }
            composable(Routes.STUDENT_HOME) {
                HomeScreen(
                    onSignOut = { authViewModel.signOut() }
                )
            }
            composable(Routes.ADMIN_DASHBOARD) {
                AdminScreen(
                    onSignOut = { authViewModel.signOut() }
                )
            }
        }
    }
}
