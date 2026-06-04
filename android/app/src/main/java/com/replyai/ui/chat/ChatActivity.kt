package com.replyai.ui.chat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.replyai.data.models.RequestTypes
import com.replyai.data.models.ToneChoices
import com.replyai.databinding.ActivityChatBinding
import com.replyai.ui.chat.ChatMessageAdapter.ChatListItem
import com.replyai.utils.copyToClipboard
import com.replyai.utils.showSnackbar
import com.replyai.utils.toDisplayTone
import com.replyai.viewmodel.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var messageAdapter: ChatMessageAdapter

    private var sessionId: String = ""
    /** API tone value: formal | friendly | neutral | assertive | empathetic */
    private var selectedTone: String = ToneChoices.FRIENDLY

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
        binding.chipFormal.setOnClickListener { selectTone(ToneChoices.FORMAL) }
        binding.chipFriendly.setOnClickListener { selectTone(ToneChoices.FRIENDLY) }
        binding.chipEmpathic.setOnClickListener { selectTone(ToneChoices.EMPATHETIC) }
        selectTone(ToneChoices.FRIENDLY)
    }

    private fun selectTone(apiTone: String) {
        selectedTone = ToneChoices.toApiValue(apiTone)
        binding.chipFormal.isChecked = selectedTone == ToneChoices.FORMAL
        binding.chipFriendly.isChecked = selectedTone == ToneChoices.FRIENDLY
        binding.chipEmpathic.isChecked = selectedTone == ToneChoices.EMPATHETIC
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
                request.extractGeneratedText()?.let { text ->
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
            val text = response?.extractGeneratedText()
            if (!text.isNullOrBlank()) {
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
                binding.root.showSnackbar(getString(com.replyai.R.string.whats_hard))
                return@setOnClickListener
            }
            viewModel.askAI(
                sessionId = sessionId,
                userPrompt = prompt,
                tone = selectedTone,
                requestType = RequestTypes.REPLY_HELP
            )
        }

        binding.btnCopy.setOnClickListener {
            val text = binding.tvAiResult.text?.toString().orEmpty()
            if (text.isNotBlank()) copyToClipboard("AI Reply", text)
        }

        binding.btnRegenerate.setOnClickListener {
            val prompt = binding.etPrompt.text?.toString()?.trim().orEmpty()
            if (prompt.isNotBlank()) {
                viewModel.askAI(sessionId, prompt, selectedTone, RequestTypes.REPLY_HELP)
            }
        }

        binding.cardToneFormal.setOnClickListener {
            selectTone(ToneChoices.FORMAL)
            regenerateIfNeeded()
        }
        binding.cardToneFriendly.setOnClickListener {
            selectTone(ToneChoices.FRIENDLY)
            regenerateIfNeeded()
        }
        binding.cardToneEmpathic.setOnClickListener {
            selectTone(ToneChoices.EMPATHETIC)
            regenerateIfNeeded()
        }
    }

    private fun regenerateIfNeeded() {
        val prompt = binding.etPrompt.text?.toString()?.trim().orEmpty()
        if (prompt.isNotBlank() && binding.cardAiResult.visibility == View.VISIBLE) {
            viewModel.askAI(sessionId, prompt, selectedTone, RequestTypes.REPLY_HELP)
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
