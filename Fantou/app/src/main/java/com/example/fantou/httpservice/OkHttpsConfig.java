package com.example.fantou.httpservice;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import com.ejlchina.okhttps.Config;
import com.ejlchina.okhttps.HTTP;
import com.ejlchina.okhttps.HttpResult;
import com.ejlchina.okhttps.HttpTask;
import com.ejlchina.okhttps.OkHttps;
import com.ejlchina.okhttps.OnCallback;
import com.tencent.mmkv.MMKV;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class OkHttpsConfig implements Config {

    // 绑定到主线程的 Handler
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    //在直接使用Okhtts时调用该方法构建http
    @Override
    public void with(HTTP.Builder builder) {

        builder.baseUrl(Urls.BASE_URL)          // 配置 BaseURL

                // 如果默认请求体是JSON，则开启，否则默认为表单
                .bodyType(OkHttps.JSON)

                // 配置默认回调在主线程执行
                .callbackExecutor(run -> mainHandler.post(run))

                // 生命周期绑定
                .addPreprocessor(chain -> {
                    HttpTask<?> task = chain.getTask();
                    Object bound = task.getBound();
                    task.bind(new BoundWrapper(task, bound));
                    chain.proceed();
                })

                // TOKEN 处理（串行预处理）
                .addSerialPreprocessor(chain -> {
                    HttpTask<?> task = chain.getTask();
                    // 根据标签判断是否需要 Token
                    if (!task.isTagged(Tags.TOKEN)) {
                        chain.proceed();
                        return;
                    }
                    Context ctx = context(task);
                    requestTokenAndRefreshIfExpired(ctx, chain.getHttp(),
                            (String token) -> {
                                if (token != null) {
                                    // 添加 Token 头信息，名字需要和后端商定
                                    //task.addHeader("Access-Token", token);
                                } else if (ctx != null) {
                                    Log.i("token expire", "need login");
                                    // 若没有得到 Token, 则跳转登录页面
                                    //ctx.startActivity(new Intent(ctx, LoginActivity.class));
                                } else {
                                    //Log.e("OkHttps", "没有 Context 无法跳转登录页面！");
                                }
                                // 无论如何，这行代码一定要执行到，不然后续接口会一直在排队中
                                chain.proceed();
                            });
                })

                //以下为全局回调监听
                // 错误码统一处理
                .responseListener((HttpTask<?> task, HttpResult result) -> {
                    // 刷新 Token 的接口可以例外
                    if (task.getUrl().contains(Urls.TOKEN_REFRESH)
                            || result.isSuccessful()) {
                        // 这里演示的是 HTTP 状态码的处理，如果有自定义的 code, 也可以进行深层次的判断

                        return true;            // 继续接口的业务处理
                    }
                    else {
                        // 向用户展示接口的错误信息
                        showMsgToUser(task, result.getBody().toString());
                        return false;               // 阻断
                    }
                })

                // 生命周期绑定：解绑
                .completeListener((HttpTask<?> task, HttpResult.State state) -> {
                    Object bound = task.getBound();
                    if (bound instanceof BoundWrapper) {
                        ((BoundWrapper) bound).unbind();
                    }
                    // 网络错误统一处理
                    switch (state) {
                        case TIMEOUT:
                            showMsgToUser(task, "网络连接超时");
                            break;
                        case NETWORK_ERROR:
                            showMsgToUser(task, "网络错误，请检查WIFI或数据是否开启");
                            break;
                        case EXCEPTION:
                            showMsgToUser(task, "接口请求异常: " + task.getUrl());
                            break;
                    }

                    return true;
                })
                .exceptionListener((HttpTask<?> task, IOException error) -> {
                    // 所有异步请求（包括 WebSocket）发生异常都会走这里
                    Log.e("httperror:",error.toString());
                    // 返回 true 表示继续执行 task 的 OnException 回调，
                    // 返回 false 则表示不再执行，即 阻断
                    return true;
                });

    }

    // 其它 ...


    // 省略其它...

    /**
     * 获取 Context 对象
     **/
    private Context context(HttpTask<?> task) {
        Object bound = task.getBound();
        if (bound instanceof BoundWrapper) {
            bound = ((BoundWrapper) bound).bound;
        }
        if (bound instanceof Context) {
            return (Context) bound;
        }
        if (bound instanceof Fragment) {
            return ((Fragment) bound).getActivity();
        }
        // 还可以添加更多获取 Context 的策略，比如从全局 Application 里取
        return null;
    }

    // 其它 ...

    // 省略其它...
    //将存有httptask的BoundWrapper监听器注册到对应的生命周期对象上
    static class BoundWrapper implements LifecycleObserver {

        HttpTask<?> task;
        Lifecycle lifecycle;
        Object bound;

        BoundWrapper(HttpTask<?> task, Object bound) {
            this.task = task;
            if (bound instanceof LifecycleOwner) {
                lifecycle = ((LifecycleOwner) bound).getLifecycle();
                lifecycle.addObserver(this);
            }
            this.bound = bound;
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        public void onStop() {
            task.cancel();  // 在 ON_STOP 事件中，取消对应的 HTTP 任务
        }

        void unbind() {
            if (lifecycle != null) {
                lifecycle.removeObserver(this);
            }
        }

    }

    // 其它 ...





    // 省略其它...

    /**
     * 向用户展示信息
     **/
    private void showMsgToUser(HttpTask<?> task, String message) {
        // 这里就简单用 Toast 示例一下，有更高级的实现可以替换
        Context ctx = context(task);
        if (ctx != null) {
            mainHandler.post(() -> {
                Toast.makeText(ctx, message, Toast.LENGTH_LONG).show();
            });
        }
    }

    // 其它 ...



    // 省略其它...

    private ProgressDialog loading = null;
    private AtomicInteger loadings = new AtomicInteger(0);

    // 显示加载框
    private void showLoading(Context ctx) {
        if (loading == null) {
            // 这里就用 ProgressDialog 来演示了，当然可以替换成你喜爱的加载框
            loading = new ProgressDialog(ctx);
            loading.setMessage("正在加载，请稍等...");
        }
        // 增加加载框显示计数
        loadings.incrementAndGet();
        loading.show();
    }

    // 关闭加载框
    private void hideLoading() {
        // 判断是否所有显示加载框的接口都已完成
        if (loadings.decrementAndGet() <= 0
                && loading != null) {
            loading.dismiss();
            loading = null;
        }
    }

    // 其它 ...





    // 省略其它...

    /**
     * 获取TOKEN，若过期则刷新（代码中的字符串可以替换为常量）
     **/
    private void requestTokenAndRefreshIfExpired(Context ctx, HTTP http,
                                                 OnCallback<String> callback) {
        if (ctx == null) {
            callback.on(null);
            return;
        }
        // 这里演示使用 MMKV 存储，也可以使用数据库存储
        MMKV mmkv = MMKV.mmkvWithID("token");
        long now = System.currentTimeMillis();
        // 刷新令牌
        String refreshToken = mmkv.decodeString("refreshToken",null);
        // 判断有效期可以提前 60 秒，以防在接下来的网络延迟中过期了
        if (mmkv.getLong("refreshTokenExpiresAt", 0) < now + 60000
                || refreshToken == null) {
            // 刷新令牌已过期，说明长时间未使用，需要重新登录
            callback.on(null);
            return;
        }


    }

}
