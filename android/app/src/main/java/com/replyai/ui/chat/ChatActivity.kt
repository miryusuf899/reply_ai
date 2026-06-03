package com.replyai.ui.chat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.replyai.databinding.ActivityChatBinding
import com.replyai.ui.chat.ChatMessageAdapter.ChatListItem
import com.replyai.utils.copyToClipboard
import com.replyai.utils.showSnackbar
import com.replyai.utils.toDisplayTone
import com.replyai.viewmodel.ChatViewModel

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var messageAdapter: ChatMessageAdapter

    private var sessionId: String = ""
    private var selectedTone = "friendly"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionId = intent.getStringExtra(EXTRA_SESSION_ID).orEmpty()
        val title = intent.getStringExtra(EXTRA_SESSION_TITLE).orEmpty()
        binding.toolbar.title = title.ifBlank { "Chat" }
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        setupToneChips()
        setupObservers()
        setupActions()

        viewModel.loadSession(sessionId)
    }

    private fun setupRecyclerView() {
        messageAdapter = ChatMessageAdapter()
        binding.rvMessages.layoutManager = LinearLayoutManager(this)
        binding.rvMessages.adapter = messageAdapter
    }

    private fun setupToneChips() {
        binding.chipFormal.setOnClickListener { selectTone("formal") }
        binding.chipFriendly.setOnClickListener { selectTone("friendly") }
        binding.chipEmpathic.setOnClickListener { selectTone("empathic") }
        selectTone("friendly")
    }

    private fun selectTone(tone: String) {
        selectedTone = tone
        binding.chipFormal.isChecked = tone == "formal"
        binding.chipFriendly.isChecked = tone == "friendly"
        binding.chipEmpathic.isChecked = tone == "empathic"
    }

    private fun setupObservers() {
        viewModel.currentSession.observe(this) { session ->
            session ?: return@observe
            val items = mutableListOf<ChatListItem>()

            session.messages?.forEach { msg ->
                items.add(
                    ChatListItem.Message(
                        content = msg.content,
                        isUser = msg.sender == "me",
                        time = msg.timestamp
                    )
                )
            }

            session.aiRequests?.forEach { request ->
                request.userPrompt?.let { prompt ->
                    items.add(ChatListItem.Message(content = prompt, isUser = true, time = request.createdAt))
                }
                request.response?.generatedText?.let { text ->
                    items.add(
                        ChatListItem.AiCard(
                            text = text,
                            tone = request.tone?.toDisplayTone() ?: "",
                            requestId = request.id
                        )
                    )
                }
            }

            messageAdapter.submitList(items)
        }

        viewModel.loading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnGenerate.isEnabled = !loading
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                binding.root.showSnackbar(it)
                viewModel.clearError()
            }
        }

        viewModel.aiResponse.observe(this) { response ->
            response?.response?.generatedText?.let { text ->
                binding.cardAiResult.visibility = View.VISIBLE
                binding.tvAiResult.text = text
                binding.tvAiTone.text = response.tone?.toDisplayTone() ?: selectedTone.toDisplayTone()
            }
        }
    }

    private fun setupActions() {
        binding.btnGenerate.setOnClickListener {
            val prompt = binding.etPrompt.text?.toString()?.trim().orEmpty()
            if (prompt.isBlank()) {
                binding.root.showSnackbar("Enter what you need help replying to")
                return@setOnClickListener
            }
            viewModel.askAI(sessionId, prompt, selectedTone)
        }

        binding.btnCopy.setOnClickListener {
            val text = binding.tvAiResult.text?.toString().orEmpty()
            if (text.isNotBlank()) copyToClipboard("AI Reply", text)
        }

        binding.btnRegenerate.setOnClickListener {
            val prompt = binding.etPrompt.text?.toString()?.trim().orEmpty()
            if (prompt.isNotBlank()) viewModel.askAI(sessionId, prompt, selectedTone)
        }

        binding.cardToneFormal.setOnClickListener {
            selectTone("formal")
            regenerateIfNeeded()
        }
        binding.cardToneFriendly.setOnClickListener {
            selectTone("friendly")
            regenerateIfNeeded()
        }
        binding.cardToneEmpathic.setOnClickListener {
            selectTone("empathic")
            regenerateIfNeeded()
        }
    }

    private fun regenerateIfNeeded() {
        val prompt = binding.etPrompt.text?.toString()?.trim().orEmpty()
        if (prompt.isNotBlank() && binding.cardAiResult.visibility == View.VISIBLE) {
            viewModel.askAI(sessionId, prompt, selectedTone)
        }
    }

    companion object {
        private const val EXTRA_SESSION_ID = "session_id"
        private const val EXTRA_SESSION_TITLE = "session_title"

        fun newIntent(context: Context, sessionId: String, title: String): Intent {
            return Intent(context, ChatActivity::class.java).apply {
                putExtra(EXTRA_SESSION_ID, sessionId)
                putExtra(EXTRA_SESSION_TITLE, title)
            }
        }
    }
}
