/**
 * Copyright (c) 2013 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Qualcomm Technologies Confidential and Proprietary.
 */

package com.android.qrdfileexplorer;

import java.io.File;
import java.util.Comparator;

public class FileTypeComparator implements Comparator<File> {

    private final boolean mIsAsc;
    private Object mMutex = new Object();

    public FileTypeComparator(boolean isAsc) {

        super();
        mIsAsc = isAsc;
    }

    @Override
    public int compare(File file1, File file2) {

        synchronized (mMutex) {
            int ret = comp(file1, file2);

            if (mIsAsc) {
                return ret;
            } else {
                return -ret;
            }
        }
    }

    public int comp(File file1, File file2) {
        int ret = 0;

        if (file1.isDirectory() && file2.isFile())
            return -1;
        if (file1.isFile() && file2.isDirectory())
            return 1;

        String nameStr1 = file1.getName();
        String nameStr2 = file2.getName();

        if (file1.isDirectory() && file2.isDirectory()) {
            return FileNameComparator.compStringAndNumber(nameStr1, nameStr2);
        }

        String str1 = file1.getName();
        String str2 = file2.getName();

        int index = str1.lastIndexOf(".");
        if (index != -1) {
            str1 = str1.substring(index + 1);
        } else {
            str1 = null;
        }

        index = str2.lastIndexOf(".");
        if (index != -1) {
            str2 = str2.substring(index + 1);
        } else {
            str2 = null;
        }

        if (str1 == null && str2 != null) {
            return -1;
        } else if (str1 != null && str2 == null) {
            return 1;
        } else {
            // do nothing
        }

        if (str1 != null && str2 != null) {
            str1 = str1.toLowerCase();
            str2 = str2.toLowerCase();

            ret = FileNameComparator.compStringAndNumber(str1, str2);
            if (ret != 0)
                return ret;
        }

        return FileNameComparator.compStringAndNumber(nameStr1, nameStr2);
    }
}
