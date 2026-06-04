package com.replyai.ui.main

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.replyai.R
import com.replyai.databinding.ActivityMainBinding
import com.replyai.viewmodel.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val chatViewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHost.navController
        binding.bottomNav.setupWithNavController(navController)

        binding.fabNewSession.setOnClickListener {
            if (navController.currentDestination?.id != R.id.chatsFragment) {
                navController.navigate(R.id.chatsFragment)
            }
            NewSessionBottomSheet { messenger, title ->
                chatViewModel.createSession(messenger, title)
            }.show(supportFragmentManager, "new_session")
        }

        chatViewModel.sessionCreated.observe(this) { session ->
            session?.let {
                startActivity(
                    com.replyai.ui.chat.ChatActivity.newIntent(this, it.id, it.title ?: "")
                )
                chatViewModel.clearSessionCreated()
            }
        }
    }
}
