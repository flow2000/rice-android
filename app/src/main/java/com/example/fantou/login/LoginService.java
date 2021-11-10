package com.example.fantou.login;


import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import androidx.annotation.Nullable;

import com.ejlchina.data.Mapper;
import com.ejlchina.okhttps.HttpResult;
import com.ejlchina.okhttps.OkHttps;
import com.example.fantou.httpservice.Tags;
import com.tencent.mmkv.MMKV;

import java.util.HashMap;
import java.util.Map;

public class LoginService extends Service {


    //生命周期方法
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.i("LoginService","stop");
    }


    //为了给绑定者返回本服务实例
    public class MyBinder extends Binder{
        public LoginService getService(){
            return LoginService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        Log.i("onBind","onBind()");
        return new MyBinder();
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }


    //提供的服务
    public void loginService(LoginHandler handler,  String username, String password){

        new Thread(new Runnable() {
            @Override
            public void run() {
                Map<String, Object> params = new HashMap<>();
                params.put("userName", username);
                params.put("password", password);

                HttpResult result =OkHttps.sync("prod-api/app/user/login")
                        .bind(handler.getActivity())             // 绑定（生命周期|Context获取）
                        .tag(Tags.TOKEN)
                        .addBodyPara(params)
                        .post();
                Mapper mapper = result.getBody().toMapper();
                int loginCode = mapper.getInt("code");
                loginCode = (loginCode == 200 ? 1:0);
                String token = mapper.getString("token");
                Log.i("loginresult:", mapper.toString());


                //发送整理好的消息给handler
                Bundle bundle = new Bundle();
                bundle.putInt("loginCode",loginCode);
                bundle.putString("refreshToken", token);
                Message msg = Message.obtain(handler,handler.MSG_LOGIN);
                msg.setData(bundle);
                handler.sendMessage(msg);
            }
        }).start();

    }

    //保存token
    public void saveToken(String refreshToken)
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                MMKV mmkv = MMKV.mmkvWithID("token");
                String s = mmkv.decodeString("refreshToken");
                Long refreshTokenExpiresIn = 100*1000L;   //设置token有效期，单位毫秒
                Long refreshTokenExpiresAt = System.currentTimeMillis() + refreshTokenExpiresIn;
                mmkv.encode("refreshToken",refreshToken);
                mmkv.encode("refreshTokenExpiresAt",refreshTokenExpiresAt);
            }
        }).start();

    }

    @Nullable


    private void send()
    {
        String sendJsonData;

    }



    //为了给绑定者获取服务结果
//    public interface CallBack
//    {
//        void analyseLoginStatus(Integer loginStatus);
//    }
//    private CallBack callBack;
//    public void setCallBack(CallBack callBack)
//    {
//        this.callBack = callBack;
//    }
}
