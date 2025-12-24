package com.mailfirekindle.app.ui

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mailfirekindle.app.AppConfig
import com.mailfirekindle.app.R
import com.mailfirekindle.app.auth.AuthManager
import com.mailfirekindle.app.data.ApiResult
import com.mailfirekindle.app.data.GraphClient
import com.mailfirekindle.app.data.Message
import com.mailfirekindle.app.data.PaginatedMessages
import com.mailfirekindle.app.databinding.ActivityInboxBinding
import kotlinx.coroutines.launch

/**
 * Inbox screen displaying list of emails with pagination support.
 * Loads up to 1000 emails progressively.
 */
class InboxActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityInboxBinding
    private lateinit var authManager: AuthManager
    private lateinit var graphClient: GraphClient
    private lateinit var adapter: MessageAdapter
    
    // Cached messages and pagination state
    private val cachedMessages = mutableListOf<Message>()
    private var nextPageUrl: String? = null
    private var isLoadingMore = false
    private var hasMoreMessages = true
    private var currentAccessToken: String? = null
    
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
        setupLoadMore()
        
        loadMessages(refresh = true)
    }
    
    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_refresh -> {
                    loadMessages(refresh = true)
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
        
        // Auto-load more when scrolling near the bottom
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                if (dy > 0) { // Scrolling down
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    
                    // Load more when within 10 items of the end
                    if (!isLoadingMore && hasMoreMessages &&
                        (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 10) {
                        loadMoreMessages()
                    }
                }
            }
        })
    }
    
    private fun setupFab() {
        binding.fabCompose.setOnClickListener {
            val intent = Intent(this, ComposeActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun setupRetry() {
        binding.btnRetry.setOnClickListener {
            loadMessages(refresh = true)
        }
    }
    
    private fun setupLoadMore() {
        binding.btnLoadMore.setOnClickListener {
            loadMoreMessages()
        }
    }
    
    private fun loadMessages(refresh: Boolean = false) {
        if (!isNetworkAvailable()) {
            showError(getString(R.string.error_no_network))
            return
        }
        
        if (refresh) {
            cachedMessages.clear()
            nextPageUrl = null
            hasMoreMessages = true
        }
        
        showLoading(true)
        hideError()
        hideLoadMore()
        
        authManager.acquireTokenSilent(object : AuthManager.AuthCallback {
            override fun onSuccess(accessToken: String) {
                currentAccessToken = accessToken
                fetchMessages(accessToken, null)
            }
            
            override fun onError(error: String) {
                runOnUiThread {
                    showLoading(false)
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
    
    private fun loadMoreMessages() {
        if (isLoadingMore || !hasMoreMessages || nextPageUrl == null) return
        if (cachedMessages.size >= AppConfig.MAX_CACHED_MESSAGES) {
            hasMoreMessages = false
            updateLoadMoreVisibility()
            return
        }
        
        val token = currentAccessToken
        if (token == null) {
            loadMessages(refresh = false)
            return
        }
        
        isLoadingMore = true
        showLoadingMore(true)
        
        lifecycleScope.launch {
            fetchMessages(token, nextPageUrl)
        }
    }
    
    private fun fetchMessages(accessToken: String, pageUrl: String?) {
        lifecycleScope.launch {
            when (val result = graphClient.getInboxMessages(accessToken, pageUrl)) {
                is ApiResult.Success -> {
                    handleMessagesResult(result.data)
                }
                is ApiResult.Error -> {
                    runOnUiThread {
                        showLoading(false)
                        showLoadingMore(false)
                        isLoadingMore = false
                        showError("API Error (${result.code}): ${result.message}")
                    }
                }
            }
        }
    }
    
    private fun handleMessagesResult(paginatedMessages: PaginatedMessages) {
        runOnUiThread {
            showLoading(false)
            showLoadingMore(false)
            isLoadingMore = false
            
            // Add new messages to cache
            cachedMessages.addAll(paginatedMessages.messages)
            
            // Update pagination state
            nextPageUrl = paginatedMessages.nextPageUrl
            hasMoreMessages = paginatedMessages.hasMore && 
                              cachedMessages.size < AppConfig.MAX_CACHED_MESSAGES
            
            if (cachedMessages.isEmpty()) {
                showEmpty()
            } else {
                showMessages(cachedMessages.toList())
                updateLoadMoreVisibility()
                updateMessageCount()
            }
        }
    }
    
    private fun updateLoadMoreVisibility() {
        if (hasMoreMessages && nextPageUrl != null && cachedMessages.size < AppConfig.MAX_CACHED_MESSAGES) {
            binding.btnLoadMore.visibility = View.VISIBLE
            binding.btnLoadMore.text = "Load More (${cachedMessages.size} of ${AppConfig.MAX_CACHED_MESSAGES} max)"
        } else {
            binding.btnLoadMore.visibility = View.GONE
        }
    }
    
    private fun updateMessageCount() {
        val subtitle = "${cachedMessages.size} emails loaded"
        binding.toolbar.subtitle = subtitle
    }
    
    private fun openMessageDetail(message: Message) {
        val intent = Intent(this, MessageDetailActivity::class.java)
        intent.putExtra(MessageDetailActivity.EXTRA_MESSAGE_ID, message.id)
        intent.putExtra(MessageDetailActivity.EXTRA_MESSAGE_SUBJECT, message.subject)
        startActivity(intent)
    }
    
    private fun signOut() {
        authManager.signOut { _, _ ->
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
            binding.btnLoadMore.visibility = View.GONE
        }
    }
    
    private fun showLoadingMore(show: Boolean) {
        binding.progressLoadMore.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnLoadMore.isEnabled = !show
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
        binding.btnLoadMore.visibility = View.GONE
    }
    
    private fun showError(message: String) {
        binding.recyclerView.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE
        binding.errorLayout.visibility = View.VISIBLE
        binding.tvError.text = message
        binding.btnLoadMore.visibility = View.GONE
    }
    
    private fun hideError() {
        binding.errorLayout.visibility = View.GONE
    }
    
    private fun hideLoadMore() {
        binding.btnLoadMore.visibility = View.GONE
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
