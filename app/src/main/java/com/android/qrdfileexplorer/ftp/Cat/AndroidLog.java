package com.android.qrdfileexplorer.ftp.Cat;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by zhanghao14 on 2018/1/10.
 */

public class AndroidLog implements CatPrinter {

    @Override
    public void println(int priority, @NonNull String tag, @NonNull String message, @Nullable Throwable t) {
        if (t != null) {
            message += '\n' + Log.getStackTraceString(t);
        }

        Log.println(priority, tag, message);
    }
}
