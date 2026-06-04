package com.replyai.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.replyai.BuildConfig
import com.replyai.databinding.ActivityLoginBinding
import com.replyai.ui.main.MainActivity
import com.replyai.utils.showSnackbar
import com.replyai.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    private val googleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(Exception::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                viewModel.googleLogin(idToken)
            } else {
                binding.root.showSnackbar("Не удалось получить Google токен")
            }
        } catch (e: Exception) {
            binding.root.showSnackbar(e.message ?: "Ошибка Google Sign In")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etEmail.doAfterTextChanged { validate() }
        binding.etPassword.doAfterTextChanged { validate() }

        binding.btnSignIn.setOnClickListener {
            viewModel.login(
                binding.etEmail.text.toString().trim(),
                binding.etPassword.text.toString()
            )
        }

        binding.btnGoogle.setOnClickListener { signInWithGoogle() }
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        viewModel.loading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnSignIn.isEnabled = !loading
            binding.btnGoogle.isEnabled = !loading
        }

        viewModel.error.observe(this) { it?.let { binding.root.showSnackbar(it) } }

        viewModel.loginSuccess.observe(this) { success ->
            if (success) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }

    private fun validate() {
        val email = binding.etEmail.text.toString()
        val pass = binding.etPassword.text.toString()
        binding.btnSignIn.isEnabled = email.contains('@') && pass.length >= 6
    }

    private fun signInWithGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(this, gso)
        googleLauncher.launch(client.signInIntent)
    }
}
