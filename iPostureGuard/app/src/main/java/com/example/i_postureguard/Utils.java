package com.example.i_postureguard;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;

import java.util.Locale;

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

    public static void updateToFirebase(Context c){
        User user = getUser(c);
        DatabaseReference db = FirebaseDatabase.getInstance().getReference().child("users");
        db.child(user.phone).setValue(user).addOnCompleteListener(new OnCompleteListener<Void>() {
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

    public static void updateToFirebase(User user, String field, String value){
        DatabaseReference db = FirebaseDatabase.getInstance().getReference().child("users");
        db.child(user.phone).child(field).setValue(value).addOnCompleteListener(new OnCompleteListener<Void>() {
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

    public static User getUser(Context c){
        SharedPreferences prefs = c.getSharedPreferences(PREF_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String userJson = getString(c, "user", "");
        if (userJson == null || userJson.isEmpty()) {
            return new User("");
        }
            return gson.fromJson(userJson, User.class);
    }

    public static void setUser(Context c, User user){
        SharedPreferences prefs = c.getSharedPreferences(PREF_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String userJson = gson.toJson(user);
        editor.putString("user", userJson);
        editor.apply();
    }
}
