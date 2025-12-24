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
     * Using full Microsoft Graph scope URLs for MSAL 2.x compatibility.
     */
    val SCOPES = arrayOf(
        "https://graph.microsoft.com/User.Read",
        "https://graph.microsoft.com/Mail.Read", 
        "https://graph.microsoft.com/Mail.Send"
    )
    
    /**
     * Number of messages to load in inbox
     */
    const val INBOX_PAGE_SIZE = 25
}

