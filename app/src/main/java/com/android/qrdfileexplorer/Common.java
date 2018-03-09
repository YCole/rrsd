/**
 * Copyright (c) 2013 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Qualcomm Technologies Confidential and Proprietary.
 */

package com.android.qrdfileexplorer;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.PowerManager;
import android.text.TextUtils;

import java.io.File;
import java.util.HashMap;


public class Common {
    public static String TAG = "Common";

    public static final String PREF_FILE_EXPLORER = "file_explorer_preference";

    public static final boolean ID_SORT_ASC = true;
    public static final boolean ID_SORT_DEC = false;

    public static String MEDIA_SCAN_DATA_KEY = "FileChange";
    public static String MEDIA_SCAN_DATA_VALUE = "FileExplore";

    // support media type information
    public static final String TYPE_PICTURE = "jpg" + "|" + "jpeg" + "|"
            + "gif" + "|" + "png" + "|" + "bmp" + "|" + "wbmp" + "|" + "webp";

    public static final String TYPE_VIDEO = "mpeg" + "|" + "mpg" + "|"
            + "mp4" + "|" + "m4v" + "|" + "3gp" + "|" + "3gpp" + "|" + "3g2" + "|"
            + "mkv" + "|" + "webm" + "|" + "ts" + "|" + "avi";

    public static final String TYPE_AUDIO = "mp3" + "|" + "mid" + "|" + "midi" + "|" + "m4a" + "|"
            + "aac" + "|" + "amr" + "|" + "ogg" + "|" + "oga" + "|" + "wav"
            + "|" + "mka";

    public static final String TYPE_VCF = "vcf";
    public static final String TYPE_VCS = "vcs";

    public static final String TYPE_WORD = "doc" + "|" + "docx";
    public static final String TYPE_XLS = "xls" + "|" + "xlsx";
    public static final String TYPE_PPT = "ppt" + "|" + "pps" + "|" + "pptx" + "|" + "ppsx";
    public static final String TYPE_PDF = "pdf";
    public static final String TYPE_TEXT = "txt" + "|" + "text";

    public static final String TYPE_APK = "apk";

    public static final String TYPE_ZIP = "zip";
    public static final String TYPE_JAR = "jar";
    public static final String TYPE_RAR = "rar";

    public static final int ID_TYPE_PICTURE = 100;
    public static final int ID_TYPE_VIDEO = 101;
    public static final int ID_TYPE_AUDIO = 102;
    public static final int ID_TYPE_VCF = 103;
    public static final int ID_TYPE_VCS = 104;
    public static final int ID_TYPE_WORD = 105;
    public static final int ID_TYPE_XLS = 106;
    public static final int ID_TYPE_PPT = 107;
    public static final int ID_TYPE_PDF = 108;
    public static final int ID_TYPE_APK = 109;
    public static final int ID_TYPE_JAR = 110;
    public static final int ID_TYPE_RAR = 111;
    public static final int ID_TYPE_ZIP = 112;
    public static final int ID_TYPE_FOLDER = 113;
    public static final int ID_TYPE_UNKOWN = 114;
    public static final int ID_TYPE_TEXT = 115;

    // sort id inforation
    public static final int ID_SORT_NAME_ASC = 0;
    public static final int ID_SORT_NAME_DEC = 1;
    public static final int ID_SORT_TYPE_ASC = 2;
    public static final int ID_SORT_TYPE_DEC = 3;
    public static final int ID_SORT_SIZE_ASC = 4;
    public static final int ID_SORT_SIZE_DEC = 5;
    public static final int ID_SORT_TIME_ASC = 6;
    public static final int ID_SORT_TIME_DEC = 7;

    public static final String KEY_SORT_TYPE_FOR_CATEGORY = "SortType_For_Category";
    public static final String KEY_SORT_TYPE_FOR_FOLDER = "SortType_For_Folder";
    public static final String KEY_SORT_TYPE_FOR_FAVORITE = "SortType_For_Favorite";
    public static final int DEFAULT_SORT_TYPE_FOR_CATEGORY = ID_SORT_TIME_ASC;
    public static final int DEFAULT_SORT_TYPE_FOR_FAVORITE = ID_SORT_NAME_ASC;
    public static final int DEFAULT_SORT_TYPE_FOR_FOLDER = ID_SORT_NAME_ASC;
    //used to remember sort type for Category, Favorite, Folder
    private static HashMap<String, Integer> sortType_hashMap = new HashMap<String, Integer>();

    private static PowerManager.WakeLock mLock = null;

    private static class IconData {
        private int mTypeId;
        private int mResId;

        public IconData(int typeId, int resId) {
            mTypeId = typeId;
            mResId = resId;
        }
    }

    private static IconData[] mIconData = {

            new IconData(ID_TYPE_FOLDER, R.drawable.icon_folder),
            new IconData(ID_TYPE_PICTURE, R.drawable.icon_file_pic),
            new IconData(ID_TYPE_VIDEO, R.drawable.icon_file_video),
            new IconData(ID_TYPE_AUDIO, R.drawable.icon_file_audio),
            new IconData(ID_TYPE_VCF, R.drawable.icon_file_vcf),
            new IconData(ID_TYPE_WORD, R.drawable.icon_file_word),
            new IconData(ID_TYPE_XLS, R.drawable.icon_file_xls),
            new IconData(ID_TYPE_PPT, R.drawable.icon_file_ppt),
            new IconData(ID_TYPE_PDF, R.drawable.icon_file_pdf),
            new IconData(ID_TYPE_TEXT, R.drawable.icon_file_txt),
            new IconData(ID_TYPE_APK, R.drawable.icon_file_apk),
            new IconData(ID_TYPE_JAR, R.drawable.icon_archive),
            new IconData(ID_TYPE_RAR, R.drawable.icon_archive),
            new IconData(ID_TYPE_ZIP, R.drawable.icon_archive),
            new IconData(ID_TYPE_VCS, R.drawable.icon_file_vcs),
            new IconData(ID_TYPE_UNKOWN, R.drawable.icon_file_unknown),
    };

    public static void setSortType(String key_sort, int sortType) {
        sortType_hashMap.put(key_sort, new Integer(sortType));
    }

    public static int getSortType(String key_sort) {
        return sortType_hashMap.get(key_sort).intValue();
    }

    /**
     * Get file type id information
     */
    public static int getFileTypeId(File file) {
        int ret = ID_TYPE_UNKOWN;

        if (file.isDirectory()) {
            ret = ID_TYPE_FOLDER;
        } else {
            String fileName = file.getName();
            String ext = fileName.substring(fileName.lastIndexOf(".")
                    + 1, fileName.length()).toLowerCase();

            if (null == ext) {
                return ret;
            }
            if (ext.matches(TYPE_PICTURE)) {
                ret = ID_TYPE_PICTURE;
            } else if (ext.matches(TYPE_VIDEO)) {
                ret = ID_TYPE_VIDEO;
            } else if (ext.matches(TYPE_AUDIO)) {
                ret = ID_TYPE_AUDIO;
            } else if (ext.matches(TYPE_VCF)) {
                ret = ID_TYPE_VCF;
            } else if (ext.matches(TYPE_VCS)) {
                ret = ID_TYPE_VCS;
            } else if (ext.matches(TYPE_WORD)) {
                ret = ID_TYPE_WORD;
            } else if (ext.matches(TYPE_XLS)) {
                ret = ID_TYPE_XLS;
            } else if (ext.matches(TYPE_PPT)) {
                ret = ID_TYPE_PPT;
            } else if (ext.matches(TYPE_PDF)) {
                ret = ID_TYPE_PDF;
            } else if (ext.matches(TYPE_APK)) {
                ret = ID_TYPE_APK;
            } else if (ext.matches(TYPE_TEXT)) {
                ret = ID_TYPE_TEXT;
            } else if (ext.matches(TYPE_ZIP)) {
                ret = ID_TYPE_ZIP;
            } else if (ext.matches(TYPE_JAR)) {
                ret = ID_TYPE_JAR;
            } else if (ext.matches(TYPE_RAR)) {
                ret = ID_TYPE_RAR;
            }
        }

        return ret;
    }

  public static int getFileType(File f) {
        int type = -1;
        String fileName = f.getName();
        String ext = fileName.substring(fileName.lastIndexOf(".")
                + 1, fileName.length()).toLowerCase();
        if (ext.equals("mp3") || ext.equals("amr") || ext.equals("wma")
                || ext.equals("aac") || ext.equals("m4a") || ext.equals("mid")
                || ext.equals("xmf") || ext.equals("ogg") || ext.equals("wav")
                || ext.equals("qcp") || ext.equals("awb")) {
            type = CategoryFragment.MUSIC;
        }
        else if (ext.equals("3gp") || ext.equals("avi") || ext.equals("mp4")
                || ext.equals("3g2") || ext.equals("wmv") || ext.equals("divx")
                || ext.equals("mkv") || ext.equals("webm") || ext.equals("ts")
                || ext.equals("asf") ) {
            type = CategoryFragment.VIDEO;
        }
        else if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("gif")
                || ext.equals("png") || ext.equals("bmp")) {
            type = CategoryFragment.IMAGE;
        }
        else if (ext.equals("doc") || ext.equals("docx") || ext.equals("xls")
                || ext.equals("xlsx") || ext.equals("ppt") || ext.equals("pptx")
                || ext.equals("txt") || ext.equals("text") || ext.equals("pdf")) {
            type = CategoryFragment.DOC;
        }
        else if (ext.equals("rar") || ext.equals("zip") || ext.equals("tar") || ext.equals("gz")
                || ext.equals("iso") || ext.equals("jar") || ext.equals("cab") || ext.equals("7z")
                || ext.equals("ace")) {
            type = CategoryFragment.ARCHIVE;
        }
        else if (ext.equals("apk")) {
            type = CategoryFragment.APK;
        }
        else if ( ext.equals("3gpp")) {
            type = CategoryFragment.MUSICandVIDEO;
        }
        else {
            type = CategoryFragment.TOTAL;
        }
        return type;
    }

    public static int getFileType(String type) {
        if (!TextUtils.isEmpty(type)) {
            if ("image/*".equalsIgnoreCase(type)) {
                return CategoryFragment.IMAGE;
            } else if ("video/*".equalsIgnoreCase(type)) {
                return CategoryFragment.VIDEO;
            } else if ("audio/*".equalsIgnoreCase(type)) {
                return CategoryFragment.MUSIC;
            } else if ("application/*".equalsIgnoreCase(type)) {
                return CategoryFragment.APK;
            }
        }
        return CategoryFragment.INVALID_TYPE;
    }

    /**
     * Load the icon bitmap information
     */
    public static Bitmap getDefaultIcon(Context context, File file) {
        Bitmap ret = null;

        int typeId = getFileTypeId(file);

        for (int i = 0; i < mIconData.length; i++) {

            if (typeId == mIconData[i].mTypeId) {
                ret = BitmapFactory.decodeResource(context.getResources(),
                        mIconData[i].mResId);
                break;
            }
        }

        return ret;

    }

    public static void setWakelock(Context context) {
        if (null != mLock) {
            mLock.release();
            mLock = null;
        }

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Lock");
        mLock.setReferenceCounted(true);
        mLock.acquire();
    }

    public static void unlock() {
        if (null != mLock) {
            mLock.release();
            mLock = null;
        }
    }
}
