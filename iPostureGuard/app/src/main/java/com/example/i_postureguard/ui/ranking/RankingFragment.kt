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
import com.example.i_postureguard.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class RankingFragment : Fragment() {
    private var rankingContainer: LinearLayout? = null
    private var currentRankingType = "Time"
    private var currentUserData: User? = null
    private var databaseReference: DatabaseReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化 Firebase Realtime Database
        databaseReference = FirebaseDatabase.getInstance().getReference("users")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_ranking, container, false)

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
        val settings = requireActivity().getSharedPreferences("iPostureGuard", 0)
        val phone = settings.getString("phone", null)

        if (phone != null) {
            databaseReference!!.child(phone)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        currentUserData = snapshot.getValue(User::class.java)
                        if (currentUserData != null) {
                            updateRankingDisplay()
                        } else {
                            addRankingItem("No Data", "Please add some data", R.drawable.top1)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        addRankingItem("Error", "Failed to load data", R.drawable.top1)
                    }
                })
        } else {
            addRankingItem("Not Logged In", "Please log in", R.drawable.top1)
        }
    }

    private fun updateRankingDisplay() {
        rankingContainer!!.removeAllViews()
        if (currentUserData == null || currentUserData!!.data == null) {
            addRankingItem("No Data", "No data available", R.drawable.top1)
            return
        }

        val latestDate = latestDate
        if (latestDate != null && currentUserData!!.data.containsKey(latestDate)) {
            val dailyData = currentUserData!!.data[latestDate]
            when (currentRankingType) {
                "Time" ->
                    addRankingItem(
                        currentUserData!!.name,
                        dailyData!!.time.toString() + "s",
                        R.drawable.top1
                    )

                "Posture" -> {
                    val postureCount = if (dailyData!!.posture != null) dailyData.posture.stream()
                        .mapToInt { obj: Int -> obj.toInt() }
                        .sum() else 0
                    addRankingItem(currentUserData!!.name, "$postureCount times", R.drawable.top1)
                }

                "Sports" -> {
                    val exerciseTotal =
                        if (dailyData!!.exercise != null) dailyData.exercise.stream()
                            .mapToInt { obj: Int -> obj.toInt() }
                            .sum() else 0
                    addRankingItem(
                        currentUserData!!.name,
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
            if (currentUserData!!.data == null || currentUserData!!.data.isEmpty()) {
                return null
            }
            return currentUserData!!.data.keys.stream()
                .max { obj: String, anotherString: String? ->
                    obj.compareTo(
                        anotherString!!
                    )
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