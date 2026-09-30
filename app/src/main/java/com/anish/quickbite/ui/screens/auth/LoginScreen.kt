package com.anish.quickbite.ui.screens.auth

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anish.quickbite.viewmodel.AuthState

@Composable
fun LoginScreen(
    authState: AuthState,
    onGoogleSignInClicked: (Context) -> Unit,
    onDismissError: () -> Unit
) {
    val context = LocalContext.current

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "QuickBite",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Pre-order your food. Skip the wait.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                if (authState is AuthState.SigningIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Selecting Google account...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Button(
                        onClick = { onGoogleSignInClicked(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = "Continue with Google",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Error & Status Dialogs
            when (authState) {
                is AuthState.Error -> {
                    AlertDialog(
                        onDismissRequest = onDismissError,
                        title = { Text("Authentication Error") },
                        text = { Text(authState.message) },
                        confirmButton = {
                            TextButton(onClick = onDismissError) {
                                Text("OK")
                            }
                        }
                    )
                }
                is AuthState.OAuthCancelled -> {
                    AlertDialog(
                        onDismissRequest = onDismissError,
                        title = { Text("Sign-In Cancelled") },
                        text = { Text("Google account selection was cancelled.") },
                        confirmButton = {
                            TextButton(onClick = onDismissError) {
                                Text("OK")
                            }
                        }
                    )
                }
                is AuthState.NetworkError -> {
                    AlertDialog(
                        onDismissRequest = onDismissError,
                        title = { Text("Network Error") },
                        text = { Text("Unable to reach authentication server. Please check your internet connection.") },
                        confirmButton = {
                            TextButton(onClick = onDismissError) {
                                Text("OK")
                            }
                        }
                    )
                }
                is AuthState.MissingProfile -> {
                    AlertDialog(
                        onDismissRequest = onDismissError,
                        title = { Text("Profile Setup Error") },
                        text = { Text("Your Google account is authenticated, but QuickBite profile creation failed in the database. Please check your database permissions or RLS policies.") },
                        confirmButton = {
                            TextButton(onClick = onDismissError) {
                                Text("OK")
                            }
                        }
                    )
                }
                is AuthState.SessionRestorationFailed -> {
                    AlertDialog(
                        onDismissRequest = onDismissError,
                        title = { Text("Session Expired") },
                        text = { Text("Your previous session could not be restored. Please sign in again.") },
                        confirmButton = {
                            TextButton(onClick = onDismissError) {
                                Text("OK")
                            }
                        }
                    )
                }
                else -> {}
            }
        }
    }
}
