package com.example.fantou.login;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fantou.R;
import com.example.fantou.util.OnNoFastClickListener;


public class LoginActivity extends AppCompatActivity {

    private Context ctx = this;
    private LoginService  loginServiceBinded;
    LoginHandler handler = new LoginHandler(LoginActivity.this); //使得子线程能够操作主线程的UI组件
    private ServiceConnection scon = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            loginServiceBinded = ((LoginService.MyBinder)iBinder).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    //服务的绑定与解绑
    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(LoginActivity.this,LoginService.class), scon, BIND_AUTO_CREATE);
    }
    @Override
    protected void onStop() {
        super.onStop();
        unbindService(scon);
    }


    //获取、创建UI组件，设置监听器等
    // UI references.
    private EditText mPhoneView;
    private EditText mPasswordView;
    private EditText mNicknameView;
    private EditText mPasswordAgainView;
    private View mProgressView;
    private View mLoginFormView;
    private Button mSignInButton;
    private Button mSwitchButton;

    private Boolean isRegiterAction=false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.
        mPhoneView = (EditText) findViewById(R.id.et_phone);
        mPasswordView = (EditText) findViewById(R.id.et_password);
        mPasswordAgainView= (EditText) findViewById(R.id.et_password_again);
        mNicknameView= (EditText) findViewById(R.id.et_nickname);

        mSignInButton = (Button) findViewById(R.id.btn_login_register);
        mSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isRegiterAction){
                    //attemptRegister();
                }else{
                    attemptLogin();
                }
            }
        });
        mSwitchButton= (Button) findViewById(R.id.switch_button);
        mSwitchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isRegiterAction==false){
                    isRegiterAction=true;
                    mSwitchButton.setText("已有帐号?");
                    mSignInButton.setText("注册账号");
                    clearText();
                    mNicknameView.setVisibility(View.VISIBLE);
                    mPasswordAgainView.setVisibility(View.VISIBLE);
                }else{
                    clearText();
                    mSignInButton.setText("登录");
                    mSwitchButton.setText("还没有帐号?");
                    isRegiterAction=false;
                    mNicknameView.setVisibility(View.GONE);
                    mPasswordAgainView.setVisibility(View.GONE);
                }
            }
        });
        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    private void clearText() {
        mPasswordAgainView.setText("");
        mNicknameView.setText("");
        mPasswordView.setText("");
    }


    protected void attemptLogin() {
        String username = mPhoneView.getText().toString();
        String password = mPasswordView.getText().toString();
        Toast mThoast = Toast.makeText(this,"",Toast.LENGTH_LONG);
        handler.setCallBack(new LoginHandler.Handler.CB_handleLoginResult() {

            @Override
            public void handleLoginSuccess(String refreshToken) {
                //显示登录结果信息
                mThoast.setText("登录成功");
                mThoast.show();
                //保存token
                loginServiceBinded.saveToken(refreshToken);
            }

            @Override
            public void handleLoginFail() {
                //显示登录结果信息
                mThoast.setText("账号或密码错误");
                mThoast.show();
            }

        });
        loginServiceBinded.loginService(handler, username,password);

//        //获取页面控件
//        usernameEditText = findViewById(R.id.et_userName);
//        passwordEditText = findViewById(R.id.et_password);
//        loginButton = findViewById(R.id.btn_login);


        //登录按钮点击事件监听
//        loginButton.setOnClickListener(new OnNoFastClickListener() {
//            @Override
//            public void onNoFastClick(View view) {
//                String username = mPhoneView.getText().toString();
//                String password = mPasswordView.getText().toString();
//
//                handler.setCallBack(new LoginHandler.Handler.CB_handleLoginResult() {
//                    @Override
//                    public void handleLoginSuccess(String refreshToken) {
//                        //显示登录结果信息
//                        mThoast.setText("登录成功");
//                        mThoast.show();
//                        //保存token
//                        loginServiceBinded.saveToken(refreshToken);
//                    }
//
//                    @Override
//                    public void handleLoginFail() {
//                        //显示登录结果信息
//                        mThoast.setText("账号或密码错误");
//                        mThoast.show();
//                    }
//
//                });
//                loginServiceBinded.loginService(handler, username,password);
//            }
//        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler = null;
    }
}