package com.replyai.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.replyai.databinding.FragmentProfileBinding
import com.replyai.databinding.ItemPlanBinding
import com.replyai.data.models.SubscriptionPlan
import com.replyai.service.FloatingOverlayService
import com.replyai.ui.auth.LoginActivity
import com.replyai.viewmodel.AuthViewModel
import com.replyai.viewmodel.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val profileViewModel: ProfileViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvPlans.layoutManager = LinearLayoutManager(requireContext())
        val planAdapter = PlanAdapter()
        binding.rvPlans.adapter = planAdapter

        profileViewModel.profile.observe(viewLifecycleOwner) { profile ->
            binding.tvName.text = profile?.fullName ?: "—"
            binding.tvEmail.text = profile?.email ?: "—"
        }

        profileViewModel.subscription.observe(viewLifecycleOwner) { sub ->
            binding.tvPlan.text = sub?.plan?.name ?: "—"
        }

        profileViewModel.usage.observe(viewLifecycleOwner) { usage ->
            val sub = profileViewModel.subscription.value
            val limit = sub?.plan?.dailyRequestLimit ?: 0
            binding.tvUsage.text = if (usage != null) {
                "Сегодня: ${usage.dailyCount} / $limit\nВсего: ${usage.totalRequests}"
            } else "—"
        }

        profileViewModel.plans.observe(viewLifecycleOwner) { planAdapter.submitList(it) }

        profileViewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.shimmer.visibility = if (loading) View.VISIBLE else View.GONE
            if (loading) binding.shimmer.startShimmer() else binding.shimmer.stopShimmer()
        }

        binding.btnLogout.setOnClickListener {
            authViewModel.logout()
            FloatingOverlayService.stop(requireContext())
            startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            requireActivity().finish()
        }
    }

    override fun onResume() {
        super.onResume()
        profileViewModel.loadAll()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class PlanAdapter : androidx.recyclerview.widget.ListAdapter<SubscriptionPlan, PlanAdapter.VH>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<SubscriptionPlan>() {
        override fun areItemsTheSame(a: SubscriptionPlan, b: SubscriptionPlan) = a.id == b.id
        override fun areContentsTheSame(a: SubscriptionPlan, b: SubscriptionPlan) = a == b
    }
) {
    class VH(val binding: ItemPlanBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPlanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val plan = getItem(position)
        holder.binding.tvPlanName.text = plan.name
        holder.binding.tvPlanDetails.text =
            "День: ${plan.dailyRequestLimit} · Месяц: ${plan.monthlyRequestLimit} · \$${plan.priceUsd ?: "0"}"
    }
}
