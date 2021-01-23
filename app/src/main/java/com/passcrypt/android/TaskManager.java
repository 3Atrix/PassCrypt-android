package com.passcrypt.android;

import android.os.Handler;
import android.os.Looper;

public class TaskManager {

    public static void runOnThread(Runnable runnable) {
        new Thread(runnable).start();
    }

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void runOnMainThread(Runnable runnable) {
        synchronized (mainHandler) {
            mainHandler.post(runnable);
        }
    }
}
