package com.example.i_postureguard;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Utils {
    public static final String PREF_NAME = "prefsFile";
    public static final DatabaseReference db = FirebaseDatabase.getInstance().getReference("users");
    public static final Gson gson = new Gson();
    public static final int detectionDelay = 8000;

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

    // Category: e = exercise, p = posture
    public static void localAddData(Context c, String category, int type){
        SharedPreferences prefs = c.getSharedPreferences(PREF_NAME, 0);
        String jsonData = prefs.getString("dailyData", null);
        Map<String, DailyData> dailyData = new HashMap<String, DailyData>();
        DailyData data = new DailyData();

        //Find today's data, if any
        if (jsonData != null) {

            dailyData = gson.fromJson(jsonData, new TypeToken<Map<String, DailyData>>() {}.getType());
            data = dailyData.get(todayToString());
        }

        int original = 0;
        if(category.equals("e")){
            original = data.exercise.get(type);
            data.exercise.set(type, original+1);
            Log.d("Adding data", "local, "+type+", "+data.exercise.get(type));
        } else if(category.equals("p")){
            original = data.posture.get(type);
            data.posture.set(type, original+1);
            Log.d("Adding data", "local, "+type+", "+data.posture.get(type));
        }
        data.time += detectionDelay/1000;
        dailyData.put(todayToString(), data);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("dailyData", gson.toJson(new HashMap<String, DailyData>()));
        editor.apply();
    }

    // category: e = exercise, p = posture
    public static void firebaseAddData(Context c, String category, int type){
        DatabaseReference userDataRef = db.child(getString(c, "phone", "")).child("data").child(todayToString());
        userDataRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                DailyData dailyData = mutableData.getValue(DailyData.class);
                if (dailyData == null) {
                    dailyData = new DailyData();
                    dailyData.date = todayToString();
                }
                // Increment the value of category_long(index)
                int original = 0;
                if(category.equals("e")){
                    original = dailyData.exercise.get(type);
                    dailyData.exercise.set(type, original+1);
                    Log.d("Adding data", "local, "+type+", "+dailyData.exercise.get(type));
                } else if(category.equals("p")){
                    original = dailyData.posture.get(type);
                    dailyData.posture.set(type, original+1);
                    Log.d("Adding data", "local, "+type+", "+dailyData.posture.get(type));
                }
                mutableData.setValue(dailyData);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean committed, DataSnapshot dataSnapshot) {
                if (committed) {
                    System.out.println("Transaction successful!");
                } else {
                    System.err.println("Transaction failed: " + databaseError.getMessage());
                }
            }
        });
    }

    public static String todayToString(){
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        return today.format(formatter);
    }

    // Create SharedPreferences only stores DailyData if user not to login
    public static void createLocalDailyData(Context c){
        SharedPreferences prefs = c.getSharedPreferences(PREF_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("dailyData", gson.toJson(new HashMap<String, DailyData>()));
        editor.apply();
    }

    //Sync local DailyData to firebase
    public static void syncLocalDataToFirebase(Context c, String phone){
        // Get the local DailyData
        Map<String, DailyData> localData = new HashMap<String, DailyData>();
        SharedPreferences sharedPreferences = c.getSharedPreferences(PREF_NAME, 0);
        String jsonData = sharedPreferences.getString("dailyData", null);
        if (jsonData != null) {
            Type type = new TypeToken<Map<String, DailyData>>() {}.getType();
            localData = gson.fromJson(jsonData, type);
        }
        // Get the reference to the user's data in Firebase
        DatabaseReference userDataRef = db.child(phone).child("data");

        // Use updateChildren() to merge the local data with the existing data
        Map<String, Object> updates = new HashMap<>();

        // Prepare the data for update (convert each DailyData to a Map)
        for (Map.Entry<String, DailyData> entry : localData.entrySet()) {
            updates.put(entry.getKey(), entry.getValue().toMap());
        }

        // Push updates to Firebase without overwriting
        userDataRef.updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    System.out.println("Data synced successfully!");
                })
                .addOnFailureListener(e -> {
                    System.err.println("Error syncing data: " + e.getMessage());
                });
    }

    /*
    * Way to use getUserProfileFromDB:
      getUserProfileFromDB(context, new UserCallback() {
        @Override
        public void onSuccess(User user) {
            // Handle the User object here
            System.out.println("User: " + user);
        }

        @Override
        public void onFailure(String errorMessage) {
            // Handle the error here
            System.err.println("Error: " + errorMessage);
        }
    });
     */
    public static void getUserProfileFromDB(Context c, UserCallback callback) {
        String phone = getString(c, "phone", "");
        db.child(phone).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Create User object from the data
                    User user = new User(
                            dataSnapshot.child("name").getValue(String.class),
                            dataSnapshot.child("dob").getValue(String.class),
                            dataSnapshot.child("gender").getValue(String.class),
                            dataSnapshot.child("carer").getValue(String.class)
                    );
                    // Pass the User object to the callback
                    callback.onSuccess(user);
                } else {
                    callback.onFailure("User not found in the database.");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle errors
                callback.onFailure(databaseError.getMessage());
            }
        });
    }

    /*
    * Way to use getDailyDataFromDB:
      getDailyDataFromDB(c, new DataCallback() {
            @Override
            public void onSuccess(Map<String, DailyData> data) {
                // use the data in this block
                System.out.println("Data retrieved: " + data);
            }

            @Override
            public void onFailure(String error) {
                System.err.println("Error: " + error);
            }
        });
     */
    public static void getDailyDataFromDB(Context c, DataCallback callback) {
        DatabaseReference userDataRef = db.child(getString(c, "phone", ""))
                .child("data");

        userDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Map<String, DailyData> output = new HashMap<>();
                if (snapshot.exists()) {
                    for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                        DailyData dailyData = childSnapshot.getValue(DailyData.class);
                        if (dailyData != null) {
                            dailyData.date = childSnapshot.getKey();
                            output.put(childSnapshot.getKey(), dailyData);
                        }
                    }
                    callback.onSuccess(output); // Callback when data is ready
                } else {
                    callback.onFailure("User record does not exist.");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onFailure("Error fetching data: " + error.getMessage());
            }
        });
    }

    /*
    * How to use updateValueToFirebase:
    *  Map<String, Object> updates = new HashMap<>();
    *  updates.put("key", value);
    *   updateValueToFirebase(requireContext() / getApplicationContext(), updates);
    * */
    public static void updateValueToFirebase(Context c, Map<String, Object> updates){
        DatabaseReference userDataRef = db.child(getString(c, "phone", ""));
        userDataRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
                    // Handle success
                    System.out.println("User fields updated successfully.");
                })
                .addOnFailureListener(e -> {
                    // Handle failure
                    System.err.println("Failed to update user fields: " + e.getMessage());
                });
    }

    // Define the callback interface
    public interface DataCallback {
        void onSuccess(Map<String, DailyData> data);
        void onFailure(String error);
    }

    public interface UserCallback {
        void onSuccess(User user);
        void onFailure(String error);
    }
}
