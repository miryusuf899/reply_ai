package com.replyai.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.replyai.databinding.ItemAiResponseBinding
import com.replyai.databinding.ItemChatMessageBinding
import com.replyai.utils.formatApiDate

class ChatMessageAdapter : ListAdapter<ChatMessageAdapter.ChatListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    sealed class ChatListItem {
        data class Message(val content: String, val isUser: Boolean, val time: String?) : ChatListItem()
        data class AiCard(val text: String, val tone: String, val requestId: String?) : ChatListItem()
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is ChatListItem.Message -> 0
        is ChatListItem.AiCard -> 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> MessageViewHolder(ItemChatMessageBinding.inflate(inflater, parent, false))
            else -> AiViewHolder(ItemAiResponseBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ChatListItem.Message -> (holder as MessageViewHolder).bind(item)
            is ChatListItem.AiCard -> (holder as AiViewHolder).bind(item)
        }
    }

    class MessageViewHolder(private val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatListItem.Message) {
            binding.tvMessage.text = item.content
            binding.tvTime.text = item.time.formatApiDate()
            binding.cardMessage.setBackgroundResource(
                if (item.isUser) com.replyai.R.drawable.bg_message_user
                else com.replyai.R.drawable.bg_message_other
            )
        }
    }

    class AiViewHolder(private val binding: ItemAiResponseBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatListItem.AiCard) {
            binding.tvAiText.text = item.text
            binding.tvTone.text = item.tone
            binding.btnCopy.visibility = View.GONE
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChatListItem>() {
        override fun areItemsTheSame(old: ChatListItem, new: ChatListItem): Boolean = old == new
        override fun areContentsTheSame(old: ChatListItem, new: ChatListItem): Boolean = old == new
    }
}
