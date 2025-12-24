package com.mailfirekindle.app.ui

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mailfirekindle.app.R
import com.mailfirekindle.app.auth.AuthManager
import com.mailfirekindle.app.data.ApiResult
import com.mailfirekindle.app.data.GraphClient
import com.mailfirekindle.app.data.Message
import com.mailfirekindle.app.databinding.ActivityMessageDetailBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Message detail screen showing full email content.
 */
class MessageDetailActivity : AppCompatActivity() {
    
    companion object {
        const val EXTRA_MESSAGE_ID = "message_id"
        const val EXTRA_MESSAGE_SUBJECT = "message_subject"
    }
    
    private lateinit var binding: ActivityMessageDetailBinding
    private lateinit var authManager: AuthManager
    private lateinit var graphClient: GraphClient
    
    private var messageId: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        authManager = AuthManager.getInstance(this)
        graphClient = GraphClient()
        
        messageId = intent.getStringExtra(EXTRA_MESSAGE_ID)
        val subject = intent.getStringExtra(EXTRA_MESSAGE_SUBJECT)
        
        setupToolbar(subject)
        setupRetry()
        
        if (messageId != null) {
            loadMessage()
        } else {
            showError(getString(R.string.error_loading_message))
        }
    }
    
    private fun setupToolbar(subject: String?) {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        if (subject != null) {
            binding.toolbar.title = subject
        }
    }
    
    private fun setupRetry() {
        binding.btnRetry.setOnClickListener {
            loadMessage()
        }
    }
    
    private fun loadMessage() {
        val id = messageId ?: return
        
        showLoading(true)
        hideError()
        
        authManager.acquireTokenSilent(object : AuthManager.AuthCallback {
            override fun onSuccess(accessToken: String) {
                fetchMessage(accessToken, id)
            }
            
            override fun onError(error: String) {
                runOnUiThread {
                    showLoading(false)
                    showError(getString(R.string.error_token_expired))
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
                    showError(getString(R.string.error_unauthorized))
                }
            }
        })
    }
    
    private fun fetchMessage(accessToken: String, messageId: String) {
        lifecycleScope.launch {
            when (val result = graphClient.getMessage(accessToken, messageId)) {
                is ApiResult.Success -> {
                    showLoading(false)
                    displayMessage(result.data)
                }
                is ApiResult.Error -> {
                    showLoading(false)
                    showError(result.message)
                }
            }
        }
    }
    
    private fun displayMessage(message: Message) {
        binding.scrollView.visibility = View.VISIBLE
        
        // Subject
        binding.tvSubject.text = message.subject ?: "(No subject)"
        
        // From
        val senderName = message.from?.emailAddress?.name 
            ?: message.from?.emailAddress?.address 
            ?: "Unknown"
        val senderEmail = message.from?.emailAddress?.address ?: ""
        
        binding.tvFrom.text = senderName
        binding.tvFromEmail.text = senderEmail
        
        // Initial
        val initial = senderName.firstOrNull()?.uppercaseChar() ?: '?'
        binding.tvInitial.text = initial.toString()
        
        // Date
        binding.tvDate.text = formatDate(message.receivedDateTime)
        
        // Body
        val bodyContent = message.body?.content ?: message.bodyPreview ?: ""
        val contentType = message.body?.contentType ?: "Text"
        
        if (contentType.equals("HTML", ignoreCase = true)) {
            // Parse HTML content
            binding.tvBody.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(bodyContent, Html.FROM_HTML_MODE_LEGACY)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(bodyContent)
            }
        } else {
            binding.tvBody.text = bodyContent
        }
    }
    
    private fun formatDate(dateString: String?): String {
        if (dateString == null) return ""
        
        return try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val displayFormat = SimpleDateFormat("MMM dd, yyyy\nhh:mm a", Locale.getDefault())
            
            val date = isoFormat.parse(dateString)
            if (date != null) {
                displayFormat.format(date)
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }
    
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            binding.scrollView.visibility = View.GONE
            binding.errorLayout.visibility = View.GONE
        }
    }
    
    private fun showError(message: String) {
        binding.scrollView.visibility = View.GONE
        binding.errorLayout.visibility = View.VISIBLE
        binding.tvError.text = message
    }
    
    private fun hideError() {
        binding.errorLayout.visibility = View.GONE
    }
}

