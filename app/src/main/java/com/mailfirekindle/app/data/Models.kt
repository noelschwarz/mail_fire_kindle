package com.mailfirekindle.app.data

import com.google.gson.annotations.SerializedName

/**
 * Data models for Microsoft Graph API responses.
 */

/**
 * Represents an email message from the Graph API.
 */
data class Message(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("subject")
    val subject: String?,
    
    @SerializedName("from")
    val from: EmailAddress?,
    
    @SerializedName("receivedDateTime")
    val receivedDateTime: String?,
    
    @SerializedName("bodyPreview")
    val bodyPreview: String?,
    
    @SerializedName("body")
    val body: MessageBody?
)

/**
 * Represents an email address with optional name.
 */
data class EmailAddress(
    @SerializedName("emailAddress")
    val emailAddress: EmailAddressDetails?
)

data class EmailAddressDetails(
    @SerializedName("name")
    val name: String?,
    
    @SerializedName("address")
    val address: String?
)

/**
 * Represents the body content of a message.
 */
data class MessageBody(
    @SerializedName("contentType")
    val contentType: String?,
    
    @SerializedName("content")
    val content: String?
)

/**
 * Response wrapper for message list from Graph API.
 */
data class MessagesResponse(
    @SerializedName("value")
    val value: List<Message>,
    
    @SerializedName("@odata.nextLink")
    val nextLink: String? = null
)

/**
 * Paginated messages result with metadata.
 */
data class PaginatedMessages(
    val messages: List<Message>,
    val nextPageUrl: String?,
    val hasMore: Boolean
)

/**
 * Request body for sending an email.
 */
data class SendMailRequest(
    @SerializedName("message")
    val message: OutgoingMessage,
    
    @SerializedName("saveToSentItems")
    val saveToSentItems: Boolean = true
)

data class OutgoingMessage(
    @SerializedName("subject")
    val subject: String,
    
    @SerializedName("body")
    val body: MessageBody,
    
    @SerializedName("toRecipients")
    val toRecipients: List<Recipient>
)

data class Recipient(
    @SerializedName("emailAddress")
    val emailAddress: RecipientEmail
)

data class RecipientEmail(
    @SerializedName("address")
    val address: String
)

/**
 * Result wrapper for API operations.
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int = 0) : ApiResult<Nothing>()
}

