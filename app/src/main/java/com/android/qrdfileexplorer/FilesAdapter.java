/*
 * Copyright (c) 2011, 2013 Qualcomm Technologies inc
 * All Rights Reserved.
 * Qualcomm Technologies Confidential and Proprietary.
 */

package com.android.qrdfileexplorer;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaFile;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.lang.ref.SoftReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * File adapter for list view
 */
public class FilesAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private Context mContext;

    public List<String> paths;
    private int startMode;
    private int mCategory;
    private Bitmap mIconSD;
    private Bitmap mIconFolder;
    private Bitmap mIconFile;
    private Bitmap mIconFileApk;
    private Bitmap mIconFilePdf;

    private ListView mListView;

    // Image thumbnail bitmap cache
    private HashMap<String, SoftReference<Bitmap>> mImageThumbnailCache;
    // Music icon
    private Bitmap mIconFileAudio;
    // Video icon
    private Bitmap mIconFileVideo;
    // Image loading icon
    private Bitmap mIconLoading;
    //favorite list paths
    private ArrayList<String> favoriteList = null;
    private String TAG = "FilesAdapter";

    // Set of selected item paths.
    private final HashSet<String> mSelectedSet = new HashSet<String>();

    private SyncThumbnailDecoder mSyncThumbnailDecoder;

    // Structure for the list item
    private class ViewHolder {
        TextView text;
        TextView info;
        TextView ext_info;
        ImageView icon;
        ImageView icon_favorite;
    }

    // Stop the DecodeThread when activity onDestroy.
    public void onStop() {
        mSyncThumbnailDecoder.stop();
    }

    /**
     * Initialize the class
     */
    public FilesAdapter(Context context, List<String> p, int mode, int category) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        paths = p;
        startMode = mode;
        mCategory = category;
        // Initialize the icon bitmap cache list
        mImageThumbnailCache = new HashMap<String, SoftReference<Bitmap>>();

        // Load icons from resource file or system
        // mIconSD = BitmapFactory.decodeResource(context.getResources(),
        // R.drawable.icon_sd);
        mIconFolder = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.icon_folder);
        mIconFile = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.icon_file_pic);
        mIconFileApk = BitmapFactory.decodeResource(context.getResources(),
                android.R.drawable.sym_def_app_icon);
        mIconFilePdf = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.icon_file_pdf);
        mIconFileVideo = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.icon_file_video);
        mIconLoading = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.apk);
        mIconFileAudio = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.icon_file_audio);

        favoriteList = CategoryFragment.getFavoriteList();
        mSyncThumbnailDecoder = new SyncThumbnailDecoder(context);
    }

    // Obtain listview to find ImageView with tag.
    public void setListView(ListView listview) {
        mListView = listview;
    }

    @Override
    public int getCount() {
        return paths.size();
    }

    @Override
    public Object getItem(int pos) {
        return paths.get(pos);
    }

    @Override
    public long getItemId(int pos) {
        return pos;
    }

    // Update the list view
    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        ViewHolder holder;
        // Initialize the items of ViewHolder structure
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.file_row, null);
            holder = new ViewHolder();
            holder.text = (TextView) convertView.findViewById(R.id.text);
            holder.info = (TextView) convertView.findViewById(R.id.info);
            holder.ext_info = (TextView) convertView.findViewById(R.id.ext_info);
            holder.icon = (ImageView) convertView.findViewById(R.id.icon);
            holder.icon_favorite = (ImageView) convertView.findViewById(R.id.favorite_icon);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Avoid OutOfBoundsException
        if (pos >= paths.size()) {
            return convertView;
        }

        // Display the file name
        File f = new File(paths.get(pos).toString());

        // Set a tag for every item's ImageView to avoid bitmap display disorder
        holder.icon.setTag(paths.get(pos).toString());

        // retrieve the most appropriate title string
        holder.text.setText(FileUtils.getPathTitle(mContext, f));

        // Clean up the information text
        holder.ext_info.setText("");
        holder.ext_info.setVisibility(View.GONE);

        // Set the color to gray if it is a hidden file or folder
        if (f.getPath().contains("/.")) {
            holder.text.setTextColor(Color.GRAY);
        } else {
            holder.text.setTextColor(Color.BLACK);
        }

        //whether the file or folder has added to favorite
        if (favoriteList != null) {
            boolean isFavorite = favoriteList.contains(paths.get(pos).toString());
            if (isFavorite && mCategory != CategoryFragment.FAVORITE) {
                holder.icon_favorite.setVisibility(View.VISIBLE);
            } else {
                holder.icon_favorite.setVisibility(View.GONE);
            }
        }

        // Set the icon for file or folder
        if (f.isDirectory()) {
            holder.info.setText("");
            holder.info.setVisibility(View.GONE);
            holder.icon.setImageBitmap(mIconFolder);
            return convertView;
        }

        // Display the file size
        holder.info.setText(getSizeString(f.length()));
        holder.info.setVisibility(View.VISIBLE);
        holder.icon.setImageBitmap(Common.getDefaultIcon(mContext, f));
        // Display the file type icon
        String fileType = getFileExt(f).toLowerCase();
        String filePath = f.getPath();
        if (fileType.equals("apk")) {
            if (mImageThumbnailCache.get(filePath) != null &&
                    mImageThumbnailCache.get(filePath).get() != null) {
                holder.icon.setImageBitmap(mImageThumbnailCache.get(filePath).get());
            } else {
                new ApkInfoLoaderTask(holder, f).execute();
                holder.icon.setImageBitmap(mIconLoading);
            }
        } else if (MediaFile.isImageFileType(FileUtils.getFileType(f))) {
            if (mImageThumbnailCache.get(filePath) != null &&
                    mImageThumbnailCache.get(filePath).get() != null) {
                holder.icon.setImageBitmap(mImageThumbnailCache.get(filePath).get());
            } else {
                mSyncThumbnailDecoder.decodeThumbnail(holder,f);
                holder.icon.setImageBitmap(mIconFile);
            }
        } else if (MediaFile.isAudioFileType(FileUtils.getFileType(f))) {
            holder.icon.setImageBitmap(mIconFileAudio);
        } else if (fileType.equals("pdf")) {
            holder.icon.setImageBitmap(mIconFilePdf);
        } else if (MediaFile.isVideoFileType(FileUtils.getFileType(f))) {
            holder.icon.setImageBitmap(mIconFileVideo);
        }

        File file = new File(paths.get(pos));
        if (file.isHidden() && holder.icon != null) {
            holder.icon.setAlpha(150);
        }
        return convertView;
    }

    /**
     * Get the file extension name
     */
    public String getFileExt(File f) {
        String fileName = f.getName();
        String ext = fileName.substring(fileName.lastIndexOf(".")
                + 1, fileName.length()).toLowerCase();

        return ext;
    }

    /**
     * Convert the file size from long to string
     */
    public static String getSizeString(long size) {
        if (size < 1024) {
            return String.valueOf(size) + "B";
        } else {
            size = size / 1024;
        }
        if (size < 1024) {
            return String.valueOf(size) + "KB";
        } else {
            size = size * 100 / 1024;
        }

        return String.valueOf((size / 100)) + "." + ((size % 100) < 10 ? "0" : "")
                + String.valueOf((size % 100)) + "MB";
    }

    /**
     * Load information from apk file
     */
    public ApkInfo loadApkInfo(Context context, String path) {
        ApkInfo apkInfo = new ApkInfo();
        try {
            Class parserCls = Class.forName("android.content.pm.PackageParser");
            Constructor parserCt = parserCls.getConstructor(new Class[]{
                    String.class
            });
            Object parser = parserCt.newInstance(new Object[]{
                    path
            });
            DisplayMetrics metrics = new DisplayMetrics();
            metrics.setToDefaults();
            Method parsePkgMtd = parserCls.getDeclaredMethod("parsePackage",
                    new Class[]{
                            File.class, String.class, DisplayMetrics.class, Integer.TYPE
                    });
            Object parserPkg = parsePkgMtd.invoke(parser, new Object[]{
                    new File(path), path, metrics, 0
            });
            Field fld = parserPkg.getClass().getDeclaredField("applicationInfo");
            ApplicationInfo info = (ApplicationInfo) fld.get(parserPkg);
            Class magCls = Class.forName("android.content.res.AssetManager");
            Constructor magCt = magCls.getConstructor((Class[]) null);
            Object assetMag = magCt.newInstance((Object[]) null);
            Method mtd = magCls.getDeclaredMethod("addAssetPath",
                    new Class[]{
                            String.class
                    });
            mtd.invoke(assetMag, new Object[]{
                    path
            });
            Resources res = context.getResources();
            Constructor resCt = Resources.class.getConstructor(new Class[]{
                    assetMag.getClass(), res.getDisplayMetrics().getClass(),
                    res.getConfiguration().getClass()
            });
            res = (Resources) resCt.newInstance(new Object[]{
                    assetMag, res.getDisplayMetrics(), res.getConfiguration()
            });
            if (info.labelRes != 0) {
                apkInfo.labelString = res.getText(info.labelRes).toString();
            }
            if (info.icon != 0) {
                apkInfo.drawable = res.getDrawable(info.icon);
            }
            synchronized (mImageThumbnailCache) {
                if (apkInfo.drawable != null) {
                    Bitmap bm = BitmapFactory.decodeResource(res, info.icon);
                    mImageThumbnailCache.put(path, new SoftReference(bm));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return apkInfo;
    }

    public Set<String> getSelectedSet() {
        return mSelectedSet;
    }

    private void updateSelected(String path, boolean newSelected) {
        if (newSelected) {
            mSelectedSet.add(path);
        } else {
            mSelectedSet.remove(path);
        }
    }

    /**
     * Recycle all thumbnail in the thumbnail cache
     */
    void clearThunmnailCache() {
        synchronized (mImageThumbnailCache) {
            Set<String> sets = mImageThumbnailCache.keySet();
            for (String path : sets) {
                SoftReference<Bitmap> softReference = mImageThumbnailCache.get(path);
                if (softReference != null) {
                    Bitmap bitmap = softReference.get();
                    if (bitmap != null && !bitmap.isRecycled()) {
                        bitmap.recycle();
                        softReference.clear();
                    }
                }
            }
            mImageThumbnailCache.clear();
        }
    }

    /**
     * Async task to load the apk file image.
     */
    private class ApkInfoLoaderTask extends AsyncTask<Void, Void, ApkInfo> {
        private ViewHolder mHolder;
        private File mFile;

        public ApkInfoLoaderTask(ViewHolder holder, File file) {
            super();
            mHolder = holder;
            mFile = file;
        }

        @Override
        protected ApkInfo doInBackground(Void... args) {
            return loadApkInfo(mContext, mFile.getPath());
        }

        @Override
        protected void onPostExecute(ApkInfo apkInfo) {
            String fileTag = mFile.getPath().toString();
            ImageView imageView = (ImageView) mListView.findViewWithTag(fileTag);

            if (apkInfo.drawable == null) {
                mHolder.icon.setImageBitmap(mIconFileApk);
            } else {
                if (imageView != null && mHolder.icon.getTag().equals(fileTag)) {
                    mHolder.icon.setImageDrawable(apkInfo.drawable);
                }
            }

            if (!TextUtils.isEmpty(apkInfo.labelString) && imageView != null
                    && mHolder.icon.getTag().equals(fileTag)) {
                mHolder.ext_info.setText(apkInfo.labelString);
                mHolder.ext_info.setVisibility(View.VISIBLE);
            }
        }
    }

    // Structure for the apkinfo item
    private class ApkInfo {
        Drawable drawable;
        String labelString;
    }

    private class SyncThumbnailDecoder implements Handler.Callback {
        private static final String LOADER_THREAD_NAME = "ThumbnailDecoder";
        private static final int MESSAGE_REQUEST_DECODE = 1;
        private static final int MESSAGE_THUMBNAIL_DECODE_DONE = 2;
        private Context mContext;
        private final ConcurrentHashMap<ViewHolder, File> mPendingRequests =
                new ConcurrentHashMap<ViewHolder, File>();
        final Handler mMainHandler = new Handler(this);
        DecodeThread mDecodeThread;

        public SyncThumbnailDecoder(Context context) {
            mContext = context;
        }

        public void decodeThumbnail(ViewHolder holder, File file) {
            mPendingRequests.put(holder, file);
            mMainHandler.sendEmptyMessage(MESSAGE_REQUEST_DECODE);
        }

        public void stop() {
            if (mDecodeThread != null) {
                mDecodeThread.quit();
                mDecodeThread = null;
            }
            clearThunmnailCache();
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_REQUEST_DECODE:
                    if (mDecodeThread == null) {
                        mDecodeThread = new DecodeThread();
                        mDecodeThread.start();
                    }
                    mDecodeThread.requestDecoding();
                    return true;
                case MESSAGE_THUMBNAIL_DECODE_DONE:
                    Iterator<ViewHolder> iterator = mPendingRequests.keySet().iterator();
                    while (iterator.hasNext()) {
                        ViewHolder holder = iterator.next();
                        File f = mPendingRequests.get(holder);
                        String filePath = f.getPath();

                        if (mImageThumbnailCache.get(filePath) != null &&
                                mImageThumbnailCache.get(filePath).get() != null) {
                            holder.icon.setImageBitmap(
                                    mImageThumbnailCache.get(filePath).get());
                            iterator.remove();
                        }
                    }
                    break;
                default:
                    break;
            }
            return true;
        }

        private class DecodeThread extends HandlerThread implements Handler.Callback {
            private Handler mDecoderHandler;

            public DecodeThread() {
                super(LOADER_THREAD_NAME);
            }

            public boolean handleMessage(Message msg) {
                Iterator<ViewHolder> iterator = mPendingRequests.keySet().iterator();
                while (iterator.hasNext()) {
                    ViewHolder holder = iterator.next();
                    File f = mPendingRequests.get(holder);
                    if (f != null) {
                        Bitmap bitmap = FileUtils.getImageThumbnail(f, mIconFile);
                        if (bitmap != null) {
                            mImageThumbnailCache.put(f.getPath(), new SoftReference(bitmap));
                        }
                    }
                }
                mMainHandler.sendEmptyMessage(MESSAGE_THUMBNAIL_DECODE_DONE);
                return true;
            }

            public void requestDecoding() {
                if (mDecoderHandler == null) {
                    mDecoderHandler = new Handler(getLooper(), this);
                }
                mDecoderHandler.sendEmptyMessage(0);
            }
        }
    }
}
