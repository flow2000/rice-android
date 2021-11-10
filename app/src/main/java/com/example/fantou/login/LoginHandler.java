package com.example.fantou.login;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

public class LoginHandler extends Handler {
    public final int MSG_LOGIN = 0x00000001;
    private final WeakReference<LoginActivity> weakActivity;//弱引用防止内存泄漏

    public LoginHandler(LoginActivity activity) {
        this.weakActivity = new WeakReference<LoginActivity>(activity);
        this.handler = new Handler(this.getActivity());
    }

    public  Activity getActivity()
    {
        return this.weakActivity.get();
    }

    @Override
    public void handleMessage(@NonNull Message msg) {

        LoginActivity activity = (LoginActivity) this.getActivity();
        if(activity != null && !activity.isFinishing())
        {
            Bundle bundle = msg.getData();//只接受Bundle的消息
            switch (msg.what)
            {
                case MSG_LOGIN:
                    handler.handleLoginResult(bundle);
                    break;
                default:
                    break;
            }
        }


    }


    public enum MessageType
    {
        LoginResult
    }

    Handler handler;
    public void setCallBack(Handler.CallBack callBack)
    {
        handler.addCallBack(callBack);
    }
    public static class Handler
    {
        public Handler(Context ctx)
        {
            this.ctx = ctx;
        }
        private Context ctx;
        public final int FAIL = 0;
        public final int SUCCESS = 1;

        public interface CallBack{}
        private Set<CallBack> cbs = new HashSet<>();
        public void addCallBack(CallBack cb)
        {
            cbs.add(cb);
        }
        public void removeCallBack(CallBack cb)
        {
            cbs.remove(cb);
        }

        public CallBack getCallBack(Class<? extends CallBack> cls)
        {
            for(CallBack c: cbs)
            {
                if(cls.isAssignableFrom(c.getClass()))
                    return c;
            }
            return null;
        }

        //以下为消息处理
        //处理登录消息
        private void handleLoginResult(Bundle bundle)
        {
            //预处理
            int loginCode = bundle.getInt("loginCode");
            String refreshToken = bundle.getString("refreshToken");

            //获取回调函数
            CallBack cb = this.getCallBack(CB_handleLoginResult.class);
            CB_handleLoginResult cb_hlr = (CB_handleLoginResult)cb;
            //处理
            if(loginCode == SUCCESS)
            {
                cb_hlr.handleLoginSuccess(refreshToken);
            }else if(loginCode == FAIL){
                cb_hlr.handleLoginFail();
            }
            //移除回调函数
            this.removeCallBack(cb);
        }
        public interface CB_handleLoginResult extends CallBack
        {
            void handleLoginSuccess(String refreshToken);
            void handleLoginFail();
        }

    }




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
