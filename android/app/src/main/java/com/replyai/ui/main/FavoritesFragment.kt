package com.replyai.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.replyai.databinding.FragmentFavoritesBinding
import com.replyai.databinding.ItemFavoriteResponseBinding
import com.replyai.viewmodel.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionAdapter = SessionAdapter(onClick = {}, onFavorite = {})
        binding.rvFavoriteSessions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFavoriteSessions.adapter = sessionAdapter

        val responseAdapter = FavoriteResponseAdapter()
        binding.rvFavoriteResponses.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFavoriteResponses.adapter = responseAdapter

        viewModel.favoriteSessions.observe(viewLifecycleOwner) {
            sessionAdapter.submitList(it)
        }
        viewModel.favoriteResponses.observe(viewLifecycleOwner) {
            responseAdapter.submitList(it)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.shimmer.visibility = View.VISIBLE
        binding.shimmer.startShimmer()
        viewModel.loadFavorites()
        binding.shimmer.stopShimmer()
        binding.shimmer.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class FavoriteResponseAdapter :
    androidx.recyclerview.widget.ListAdapter<
        com.replyai.data.models.FavoriteAIResponse,
        FavoriteResponseAdapter.VH
        >(object : androidx.recyclerview.widget.DiffUtil.ItemCallback<com.replyai.data.models.FavoriteAIResponse>() {
        override fun areItemsTheSame(a: com.replyai.data.models.FavoriteAIResponse, b: com.replyai.data.models.FavoriteAIResponse) = a.id == b.id
        override fun areContentsTheSame(a: com.replyai.data.models.FavoriteAIResponse, b: com.replyai.data.models.FavoriteAIResponse) = a == b
    }) {

    class VH(val binding: ItemFavoriteResponseBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemFavoriteResponseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.binding.tvText.text = getItem(position).generatedText
    }
}
