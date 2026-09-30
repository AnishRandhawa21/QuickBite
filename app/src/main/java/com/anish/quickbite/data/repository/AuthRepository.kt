package com.anish.quickbite.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.anish.quickbite.BuildConfig
import com.anish.quickbite.SupabaseClient
import com.anish.quickbite.data.model.UserProfile
import com.anish.quickbite.data.model.UserRole
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

interface AuthRepository {
    val sessionStatus: StateFlow<SessionStatus>
    suspend fun signInWithGoogleNative(context: Context): Result<Unit>
    suspend fun getUserProfile(userId: String): Result<UserProfile?>
    suspend fun createInitialProfileIfMissing(userId: String): Result<UserProfile>
    suspend fun signOut(): Result<Unit>
    fun currentUserId(): String?
}

class AuthRepositoryImpl : AuthRepository {
    private val client = SupabaseClient.client

    override val sessionStatus: StateFlow<SessionStatus>
        get() = client.auth.sessionStatus

    override suspend fun signInWithGoogleNative(context: Context): Result<Unit> {
        return runCatching {
            val credentialManager = CredentialManager.create(context)
            val webClientId = BuildConfig.GOOGLE_CLIENT_ID

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val googleIdToken = googleIdTokenCredential.idToken

                client.auth.signInWith(IDToken) {
                    idToken = googleIdToken
                    provider = Google
                }
            } else {
                throw IllegalStateException("Unexpected credential returned: ${credential.type}")
            }
        }
    }

    override suspend fun getUserProfile(userId: String): Result<UserProfile?> {
        return runCatching {
            client.postgrest["profiles"]
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<UserProfile>()
        }
    }

    override suspend fun createInitialProfileIfMissing(userId: String): Result<UserProfile> {
        return runCatching {
            val user = client.auth.currentUserOrNull()
            val metadata = user?.userMetadata
            val name = metadata?.get("full_name")?.jsonPrimitive?.contentOrNull
                ?: metadata?.get("name")?.jsonPrimitive?.contentOrNull
                ?: user?.email?.substringBefore("@")
                ?: "QuickBite User"

            val newProfile = UserProfile(
                id = userId,
                name = name,
                email = user?.email,
                role = UserRole.STUDENT
            )

            client.postgrest["profiles"].insert(newProfile)
            newProfile
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return runCatching {
            client.auth.signOut()
        }
    }

    override fun currentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }
}
