package com.makerchen.taskscheduler;

import android.util.Log;

/**
 * @author MakerChen
 * @date 2017/4/21
 * @see
 */
public final class L {

    private static final String TAG = "TaskScheduler";

    public static final void d(String message) {
        d(message, null);
    }

    public static final void d(Throwable t) {
        d(null, t);
    }

    public static final void d(String message, Throwable t) {
        Log.d(TAG, message, t);
    }

}