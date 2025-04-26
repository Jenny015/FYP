package com.example.i_postureguard;

import java.util.List;

public class DailyData {
    public String date; // "dd-mm-yyyy" format
    public int duration;
    public int time;
    public int sports;
    public List<Integer> exercise;
    public List<Integer> posture;


    public DailyData() {}

    public DailyData(String date, int duration, int time, int sports, List<Integer> exercise, List<Integer> posture) {
        this.date = date;
        this.duration = duration;
        this.time = time;
        this.sports = sports;
        this.exercise = exercise;
        this.posture = posture;
    }

}