/**
  Copyright (c) 2013, Qualcomm Technologies, Inc. All Rights Reserved.
  Qualcomm Technologies Proprietary and Confidential.
 */

package com.android.qrdfileexplorer;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import android.app.Fragment;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.TextView;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

public class CategoryFragment extends Fragment implements OnItemClickListener {
    private GridView gridview;
    private CategoryAdapter adapter;
    private static final String TITLE = "title";
    private static final String NUM = "num";
    private static final String PREF_FAVORITE_IDS = "favorite_filepaths";
    private Context context;
    private View mView;
    private SharedPreferences prefs;
    // file category type
    public static final int INVALID_TYPE = -1;
    public static final int MUSIC = 0;
    public static final int VIDEO = 1;
    public static final int IMAGE = 2;
    public static final int DOC = 3;
    public static final int ARCHIVE = 4;
    public static final int APK = 5;
    public static final int FAVORITE = 6;
    public static final int TOTAL = 7;
    public static final int MUSICandVIDEO = 8;
    private static ArrayList<String> favoriteList = new ArrayList<String>();
    private ArrayList<String> c_num = null;

    private BroadcastReceiver receiver;
    private IntentFilter filter;
    private static Set<String> favorite_paths = null;

    private final int MAX_FAVORITE_NUM = 200;

    private static int music_num = 0;
    private static int video_num = 0;
    private static int image_num = 0;
    private static int doc_num = 0;
    private static int archive_num = 0;
    private static int apk_num = 0;

    OnCategoryListener mCallback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // update category list when storage mounted
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(FileUtils.TAG, "Media_MOUNTED/UNMOUNTED");
                mCallback.onFileUpdate();
            }
        };

        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addDataScheme("file");
        context.registerReceiver(receiver, filter);
    }

    public CategoryFragment() {
    }

    @Override
    public void onResume() {
        super.onResume();
        mCallback.onFileUpdate();
        c_num = new ArrayList<String>();
        prefs = context.getSharedPreferences(Common.PREF_FILE_EXPLORER, Context.MODE_PRIVATE);

        //Set camera folder as default in favorite category
        if (!prefs.contains(PREF_FAVORITE_IDS)) {
            SharedPreferences.Editor ed = prefs.edit();
            Set<String> default_paths = new HashSet<String>();
            String camera_path = FileUtils.getFavoritePath();
            default_paths.add(camera_path);
            ed.putStringSet(PREF_FAVORITE_IDS, default_paths);
            ed.commit();
        }

        favorite_paths =
                prefs.getStringSet(PREF_FAVORITE_IDS, new HashSet<String>());
        updateFavoriteList();
        loadData();
        adapter = new CategoryAdapter(context, c_num);
        gridview.setAdapter(adapter);
        // new FileLoadTask().execute((Void)null);

        mCallback.onPageShowing();
    }

    public void onPause() {
        super.onPause();

    }

    @Override
    public void onDestroy() {
        // Recycle all thumbnail in cache
        super.onDestroy();
        context.unregisterReceiver(receiver);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        mView = inflater.inflate(R.layout.category_view, container, false);
        gridview = (GridView) mView.findViewById(R.id.gridview);
        gridview.setOnItemClickListener(this);
        return mView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        try {
            mCallback = (OnCategoryListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnCategoryListener");
        }

    }

    private void loadData() {
        // c_num may be null, when updateView was called before onResume.
        if (c_num == null) return;
        c_num.clear();
        c_num.add(context.getString(R.string.category_num, music_num));
        c_num.add(context.getString(R.string.category_num, video_num));
        c_num.add(context.getString(R.string.category_num, image_num));
        c_num.add(context.getString(R.string.category_num, doc_num));
        c_num.add(context.getString(R.string.category_num, archive_num));
        if (FileUtils.getIsAppsInstallingAllowed()) {
            c_num.add(context.getString(R.string.category_num, apk_num));
        }
        c_num.add(context.getString(R.string.category_num, favoriteList.size()));
    }

    private void updateFavoriteList() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            synchronized(favorite_paths) {
                favoriteList.clear();
                ArrayList<String> delList = new ArrayList<String>();
                for (String path : favorite_paths) {
                    File f = new File(path);
                    if (f.exists()){
                        favoriteList.add(path);
                    }
                    else {
                        delList.add(path);
                    }
                }
                favorite_paths.remove(delList);
                savePreference();
            }
        }
    }

    public boolean addToFavorite(String path) {
        synchronized(favorite_paths) {
            if (favorite_paths.size() <= MAX_FAVORITE_NUM &&
                    !favorite_paths.contains(path)) {
                favorite_paths.add(path);
                updateFavoriteList();
                return true;
            }
        }
        return false;
    }

    public boolean removeFavorite(String path) {
        synchronized(favorite_paths) {
            if (favorite_paths.contains(path)) {
                favorite_paths.remove(path);
                updateFavoriteList();
                return true;
            }
        }
        return false;
    }

    private void savePreference() {
        SharedPreferences.Editor ed = prefs.edit();
        ed.remove(PREF_FAVORITE_IDS);
        ed.commit();
        ed.putStringSet(PREF_FAVORITE_IDS, favorite_paths);
        ed.commit();
    }

    public static ArrayList<String> getFavoriteList() {
        return favoriteList;
    }

    public void updateView() {
        music_num = MainActivity.musicFile.size();
        video_num = MainActivity.videoFile.size();
        image_num = MainActivity.imageFile.size();
        doc_num = MainActivity.docFile.size();
        archive_num = MainActivity.archiveFile.size();
        apk_num = MainActivity.apkFile.size();
        loadData();
        if(adapter != null)
        adapter.notifyDataSetChanged();
    }

    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        CategoryAdapter.ViewHolder holder = (CategoryAdapter.ViewHolder) arg1.getTag();
        switch (holder.position) {
            case MUSIC:
                mCallback.onCategoryFileSelected(MainActivity.musicFile, MUSIC);
                break;
            case VIDEO:
                mCallback.onCategoryFileSelected(MainActivity.videoFile, VIDEO);
                break;
            case IMAGE:
                mCallback.onCategoryFileSelected(MainActivity.imageFile, IMAGE);
                break;
            case DOC:
                mCallback.onCategoryFileSelected(MainActivity.docFile, DOC);
                break;
            case ARCHIVE:
                mCallback.onCategoryFileSelected(MainActivity.archiveFile, ARCHIVE);
                break;
            case APK:
                mCallback.onCategoryFileSelected(MainActivity.apkFile, APK);
                break;
            case FAVORITE:
                mCallback.onCategoryFileSelected(favoriteList, FAVORITE);
                break;
        }
    }

    /*
     * if findfile takes time more than 5s,pls use this task. class FileLoadTask
     * extends AsyncTask<Void, Void, Integer>{
     * @Override protected Integer doInBackground(Void... params) {
     * findAllFiles(Environment.getExternalStorageDirectory().toString());
     * findAllFiles(Environment.getInternalStorageDirectory().toString());
     * return null; }
     * @Override protected void onPostExecute(Integer result) { loadData();
     * adapter = new CategoryAdapter(context, c_num);
     * gridview.setAdapter(adapter); } }
     */

    static class CategoryAdapter extends BaseAdapter {
        private LayoutInflater inflater;
        private Context mcontext;
        private List<String> appList;
        private Integer[] icons = {
                R.drawable.category_music, R.drawable.category_video, R.drawable.category_image,
                R.drawable.category_doc,
                R.drawable.category_archive, R.drawable.category_apk, R.drawable.category_favorite
        };
        private Integer[] title = {
                R.string.music, R.string.video, R.string.image, R.string.doc, R.string.archive,
                R.string.apk, R.string.favorite
        };
        private Integer[] index = {
                MUSIC, VIDEO, IMAGE, DOC, ARCHIVE, APK, FAVORITE
        };

        public CategoryAdapter(Context context, List<String> p) {
            mcontext = context;
            inflater = LayoutInflater.from(mcontext);
            appList = p;
            if (!FileUtils.getIsAppsInstallingAllowed()) {
                List<Integer> iconList = new ArrayList<Integer> (Arrays.asList(icons));
                iconList.remove(APK);
                icons = iconList.toArray(new Integer[iconList.size()]);
                List<Integer> titleList = new ArrayList<Integer> (Arrays.asList(title));
                titleList.remove(APK);
                title = titleList.toArray(new Integer[titleList.size()]);
                List<Integer> indexList = new ArrayList<Integer> (Arrays.asList(index));
                indexList.remove(APK);
                index = indexList.toArray(new Integer[indexList.size()]);
            }
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.griditem, null);
                holder = new ViewHolder();
                holder.title = (TextView) convertView.findViewById(R.id.category_title);
                holder.num = (TextView) convertView.findViewById(R.id.category_num);
                holder.icon = (ImageView) convertView.findViewById(R.id.category_icon);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.position = index[position];
            holder.icon.setImageResource(icons[position]);
            holder.title.setText(mcontext.getString(title[position]));
            holder.num.setText(appList.get(position));
            return convertView;
        }

        @Override
        public int getCount() {
            return appList.size();
        }

        @Override
        public Object getItem(int pos) {
            return appList.get(pos);
        }

        @Override
        public long getItemId(int pos) {
            return pos;
        }

        static class ViewHolder {
            int position;
            TextView title;
            TextView num;
            ImageView icon;
        }
    }

    public interface OnCategoryListener {

        public void onCategoryFileSelected(ArrayList<String> categoryList, int type);

        public void onFileUpdate();

        public void onPageShowing();
    }
}
