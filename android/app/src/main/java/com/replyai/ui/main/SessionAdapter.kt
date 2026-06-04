package com.replyai.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.replyai.R
import com.replyai.data.models.ChatSession
import com.replyai.databinding.ItemSessionBinding
import com.replyai.utils.formatApiDate
import com.replyai.utils.messengerIconRes

class SessionAdapter(
    private val onClick: (ChatSession) -> Unit,
    private val onFavorite: (ChatSession) -> Unit
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
                "${session.requestCount} AI запросов"
            } ?: "${session.requestCount} AI запросов"
            binding.tvTime.text = session.updatedAt.formatApiDate()

            val starColor = if (session.isFavorite) R.color.accent else R.color.text_hint
            binding.btnFavorite.setColorFilter(
                ContextCompat.getColor(binding.root.context, starColor)
            )

            binding.root.setOnClickListener { onClick(session) }
            binding.btnFavorite.setOnClickListener { onFavorite(session) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChatSession>() {
        override fun areItemsTheSame(old: ChatSession, new: ChatSession) = old.id == new.id
        override fun areContentsTheSame(old: ChatSession, new: ChatSession) = old == new
    }
}
