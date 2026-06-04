package com.replyai.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.replyai.databinding.ActivitySplashBinding
import com.replyai.ui.auth.LoginActivity
import com.replyai.ui.main.MainActivity
import com.replyai.utils.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Handler(Looper.getMainLooper()).postDelayed({
            val target = if (tokenManager.isLoggedIn()) MainActivity::class.java else LoginActivity::class.java
            startActivity(Intent(this, target))
            finish()
        }, 1200)
    }
}
