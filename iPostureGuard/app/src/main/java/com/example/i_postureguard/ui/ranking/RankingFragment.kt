package com.example.i_postureguard.ui.ranking

import android.os.Bundle
import android.util.Log
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.HashMap
import java.util.Locale

class RankingFragment : Fragment() {
    private var rankingContainer: LinearLayout? = null
    private var currentRankingType = "Time"
    private var currentUserData: User? = null
    private var databaseReference: DatabaseReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseReference = FirebaseDatabase.getInstance().getReference("users")
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
        val phone = Utils.getString(requireContext(), "phone", "")
        Log.d("RankingFragment", "Phone: $phone")

        if (phone.isNotEmpty()) {
            databaseReference!!.child(phone).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Log.d("RankingFragment", "No user data found for phone: $phone")
                        addRankingItem("No Data", "Please add some data", R.drawable.top1)
                        return
                    }

                    Log.d("RankingFragment", "Snapshot exists: ${snapshot.value}")
                    currentUserData = snapshot.getValue(User::class.java)
                    if (currentUserData != null) {
                        Log.d("RankingFragment", "User data loaded: ${currentUserData!!.name}")
                        Log.d("RankingFragment", "Data map: ${currentUserData!!.data}")
                        if (currentUserData!!.data != null && currentUserData!!.data.isNotEmpty()) {
                            updateRankingDisplay()
                        } else {
                            Log.d("RankingFragment", "No data entries found")
                            addRankingItem("No Data", "Please add some data", R.drawable.top1)
                        }
                    } else {
                        Log.d("RankingFragment", "Failed to parse user data")
                        addRankingItem("Error", "Failed to load user data", R.drawable.top1)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("RankingFragment", "Failed to load data: ${error.message}")
                    addRankingItem("Error", "Failed to load data", R.drawable.top1)
                }
            })
        } else {
            Log.d("RankingFragment", "User not logged in")
            addRankingItem("Not Logged In", "Please log in", R.drawable.top1)
        }
    }

    private fun updateRankingDisplay() {
        rankingContainer!!.removeAllViews()
        if (currentUserData == null || currentUserData!!.data == null || currentUserData!!.data.isEmpty()) {
            addRankingItem("No Data", "No data available", R.drawable.top1)
            return
        }

        val latestDate = latestDate
        if (latestDate != null && currentUserData!!.data.containsKey(latestDate)) {
            val dailyData = currentUserData!!.data[latestDate]
            when (currentRankingType) {
                "Time" -> {
                    val timeValue =
                        if (dailyData!!.time != 0) dailyData.time else dailyData.duration
                    addRankingItem(
                        currentUserData!!.name + " (" + dailyData.date + ")",
                        timeValue.toString() + "s",
                        R.drawable.top1
                    )
                }

                "Posture" -> {
                    val postureCount = if (dailyData!!.posture != null) dailyData.posture.stream()
                        .mapToInt { obj: Int -> obj.toInt() }
                        .sum() else 0
                    addRankingItem(
                        currentUserData!!.name + " (" + dailyData.date + ")",
                        "$postureCount times",
                        R.drawable.top1
                    )
                }

                "Sports" -> {
                    val exerciseTotal =
                        if (dailyData!!.exercise != null) dailyData.exercise.stream()
                            .mapToInt { obj: Int -> obj.toInt() }
                            .sum() else 0
                    addRankingItem(
                        currentUserData!!.name + " (" + dailyData.date + ")",
                        dailyData.sports.toString() + "s (Exercise: " + exerciseTotal + "s)",
                        R.drawable.top1
                    )
                }
            }
        } else {
            addRankingItem("No Data", "No data for $currentRankingType", R.drawable.top1)
        }
    }

    private val latestDate: String?
        get() {
            if (currentUserData == null || currentUserData!!.data == null || currentUserData!!.data.isEmpty()) {
                return null
            }

            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            return currentUserData!!.data.keys.stream()
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