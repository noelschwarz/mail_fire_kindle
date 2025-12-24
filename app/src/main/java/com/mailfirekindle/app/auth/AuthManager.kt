package com.mailfirekindle.app.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import com.mailfirekindle.app.AppConfig
import com.mailfirekindle.app.R
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException

/**
 * Manages Microsoft authentication using MSAL.
 * Implements single account mode with email verification.
 */
class AuthManager private constructor(private val context: Context) {
    
    private var msalClient: ISingleAccountPublicClientApplication? = null
    private var currentAccount: IAccount? = null
    
    companion object {
        private const val TAG = "AuthManager"
        
        @Volatile
        private var instance: AuthManager? = null
        
        fun getInstance(context: Context): AuthManager {
            return instance ?: synchronized(this) {
                instance ?: AuthManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    /**
     * Initialize MSAL client. Must be called before any auth operations.
     */
    fun initialize(callback: (Boolean, String?) -> Unit) {
        PublicClientApplication.createSingleAccountPublicClientApplication(
            context,
            R.raw.auth_config_single_account,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    msalClient = application
                    Log.d(TAG, "MSAL client created successfully")
                    loadAccount(callback)
                }
                
                override fun onError(exception: MsalException) {
                    Log.e(TAG, "Failed to create MSAL client", exception)
                    callback(false, exception.message)
                }
            }
        )
    }
    
    /**
     * Load any existing signed-in account from cache.
     */
    private fun loadAccount(callback: (Boolean, String?) -> Unit) {
        msalClient?.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
            override fun onAccountLoaded(activeAccount: IAccount?) {
                currentAccount = activeAccount
                if (activeAccount != null) {
                    Log.d(TAG, "Account loaded: ${activeAccount.username}")
                    // Verify the account email
                    if (isAllowedEmail(activeAccount.username)) {
                        callback(true, null)
                    } else {
                        // Sign out unauthorized account
                        signOut { _, _ ->
                            callback(false, "Unauthorized account")
                        }
                    }
                } else {
                    Log.d(TAG, "No account loaded")
                    callback(true, null)
                }
            }
            
            override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                this@AuthManager.currentAccount = currentAccount
                Log.d(TAG, "Account changed from ${priorAccount?.username} to ${currentAccount?.username}")
            }
            
            override fun onError(exception: MsalException) {
                Log.e(TAG, "Error loading account", exception)
                callback(false, exception.message)
            }
        })
    }
    
    /**
     * Check if user is currently signed in.
     */
    fun isSignedIn(): Boolean = currentAccount != null
    
    /**
     * Get the current signed-in account.
     */
    fun getCurrentAccount(): IAccount? = currentAccount
    
    /**
     * Get the username/email of the signed-in user.
     */
    fun getCurrentUserEmail(): String? = currentAccount?.username
    
    /**
     * Check if email is the allowed account.
     */
    private fun isAllowedEmail(email: String?): Boolean {
        return email?.equals(AppConfig.ALLOWED_EMAIL, ignoreCase = true) == true
    }
    
    /**
     * Initiate interactive sign-in flow.
     */
    fun signIn(activity: Activity, callback: AuthCallback) {
        val client = msalClient
        if (client == null) {
            callback.onError("MSAL client not initialized")
            return
        }
        
        val parameters = SignInParameters.builder()
            .withActivity(activity)
            .withScopes(AppConfig.SCOPES.toList())
            .withCallback(object : AuthenticationCallback {
                override fun onSuccess(authenticationResult: IAuthenticationResult) {
                    val account = authenticationResult.account
                    Log.d(TAG, "Sign-in successful: ${account.username}")
                    
                    // Verify the account email
                    if (isAllowedEmail(account.username)) {
                        currentAccount = account
                        callback.onSuccess(authenticationResult.accessToken)
                    } else {
                        // Sign out unauthorized account
                        signOut { _, _ -> }
                        callback.onUnauthorizedAccount(
                            "Only ${AppConfig.ALLOWED_EMAIL} is allowed to sign in"
                        )
                    }
                }
                
                override fun onError(exception: MsalException) {
                    Log.e(TAG, "Sign-in error", exception)
                    callback.onError(exception.message ?: "Unknown error")
                }
                
                override fun onCancel() {
                    Log.d(TAG, "Sign-in cancelled")
                    callback.onCancel()
                }
            })
            .build()
        
        client.signIn(parameters)
    }
    
    /**
     * Acquire token silently (from cache or refresh).
     */
    fun acquireTokenSilent(callback: AuthCallback) {
        val client = msalClient
        val account = currentAccount
        
        if (client == null) {
            callback.onError("MSAL client not initialized")
            return
        }
        
        if (account == null) {
            callback.onError("No account signed in")
            return
        }
        
        val parameters = AcquireTokenSilentParameters.Builder()
            .forAccount(account)
            .fromAuthority(account.authority)
            .withScopes(AppConfig.SCOPES.toList())
            .withCallback(object : AuthenticationCallback {
                override fun onSuccess(authenticationResult: IAuthenticationResult) {
                    Log.d(TAG, "Silent token acquisition successful")
                    callback.onSuccess(authenticationResult.accessToken)
                }
                
                override fun onError(exception: MsalException) {
                    Log.e(TAG, "Silent token acquisition failed", exception)
                    callback.onError(exception.message ?: "Token refresh failed")
                }
                
                override fun onCancel() {
                    callback.onCancel()
                }
            })
            .build()
        
        client.acquireTokenSilentAsync(parameters)
    }
    
    /**
     * Sign out the current user.
     */
    fun signOut(callback: (Boolean, String?) -> Unit) {
        val client = msalClient
        if (client == null) {
            callback(false, "MSAL client not initialized")
            return
        }
        
        client.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
            override fun onSignOut() {
                currentAccount = null
                Log.d(TAG, "Sign-out successful")
                callback(true, null)
            }
            
            override fun onError(exception: MsalException) {
                Log.e(TAG, "Sign-out error", exception)
                callback(false, exception.message)
            }
        })
    }
    
    /**
     * Callback interface for auth operations.
     */
    interface AuthCallback {
        fun onSuccess(accessToken: String)
        fun onError(error: String)
        fun onCancel()
        fun onUnauthorizedAccount(message: String)
    }
}

