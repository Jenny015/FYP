package com.example.i_postureguard;
import com.example. i_postureguard.R.id.*;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class FragmentLoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_login);

        ImageButton btn_skip = findViewById(R.id.btn_skip);
        TextView tv_skip = findViewById(R.id.tv_skip);
        Button btn_login = findViewById(R.id.btn_login);
        Button btn_reg = findViewById(R.id.btn_reg);
        Button btn_forget_pwd = findViewById(R.id.btn_reg);

        View.OnClickListener listener = new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if (view.getId() == R.id.btn_skip || view.getId() == R.id.tv_skip){
                    intent(MainActivity.class);
                } else if(view.getId() == R.id.btn_login){
                    if(checkLogin()){
                        intent(MainActivity.class);
                    }
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
    public boolean checkLogin(){
        EditText et_phone = findViewById(R.id.et_phone);
        EditText et_pwd = findViewById(R.id.et_pwd);
        //TODO: login logic
        String phone = et_phone.toString();
        String pwd = et_pwd.toString();
        if (!phone.isEmpty() && !pwd.isEmpty()){
            return true;
        } else {
            return false;
        }
    }

    public void intent(Class page){
        Intent intent = new Intent(FragmentLoginActivity.this, page);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}