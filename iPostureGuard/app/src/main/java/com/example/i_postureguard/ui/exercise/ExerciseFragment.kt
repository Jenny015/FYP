package com.example.i_postureguard.ui.exercise

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.i_postureguard.databinding.FragmentExerciseBinding

class ExerciseFragment : Fragment() {

    private var _binding: FragmentExerciseBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val exerciseViewModel =
            ViewModelProvider(this).get(ExerciseViewModel::class.java)

        _binding = FragmentExerciseBinding.inflate(inflater, container, false)
        val root: View = binding.root



        val eyesExerciseButton: ImageButton = binding.eyesExerciseButton
        val shoulderExerciseButton: ImageButton = binding.shoulderExerciseButton
        val neckExerciseButton: ImageButton = binding.neckExerciseButton

        eyesExerciseButton.setOnClickListener {
            // Handle Eyes Exercise button click
        }

        shoulderExerciseButton.setOnClickListener {
            // Handle Shoulder Exercise button click
        }

        neckExerciseButton.setOnClickListener {
            // Handle Neck Exercise button click
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
