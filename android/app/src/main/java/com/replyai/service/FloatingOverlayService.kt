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
import com.replyai.data.models.RequestTypes
import com.replyai.data.models.ToneChoices
import com.replyai.data.repository.ChatRepository
import com.replyai.databinding.OverlayBubbleBinding
import com.replyai.databinding.OverlayGenerateBinding
import com.replyai.databinding.OverlayMenuBinding
import com.replyai.ui.main.MainActivity
import com.replyai.utils.copyToClipboard
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class FloatingOverlayService : Service() {

    @Inject lateinit var overlayWorkflow: OverlayWorkflow
    @Inject lateinit var chatRepository: ChatRepository

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var overlayView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var selectedRequestType = RequestTypes.REPLY_HELP
    /** Backend tone key: formal | friendly | neutral | assertive | empathetic */
    private var selectedTone = ToneChoices.FRIENDLY
    private var lastResponseId: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        showBubble()
    }

    override fun onDestroy() {
        removeBubble()
        removeOverlay()
        scope.cancel()
        super.onDestroy()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showBubble() {
        val binding = OverlayBubbleBinding.inflate(LayoutInflater.from(this))
        bubbleView = binding.root
        binding.ivBubble.setImageResource(R.drawable.ic_reply_ai)

        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 400
        }

        var initX = 0
        var initY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false

        bubbleView!!.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initX = bubbleParams!!.x
                    initY = bubbleParams!!.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (abs(dx) > 4 || abs(dy) > 4) moved = true
                    bubbleParams!!.x = initX + dx
                    bubbleParams!!.y = initY + dy
                    windowManager.updateViewLayout(bubbleView, bubbleParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) showMenuSheet() else snapToEdge()
                    true
                }
                else -> false
            }
        }

        windowManager.addView(bubbleView, bubbleParams)
    }

    private fun snapToEdge() {
        val w = resources.displayMetrics.widthPixels
        bubbleParams?.let { p ->
            p.x = if (p.x < w / 2) 0 else w - (bubbleView?.width ?: 140)
            windowManager.updateViewLayout(bubbleView, p)
        }
    }

    private fun showMenuSheet() {
        removeOverlay()
        bubbleView?.visibility = View.GONE

        val binding = OverlayMenuBinding.inflate(LayoutInflater.from(android.view.ContextThemeWrapper(this, R.style.Theme_ReplyAI)))
        overlayView = binding.root

        binding.btnCloseMenu.setOnClickListener { closeOverlay() }
        binding.btnReplyHelp.setOnClickListener { openGenerateSheet(RequestTypes.REPLY_HELP) }
        binding.btnTranslate.setOnClickListener { openGenerateSheet(RequestTypes.TRANSLATION) }
        binding.btnToneChange.setOnClickListener { openGenerateSheet(RequestTypes.TONE_CHANGE) }
        binding.btnAnalyze.setOnClickListener { openGenerateSheet(RequestTypes.ANALYZE) }

        addOverlay(binding.root)
    }

    private fun openGenerateSheet(requestType: String) {
        selectedRequestType = requestType
        removeOverlay()

        val binding = OverlayGenerateBinding.inflate(LayoutInflater.from(android.view.ContextThemeWrapper(this, R.style.Theme_ReplyAI)))
        overlayView = binding.root

        binding.tvActionTitle.text = overlayWorkflow.requestTypeLabel(requestType)
        binding.tvContextStatus.text = getString(R.string.messages_syncing)

        setupToneChips(binding)
        selectToneChip(binding, ToneChoices.FRIENDLY)

        scope.launch {
            val count = withContext(Dispatchers.IO) {
                ReplyAIAccessibilityService.forceRefreshMessages()
            }
            binding.tvContextStatus.text = getString(R.string.messages_synced, count)
        }

        binding.btnBack.setOnClickListener { showMenuSheet() }
        binding.btnGenerate.setOnClickListener {
            val prompt = binding.etPrompt.text?.toString()?.trim().orEmpty()
            if (prompt.isBlank()) {
                Toast.makeText(this, getString(R.string.overlay_prompt_hint), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            generate(binding, prompt)
        }

        binding.btnCopy.setOnClickListener {
            val text = binding.tvResult.text?.toString().orEmpty()
            if (text.isNotBlank()) copyToClipboard("Reply AI", text)
        }

        binding.btnLike.setOnClickListener { sendFeedback(1) }
        binding.btnDislike.setOnClickListener { sendFeedback(-1) }
        binding.btnFavorite.setOnClickListener { saveFavorite() }

        addOverlay(binding.root)
    }

    private fun generate(binding: OverlayGenerateBinding, prompt: String) {
        binding.loadingContainer.visibility = View.VISIBLE
        binding.lottieLoading.playAnimation()
        binding.btnGenerate.isEnabled = false
        binding.cardResult.visibility = View.GONE

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                overlayWorkflow.run(
                    requestType = selectedRequestType,
                    userPrompt = prompt,
                    tone = selectedTone
                )
            }
            binding.loadingContainer.visibility = View.GONE
            binding.lottieLoading.cancelAnimation()
            binding.btnGenerate.isEnabled = true

            result.onSuccess { overlayResult ->
                lastResponseId = overlayResult.responseId
                binding.tvResult.text = overlayResult.generatedText
                binding.cardResult.visibility = View.VISIBLE
                // Скрываем overlay и вставляем текст с задержкой
                val generatedText = overlayResult.generatedText
                removeOverlay()
                delay(300)
                val inserted = ReplyAIAccessibilityService.insertText(generatedText)
                val msg = if (inserted) {
                    getString(R.string.text_injected)
                } else {
                    getString(R.string.insert_failed)
                }
                Toast.makeText(this@FloatingOverlayService, msg, Toast.LENGTH_LONG).show()
            }.onFailure {
                Toast.makeText(
                    this@FloatingOverlayService,
                    it.message ?: "Ошибка",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun sendFeedback(value: Int) {
        val id = lastResponseId ?: return
        scope.launch {
            chatRepository.sendFeedback(id, feedback = value)
                .onSuccess { Toast.makeText(this@FloatingOverlayService, "👍", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(this@FloatingOverlayService, it.message, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun saveFavorite() {
        val id = lastResponseId ?: return
        scope.launch {
            chatRepository.sendFeedback(id, isFavorite = true)
                .onSuccess {
                    Toast.makeText(this@FloatingOverlayService, getString(R.string.favorite), Toast.LENGTH_SHORT).show()
                }
                .onFailure { Toast.makeText(this@FloatingOverlayService, it.message, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun setupToneChips(binding: OverlayGenerateBinding) {
        binding.chipFormal.setOnClickListener { selectToneChip(binding, ToneChoices.FORMAL) }
        binding.chipFriendly.setOnClickListener { selectToneChip(binding, ToneChoices.FRIENDLY) }
        binding.chipNeutral.setOnClickListener { selectToneChip(binding, ToneChoices.NEUTRAL) }
        binding.chipAssertive.setOnClickListener { selectToneChip(binding, ToneChoices.ASSERTIVE) }
        binding.chipEmpathic.setOnClickListener { selectToneChip(binding, ToneChoices.EMPATHETIC) }
    }

    private fun selectToneChip(binding: OverlayGenerateBinding, apiTone: String) {
        selectedTone = ToneChoices.toApiValue(apiTone)
        binding.chipFormal.isChecked = selectedTone == ToneChoices.FORMAL
        binding.chipFriendly.isChecked = selectedTone == ToneChoices.FRIENDLY
        binding.chipNeutral.isChecked = selectedTone == ToneChoices.NEUTRAL
        binding.chipAssertive.isChecked = selectedTone == ToneChoices.ASSERTIVE
        binding.chipEmpathic.isChecked = selectedTone == ToneChoices.EMPATHETIC
    }

    private fun addOverlay(view: View) {
        val params = WindowManager.LayoutParams(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }
        windowManager.addView(view, params)
    }

    private fun closeOverlay() {
        removeOverlay()
        bubbleView?.visibility = View.VISIBLE
    }

    private fun removeOverlay() {
        overlayView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        overlayView = null
    }

    private fun removeBubble() {
        bubbleView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        bubbleView = null
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Reply AI", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Reply AI")
            .setContentText("Плавающий ассистент активен")
            .setSmallIcon(R.drawable.ic_reply_ai)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "replyai_overlay"
        private const val NOTIFICATION_ID = 42

        fun start(context: Context) {
            val i = Intent(context, FloatingOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingOverlayService::class.java))
        }
    }
}
