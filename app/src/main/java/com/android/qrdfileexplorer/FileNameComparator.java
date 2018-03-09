/**
 * Copyright (c) 2013 Qualcomm Technologies,Inc.
 * All Rights Reserved.
 * Qualcomm Technologies Confidential and Proprietary.
 */

package com.android.qrdfileexplorer;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;

public class FileNameComparator implements Comparator<File> {

    private final boolean mIsAsc;
    private Object mMutex = new Object();

    private final static Collator sCollator = Collator.getInstance();

    public FileNameComparator(boolean isAsc) {

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

        if (file1.isDirectory() && file2.isFile())
            return -1;
        if (file1.isFile() && file2.isDirectory())
            return 1;

        String str1 = file1.getName();
        String str2 = file2.getName();

        return compStringAndNumber(str1, str2);
    }

    static class StrAndNum {

        String str;
        boolean isNumber = false;

        StrAndNum(String string, boolean isNum) {

            str = string;
            isNumber = isNum;
        }

        public int comp(StrAndNum strAndNum2) {

            if ((isNumber && !strAndNum2.isNumber) ||
                    (!isNumber && strAndNum2.isNumber) ||
                    (!isNumber && !strAndNum2.isNumber)) {
                return sCollator.compare(str, strAndNum2.str);
            }

            return compNum(str, strAndNum2.str);
        }
    }

    public static int compStringAndNumber(String str1, String str2) {

        ArrayList<StrAndNum> arrayList1 = sscanfNumber(str1);
        ArrayList<StrAndNum> arrayList2 = sscanfNumber(str2);

        int arrayListSize1 = arrayList1.size();
        int arrayListSize2 = arrayList2.size();
        int loopMax = arrayListSize1 < arrayListSize2 ? arrayListSize1 : arrayListSize2;

        int numStringLength = 0;
        for (int loop = 0; loop < loopMax; loop++) {

            StrAndNum andNum1 = arrayList1.get(loop);
            StrAndNum andNum2 = arrayList2.get(loop);

            int ret = andNum1.comp(andNum2);
            if (ret != 0) {
                return ret;
            }

            if (numStringLength == 0) {
                if (andNum1.str.length() != andNum2.str.length()) {
                    numStringLength = andNum1.str.length() > andNum2.str.length() ? -1 : 1;
                }
            }
        }

        if (arrayListSize1 != arrayListSize2) {
            return arrayListSize1 > arrayListSize2 ? 1 : -1;
        }

        return numStringLength;
    }

    private static int getNumStart(String str) {

        int loop = 0;
        int max = str.length();
        for (loop = 0; loop < max; loop++) {
            if (str.charAt(loop) != '0')
                break;
        }
        return loop;
    }

    public static int compNum(String str1, String str2) {

        int numStart1 = getNumStart(str1);
        int numStart2 = getNumStart(str2);
        int numLength1 = str1.length() - numStart1;
        int numLength2 = str2.length() - numStart2;

        if (numLength1 != numLength2) {
            return numLength1 > numLength2 ? 1 : -1;
        }

        final int LONG10LENGTH = 16;

        long num1 = 0;
        long num2 = 0;
        int subNumBegin1 = numStart1;
        int subNumBegin2 = numStart2;

        for (; subNumBegin1 < str1.length() || subNumBegin2 < str2.length(); ) {
            int subNumEnd1 = (subNumBegin1 + LONG10LENGTH) > str1.length() ? str1.length()
                    : subNumBegin1 + LONG10LENGTH;
            int subNumEnd2 = (subNumBegin2 + LONG10LENGTH) > str2.length() ? str2.length()
                    : subNumBegin2 + LONG10LENGTH;

            num1 = Long.valueOf(str1.substring(subNumBegin1, subNumEnd1));
            num2 = Long.valueOf(str2.substring(subNumBegin2, subNumEnd2));
            if (num1 != num2) {
                return num1 > num2 ? 1 : -1;
            }

            subNumBegin1 = subNumEnd1;
            subNumBegin2 = subNumEnd2;
        }

        return 0;
    }

    public static int compString(String str1, String str2) {

        int len1 = str1.length();
        int len2 = str2.length();
        int len = len1 <= len2 ? len1 : len2;
        for (int i = 0; i < len; i++) {

            int value1 = str1.codePointAt(i);
            int value2 = str2.codePointAt(i);

            // 'A' -> 'a'
            if (value1 > 64 && value1 < 91)
                value1 = value1 + 32;
            if (value2 > 64 && value2 < 91)
                value2 = value2 + 32;

            if (value1 == value2)
                continue;

            if (value1 > 0x80 && value2 > 0x80) {
                int ret = compHanzi(value1, value2);
                if (ret != 0)
                    return ret;
            }

            return value1 > value2 ? 1 : -1;
        }

        if (len1 == len2) {
            return 0;
        } else {
            return len1 > len2 ? 1 : -1;
        }

    }

    @SuppressWarnings("unused")
    public static int compHanzi(int wenzi1, int wenzi2) {

        String py1 = "null";
        String py2 = "null";

        if (py1 == null || py2 == null) {
            return wenzi1 > wenzi2 ? 1 : -1;
        }

        int pyLen1 = py1.length();
        int pyLen2 = py2.length();

        int pyLen = (pyLen1 <= pyLen2 ? pyLen1 : pyLen2);
        for (int j = 0; j < pyLen; j++) {

            int py1cur = py1.charAt(j);
            int py2cur = py2.charAt(j);

            if (py1cur > py2cur)
                return 1;
            if (py1cur < py2cur)
                return -1;
        }
        if (pyLen1 == pyLen2) {
            return 0;
        } else {
            return pyLen1 > pyLen2 ? 1 : -1;
        }
    }

    private static ArrayList<StrAndNum> sscanfNumber(String str) {

        ArrayList<StrAndNum> stringList = new ArrayList<StrAndNum>();
        StringBuffer bf = new StringBuffer();
        boolean isNumber = false;

        for (char a : str.toCharArray()) {

            if ('0' <= a && a <= '9') {

                if (!isNumber && !bf.toString().isEmpty()) {
                    stringList.add(new StrAndNum(bf.toString(), isNumber));
                    bf = new StringBuffer();
                }
                isNumber = true;
            } else {
                if (isNumber && !bf.toString().isEmpty()) {
                    stringList.add(new StrAndNum(bf.toString(), isNumber));
                    bf = new StringBuffer();
                }
                isNumber = false;
            }
            bf.append(a);
        }
        stringList.add(new StrAndNum(bf.toString(), isNumber));

        return stringList;
    }
}
