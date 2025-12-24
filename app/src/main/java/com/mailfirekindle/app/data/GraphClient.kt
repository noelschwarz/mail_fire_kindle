package com.mailfirekindle.app.data

import android.util.Log
import com.google.gson.Gson
import com.mailfirekindle.app.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Client for Microsoft Graph API operations.
 */
class GraphClient {
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    companion object {
        private const val TAG = "GraphClient"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
    
    /**
     * Fetch inbox messages.
     * 
     * @param accessToken The OAuth access token
     * @return ApiResult containing list of messages or error
     */
    suspend fun getInboxMessages(accessToken: String): ApiResult<List<Message>> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "${AppConfig.GRAPH_BASE_URL}/me/mailFolders/inbox/messages" +
                        "?\$top=${AppConfig.INBOX_PAGE_SIZE}" +
                        "&\$select=id,subject,from,receivedDateTime,bodyPreview" +
                        "&\$orderby=receivedDateTime desc"
                
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .get()
                    .build()
                
                Log.d(TAG, "Fetching inbox messages")
                
                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string()
                
                when {
                    response.isSuccessful && responseBody != null -> {
                        val messagesResponse = gson.fromJson(responseBody, MessagesResponse::class.java)
                        Log.d(TAG, "Fetched ${messagesResponse.value.size} messages")
                        ApiResult.Success(messagesResponse.value)
                    }
                    response.code == 401 -> {
                        Log.w(TAG, "Unauthorized - token may be expired")
                        ApiResult.Error("Unauthorized", 401)
                    }
                    else -> {
                        Log.e(TAG, "Error fetching messages: ${response.code} - $responseBody")
                        ApiResult.Error("Failed to load messages", response.code)
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error fetching messages", e)
                ApiResult.Error("Network error: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching messages", e)
                ApiResult.Error("Error: ${e.message}")
            }
        }
    }
    
    /**
     * Fetch a single message by ID with full details.
     * 
     * @param accessToken The OAuth access token
     * @param messageId The message ID
     * @return ApiResult containing the message or error
     */
    suspend fun getMessage(accessToken: String, messageId: String): ApiResult<Message> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "${AppConfig.GRAPH_BASE_URL}/me/messages/$messageId" +
                        "?\$select=id,subject,from,receivedDateTime,body,bodyPreview"
                
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .get()
                    .build()
                
                Log.d(TAG, "Fetching message: $messageId")
                
                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string()
                
                when {
                    response.isSuccessful && responseBody != null -> {
                        val message = gson.fromJson(responseBody, Message::class.java)
                        Log.d(TAG, "Fetched message: ${message.subject}")
                        ApiResult.Success(message)
                    }
                    response.code == 401 -> {
                        Log.w(TAG, "Unauthorized - token may be expired")
                        ApiResult.Error("Unauthorized", 401)
                    }
                    else -> {
                        Log.e(TAG, "Error fetching message: ${response.code} - $responseBody")
                        ApiResult.Error("Failed to load message", response.code)
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error fetching message", e)
                ApiResult.Error("Network error: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching message", e)
                ApiResult.Error("Error: ${e.message}")
            }
        }
    }
    
    /**
     * Send an email.
     * 
     * @param accessToken The OAuth access token
     * @param to Recipient email address
     * @param subject Email subject
     * @param body Email body (plain text)
     * @return ApiResult indicating success or error
     */
    suspend fun sendMail(
        accessToken: String,
        to: String,
        subject: String,
        body: String
    ): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "${AppConfig.GRAPH_BASE_URL}/me/sendMail"
                
                val sendMailRequest = SendMailRequest(
                    message = OutgoingMessage(
                        subject = subject,
                        body = MessageBody(
                            contentType = "Text",
                            content = body
                        ),
                        toRecipients = listOf(
                            Recipient(
                                emailAddress = RecipientEmail(address = to)
                            )
                        )
                    ),
                    saveToSentItems = true
                )
                
                val jsonBody = gson.toJson(sendMailRequest)
                Log.d(TAG, "Sending mail to: $to")
                
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                
                val response = httpClient.newCall(request).execute()
                
                when {
                    response.isSuccessful || response.code == 202 -> {
                        Log.d(TAG, "Email sent successfully")
                        ApiResult.Success(Unit)
                    }
                    response.code == 401 -> {
                        Log.w(TAG, "Unauthorized - token may be expired")
                        ApiResult.Error("Unauthorized", 401)
                    }
                    else -> {
                        val responseBody = response.body?.string()
                        Log.e(TAG, "Error sending mail: ${response.code} - $responseBody")
                        ApiResult.Error("Failed to send email", response.code)
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error sending mail", e)
                ApiResult.Error("Network error: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending mail", e)
                ApiResult.Error("Error: ${e.message}")
            }
        }
    }
}

