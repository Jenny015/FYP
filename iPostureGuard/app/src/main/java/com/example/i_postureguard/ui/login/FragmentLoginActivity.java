package com.example.i_postureguard.ui.login;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.i_postureguard.MainActivity;
import com.example.i_postureguard.R;
import com.example.i_postureguard.User;
import com.example.i_postureguard.Utils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FragmentLoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_login);

        if(Utils.isLogin(this)){
            intent(MainActivity.class);
        }

        ImageButton btn_skip = findViewById(R.id.btn_skip);
        TextView tv_skip = findViewById(R.id.tv_skip);
        Button btn_login = findViewById(R.id.btn_login);
        Button btn_reg = findViewById(R.id.btn_reg);
        Button btn_forget_pwd = findViewById(R.id.btn_forget_pwd); // 修正 id

        View.OnClickListener listener = new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if (view.getId() == R.id.btn_skip || view.getId() == R.id.tv_skip){
                    Utils.setUser(getApplicationContext(), new User(""));
                    intent(MainActivity.class);
                } else if(view.getId() == R.id.btn_login){
                    Login();
                } else if(view.getId() == R.id.btn_reg){
                    intent(FragmentRegisterActivity.class);
                } else if(view.getId() == R.id.btn_forget_pwd){
                    intent(FragmentForgetpasswordActivity.class);
                }
            }
        };
        btn_skip.setOnClickListener(listener);
        tv_skip.setOnClickListener(listener);
        btn_login.setOnClickListener(listener);
        btn_reg.setOnClickListener(listener);
        btn_forget_pwd.setOnClickListener(listener);

    }

    private void Login(){
        EditText et_phone = findViewById(R.id.et_phone);
        EditText et_pwd = findViewById(R.id.et_pwd);

        Intent intent = getIntent();
        if (intent.hasExtra("phone")) {
            String phone = intent.getStringExtra("phone");
            et_phone.setText(phone);
        }

        String phone = et_phone.getText().toString().trim();
        String pwd = et_pwd.getText().toString().trim();

        DatabaseReference db = FirebaseDatabase.getInstance().getReference().child("users");

        db.child(phone).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String storedPassword = dataSnapshot.child("password").getValue(String.class);
                    if (storedPassword.equals(pwd)) {
                        writePrefsFile(phone);
                        Utils.setUser(getApplicationContext(), dataSnapshot.getValue(User.class));
                        intent(MainActivity.class);
                    } else {
                        showPopUp(R.string.wrongPassword);
                    }
                } else {
                    showPopUp(R.string.userNotExist);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                showPopUp(R.string.firebaseDisconnect);
            }
        });
    }

    private void intent(Class<?> page){
        Intent intent = new Intent(FragmentLoginActivity.this, page);
        if(page == MainActivity.class){
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else{
            startActivity(intent);
        }
    }

    private void showPopUp(int textId) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.popupwindow, null);
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        PopupWindow popup  = new PopupWindow(view, width, height, true);
        TextView msg = view.findViewById(R.id.tv_display);
        msg.setText(getString(textId));
        popup.showAtLocation(view, Gravity.CENTER, 0,0);
        Button btnClose = view.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popup.dismiss();
            }
        });
    }

    private void writePrefsFile(String phone) {
        SharedPreferences settings = getSharedPreferences(Utils.PREF_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("phone", phone);
        editor.putBoolean("login", true);
        editor.apply();
        Utils.changeAppLanguage(this, settings.getString("lang", "en"));
    }
}
