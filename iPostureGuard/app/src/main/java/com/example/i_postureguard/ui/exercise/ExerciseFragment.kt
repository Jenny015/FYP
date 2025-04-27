package com.example.i_postureguard.ui.exercise

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
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

        _binding = FragmentExerciseBinding.inflate(inflater, container, false)
        val root: View = binding.root



        val eyesExerciseButton: ImageButton = binding.eyesExerciseButton

        val neckExerciseButton: ImageButton = binding.neckExerciseButton

        eyesExerciseButton.setOnClickListener {
            // Handle Eyes Exercise button click
            val intent = Intent(activity, FragmentExerciseEyesActivity::class.java)
            startActivity(intent)
        }



        neckExerciseButton.setOnClickListener {
            // Handle Neck Exercise button click
            val intent = Intent(activity, fragment_exercise_neck::class.java)
            startActivity(intent)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
