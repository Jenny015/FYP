package com.example.i_postureguard.ui.profile;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class User {
    public String name;
    public String password;
    public String dob; // date of birth in dd/mm/yyyy
    public String gender;
    public String carer; //email of carer
    public Map<String, DailyData> data;

    public User(){
        name = "";
        password = "";
        dob = "";
        gender = "";
        carer = "";
        data = new HashMap<String, DailyData>();
    }

    public User(String name, String password, String dob, String gender){
        this.name = name;
        this. password = password;
        this.dob = dob;
        this.gender = gender;
        carer = "";
        data = new HashMap<String, DailyData>();
    }

    public User(String name, String password, String dob, String gender, String carer, HashMap<String, DailyData> data){
        this.name = name;
        this. password = password;
        this.dob = dob;
        this.gender = gender;
        this.carer = carer;
        this.data = data;
    }
}