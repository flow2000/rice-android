package com.example.fantou;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.fantou.login.LoginActivity;
import com.tencent.mmkv.MMKV;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        setContentView(R.layout.activity_main);

        //Setup MMKV on App startup， /只需一次初始化mmkv,为后续使用Tencent的MMKV存储数据
        String rootDir = MMKV.initialize(getApplicationContext());

        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}