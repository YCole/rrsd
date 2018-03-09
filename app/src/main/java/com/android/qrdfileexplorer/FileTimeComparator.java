/**
 * Copyright (c) 2013 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Qualcomm Technologies Confidential and Proprietary.
 */

package com.android.qrdfileexplorer;

import java.io.File;
import java.util.Comparator;

public class FileTimeComparator implements Comparator<File> {

    private final boolean mIsAsc;

    public FileTimeComparator(boolean isAsc) {

        super();
        mIsAsc = isAsc;
    }

    @Override
    public int compare(File file1, File file2) {

        long retLong = comp(file1, file2);

        retLong = mIsAsc ? retLong : -retLong;

        if (retLong >= Integer.MAX_VALUE)
            retLong = Integer.MAX_VALUE;
        if (retLong <= Integer.MIN_VALUE)
            retLong = Integer.MIN_VALUE;

        int ret = (int) retLong;

        return ret;
    }

    public long comp(File file1, File file2) {

        if (file1.isDirectory() && file2.isFile())
            return -1;
        if (file1.isFile() && file2.isDirectory())
            return 1;
        return (file1.lastModified() - file2.lastModified());
    }
}
