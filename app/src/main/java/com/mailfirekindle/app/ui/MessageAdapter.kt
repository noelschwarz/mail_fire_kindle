package com.mailfirekindle.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mailfirekindle.app.data.Message
import com.mailfirekindle.app.databinding.ItemMessageBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * RecyclerView adapter for displaying email messages.
 */
class MessageAdapter(
    private val onItemClick: (Message) -> Unit
) : ListAdapter<Message, MessageAdapter.MessageViewHolder>(MessageDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageViewHolder(binding, onItemClick)
    }
    
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class MessageViewHolder(
        private val binding: ItemMessageBinding,
        private val onItemClick: (Message) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        
        fun bind(message: Message) {
            val senderName = message.from?.emailAddress?.name 
                ?: message.from?.emailAddress?.address 
                ?: "Unknown"
            
            binding.tvFrom.text = senderName
            binding.tvSubject.text = message.subject ?: "(No subject)"
            binding.tvPreview.text = message.bodyPreview ?: ""
            
            // Set initial
            val initial = senderName.firstOrNull()?.uppercaseChar() ?: '?'
            binding.tvInitial.text = initial.toString()
            
            // Format date
            binding.tvDate.text = formatDate(message.receivedDateTime)
            
            // Click listener
            binding.root.setOnClickListener {
                onItemClick(message)
            }
        }
        
        private fun formatDate(dateString: String?): String {
            if (dateString == null) return ""
            
            return try {
                val date = isoFormat.parse(dateString)
                if (date != null) {
                    val today = Calendar.getInstance()
                    val messageDate = Calendar.getInstance().apply { time = date }
                    
                    if (today.get(Calendar.YEAR) == messageDate.get(Calendar.YEAR) &&
                        today.get(Calendar.DAY_OF_YEAR) == messageDate.get(Calendar.DAY_OF_YEAR)) {
                        // Today - show time
                        timeFormat.format(date)
                    } else {
                        // Other day - show date
                        dateFormat.format(date)
                    }
                } else {
                    ""
                }
            } catch (e: Exception) {
                ""
            }
        }
    }
    
    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}

