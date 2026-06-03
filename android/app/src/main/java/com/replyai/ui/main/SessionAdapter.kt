package com.replyai.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.replyai.data.models.ChatSession
import com.replyai.databinding.ItemSessionBinding
import com.replyai.utils.formatApiDate
import com.replyai.utils.messengerIconRes

class SessionAdapter(
    private val onClick: (ChatSession) -> Unit
) : ListAdapter<ChatSession, SessionAdapter.SessionViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemSessionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SessionViewHolder(
        private val binding: ItemSessionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(session: ChatSession) {
            binding.ivMessenger.setImageResource(session.messenger.messengerIconRes())
            binding.tvTitle.text = session.title?.ifBlank {
                session.messengerDisplay ?: session.messenger.replaceFirstChar { it.uppercase() }
            }
            binding.tvSubtitle.text = session.contextSummary?.ifBlank {
                "${session.requestCount} AI requests"
            } ?: "${session.requestCount} AI requests"
            binding.tvTime.text = session.updatedAt.formatApiDate()
            binding.root.setOnClickListener { onClick(session) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChatSession>() {
        override fun areItemsTheSame(old: ChatSession, new: ChatSession) = old.id == new.id
        override fun areContentsTheSame(old: ChatSession, new: ChatSession) = old == new
    }
}
