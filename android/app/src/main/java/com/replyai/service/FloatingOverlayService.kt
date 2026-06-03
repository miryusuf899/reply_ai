package com.replyai.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.replyai.R
import com.replyai.databinding.OverlayBubbleBinding
import com.replyai.databinding.OverlayPanelBinding
import com.replyai.data.models.AskRequest
import com.replyai.data.repository.ChatRepository
import com.replyai.ui.main.MainActivity
import com.replyai.utils.copyToClipboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class FloatingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var panelView: View? = null
    private var bubbleBinding: OverlayBubbleBinding? = null
    private var panelBinding: OverlayPanelBinding? = null
    private var isExpanded = false

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val chatRepository = ChatRepository()

    private var layoutParams: WindowManager.LayoutParams? = null
    private var selectedTone = "friendly"
    private var overlaySessionId: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(NOTIFICATION_ID, createNotification())
        showBubble()
        ensureOverlaySession()
    }

    override fun onDestroy() {
        removeBubble()
        removePanel()
        serviceScope.cancel()
        super.onDestroy()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showBubble() {
        bubbleBinding = OverlayBubbleBinding.inflate(LayoutInflater.from(this))
        bubbleView = bubbleBinding!!.root

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 300
        }

        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false

        bubbleView!!.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams!!.x
                    initialY = layoutParams!!.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (abs(dx) > 5 || abs(dy) > 5) moved = true
                    layoutParams!!.x = initialX + dx
                    layoutParams!!.y = initialY + dy
                    windowManager.updateViewLayout(bubbleView, layoutParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) togglePanel()
                    snapToEdge()
                    true
                }
                else -> false
            }
        }

        windowManager.addView(bubbleView, layoutParams)
    }

    private fun snapToEdge() {
        val display = resources.displayMetrics
        val centerX = display.widthPixels / 2
        layoutParams?.let { params ->
            params.x = if (params.x + (bubbleView?.width ?: 0) / 2 < centerX) 0
            else display.widthPixels - (bubbleView?.width ?: 120)
            windowManager.updateViewLayout(bubbleView, params)
        }
    }

    private fun togglePanel() {
        if (isExpanded) collapsePanel() else expandPanel()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun expandPanel() {
        if (panelView != null) return
        isExpanded = true
        bubbleView?.visibility = View.GONE

        panelBinding = OverlayPanelBinding.inflate(LayoutInflater.from(this))
        panelView = panelBinding!!.root

        val panelParams = WindowManager.LayoutParams(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        setupPanelActions()

        panelBinding!!.btnClose.setOnClickListener { collapsePanel() }

        windowManager.addView(panelView, panelParams)
    }

    private fun setupPanelActions() {
        val binding = panelBinding ?: return

        binding.chipFormal.setOnClickListener { selectOverlayTone("formal") }
        binding.chipFriendly.setOnClickListener { selectOverlayTone("friendly") }
        binding.chipEmpathic.setOnClickListener { selectOverlayTone("empathic") }
        selectOverlayTone("friendly")

        binding.btnGenerate.setOnClickListener {
            val prompt = binding.etInput.text?.toString()?.trim().orEmpty()
            if (prompt.isBlank()) {
                Toast.makeText(this, "Enter your message context", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            generateReply(prompt)
        }

        binding.btnCopy.setOnClickListener {
            val text = binding.tvResult.text?.toString().orEmpty()
            if (text.isNotBlank()) copyToClipboard("AI Reply", text)
        }

        binding.btnInsert.setOnClickListener {
            val text = binding.tvResult.text?.toString().orEmpty()
            if (text.isNotBlank()) {
                ReplyAIAccessibilityService.insertText(text)
                collapsePanel()
            }
        }
    }

    private fun selectOverlayTone(tone: String) {
        selectedTone = tone
        panelBinding?.chipFormal?.isChecked = tone == "formal"
        panelBinding?.chipFriendly?.isChecked = tone == "friendly"
        panelBinding?.chipEmpathic?.isChecked = tone == "empathic"
    }

    private fun generateReply(prompt: String) {
        val sessionId = overlaySessionId
        if (sessionId == null) {
            Toast.makeText(this, "No active session", Toast.LENGTH_SHORT).show()
            return
        }

        panelBinding?.progressBar?.visibility = View.VISIBLE
        panelBinding?.btnGenerate?.isEnabled = false

        serviceScope.launch {
            val result = withContext(Dispatchers.IO) {
                chatRepository.askAI(
                    sessionId = sessionId,
                    userPrompt = prompt,
                    tone = selectedTone,
                    requestType = AskRequest.REQUEST_TYPE_REPLY_HELP
                )
            }
            panelBinding?.progressBar?.visibility = View.GONE
            panelBinding?.btnGenerate?.isEnabled = true

            result.onSuccess { response ->
                val text = response.response?.generatedText.orEmpty()
                panelBinding?.tvResult?.text = text
                panelBinding?.cardResult?.visibility = if (text.isNotBlank()) View.VISIBLE else View.GONE
            }.onFailure {
                Toast.makeText(this@FloatingOverlayService, it.message ?: "Error", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun ensureOverlaySession() {
        serviceScope.launch {
            val sessions = withContext(Dispatchers.IO) {
                chatRepository.getSessions().getOrNull()
            }
            overlaySessionId = sessions?.firstOrNull()?.id
            if (overlaySessionId == null) {
                val created = withContext(Dispatchers.IO) {
                    chatRepository.createSession("telegram", "Overlay Quick Reply").getOrNull()
                }
                overlaySessionId = created?.id
            }
        }
    }

    private fun collapsePanel() {
        isExpanded = false
        removePanel()
        bubbleView?.visibility = View.VISIBLE
    }

    private fun removeBubble() {
        bubbleView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        bubbleView = null
        bubbleBinding = null
    }

    private fun removePanel() {
        panelView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        panelView = null
        panelBinding = null
    }

    private fun overlayWindowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ReplyAI Overlay",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ReplyAI")
            .setContentText("Floating assistant is active")
            .setSmallIcon(R.drawable.ic_chat)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "replyai_overlay"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingOverlayService::class.java))
        }
    }
}
