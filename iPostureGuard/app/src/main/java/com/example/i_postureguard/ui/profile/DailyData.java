package com.example.i_postureguard.ui.profile;

public class DailyData{
    public String date; // "dd/mm/yyyy" format
    public int duration;
    public int[]exercise;
    public int[] posture;

    public DailyData(){
        date = "";
        duration = 0;
        exercise = new int[3];
        posture = new int[6];
    }
    public DailyData(String date, int duration, int[] exercise, int[] posture){
        this.date = date;
        this.duration = duration;
        this.exercise = exercise;
        this.posture = posture;
    }
}
