package com.mailfirekindle.app

/**
 * Application configuration constants.
 * 
 * IMPORTANT: Replace placeholders before building!
 */
object AppConfig {
    
    /**
     * The only email address allowed to sign in.
     * This is verified after successful authentication.
     */
    const val ALLOWED_EMAIL = "josef.schwarz@hotmail.de"
    
    /**
     * Microsoft Graph API base URL
     */
    const val GRAPH_BASE_URL = "https://graph.microsoft.com/v1.0"
    
    /**
     * OAuth scopes required for the app.
     * - User.Read: Read user profile
     * - Mail.Read: Read user's email
     * - Mail.Send: Send email on behalf of user
     * - offline_access: Get refresh tokens for persistent access
     */
    val SCOPES = arrayOf(
        "User.Read",
        "Mail.Read", 
        "Mail.Send",
        "offline_access"
    )
    
    /**
     * Number of messages to load in inbox
     */
    const val INBOX_PAGE_SIZE = 25
}

