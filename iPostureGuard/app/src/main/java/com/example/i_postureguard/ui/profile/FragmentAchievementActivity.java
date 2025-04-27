package com.example.i_postureguard.ui.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.i_postureguard.R;

public class FragmentAchievementActivity extends AppCompatActivity {

    private LinearLayout achievement1Container, achievement2Container, achievement3Container, achievement4Container,
            achievement5Container, achievement6Container, achievement7Container, achievement8Container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.fragment_achievement);

        achievement1Container = findViewById(R.id.achievement_1_container);
        achievement2Container = findViewById(R.id.achievement_2_container);
        achievement3Container = findViewById(R.id.achievement_3_container);
        achievement4Container = findViewById(R.id.achievement_4_container);
        achievement5Container = findViewById(R.id.achievement_5_container);
        achievement6Container = findViewById(R.id.achievement_6_container);
        achievement7Container = findViewById(R.id.achievement_7_container);
        achievement8Container = findViewById(R.id.achievement_8_container);

        Spinner spinner = findViewById(R.id.spinner_achievement_filter);
        String[] filterOptions = {"All", "Beginner", "Intermediate", "Advanced"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filterOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedFilter = filterOptions[position];
                updateAchievementVisibility(selectedFilter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                updateAchievementVisibility("All");
            }
        });


        Button btnBackToProfile = findViewById(R.id.btn_back_to_profile);
        btnBackToProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void updateAchievementVisibility(String filter) {
        achievement1Container.setVisibility(View.GONE);
        achievement2Container.setVisibility(View.GONE);
        achievement3Container.setVisibility(View.GONE);
        achievement4Container.setVisibility(View.GONE);
        achievement5Container.setVisibility(View.GONE);
        achievement6Container.setVisibility(View.GONE);
        achievement7Container.setVisibility(View.GONE);
        achievement8Container.setVisibility(View.GONE);

        switch (filter) {
            case "All":
                achievement1Container.setVisibility(View.VISIBLE);
                achievement2Container.setVisibility(View.VISIBLE);
                achievement3Container.setVisibility(View.VISIBLE);
                achievement4Container.setVisibility(View.VISIBLE);
                achievement5Container.setVisibility(View.VISIBLE);
                achievement6Container.setVisibility(View.VISIBLE);
                achievement7Container.setVisibility(View.VISIBLE);
                achievement8Container.setVisibility(View.VISIBLE);
                break;
            case "Beginner":
                achievement1Container.setVisibility(View.VISIBLE);
                achievement2Container.setVisibility(View.VISIBLE);
                break;
            case "Intermediate":
                achievement3Container.setVisibility(View.VISIBLE);
                achievement4Container.setVisibility(View.VISIBLE);
                achievement5Container.setVisibility(View.VISIBLE);
                break;
            case "Advanced":
                achievement6Container.setVisibility(View.VISIBLE);
                achievement7Container.setVisibility(View.VISIBLE);
                achievement8Container.setVisibility(View.VISIBLE);
                break;
        }
    }
}