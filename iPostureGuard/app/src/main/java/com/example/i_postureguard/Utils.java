package com.example.i_postureguard;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Utils {
    public static final String PREF_NAME = "prefsFile";

    public static boolean isLogin(Context c) {
        SharedPreferences prefs = c.getSharedPreferences(PREF_NAME, 0);
        return prefs.getBoolean("login", false);
    }

    //TODO: Cannot change Language yet, don't know why
    public static void changeAppLanguage(Context c, String code) {
        Locale locale = new Locale(code);
        Locale.setDefault(locale);

        Resources resources = c.getResources();
        Configuration config = resources.getConfiguration();

        config.setLocale(locale);
        c.createConfigurationContext(config);

        // Update application context
        Resources appResources = c.getApplicationContext().getResources();
        Configuration appConfig = appResources.getConfiguration();
        appConfig.setLocale(locale);
        c.getApplicationContext().createConfigurationContext(appConfig);
    }

    public static void logout(Context c){
        SharedPreferences prefs = c.getSharedPreferences(PREF_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }

    public static String getString(Context c, String key, String def){
        SharedPreferences prefs = c.getSharedPreferences(PREF_NAME, 0);
        return prefs.getString(key, def);
    }

    public static void putString(Context c, String key, String value){
        SharedPreferences prefs = c.getSharedPreferences(PREF_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

    // Get User from Firebase to Local preference
    public static void setLocalUserFromFirebase(Context c, String phone){
        DatabaseReference db = FirebaseDatabase.getInstance().getReference().child("users");
        db.child(phone).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    Map<String, DailyData> dailyDataMap = new HashMap<>();
                    for (DataSnapshot snapshot1 : snapshot.child("data").getChildren()){
                        dailyDataMap.put(snapshot1.getKey(), snapshot1.getValue(DailyData.class));
                    }

                    // Check if local has daily data
                    User local = getLocalUser(c);
                    if(!local.data.isEmpty()){
                        //Update local daily record to firebase
                        dailyDataMap.putAll(local.data);
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("data", dailyDataMap);
                        updateToFirebase(c, updates);
                    }
                    // Pull firebase data to local
                    createLocalUser(c, new User(
                            snapshot.child("name").getValue(String.class),
                            snapshot.child("dob").getValue(String.class),
                            snapshot.child("gender").getValue(String.class),
                            snapshot.child("carer").getValue(String.class),
                            dailyDataMap
                    ));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    public static User getUserFromFirebase(Context c, String phone){
        setLocalUserFromFirebase(c, phone);
        return getLocalUser(c);
    }

    public static void updateToFirebase(Context c, Map<String, Object> update){
        DatabaseReference db = FirebaseDatabase.getInstance().getReference().child("users");
        db.child(getLocalUser(c).phone).updateChildren(update).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    // Update successful
                    Log.d("Firebase", "User data updated successfully.");
                } else {
                    // Update failed
                    Log.d("Firebase", "Failed to update user data.");
                }
            }
        });
    }

    // Get local user
    public static User getLocalUser(Context c){
        SharedPreferences prefs = c.getSharedPreferences(PREF_NAME, 0);
        Gson gson = new Gson();
        String userJson = prefs.getString("user", "");
        if (userJson == null || userJson.isEmpty()) {
            return new User("");
        }
        return gson.fromJson(userJson, User.class);
    }

    //Create local user entity
    public static void createLocalUser(Context c, User user){
        SharedPreferences prefs = c.getSharedPreferences(PREF_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String userJson = gson.toJson(user);
        editor.putString("user", userJson);
        editor.apply();
    }
}
