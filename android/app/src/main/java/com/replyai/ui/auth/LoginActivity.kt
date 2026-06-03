package com.replyai.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.replyai.databinding.ActivityLoginBinding
import com.replyai.ui.main.MainActivity
import com.replyai.utils.showSnackbar
import com.replyai.viewmodel.AuthViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupValidation()
        setupObservers()
        setupClicks()
    }

    private fun setupValidation() {
        binding.etEmail.doAfterTextChanged { validateForm() }
        binding.etPassword.doAfterTextChanged { validateForm() }
    }

    private fun validateForm() {
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        binding.btnSignIn.isEnabled = email.contains('@') && password.length >= 6
    }

    private fun setupObservers() {
        viewModel.loading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnSignIn.isEnabled = !loading && binding.etEmail.text.toString().contains('@')
        }

        viewModel.error.observe(this) { error ->
            error?.let { binding.root.showSnackbar(it) }
        }

        viewModel.loginSuccess.observe(this) { success ->
            if (success) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }

    private fun setupClicks() {
        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val password = binding.etPassword.text?.toString().orEmpty()
            viewModel.login(email, password)
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
