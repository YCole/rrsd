 package com.android.qrdfileexplorer.ftp.Cat;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

 /**
  * Created by zhanghao14 on 2018/1/10.
  */

 public interface CatPrinter {

     void println(int priority, @NonNull String tag, @NonNull String message, @Nullable Throwable t);
 }
