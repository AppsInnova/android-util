package com.yy.androidlib.util;

import android.os.Handler;

public abstract class DelayTask {

    private Handler mHandler;
    private long mDuration;
    private boolean hasTask;
    private long lastTime;

    public DelayTask(Handler handler, long duration) {
        mHandler = handler;
        mDuration = duration;
    }

    public abstract void runTask();


    public void run() {
        if (!hasTask) {
            long now = System.currentTimeMillis();
            long duration = now - lastTime;
            if (duration < mDuration) {
                hasTask = true;
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        runTask();
                        hasTask = false;
                        lastTime = System.currentTimeMillis();
                    }
                }, mDuration - duration);
            } else {
                runTask();
                hasTask = false;
                lastTime = System.currentTimeMillis();
            }
        }
    }

}
