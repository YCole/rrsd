 package com.android.qrdfileexplorer.ftp.Cat;

/**
 * Created by zhanghao14 on 2018/1/10.
 */

public class CatLazy extends CatLog {

    @Override
    protected String getTag() {
        return CatUtil.getCallingClassNameSimple();
    }
}
