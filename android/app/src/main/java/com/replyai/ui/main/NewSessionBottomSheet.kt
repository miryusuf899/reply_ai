package com.replyai.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.replyai.databinding.BottomSheetNewSessionBinding

class NewSessionBottomSheet(
    private val onConfirm: (messenger: String, title: String) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetNewSessionBinding? = null
    private val binding get() = _binding!!

    private var selectedMessenger = "telegram"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetNewSessionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.chipTelegram.setOnClickListener { selectMessenger("telegram") }
        binding.chipInstagram.setOnClickListener { selectMessenger("instagram") }
        binding.chipWhatsapp.setOnClickListener { selectMessenger("whatsapp") }

        binding.btnCreate.setOnClickListener {
            val title = binding.etSessionName.text?.toString()?.trim().orEmpty()
            onConfirm(selectedMessenger, title)
            dismiss()
        }

        selectMessenger("telegram")
    }

    private fun selectMessenger(messenger: String) {
        selectedMessenger = messenger
        binding.chipTelegram.isChecked = messenger == "telegram"
        binding.chipInstagram.isChecked = messenger == "instagram"
        binding.chipWhatsapp.isChecked = messenger == "whatsapp"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
