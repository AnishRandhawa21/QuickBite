package com.anish.quickbite.ui.screens.auth

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anish.quickbite.ui.theme.QuickBiteGradients
import com.anish.quickbite.viewmodel.AuthState

@Composable
fun LoginScreen(
    authState: AuthState,
    onGoogleSignInClicked: (Context) -> Unit,
    onDismissError: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Brand Logo with Reference Acid Lime
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Fastfood,
                    contentDescription = "QuickBite",
                    tint = Color.Black,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "QuickBite",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Pre-order your food. Skip the wait.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (authState is AuthState.SigningIn) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(14.dp))
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
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = "Continue with Google",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
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
