/*
 * Copyright (c) 2011,2013 Qualcomm Technologies inc, All Rights Reserved.
 * Qualcomm Technologies Confidential and Proprietary.
 */

package com.android.qrdfileexplorer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.media.MediaFile;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.Settings;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

/** File copy/delete/move */
public class FileUtils {
    public static final String TAG = "FileExplorer";

    private static String GET_PHONE_STORAGE_DIR_METHOD_NAME = "getExternalStorageDirectory";
    private static String GET_PHONE_STORAGE_STATE_METHOD_NAME = "getExternalStorageState";

    public static final String OPEN_PATH = "path";
    public static final String CATEGORY_FILE = "categoryFile";
    public static final String CATEGORY_TYPE = "categoryType";
    public static final String CATEGORY_PATH = "/category";
    public static final String REQUEST = "request";
    public static final String RESULT = "result";
    public static final String OPERATE_MODE = "operationMode";
    public static final String START_MODE = "startMode";
    public static final String USER_BACK = "userback";
    public static final int FILE_OPERATION_OK = 0;
    public static final int FILE_OP_ERR_MEMORY_FULL = 1;
    public static final int FILE_OP_ERR_NOT_SD_FILE = 2;
    public static final int FILE_OP_ERR_SRC_EQU_DEST = 3;
    public static final int FILE_OP_ERR_OTHER = 7;
    public static final int IMAGE_ICON_SIZE = 70;
    public static final long INVALID_NUM = -1;
    public static long mCountSize = 0;
    public static long mCountNum = 0;
    public static int mMoveAndDeleteFlag = 0;

    public static int processEvent = FolderFragment.EVENT_PROGRESS_EVENT;

    /** Delete files or folders */
    public static boolean deleteFiles(Context context, final String path, Handler handler,
            long mTotalFileNum) {

        if (!isFileOnExternalDrive(context, path)) {
            return false;
        }

        boolean ret = true;
        File f = new File(path);

        if (!f.isDirectory()) {
            ret = f.delete();
            sendProgressMessage(handler, mTotalFileNum);

            return ret;
        }

        Stack<File> stack = new Stack<File>();
        Stack<File> stackDir = new Stack<File>();

        stack.push(f);
        stackDir.push(f);

        // remove file here
        while (!stack.isEmpty() && (null != (f = stack.pop()))) {

            for (File file : f.listFiles()) {

                if (file.isDirectory()) {
                    stack.push(file);
                    stackDir.push(file);
                } else {
                    ret = file.delete();
                    sendProgressMessage(handler, mTotalFileNum);
                }

                if (ret == false) return false;
            }
        }

        // remove the empty directory
        while (!stackDir.isEmpty() && (null != (f = stackDir.pop()))) {
            ret = f.delete();

            if (ret == false) return false;
        }

        return ret;
    }

    private static void sendProgressMessage(Handler handler, long mTotalFileNum) {
        if (mTotalFileNum == INVALID_NUM) {
            return;

        } else {
            mCountNum++;
            int number = (int) ((100 * mCountNum) / mTotalFileNum);

            Message progressMessage = handler.obtainMessage();
            Bundle b = new Bundle();
            b.putInt("event", processEvent);
            b.putInt("number", number);
            progressMessage.setData(b);
            handler.sendMessage(progressMessage);

            // When we delete all files,reset the counter to zero
            if (mCountNum >= mTotalFileNum) {
                mCountNum = 0;
            }
        }
    }

    /** Copy folders */
    private static boolean copyDir(Context context, File srcDir, File dstDir, Handler handler,
            long totalSize) {

        if (!ableToCreateDir(dstDir)) {
            return false;
        }

        Queue<File> queue = new LinkedList<File>();
        queue.offer(srcDir);

        Queue<File> queueDst = new LinkedList<File>();
        queueDst.offer(dstDir);

        File files = null;

        while (null != (files = queue.poll()) && null != (dstDir = queueDst.poll())) {

            for (File file : files.listFiles()) {

                File dstFile = new File(dstDir, file.getName());

                if (file.isDirectory()) {
                    if (!ableToCreateDir(dstFile)) return false;

                    queue.offer(file);
                    queueDst.offer(dstFile);
                } else {
                    copyFile(context, file, dstFile, handler, totalSize);
                }
            }
        }

        return true;
    }

    private static boolean ableToCreateDir(File dstDir) {

        if (dstDir.exists()) {
            if (dstDir.isDirectory() == false) {
                return false;
            }
        } else {
            if (dstDir.mkdirs() == false) {
                return false;
            }
        }

        if (dstDir.canWrite() == false) {
            return false;
        }

        return true;
    }

    private static boolean isFileOnExtSdcard(final String srcFilePath, final String extSdcardPath) {
        return (isPathValid(extSdcardPath) && (srcFilePath.startsWith(extSdcardPath, 0)));
    }

    private static boolean isFileOnIntSdcard(final String srcFilePath, final String intSdcardPath) {
        return (isPathValid(intSdcardPath) && (srcFilePath.startsWith(intSdcardPath, 0)));
    }

    private static boolean isFileOnUsbStorage(final String srcFilePath, final String extUsbPath) {
        return (isPathValid(extUsbPath) && (srcFilePath.startsWith(extUsbPath, 0)));
    }

    private static boolean isFileOnUiccStorage(final String srcFilePath, final String extUiccPath) {
        return (isPathValid(extUiccPath) && (srcFilePath.startsWith(extUiccPath, 0)));
    }

    // Copy/Move operations just allowed on sdcard folder/file
    private static boolean isFileOnExternalDrive(Context context, final String srcFilePath) {
        if (srcFilePath == null) {
            Log.e(TAG, "Invalid source file path provided!");
            return false;
        }

        // fetch individual paths for external & internal drives
        String extUsbPath = getUSBPath(context);
        String extUiccPath = getUICCPath(context);
        String extSdcardPath = getSDPath(context);
        String intSdcardPath = getInternalPath();

        return (isFileOnExtSdcard(srcFilePath, extSdcardPath) ||
                isFileOnUsbStorage(srcFilePath, extUsbPath) ||
                isFileOnUiccStorage(srcFilePath, extUiccPath) || isFileOnIntSdcard(srcFilePath,
                    intSdcardPath));

    }

    /** Move files or folders */
    public static boolean moveFiles(Context context, File srcFile, File dstFile, Handler handler,
            long totalSize) {
        if (!isFileOnExternalDrive(context, srcFile.getPath())) {
            return false;
        }

        boolean ret = true;
        ret = srcFile.compareTo(dstFile) != 0
                && (srcFile.renameTo(dstFile) ||
                copyFiles(context, srcFile, dstFile, handler, totalSize) == FILE_OPERATION_OK);
        if (ret) {
            deleteFiles(context, srcFile.getPath(), handler, INVALID_NUM);
        }
        return ret;
    }

    /** Copy files or folders */
    public static int copyFiles(Context context, File srcFile, File dstFile, Handler handler,
            long totalSize) {
        if (!isFileOnExternalDrive(context, srcFile.getPath())) {
            return FILE_OP_ERR_NOT_SD_FILE;
        }

        boolean ret = true;

        if (srcFile.getParent().equals(dstFile)) {
            return FILE_OP_ERR_SRC_EQU_DEST;
        }

        if (checkRemainSpace(srcFile, dstFile)) {
            Log.d(TAG, "------checkRemainSpace fail");
            return FILE_OP_ERR_MEMORY_FULL;
        }

        if (srcFile.isDirectory()) {
            // We will not copy the dir only when the dstFile is sub-directory
            // of
            // the scrFile. If they are in the same directory, need to copy.
            if (dstFile.getPath().indexOf(srcFile.getPath()) == 0
                    && !dstFile.getParentFile().getAbsolutePath()
                    .equals(srcFile.getParentFile().getAbsolutePath())) {
                return FILE_OP_ERR_OTHER;
            } else {
                if (copyDir(context, srcFile, dstFile, handler, totalSize) == false) {
                    return FILE_OP_ERR_OTHER;
                }
            }
        } else {
            ret = copyFile(context, srcFile, dstFile, handler, totalSize);
        }

        return FILE_OPERATION_OK;
    }

    /** Copy binary file */
    public static boolean copyFile(Context context, File srcFile, File dstFile, Handler handler,
            long totalSize) {
        try {
            if (!isFileOnExternalDrive(context, srcFile.getPath())) {
                return false;
            }

            InputStream in = new FileInputStream(srcFile);
            if (dstFile.exists()) {
                dstFile.delete();
            }

            FileOutputStream out = new FileOutputStream(dstFile);
            FileDescriptor fd = out.getFD();

            int number = 0;

            try {
                int cnt;
                byte[] buf = new byte[4096];
                while ((cnt = in.read(buf)) >= 0) {
                    out.write(buf, 0, cnt);
                    mCountSize = mCountSize + cnt;
                    number = (int) ((100 * mCountSize) / totalSize);

                    if (number  < 100) {
                        Message progressMessage = handler.obtainMessage();
                        Bundle b = new Bundle();
                        b.putInt("event", processEvent);
                        b.putInt("number", number);
                        progressMessage.setData(b);
                        handler.sendMessage(progressMessage);
                    }
                }

                out.flush();
                fd.sync(); // ensure the buffer data write out to device

                if (number == 100) {
                    Message progressMessage = handler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putInt("event", processEvent);
                    b.putInt("number", number);
                    progressMessage.setData(b);
                    handler.sendMessage(progressMessage);
                }

                if (mCountSize >= totalSize) {
                    mCountSize = 0;
                }
            } finally {
                out.close();
                in.close();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean fileExist(String fileName) {
        boolean ret = false;

        File f = new File(fileName);
        if (f.exists()) {
            ret = true;
        }

        return ret;
    }

    protected static boolean ExternalMountableDiskExist(Context context) {
        boolean retVal = false;
        // Check if external drives like SD card / USB etc. are mounted
        retVal = SDExist(context) || USBExist(context) || UICCExist(context);

        return retVal;
    }

    public static boolean SDExist(Context context) {
        boolean ret = false;
        StorageManager storageManager = (StorageManager) context
                .getSystemService(Context.STORAGE_SERVICE);
        String sdPath = getSDPath(context);
        if (sdPath != null && storageManager.getVolumeState(getSDPath(context)).equals(
                android.os.Environment.MEDIA_MOUNTED)) {
            ret = true;
        }

        return ret;
    }

    public static boolean USBExist(Context context) {
        boolean ret = false;
        StorageManager storageManager = (StorageManager) context
                .getSystemService(Context.STORAGE_SERVICE);

        if (storageManager == null) {
            Log.e(TAG, "Invalid reference to StorageManager received.");
            return ret;
        }

        try {
            if (storageManager.getVolumeState(getUSBPath(context)).equals(
                    android.os.Environment.MEDIA_MOUNTED)) {
                ret = true;
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        return ret;
    }

    public static boolean UICCExist(Context context) {
        boolean ret = false;
        StorageManager storageManager = (StorageManager) context
                .getSystemService(Context.STORAGE_SERVICE);

        try {
            if (storageManager.getVolumeState(getUICCPath(context)).equals(
                    android.os.Environment.MEDIA_MOUNTED)) {
                ret = true;
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        return ret;
    }

    public static ArrayList<String> getExternalMountedVolumes(Context context) {
        ArrayList<String> mountPoints = new ArrayList<String>();

        String usbPath = getUSBPath(context);
        String uiccPath = getUICCPath(context);
        String sdCardPath = getSDPath(context);

        if ((usbPath != null) && (usbPath.length() > 0) && USBExist(context))
            mountPoints.add(usbPath);

        if ((uiccPath != null) && (uiccPath.length() > 0) && UICCExist(context))
            mountPoints.add(uiccPath);

        if ((sdCardPath != null) && (sdCardPath.length() > 0) && SDExist(context))
            mountPoints.add(sdCardPath);

        return mountPoints;
    }

    public static String getUSBPath(Context context) {
        String usb = null;
        String usbDescription = context.getString(R.string.storage_usb);

        StorageManager storageManager = (StorageManager) context
                .getSystemService(Context.STORAGE_SERVICE);
        StorageVolume[] volumes = storageManager.getVolumeList();

        for (int i = 0; i < volumes.length; i++) {

            if (volumes[i].isRemovable() //&& volumes[i].allowMassStorage()
                    && volumes[i].getDescription(context).contains("USB")) {
                usb = volumes[i].getPath();
            }
        }
        return usb;
    }

    public static String getUICCPath(Context context) {
        String uicc = null;
        String uiccDescription = context.getString(R.string.storage_uicc);

        StorageManager storageManager = (StorageManager) context
                .getSystemService(Context.STORAGE_SERVICE);
        StorageVolume[] volumes = storageManager.getVolumeList();

        for (int i = 0; i < volumes.length; i++) {

            if (volumes[i].isRemovable()
                    && volumes[i].getPath().contains("uicc0")) {
                uicc = volumes[i].getPath();
            }
        }
        return uicc;
    }

    public static String getSDPath(Context context) {
        String sd = null;
        String sdDescription = context.getString(R.string.storage_sd_card);

        StorageManager storageManager = (StorageManager) context
                .getSystemService(Context.STORAGE_SERVICE);
        StorageVolume[] volumes = storageManager.getVolumeList();
        for (int i = 0; i < volumes.length; i++) {
            if (volumes[i].isRemovable() && volumes[i].allowMassStorage()
                    && volumes[i].getDescription(context).contains("SD")) {
                sd = volumes[i].getPath();
            }
        }
        return sd;
    }

    public static String getInternalPath() {
        File sd = null;
        sd = Environment.getExternalStorageDirectory();
        if (sd != null) {
            return sd.toString();
        } else
            return null;
    }

    public static String getFavoritePath() {
        return getInternalPath() + "/DCIM/Camera";
    }

    private static boolean isPathValid(String filePath) {
        return ((filePath != null) && (filePath.length() > 0));
    }

    public static String getPathTitle(Context context, File file) {
        if (file == null) {
            Log.e(TAG, "Invalid File object received!");
            return null;
        }

        String retVal = null;
        String pathString = file.getPath();

        if (pathString.equals(FileUtils.getUICCPath(context))) {
            retVal = context.getString(R.string.uicc);

        } else if (pathString.equals(FileUtils.getUSBPath(context))) {
            retVal = context.getString(R.string.usb);

        } else if (pathString.equals(FileUtils.getSDPath(context))) {
            retVal = context.getString(R.string.sdcard);

        } else if (pathString.equals(FileUtils.getInternalPath())) {
            retVal = context.getString(R.string.phone_storage);

        } else {
            retVal = file.getName();
        }

        return retVal;
    }

    // transtlate the storage path to title,such as "/storage/sdcard0" to
    // "/SD card"
    public static String fromPathtoText(Context context, String path) {
        String text = "";
        // parse the category path to title
        if (path.equals(CATEGORY_PATH)) {
            switch (MainActivity.fileType) {
                case CategoryFragment.MUSIC:
                    text = context.getString(R.string.music);
                    break;
                case CategoryFragment.VIDEO:
                    text = context.getString(R.string.video);
                    break;
                case CategoryFragment.IMAGE:
                    text = context.getString(R.string.image);
                    break;
                case CategoryFragment.DOC:
                    text = context.getString(R.string.doc);
                    break;
                case CategoryFragment.ARCHIVE:
                    text = context.getString(R.string.archive);
                    break;
                case CategoryFragment.APK:
                    text = context.getString(R.string.apk);
                    break;
                case CategoryFragment.FAVORITE:
                    text = context.getString(R.string.favorite);
                default:
                    text = "Unknow";
                    break;
            }
            text = "/" + text;
            return text;
        }
        // parse the storage path to title
        String sdcard = "/" + context.getString(R.string.sdcard);

        // for usb otg
        String usb = "/" + context.getString(R.string.usb);

        // for UICC
        String uicc = "/" + context.getString(R.string.uicc);

        String phone_storage = "/" + context.getString(R.string.phone_storage);
        if (path.startsWith("/storage", 0)) {
            if (path.equals("/storage")) {
                text = "/";
            } else {

                String dirPath = null;

                if (isPathValid((dirPath = getUICCPath(context))) && (path.startsWith(dirPath, 0))) {
                    text = uicc + path.substring(getUICCPath(context).length(), path.length());

                } else if (isPathValid((dirPath = getUSBPath(context)))
                        && (path.startsWith(dirPath, 0))) {
                    text = usb + path.substring(getUSBPath(context).length(), path.length());

                } else if (isPathValid((dirPath = getSDPath(context)))
                        && (path.startsWith(dirPath, 0))) {
                    text = sdcard + path.substring(getSDPath(context).length(), path.length());

                } else if (isPathValid((dirPath = getInternalPath()))
                        && (path.startsWith(dirPath, 0))) {
                    text = phone_storage
                            + path.substring(getInternalPath().length(), path.length());

                } else { // this file is under the root path "/storage"
                    text = path.substring(8, path.length());
                }
            }
        } else {// load the "/storage" path if it is not
            text = "/storage";
        }
        return text;
    }

    // transtlate the title to storage path,such as "/SD card" to
    // "/storage/sdcard0"
    public static String fromTexttoPath(Context context, String text) {
        String path = "";
        String music = "/" + context.getString(R.string.music);
        String video = "/" + context.getString(R.string.video);
        String image = "/" + context.getString(R.string.image);
        String doc = "/" + context.getString(R.string.doc);
        String archive = "/" + context.getString(R.string.archive);
        String apk = "/" + context.getString(R.string.apk);
        String favorite = "/" + context.getString(R.string.favorite);
        String sdcard = "/" + context.getString(R.string.sdcard);
        String phone_storage = "/" + context.getString(R.string.phone_storage);

        // for usb otg
        String usb = "/" + context.getString(R.string.usb);
        // UICC
        String uicc = "/" + context.getString(R.string.uicc);

        if (text.equals(music) || text.equals(video) || text.equals(image)
                || text.equals(doc) ||
                text.equals(archive) || text.equals(apk) || text.startsWith(favorite, 0)) {
            path = CATEGORY_PATH;
        } else {
            if (text.equals("/"))
                path = "/storage";
            else {
                if (text.startsWith(usb, 0)) {
                    path = getUSBPath(context) + text.substring(usb.length(), text.length());
                } else if (text.startsWith(uicc, 0)) {
                    path = getUICCPath(context) + text.substring(uicc.length(), text.length());
                } else if (text.startsWith(sdcard, 0)) {
                    path = getSDPath(context) + text.substring(sdcard.length(), text.length());
                } else if (text.startsWith(phone_storage, 0)) {
                    path = getInternalPath()
                            + text.substring(phone_storage.length(), text.length());
                } else {// this path is not exist under the "storage"
                    path = "";
                }
            }
        }
        return path;
    }

    // add check free space function
    // true is over free space
    public static boolean checkRemainSpace(File srcFile, File dstFile) {
        if ((getAvailableAnySpace(dstFile.getParent()) - srcFile.length()) >= 0)
            return false;

        return true;
    }

    /**
     * Get the file type
     *
     * @param file the file
     */
    public static int getFileType(File file) {
        String path = file.getPath();
        String mimeType = MediaFile.getMimeTypeForFile(path);
        return MediaFile.getFileTypeForMimeType(mimeType);
    }

    /**
     * Decode and covert a image to a small square icon bitmap for display.
     *
     * @param file the image file
     * @param bitmap the icon bitmap
     */
    public static Bitmap getImageThumbnail(File file, Bitmap bitmap) {
        // try to decode from JPEG EXIF
        ExifInterface exif = null;
        byte[] thumbData = null;
        try {
            exif = new ExifInterface(file.getPath());
            if (exif != null) {
                /*
                 * Returns the JPEG compressed thumbnail inside the image file,
                 * or null if there is no JPEG compressed thumbnail.
                */
                thumbData = exif.getThumbnail();
            }
        } catch (Throwable e) {
            Log.w(TAG, "fail to get exif thumb", e);
        }

        Bitmap iconBitmap = null;
        if (thumbData != null) {
            iconBitmap = decodeFromByteArray(thumbData, bitmap);
        } else {
            iconBitmap = decodeFromFile(file, bitmap);
        }

        iconBitmap = ThumbnailUtils.extractThumbnail(iconBitmap, IMAGE_ICON_SIZE, IMAGE_ICON_SIZE,
                ThumbnailUtils.OPTIONS_RECYCLE_INPUT);

        return iconBitmap;
    }

    private static int compressSize(BitmapFactory.Options options, Bitmap bitmap) {
        int max = Math.max(options.outHeight, options.outWidth);

        // Compute the sampleSize of the options
        int size = (int) (max / (float) Math.max(bitmap.getWidth(), bitmap.getHeight()));
        if (size <= 0) {
            size = 1;
        }

        return size;
    }

    private static Bitmap decodeFromByteArray(byte[] thumbData, Bitmap bitmap) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        // Decode the width and height of the bitmap, but don't load the bitmap
        // to RAM
        BitmapFactory.decodeByteArray(thumbData, 0, thumbData.length, options);
        options.inSampleSize = compressSize(options, bitmap);

        // Decode the width and height of the bitmap and load the bitmap to RAM
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(thumbData, 0, thumbData.length, options);
    }

    private static Bitmap decodeFromFile(File file, Bitmap bitmap) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        // Decode the width and height of the bitmap, but don't load the bitmap
        // to RAM
        BitmapFactory.decodeFile(file.getPath(), options);
        options.inSampleSize = compressSize(options, bitmap);

        // Decode the width and height of the bitmap and load the bitmap to RAM
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(file.getPath(), options);
    }

    /**
     * Compute the file length.
     *
     * @param file
     * @return
     */
    public static long getFileSize(File file) {
        if (file == null || !file.exists()) {
            return 0;
        }

        if (!file.isDirectory()) {
            return file.length();
        }

        long size = 0;

        Queue<File> queue = new LinkedList<File>();
        queue.offer(file);

        while (null != (file = queue.poll())) {

            for (File fileTemp : file.listFiles()) {
                if (!fileTemp.isDirectory()) {
                    size += fileTemp.length();
                } else {
                    queue.offer(fileTemp);
                }
            }
        }

        return size;
    }

    // Get available space of local file folder.
    public static long getAvailableAnySpace(String path) {
        long remaining = 0;
        try {
            StatFs stat = new StatFs(path);
            remaining = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
        } catch (Exception e) {
            // do nothing
        }
        return remaining;
    }

    public static boolean internalStorageExist() {
        boolean ret = false;

        if (null != getInternalStoragePath()
                && Environment.MEDIA_MOUNTED.equals(getInternalStorageState())) {
            ret = true;
        }

        return ret;
    }

    public static String getInternalStorageState() {

        String ret = null;
        try {
            Method method = Environment.class.getMethod(GET_PHONE_STORAGE_STATE_METHOD_NAME,
                    (Class[]) null);
            ret = (String) method.invoke(null, (Object[]) null);
        } catch (Exception e) {
            // e.printStackTrace();
        }

        return ret;
    }

    public static String getInternalStoragePath() {
        String ret = null;

        try {
            Method method = Environment.class.getMethod(GET_PHONE_STORAGE_DIR_METHOD_NAME,
                    (Class[]) null);
            File file = (File) method.invoke(null, (Object[]) null);
            ret = file.getPath();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    private static boolean mIsAppsInstallingAllowed = false;

    public static void refreshIsAppInstallingAllowed(Context context) {
        mIsAppsInstallingAllowed = Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.INSTALL_APPS, 1) > 0;
    }

    public static boolean getIsAppsInstallingAllowed() {
        return mIsAppsInstallingAllowed;
    }
}
