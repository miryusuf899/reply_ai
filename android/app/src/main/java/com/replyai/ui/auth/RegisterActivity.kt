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

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listOf(
            binding.etFullName,
            binding.etEmail,
            binding.etPassword,
            binding.etConfirmPassword
        ).forEach { it.doAfterTextChanged { validateForm() } }

        viewModel.loading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnRegister.isEnabled = !loading
        }

        viewModel.error.observe(this) { error ->
            error?.let { binding.root.showSnackbar(it) }
        }

        viewModel.registerSuccess.observe(this) { success ->
            if (success) {
                binding.root.showSnackbar("Account created! Please sign in.")
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }

        binding.btnRegister.setOnClickListener {
            if (!validateForm(showErrors = true)) return@setOnClickListener
            viewModel.register(
                binding.etEmail.text.toString().trim(),
                binding.etFullName.text.toString().trim(),
                binding.etPassword.text.toString(),
                binding.etConfirmPassword.text.toString()
            )
        }

        binding.tvLogin.setOnClickListener { finish() }
    }

    private fun validateForm(showErrors: Boolean = false): Boolean {
        val name = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirm = binding.etConfirmPassword.text.toString()

        var valid = true

        if (name.length < 2) {
            if (showErrors) binding.tilFullName.error = "Enter your name"
            valid = false
        } else binding.tilFullName.error = null

        if (!email.contains('@')) {
            if (showErrors) binding.tilEmail.error = "Invalid email"
            valid = false
        } else binding.tilEmail.error = null

        if (password.length < 8) {
            if (showErrors) binding.tilPassword.error = "Min 8 characters"
            valid = false
        } else binding.tilPassword.error = null

        if (password != confirm) {
            if (showErrors) binding.tilConfirmPassword.error = "Passwords don't match"
            valid = false
        } else binding.tilConfirmPassword.error = null

        binding.btnRegister.isEnabled = valid
        return valid
    }
}
