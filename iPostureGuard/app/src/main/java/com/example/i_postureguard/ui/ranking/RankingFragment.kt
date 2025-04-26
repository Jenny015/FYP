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
import com.example.i_postureguard.DailyData
import com.example.i_postureguard.R
import com.example.i_postureguard.User
import com.example.i_postureguard.Utils
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class RankingFragment : Fragment() {
    private var rankingContainer: LinearLayout? = null
    private var currentRankingType = "Time"
    private var currentUserName: String? = null
    private var currentUserData: Map<String, DailyData>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root: View = inflater.inflate(R.layout.fragment_ranking, container, false)

        rankingContainer = root.findViewById(R.id.rankingContainer)

        val buttonTime = root.findViewById<Button>(R.id.button_time)
        val buttonPosture = root.findViewById<Button>(R.id.button_posture)
        val buttonSports = root.findViewById<Button>(R.id.button_sports)

        buttonTime.setOnClickListener { v: View? ->
            currentRankingType = "Time"
            updateRankingDisplay()
        }

        buttonPosture.setOnClickListener { v: View? ->
            currentRankingType = "Posture"
            updateRankingDisplay()
        }

        buttonSports.setOnClickListener { v: View? ->
            currentRankingType = "Sports"
            updateRankingDisplay()
        }

        fetchCurrentUserData()

        return root
    }

    private fun fetchCurrentUserData() {
        Utils.getDailyDataFromDB(context, object : Utils.DataCallback {
            override fun onSuccess(data: Map<String, DailyData>) {
                // Handle the User object here
                currentUserData = data
                Utils.getUserProfileFromDB(context, object : Utils.UserCallback {
                    override fun onSuccess(user: User) {
                        // Handle the User object here
                        currentUserName = user.name
                        updateRankingDisplay()
                    }

                    override fun onFailure(error: String?) {
                        // Handle the error here
                        System.err.println("Error: $error");
                    }
                })
            }

            override fun onFailure(errorMessage: String) {
                // Handle the error here
                System.err.println("Error: $errorMessage")
            }
        })
    }

    private fun updateRankingDisplay() {
        rankingContainer!!.removeAllViews()
        if (currentUserData == null || currentUserData!!.isEmpty()) {
            addRankingItem("No Data", "No data available", R.drawable.top1)
            return
        }

        val latestDate = getLatestDate
        if (latestDate != null && currentUserData!!.containsKey(latestDate)) {
            val dailyData = currentUserData!![latestDate]
            when (currentRankingType) {
                "Time" -> {
                    val timeValue =
                        if (dailyData!!.time != 0) dailyData.time else dailyData.duration
                    addRankingItem(
                        currentUserName!!+ " (" + dailyData.date + ")",
                        dailyData!!.time.toString() + "s",
                        R.drawable.top1
                    )
                }

                "Posture" -> {
                    val postureCount = if (dailyData!!.posture != null) { dailyData.posture.sum()} else { 0 }
                    addRankingItem(currentUserName!! + " (" + dailyData.date + ")",
                        "$postureCount times",
                        R.drawable.top1
                    )
                }

                "Sports" -> {
                    val exerciseTotal = if (dailyData!!.exercise != null) {dailyData.exercise.sum()} else { 0 }
                    addRankingItem(
                        currentUserName!!+ " (" + dailyData.date + ")",
                        dailyData.sports.toString() + "s (Exercise: " + exerciseTotal + "s)",
                        R.drawable.top1
                    )
                }
            }
        } else {
            addRankingItem("No Data", "No data for $currentRankingType", R.drawable.top1)
        }
    }

    private val getLatestDate: String?
        get() {
            if (currentUserData == null || currentUserData!!.isEmpty()) {
                return null
            }

            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            return currentUserData!!.keys.stream()
                .max { date1: String?, date2: String? ->
                    try {
                        return@max sdf.parse(date1).compareTo(sdf.parse(date2))
                    } catch (e: ParseException) {
                        e.printStackTrace()
                        return@max 0
                    }
                }
                .orElse(null)
        }

    private fun addRankingItem(userName: String, userMark: String, imageResId: Int) {
        val rankingView =
            LayoutInflater.from(context).inflate(R.layout.ranking_item, rankingContainer, false)

        val imageView = rankingView.findViewById<ImageView>(R.id.top1_image)
        val nameTextView = rankingView.findViewById<TextView>(R.id.user1_name)
        val markTextView = rankingView.findViewById<TextView>(R.id.user1_mark)

        imageView.setImageResource(imageResId)
        nameTextView.text = userName
        markTextView.text = userMark

        rankingContainer!!.addView(rankingView)
    }
}