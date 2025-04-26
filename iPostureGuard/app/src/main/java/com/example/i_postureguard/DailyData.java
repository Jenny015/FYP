package com.example.i_postureguard;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DailyData {
    public String date = Utils.todayToString(); // "dd-mm-yyyy" format
    public int duration = 0;
    public int time = 0;
    public int sports = 0;
    public List<Integer> exercise = Arrays.asList(0, 0, 0, 0, 0);
    public List<Integer> posture = Arrays.asList(0, 0, 0);


    public DailyData() {}

    public DailyData(String date, int duration, int time, int sports, List<Integer> exercise, List<Integer> posture) {
        this.date = date;
        this.duration = duration;
        this.time = time;
        this.sports = sports;
        this.exercise = exercise;
        this.posture = posture;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("date", date);
        result.put("duration", duration);
        result.put("time", time);
        result.put("sports", sports);
        result.put("exercise", exercise);
        result.put("posture", posture);
        return result;
    }

    @Override
    public String toString() {
        return "DailyData{" +
                "date='" + date + '\'' +
                ", duration=" + duration +
                ", time=" + time +
                ", sports=" + sports +
                ", exercise=" + exercise +
                ", posture=" + posture +
                '}';
    }
}