package com.example.fantou.util;

import android.view.View;

public abstract class OnNoFastClickListener implements View.OnClickListener {
    private static final long DELAYED_TIME = 1000;
    private long lastTime = 0;

    public abstract void onNoFastClick(View view);

    @Override
    public void onClick(View v) {
        if (System.currentTimeMillis()-lastTime > DELAYED_TIME){
            lastTime = System.currentTimeMillis();
            onNoFastClick(v);
        }
    }
}
