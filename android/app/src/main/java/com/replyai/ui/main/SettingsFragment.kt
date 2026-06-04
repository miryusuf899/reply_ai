package com.replyai.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.chip.Chip
import com.replyai.R
import com.replyai.data.api.ApiService
import com.replyai.data.models.SupportedLanguage
import com.replyai.data.models.UserSettingsPatch
import com.replyai.databinding.FragmentSettingsBinding
import com.replyai.service.FloatingOverlayService
import com.replyai.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by activityViewModels()

    @Inject lateinit var api: ApiService

    private var languages = listOf<SupportedLanguage>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        addToneChips()
        addMessengerChips()

        binding.btnOverlayPermission.setOnClickListener {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${requireContext().packageName}")
                )
            )
        }

        binding.btnStartOverlay.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !Settings.canDrawOverlays(requireContext())
            ) return@setOnClickListener
            FloatingOverlayService.start(requireContext())
        }

        binding.btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        viewModel.settings.observe(viewLifecycleOwner) { settings ->
            settings ?: return@observe
            selectToneChip(settings.defaultTone)
            selectMessengerChip(settings.preferredMessenger ?: "")
        }

        loadLanguages()
        viewModel.loadSettings()
    }

    private fun loadLanguages() {
        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { api.getLanguages() }
            }
            result.getOrNull()?.body()?.let { list ->
                languages = list
                binding.spinnerLanguage.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    list.map { it.nativeName }
                )
                binding.spinnerLanguage.setOnItemSelectedListener(
                    object : android.widget.AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, i: Int, id: Long) {
                            viewModel.patchSettings(
                                UserSettingsPatch(defaultLanguage = languages[i].code)
                            )
                        }
                        override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
                    }
                )
            }
        }
    }

    private fun addToneChips() {
        listOf(
            com.replyai.data.models.ToneChoices.FORMAL to "Формальный",
            com.replyai.data.models.ToneChoices.FRIENDLY to "Дружеский",
            com.replyai.data.models.ToneChoices.NEUTRAL to "Нейтральный",
            com.replyai.data.models.ToneChoices.ASSERTIVE to "Уверенный",
            com.replyai.data.models.ToneChoices.EMPATHETIC to "Эмпатичный"
        ).forEach { (value, label) ->
            val chip = Chip(requireContext()).apply {
                text = label
                isCheckable = true
                setOnClickListener {
                    viewModel.patchSettings(UserSettingsPatch(defaultTone = value))
                }
            }
            chip.tag = value
            binding.chipGroupTone.addView(chip)
        }
    }

    private fun addMessengerChips() {
        listOf("telegram" to "Telegram", "instagram" to "Instagram", "whatsapp" to "WhatsApp")
            .forEach { (value, label) ->
                val chip = Chip(requireContext()).apply {
                    text = label
                    isCheckable = true
                    setOnClickListener {
                        viewModel.patchSettings(UserSettingsPatch(preferredMessenger = value))
                    }
                }
                chip.tag = value
                binding.chipGroupMessenger.addView(chip)
            }
    }

    private fun selectToneChip(tone: String) {
        for (i in 0 until binding.chipGroupTone.childCount) {
            val chip = binding.chipGroupTone.getChildAt(i) as Chip
            chip.isChecked = chip.tag == tone
        }
    }

    private fun selectMessengerChip(messenger: String) {
        for (i in 0 until binding.chipGroupMessenger.childCount) {
            val chip = binding.chipGroupMessenger.getChildAt(i) as Chip
            chip.isChecked = chip.tag == messenger
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
