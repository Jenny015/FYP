package com.example.i_postureguard.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ToggleButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.i_postureguard.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val toggleButton: ToggleButton = binding.toggleButton

        // New TextViews
        val textViewTodayPosture: TextView = binding.textViewTodayPosture
        val textViewTodayCount: TextView = binding.textViewTodayCount
        val textViewTodayPercentage: TextView = binding.textViewTodayPercentage
        val textViewWeekCount: TextView = binding.textViewWeekCount
        val textViewWeekPercentage: TextView = binding.textViewWeekPercentage

        // Additional setup for toggleButton if needed

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
