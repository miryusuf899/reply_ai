package com.replyai.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.replyai.R
import com.replyai.databinding.ActivityMainBinding
import com.replyai.ui.chat.ChatActivity
import com.replyai.ui.settings.SettingsActivity
import com.replyai.utils.showSnackbar
import com.replyai.viewmodel.ChatViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: SessionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSwipeToDelete()
        setupObservers()
        setupNavigation()

        binding.fabNewSession.setOnClickListener { showNewSessionDialog() }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadSessions()
    }

    private fun setupRecyclerView() {
        adapter = SessionAdapter { session ->
            startActivity(ChatActivity.newIntent(this, session.id, session.title ?: ""))
        }
        binding.rvSessions.layoutManager = LinearLayoutManager(this)
        binding.rvSessions.adapter = adapter
    }

    private fun setupSwipeToDelete() {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val session = adapter.currentList.getOrNull(position) ?: return
                viewModel.deleteSession(session.id)
                Snackbar.make(binding.root, "Session deleted", Snackbar.LENGTH_SHORT)
                    .setAction("Undo") { viewModel.loadSessions() }
                    .show()
            }
        }).attachToRecyclerView(binding.rvSessions)
    }

    private fun setupObservers() {
        viewModel.sessions.observe(this) { sessions ->
            adapter.submitList(sessions)
            binding.emptyState.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.loading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                binding.root.showSnackbar(it)
                viewModel.clearError()
            }
        }

        viewModel.sessionCreated.observe(this) { session ->
            session?.let {
                startActivity(ChatActivity.newIntent(this, it.id, it.title ?: ""))
                viewModel.clearSessionCreated()
            }
        }
    }

    private fun setupNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    binding.rvSessions.visibility = View.VISIBLE
                    binding.emptyState.visibility =
                        if (adapter.currentList.isEmpty()) View.VISIBLE else View.GONE
                    true
                }
                R.id.nav_history -> {
                    binding.root.showSnackbar("History — favorite sessions coming soon")
                    false
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }

    private fun showNewSessionDialog() {
        NewSessionBottomSheet { messenger, title ->
            viewModel.createSession(messenger, title)
        }.show(supportFragmentManager, "new_session")
    }
}
