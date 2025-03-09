package com.example.i_postureguard.ui.ranking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.i_postureguard.R

class RankingFragment : Fragment() {
    private lateinit var rankingContainer: LinearLayout
    private var currentRankingType = "Time"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_ranking, container, false)

        rankingContainer = root.findViewById(R.id.rankingContainer)

        val buttonTime: Button = root.findViewById(R.id.button_time)
        val buttonPosture: Button = root.findViewById(R.id.button_posture)
        val buttonSports: Button = root.findViewById(R.id.button_sports)

        buttonTime.setOnClickListener {
            currentRankingType = "Time"
            updateRankingDisplay()
        }

        buttonPosture.setOnClickListener {
            currentRankingType = "Posture"
            updateRankingDisplay()
        }

        buttonSports.setOnClickListener {
            currentRankingType = "Sports"
            updateRankingDisplay()
        }

        // Initialize display
        updateRankingDisplay()

        return root
    }

    private fun updateRankingDisplay() {
        rankingContainer.removeAllViews()
        when (currentRankingType) {
            "Time" -> addRankingViewsForTime()
            "Posture" -> addRankingViewsForPosture()
            "Sports" -> addRankingViewsForSports()
        }
    }

    private fun addRankingViewsForTime() {
        addRankingItem("User 1", "100s", R.drawable.top1)
        addRankingItem("User 2", "120s", R.drawable.top2)
    }

    private fun addRankingViewsForPosture() {
        addRankingItem("User 1", "Good", R.drawable.top1)
        addRankingItem("User 2", "Average", R.drawable.top2)
    }

    private fun addRankingViewsForSports() {
        addRankingItem("User 1", "3 Goals", R.drawable.top1)
        addRankingItem("User 2", "2 Goals", R.drawable.top2)
    }

    private fun addRankingItem(userName: String, userMark: String, imageResId: Int) {
        val inflater = LayoutInflater.from(context)
        val rankingView: View = inflater.inflate(R.layout.ranking_item, rankingContainer, false)

        val imageView: ImageView = rankingView.findViewById(R.id.top1_image)
        val nameTextView: TextView = rankingView.findViewById(R.id.user1_name)
        val markTextView: TextView = rankingView.findViewById(R.id.user1_mark)

        imageView.setImageResource(imageResId)
        nameTextView.text = userName
        markTextView.text = userMark

        rankingContainer.addView(rankingView)
    }
}