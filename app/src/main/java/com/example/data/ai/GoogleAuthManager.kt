package com.example.data.ai

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Handles Google Sign-In via Jetpack Credential Manager exclusively.
 * Uses single-option GetCredentialRequest per API 36 guidance:
 * - GetGoogleIdOption for silent returning-user auto sign-in
 * - GetSignInWithGoogleOption for interactive "Sign in with Google"
 */
object GoogleAuthManager {

    private const val TAG = "Auth"

    suspend fun attemptAutoSignIn(context: Context): Boolean {
        if (Firebase.auth.currentUser != null) return true
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            return false
        }
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        val credentialManager = CredentialManager.create(context)

        return try {
            val cred = credentialManager.getCredential(context, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                true
            } else {
                false
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    sealed interface SignInOutcome {
        data object Success : SignInOutcome
        data object Cancelled : SignInOutcome
        data class Failure(val message: String) : SignInOutcome
    }

    suspend fun signInInteractive(context: Context): SignInOutcome {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            return SignInOutcome.Failure(context.getString(R.string.ai_auth_config_missing))
        }
        val activity = context.findActivity()
            ?: return SignInOutcome.Failure(context.getString(R.string.ai_auth_failed_generic))

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()
        val credentialManager = CredentialManager.create(context)

        return try {
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                SignInOutcome.Success
            } else {
                SignInOutcome.Failure(context.getString(R.string.ai_auth_failed_generic))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            SignInOutcome.Cancelled
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            SignInOutcome.Failure(e.localizedMessage ?: context.getString(R.string.ai_auth_failed_generic))
        }
    }

    suspend fun signOut(
        context: Context,
        beforeFirebaseSignOut: () -> Unit = {}
    ) {
        withContext(NonCancellable) {
            try {
                beforeFirebaseSignOut()
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
            }
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
