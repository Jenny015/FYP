package com.example.i_postureguard;

import java.util.HashMap;
import java.util.Map;

public class User {
    public String phone;
    public String name;
    public String dob; // date of birth in dd/mm/yyyy
    public String gender;
    public String carer; //email of carer
    public Map<String, DailyData> data;

    public User(){}

    public User(String initialize){
        name = "";
        dob = "";
        gender = "";
        carer = "";
        data = new HashMap<String, DailyData>();
    }

    public User(String name, String dob, String gender, String carer, Map<String, DailyData> data){
        this.name = name;
        this.dob = dob;
        this.gender = gender;
        this.carer = carer;
        this.data = data;
    }

    public String toString(){
        return phone+", "+name+", "+dob+", "+gender+", "+carer+", "+data;
    }
}