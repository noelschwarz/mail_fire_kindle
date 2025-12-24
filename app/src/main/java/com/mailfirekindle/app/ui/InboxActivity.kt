package com.mailfirekindle.app.ui

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mailfirekindle.app.R
import com.mailfirekindle.app.auth.AuthManager
import com.mailfirekindle.app.data.ApiResult
import com.mailfirekindle.app.data.GraphClient
import com.mailfirekindle.app.data.Message
import com.mailfirekindle.app.databinding.ActivityInboxBinding
import kotlinx.coroutines.launch

/**
 * Inbox screen displaying list of emails.
 */
class InboxActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityInboxBinding
    private lateinit var authManager: AuthManager
    private lateinit var graphClient: GraphClient
    private lateinit var adapter: MessageAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInboxBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        authManager = AuthManager.getInstance(this)
        graphClient = GraphClient()
        
        setupToolbar()
        setupRecyclerView()
        setupFab()
        setupRetry()
        
        loadMessages()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_refresh -> {
                    loadMessages()
                    true
                }
                R.id.action_sign_out -> {
                    signOut()
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupRecyclerView() {
        adapter = MessageAdapter { message ->
            openMessageDetail(message)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }
    
    private fun setupFab() {
        binding.fabCompose.setOnClickListener {
            val intent = Intent(this, ComposeActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun setupRetry() {
        binding.btnRetry.setOnClickListener {
            loadMessages()
        }
    }
    
    private fun loadMessages() {
        if (!isNetworkAvailable()) {
            showError(getString(R.string.error_no_network))
            return
        }
        
        showLoading(true)
        hideError()
        
        authManager.acquireTokenSilent(object : AuthManager.AuthCallback {
            override fun onSuccess(accessToken: String) {
                fetchMessages(accessToken)
            }
            
            override fun onError(error: String) {
                runOnUiThread {
                    showLoading(false)
                    // Show detailed error for debugging
                    showError("Token error: $error")
                }
            }
            
            override fun onCancel() {
                runOnUiThread {
                    showLoading(false)
                    showError("Authentication cancelled")
                }
            }
            
            override fun onUnauthorizedAccount(message: String) {
                runOnUiThread {
                    showLoading(false)
                    showError("Unauthorized: $message")
                    navigateToSignIn()
                }
            }
        })
    }
    
    private fun fetchMessages(accessToken: String) {
        lifecycleScope.launch {
            when (val result = graphClient.getInboxMessages(accessToken)) {
                is ApiResult.Success -> {
                    showLoading(false)
                    if (result.data.isEmpty()) {
                        showEmpty()
                    } else {
                        showMessages(result.data)
                    }
                }
                is ApiResult.Error -> {
                    showLoading(false)
                    // Show detailed error for debugging
                    showError("API Error (${result.code}): ${result.message}")
                }
            }
        }
    }
    
    private fun openMessageDetail(message: Message) {
        val intent = Intent(this, MessageDetailActivity::class.java)
        intent.putExtra(MessageDetailActivity.EXTRA_MESSAGE_ID, message.id)
        intent.putExtra(MessageDetailActivity.EXTRA_MESSAGE_SUBJECT, message.subject)
        startActivity(intent)
    }
    
    private fun signOut() {
        authManager.signOut { success, _ ->
            runOnUiThread {
                navigateToSignIn()
            }
        }
    }
    
    private fun navigateToSignIn() {
        val intent = Intent(this, SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.tvLoading.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            binding.recyclerView.visibility = View.GONE
            binding.tvEmpty.visibility = View.GONE
            binding.errorLayout.visibility = View.GONE
        }
    }
    
    private fun showMessages(messages: List<Message>) {
        binding.recyclerView.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
        binding.errorLayout.visibility = View.GONE
        adapter.submitList(messages)
    }
    
    private fun showEmpty() {
        binding.recyclerView.visibility = View.GONE
        binding.tvEmpty.visibility = View.VISIBLE
        binding.errorLayout.visibility = View.GONE
    }
    
    private fun showError(message: String) {
        binding.recyclerView.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE
        binding.errorLayout.visibility = View.VISIBLE
        binding.tvError.text = message
    }
    
    private fun hideError() {
        binding.errorLayout.visibility = View.GONE
    }
    
    @Suppress("DEPRECATION")
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo != null && networkInfo.isConnected
        }
    }
}

