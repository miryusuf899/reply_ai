package com.replyai.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.replyai.databinding.FragmentChatsBinding
import com.replyai.ui.chat.ChatActivity
import com.replyai.viewmodel.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatsFragment : Fragment() {

    private var _binding: FragmentChatsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by activityViewModels()
    private lateinit var adapter: SessionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = SessionAdapter(
            onClick = { session ->
                startActivity(
                    ChatActivity.newIntent(requireContext(), session.id, session.title ?: "")
                )
            },
            onFavorite = { session ->
                viewModel.toggleFavorite(session.id)
            }
        )
        binding.rvSessions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSessions.adapter = adapter

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                adapter.currentList.getOrNull(vh.bindingAdapterPosition)?.let {
                    viewModel.deleteSession(it.id)
                }
            }
        }).attachToRecyclerView(binding.rvSessions)

        binding.swipeRefresh.setOnRefreshListener { viewModel.loadSessions() }

        viewModel.sessions.observe(viewLifecycleOwner) { list ->
            binding.shimmer.visibility = View.GONE
            binding.swipeRefresh.isRefreshing = false
            adapter.submitList(list)
            binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            if (loading && adapter.currentList.isEmpty()) {
                binding.shimmer.visibility = View.VISIBLE
                binding.shimmer.startShimmer()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadSessions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
