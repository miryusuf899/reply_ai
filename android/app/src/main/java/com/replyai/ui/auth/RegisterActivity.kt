package com.replyai.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.replyai.databinding.ActivityRegisterBinding
import com.replyai.utils.showSnackbar
import com.replyai.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listOf(
            binding.etFullName, binding.etEmail,
            binding.etPassword, binding.etConfirmPassword
        ).forEach { it.doAfterTextChanged { validate(false) } }

        binding.btnRegister.setOnClickListener {
            if (!validate(true)) return@setOnClickListener
            viewModel.register(
                binding.etEmail.text.toString().trim(),
                binding.etFullName.text.toString().trim(),
                binding.etPassword.text.toString(),
                binding.etConfirmPassword.text.toString()
            )
        }
        binding.tvLogin.setOnClickListener { finish() }

        viewModel.loading.observe(this) {
            binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE
        }
        viewModel.error.observe(this) { it?.let { msg -> binding.root.showSnackbar(msg) } }
        viewModel.registerSuccess.observe(this) {
            if (it) {
                binding.root.showSnackbar("Аккаунт создан")
                finish()
            }
        }
    }

    private fun validate(showErrors: Boolean): Boolean {
        val name = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val p1 = binding.etPassword.text.toString()
        val p2 = binding.etConfirmPassword.text.toString()
        var ok = true
        if (name.length < 2) { if (showErrors) binding.tilFullName.error = "Введите имя"; ok = false }
        else binding.tilFullName.error = null
        if (!email.contains('@')) { if (showErrors) binding.tilEmail.error = "Некорректный email"; ok = false }
        else binding.tilEmail.error = null
        if (p1.length < 8) { if (showErrors) binding.tilPassword.error = "Мин. 8 символов"; ok = false }
        else binding.tilPassword.error = null
        if (p1 != p2) { if (showErrors) binding.tilConfirmPassword.error = "Пароли не совпадают"; ok = false }
        else binding.tilConfirmPassword.error = null
        binding.btnRegister.isEnabled = ok
        return ok
    }
}
