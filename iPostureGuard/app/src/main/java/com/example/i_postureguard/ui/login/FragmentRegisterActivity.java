package com.example.i_postureguard.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.i_postureguard.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class FragmentRegisterActivity extends AppCompatActivity {
    private PopupWindow popup;
    private DatabaseReference db = FirebaseDatabase.getInstance().getReference();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_register);

        // Back Button
        ImageButton btn_back = findViewById(R.id.btn_back);
        TextView tv_back = findViewById(R.id.tv_back);
        View.OnClickListener back = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                intent(FragmentLoginActivity.class, null);
            }
        };
        btn_back.setOnClickListener(back);
        tv_back.setOnClickListener(back);

        //Confirm Button
        Button btn_confirm = findViewById(R.id.btn_confirm);
        btn_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkData();
            }
        });
    }

    private void showPopUp(int textId, String phone) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.popupwindow, null);
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        popup  = new PopupWindow(view, width, height, true);
        TextView msg = view.findViewById(R.id.tv_display);
        msg.setText(getString(textId));
        popup.showAtLocation(view, Gravity.CENTER, 0,0);
        Button btnClose = view.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popup.dismiss();
                intent(FragmentLoginActivity.class, phone);
            }
        });
    }

    private String checkPassword(){
        EditText et_pwd = findViewById(R.id.pwd);
        EditText et_pwd2 = findViewById(R.id.pwd2);

        if(et_pwd.getText().toString().trim().isEmpty()){
            et_pwd.setError(getString(R.string.emptyField));
            return null;
        }
        if(!et_pwd.getText().toString().trim().equals(et_pwd2.getText().toString().trim())){
            et_pwd2.setError(getString(R.string.diffPwd));
            return null;
        }
        if(et_pwd.getText().toString().trim().length() < 8){
            et_pwd.setError(getString(R.string.longerPwd));
            return null;
        }
        return et_pwd.getText().toString();
    }

    private void register(String phone, Map<String, String> user){

        Query query = db.child("users").child(phone);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Phone number already exists
                    showPopUp(R.string.duplicateRegister, phone);
                } else {
                    db.child("users").child(phone).setValue(user);
                    showPopUp(R.string.registerSuccess, phone);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                showPopUp(R.string.firebaseDisconnect, null);
            }
        });
    }


    private void checkData(){
        EditText et_phone = findViewById(R.id.phone);
        String phone = et_phone.getText().toString().trim();
        if(phone.isEmpty()){
            et_phone.setError(getString(R.string.emptyField));
            return;
        }
        if(phone.length() != 8){
            et_phone.setError(getString(R.string.incorrPhone));
            return;
        }

        String password = checkPassword();
        if(password == null){
            return;
        }

        EditText et_lname = findViewById(R.id.lname);
        EditText et_fname = findViewById(R.id.fname);
        RadioButton rb_male = findViewById(R.id.male);
        RadioButton rb_female = findViewById(R.id.female);
        DatePicker dp_dob = findViewById(R.id.dob);

        if(TextUtils.isEmpty(et_lname.getText())){
            et_lname.setError(getString(R.string.emptyField));
            return;
        }
        if(TextUtils.isEmpty(et_fname.getText())){
            et_fname.setError(getString(R.string.emptyField));
            return;
        }
        if((!rb_male.isChecked() && !rb_female.isChecked())){
            rb_female.setError(getString(R.string.unchecked));
            return;
        }

        String name = et_lname.getText().toString().trim()+" "+et_fname.getText().toString().trim();
        String gender = rb_male.isChecked() ? "M" : "F";
        String dob = String.format("%02d-%02d-%04d", dp_dob.getDayOfMonth(), dp_dob.getMonth()+1, dp_dob.getYear());

        Map<String, String> user = new HashMap<>();
        user.put("phone", phone);
        user.put("name", name);
        user.put("password", password);
        user.put("dob", dob);
        user.put("gender", gender);
        register(phone, user);
    }

    private void intent(Class page, String phone){
        Intent intent = new Intent(FragmentRegisterActivity.this, page);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        if(phone != null){
            intent.putExtra("phone", phone);
        }
        startActivity(intent);
        finish();
    }

}
