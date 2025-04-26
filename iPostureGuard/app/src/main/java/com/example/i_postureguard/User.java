package com.example.i_postureguard;

import java.util.HashMap;
import java.util.Map;

public class User {
    public String name;
    public String dob; // date of birth in dd/mm/yyyy
    public String gender;
    public String carer; //email of carer

    public User(){}

    public User(String name, String dob, String gender, String carer){
        this.name = name;
        this.dob = dob;
        this.gender = gender;
        this.carer = carer;
    }

    public String toString(){
        return name+", "+dob+", "+gender+", "+carer;
    }
}