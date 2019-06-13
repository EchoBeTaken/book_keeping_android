package com.example.bookkeepingapplication.utils;

import android.app.Application;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;

public class MyApplication extends Application {

    private static final String TAG = "MyApplication";

    private int result = 0;
    private Intent intent = null;
    private MediaProjectionManager mMediaProjectionManager = null;

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public Intent getIntent() {
        return intent;
    }

    public void setIntent(Intent intent) {
        this.intent = intent;
    }

    public MediaProjectionManager getmMediaProjectionManager() {
        return mMediaProjectionManager;
    }

    public void setmMediaProjectionManager(MediaProjectionManager mMediaProjectionManager) {
        this.mMediaProjectionManager = mMediaProjectionManager;
    }
}
