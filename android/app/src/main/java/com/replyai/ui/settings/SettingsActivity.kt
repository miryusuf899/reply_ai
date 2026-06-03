package com.replyai.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.replyai.databinding.ActivitySettingsBinding
import com.replyai.service.FloatingOverlayService
import com.replyai.ui.auth.LoginActivity
import com.replyai.utils.TokenManager
import com.replyai.utils.showSnackbar
import com.replyai.viewmodel.AuthViewModel

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: AuthViewModel by viewModels()

    private var selectedTone = "neutral"
    private var selectedLanguage = "ru"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.tvEmail.text = TokenManager.getInstance().userEmail ?: "—"
        binding.tvUsername.text = TokenManager.getInstance().userName ?: "—"

        setupToneChips()
        setupLanguageSpinner()
        setupObservers()
        setupActions()

        viewModel.loadSettings()
        viewModel.loadProfile()
    }

    private fun setupToneChips() {
        binding.chipToneFormal.setOnClickListener { selectTone("formal") }
        binding.chipToneFriendly.setOnClickListener { selectTone("friendly") }
        binding.chipToneEmpathic.setOnClickListener { selectTone("empathetic") }
        binding.chipToneNeutral.setOnClickListener { selectTone("neutral") }
    }

    private fun selectTone(tone: String) {
        selectedTone = tone
        binding.chipToneFormal.isChecked = tone == "formal"
        binding.chipToneFriendly.isChecked = tone == "friendly"
        binding.chipToneEmpathic.isChecked = tone == "empathetic"
        binding.chipToneNeutral.isChecked = tone == "neutral"
        viewModel.updateSettings(selectedTone, selectedLanguage)
    }

    private fun setupLanguageSpinner() {
        val languages = listOf("ru" to "Russian", "en" to "English", "tg" to "Tajik")
        binding.spinnerLanguage.adapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            languages.map { it.second }
        )
        binding.spinnerLanguage.onItemSelectedListener = object :
            android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, pos: Int, id: Long) {
                selectedLanguage = languages[pos].first
                viewModel.updateSettings(selectedTone, selectedLanguage)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupObservers() {
        viewModel.settings.observe(this) { settings ->
            settings ?: return@observe
            selectTone(settings.defaultTone)
            selectedLanguage = settings.defaultLanguage
        }

        viewModel.profile.observe(this) { profile ->
            profile ?: return@observe
            binding.tvEmail.text = profile.email
            binding.tvUsername.text = profile.fullName ?: profile.email
        }

        viewModel.loading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { error ->
            error?.let { binding.root.showSnackbar(it) }
        }
    }

    private fun setupActions() {
        binding.btnOverlayPermission.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                binding.root.showSnackbar("Overlay permission already granted")
            }
        }

        binding.btnStartOverlay.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                binding.root.showSnackbar("Grant overlay permission first")
                return@setOnClickListener
            }
            FloatingOverlayService.start(this)
            binding.root.showSnackbar("Floating bubble started")
        }

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
            FloatingOverlayService.stop(this)
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }
}
