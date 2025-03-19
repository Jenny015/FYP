package com.example.i_postureguard;

import java.util.List;

public class DailyData{
    public String date; // "dd-mm-yyyy" format
    public int duration;
    public List<Integer> exercise;
    public List<Integer>posture;

    public DailyData(){}

    public DailyData(String date, int duration, List<Integer> exercise, List<Integer> posture){
        this.date = date;
        this.duration = duration;
        this.exercise = exercise;
        this.posture = posture;
    }
}
