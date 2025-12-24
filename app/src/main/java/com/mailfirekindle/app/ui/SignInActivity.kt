package com.mailfirekindle.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.mailfirekindle.app.R
import com.mailfirekindle.app.auth.AuthManager
import com.mailfirekindle.app.databinding.ActivitySignInBinding

/**
 * Sign-in screen activity.
 * Handles Microsoft OAuth authentication flow.
 */
class SignInActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySignInBinding
    private lateinit var authManager: AuthManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        authManager = AuthManager.getInstance(this)
        
        setupUI()
        initializeAuth()
    }
    
    private fun setupUI() {
        binding.btnSignIn.setOnClickListener {
            signIn()
        }
    }
    
    private fun initializeAuth() {
        showLoading(true, getString(R.string.signing_in))
        
        authManager.initialize { success, error ->
            runOnUiThread {
                if (success) {
                    if (authManager.isSignedIn()) {
                        // Already signed in, go to inbox
                        navigateToInbox()
                    } else {
                        showLoading(false)
                    }
                } else {
                    showLoading(false)
                    showError(error ?: getString(R.string.error_generic))
                }
            }
        }
    }
    
    private fun signIn() {
        showLoading(true, getString(R.string.signing_in))
        hideError()
        
        authManager.signIn(this, object : AuthManager.AuthCallback {
            override fun onSuccess(accessToken: String) {
                runOnUiThread {
                    showLoading(false)
                    navigateToInbox()
                }
            }
            
            override fun onError(error: String) {
                runOnUiThread {
                    showLoading(false)
                    showError(error)
                }
            }
            
            override fun onCancel() {
                runOnUiThread {
                    showLoading(false)
                }
            }
            
            override fun onUnauthorizedAccount(message: String) {
                runOnUiThread {
                    showLoading(false)
                    showError(getString(R.string.error_wrong_account))
                }
            }
        })
    }
    
    private fun navigateToInbox() {
        val intent = Intent(this, InboxActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun showLoading(show: Boolean, message: String? = null) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.tvStatus.visibility = if (show && message != null) View.VISIBLE else View.GONE
        binding.tvStatus.text = message
        binding.btnSignIn.isEnabled = !show
    }
    
    private fun showError(message: String) {
        binding.tvError.visibility = View.VISIBLE
        binding.tvError.text = message
    }
    
    private fun hideError() {
        binding.tvError.visibility = View.GONE
    }
}

