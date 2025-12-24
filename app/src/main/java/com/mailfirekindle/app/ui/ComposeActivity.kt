package com.mailfirekindle.app.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mailfirekindle.app.R
import com.mailfirekindle.app.auth.AuthManager
import com.mailfirekindle.app.data.ApiResult
import com.mailfirekindle.app.data.GraphClient
import com.mailfirekindle.app.databinding.ActivityComposeBinding
import kotlinx.coroutines.launch

/**
 * Compose screen for creating and sending emails.
 */
class ComposeActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityComposeBinding
    private lateinit var authManager: AuthManager
    private lateinit var graphClient: GraphClient
    
    private var isSending = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComposeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        authManager = AuthManager.getInstance(this)
        graphClient = GraphClient()
        
        setupToolbar()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            if (!isSending) {
                onBackPressed()
            }
        }
        
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_send -> {
                    if (!isSending) {
                        sendEmail()
                    }
                    true
                }
                else -> false
            }
        }
    }
    
    private fun sendEmail() {
        // Validate input
        val to = binding.etTo.text?.toString()?.trim() ?: ""
        val subject = binding.etSubject.text?.toString()?.trim() ?: ""
        val body = binding.etBody.text?.toString() ?: ""
        
        // Clear previous errors
        binding.tilTo.error = null
        binding.tilSubject.error = null
        
        // Validate
        var hasError = false
        
        if (to.isEmpty()) {
            binding.tilTo.error = "Recipient is required"
            hasError = true
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(to).matches()) {
            binding.tilTo.error = "Invalid email address"
            hasError = true
        }
        
        if (subject.isEmpty()) {
            binding.tilSubject.error = "Subject is required"
            hasError = true
        }
        
        if (hasError) return
        
        // Send
        showSending(true)
        
        authManager.acquireTokenSilent(object : AuthManager.AuthCallback {
            override fun onSuccess(accessToken: String) {
                performSend(accessToken, to, subject, body)
            }
            
            override fun onError(error: String) {
                runOnUiThread {
                    showSending(false)
                    showError(getString(R.string.error_token_expired))
                }
            }
            
            override fun onCancel() {
                runOnUiThread {
                    showSending(false)
                }
            }
            
            override fun onUnauthorizedAccount(message: String) {
                runOnUiThread {
                    showSending(false)
                    showError(getString(R.string.error_unauthorized))
                }
            }
        })
    }
    
    private fun performSend(accessToken: String, to: String, subject: String, body: String) {
        lifecycleScope.launch {
            when (val result = graphClient.sendMail(accessToken, to, subject, body)) {
                is ApiResult.Success -> {
                    showSending(false)
                    Toast.makeText(
                        this@ComposeActivity,
                        R.string.message_sent,
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
                is ApiResult.Error -> {
                    showSending(false)
                    showError(getString(R.string.send_failed) + ": " + result.message)
                }
            }
        }
    }
    
    private fun showSending(sending: Boolean) {
        isSending = sending
        binding.progressOverlay.visibility = if (sending) View.VISIBLE else View.GONE
        
        // Disable inputs while sending
        binding.etTo.isEnabled = !sending
        binding.etSubject.isEnabled = !sending
        binding.etBody.isEnabled = !sending
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!isSending) {
            super.onBackPressed()
        }
    }
}

