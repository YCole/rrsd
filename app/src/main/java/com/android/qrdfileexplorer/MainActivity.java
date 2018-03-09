/**
  Copyright (c) 2013, Qualcomm Technologies, Inc. All Rights Reserved.
  Qualcomm Technologies Proprietary and Confidential.
 */

package com.android.qrdfileexplorer;

import android.app.Activity;
import com.android.qrdfileexplorer.ftp.FtpActivity;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.util.Log;
import android.provider.MediaStore;  

import com.qcom.android.support.featurebar.FeatureBarHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class MainActivity extends Activity implements CategoryFragment.OnCategoryListener,
        FolderFragment.OnFolderListener, MenuExecutor.FileOperationListener, View.OnClickListener {
    private ActionBar actionbar;
    private static final int CATEGORY = 0;
    private static final int FOLDER = 1;
    private static final int TAB_INDEX_COUNT = 2;
    public static int fileType;
    private static final int UPDATE_DONE = 4;
    private final int RESULT_OK = -1;
    private final int RESULT_CANCEL = 2;
    private CategoryFragment catefragment;
    private FolderFragment folderfragment;
    private int mCurrentTab = CATEGORY;
    // record the last reference for FolderFragment,to transfer keycode;
    private Fragment currentFragment;

    private BroadcastReceiver mPowerOffReceiver = null;

    private ViewPager mViewPager;
    private ViewPagerAdapter mViewPagerAdapter;

    private Button mNavCategory;
    private Button mNavFolder;

    private RelativeLayout mTabCategoryBg;
    private RelativeLayout mTabFolderBg;

    private boolean DEBUG = false;
    private String TAG = "fileExplorer";

    // private static ArrayList<String> c_num=null;
    public static ArrayList<String> musicFile = null;
    public static ArrayList<String> videoFile = null;
    public static ArrayList<String> imageFile = null;
    public static ArrayList<String> docFile = null;
    public static ArrayList<String> archiveFile = null;
    public static ArrayList<String> apkFile = null;
    public static Set<File> mNeedLoadingSet = new HashSet<File>();
    private final int mMaxFileNumber = 500;

    public static final Object LOCK = new Object();

    public final PageChangeListener mPageChangeListener = new PageChangeListener();

    private static final int REQUEST_CODE_PHOTO_PICKED_WITH_DATA = 1002;
    private static final String ACTION_CROP = "com.android.camera.action.CROP";
    private static final String ACTION_CROP_TYPE = "image/*";
    private static final String CROP = "crop";
    private static final String ACTION_DIR_SEL = "com.android.fileexplorer.action.DIR_SEL";
    private static final String RESULT_DIR_SEL = "result_dir_sel";

    private FeatureBarHelper mFeatureBarHelper;
    private TextView mLeftSkView;
    private TextView mCenterSkView;
    private TextView mRightSkView;

    /**
      * If we are in ACTION_GET_CONTENT
      * do not let user to use left/right key to choose other category
      */
    private boolean mRestrictChoosingCategory;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.file_explorer_activity);
        actionbar = getActionBar();
        actionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionbar.setCustomView(R.layout.my_action_bar);

        mViewPagerAdapter = new ViewPagerAdapter(getFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mViewPagerAdapter);
        mViewPager.setOnPageChangeListener(mPageChangeListener);
        mViewPager.setOffscreenPageLimit(TAB_INDEX_COUNT);
        mViewPager.setCurrentItem(CATEGORY);

        registerPowerOffListener();

        mNavCategory = (Button) findViewById(R.id.navCategory);
        mNavCategory.setOnClickListener(this);
        mNavFolder = (Button) findViewById(R.id.navFolder);
        mNavFolder.setOnClickListener(this);
        mTabCategoryBg = (RelativeLayout) findViewById(R.id.tabCategoryBg);
        mTabFolderBg = (RelativeLayout) findViewById(R.id.tabFolderBg);

        musicFile = new ArrayList<String>();
        videoFile = new ArrayList<String>();
        imageFile = new ArrayList<String>();
        docFile = new ArrayList<String>();
        archiveFile = new ArrayList<String>();
        apkFile = new ArrayList<String>();

         //init sortType for Category, Favorite, Folder
        Common.setSortType(Common.KEY_SORT_TYPE_FOR_CATEGORY, Common.DEFAULT_SORT_TYPE_FOR_CATEGORY);
        Common.setSortType(Common.KEY_SORT_TYPE_FOR_FAVORITE, Common.DEFAULT_SORT_TYPE_FOR_FAVORITE);
        Common.setSortType(Common.KEY_SORT_TYPE_FOR_FOLDER, Common.DEFAULT_SORT_TYPE_FOR_FOLDER);

        initSoftKey();
        if (handleGetContent()) {
            // let layout show up as quickly as possible (without any data)
            // after data is ready we will call this function again
            mRestrictChoosingCategory = true;
            mNavFolder.setEnabled(false);
            mNavFolder.setFocusable(false);
            onFileUpdate();
        }
    }

    protected void initSoftKey() {
        mFeatureBarHelper = new FeatureBarHelper(this);
        mLeftSkView = (TextView) mFeatureBarHelper.getOptionsKeyView();
        mCenterSkView = (TextView) mFeatureBarHelper.getCenterKeyView();
        mRightSkView = (TextView) mFeatureBarHelper.getBackKeyView();
    }

    private void setSoftKey(int flag,boolean leftSkViewShow, boolean centerSkView, boolean rightSkView) {
        if (mFeatureBarHelper == null) {
            return;
        }
		//flag = -1;
        if (DEBUG) Log.d(TAG, "setSoftKey, [" + leftSkViewShow + "][" + centerSkView + "][" + rightSkView + "]");
		
        mLeftSkView = (TextView) mFeatureBarHelper.getOptionsKeyView();
        if (leftSkViewShow) {
			if(flag == CATEGORY){
				if(currentFragment instanceof FolderFragment){
					mLeftSkView.setText(R.string.menu_file);
				}else{
				    mLeftSkView.setText(R.string.ftp);	
				}
			}else if(flag == FOLDER){
				mLeftSkView.setText(R.string.menu_file);
			}
            mLeftSkView.setVisibility(View.VISIBLE);
        } else {
            mLeftSkView.setVisibility(View.GONE);
        }
        mCenterSkView = (TextView) mFeatureBarHelper.getCenterKeyView();
        if (centerSkView) {
            mCenterSkView.setVisibility(View.VISIBLE);
        } else {
            mCenterSkView.setVisibility(View.GONE);
        }
        mRightSkView = (TextView) mFeatureBarHelper.getBackKeyView();
        if (rightSkView) {
            mRightSkView.setVisibility(View.VISIBLE);
        } else {
            mRightSkView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        super.onConfigurationChanged(newConfig);
     }

    @Override
    protected void onRestart() {
        // TODO Auto-generated method stub
        super.onRestart();
        notifyListChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        FileUtils.refreshIsAppInstallingAllowed(this);
        if (ACTION_DIR_SEL.equals(getIntent().getAction())) {
            onFileOperationSelected(FolderFragment.MODE_DIR_SELECT, null);
        }
        mPageChangeListener.reflashFeatureBar();
    }

    private void registerPowerOffListener() {
        if (mPowerOffReceiver == null) {
            mPowerOffReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action.equals(Intent.ACTION_SHUTDOWN)) {
                        finish();
                    }
                }
            };
            IntentFilter iFilter = new IntentFilter();
            iFilter.addAction(Intent.ACTION_SHUTDOWN);
            registerReceiver(mPowerOffReceiver, iFilter);
        }
    }

    @Override
    public void onClick(View view) {
        int clickId = -1;
        if (mNavCategory == view) {
            clickId = CATEGORY;
        } else if (mNavFolder == view) {
            clickId = FOLDER;
        }
        if (DEBUG) Log.d(TAG, "onClick, clickId=" + clickId);
        if (currentFragment != null && clickId != -1) {
            int currentId = mViewPager.getCurrentItem();
            if (DEBUG) Log.d(TAG, "onClick, currentId=" + currentId);
            if (currentFragment instanceof FolderFragment) {
                FolderFragment f = (FolderFragment) currentFragment;
                f.leaveSelectionMode();

                // If user has chosen any category, return to category mode first
                if (clickId == CATEGORY) {
                    getFragmentManager().popBackStack();
                    mViewPagerAdapter.notifyDataSetChanged();
                }
            }
            if (currentId != clickId) {
                mViewPager.setCurrentItem(clickId, true);
            } else {
                if (currentFragment instanceof CategoryFragment) {
                    CategoryFragment f = (CategoryFragment)currentFragment;
                    f.updateView();
                }
            }
        }
    }

    public class ViewPagerAdapter extends FragmentPagerAdapter {
        private FragmentManager mFragmentManager = null;
        private FragmentTransaction mCurTransaction = null;

        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
            mFragmentManager = fm;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case CATEGORY:
                    catefragment = new CategoryFragment();
                    return catefragment;
                case FOLDER:
                    folderfragment = new FolderFragment();
                    return folderfragment;
            }
            throw new IllegalStateException("No fragment at position " + position);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            if (object == null || container == null) return;
            // The parent's setPrimaryItem() also calls setMenuVisibility(), so we want to know
            // when it happens.
            super.setPrimaryItem(container, position, object);
            Resources resources = getResources();
            if (position == CATEGORY) {
                mNavCategory.setTypeface(Typeface.DEFAULT_BOLD);
                mNavCategory.setTextColor(resources.getColor(R.color.tabTextSelectedColor));
                mNavFolder.setTypeface(Typeface.DEFAULT);
                mNavFolder.setTextColor(resources.getColor(R.color.tabTextColor));
                mTabCategoryBg.setBackgroundResource(R.drawable.tab_left_selected);
                mTabFolderBg.setBackgroundResource(R.drawable.tab_right_normal);
            } else if (position == FOLDER) {
                mNavCategory.setTypeface(Typeface.DEFAULT);
                mNavCategory.setTextColor(resources.getColor(R.color.tabTextColor));
                mNavFolder.setTypeface(Typeface.DEFAULT_BOLD);
                mNavFolder.setTextColor(resources.getColor(R.color.tabTextSelectedColor));
                mTabCategoryBg.setBackgroundResource(R.drawable.tab_left_normal);
                mTabFolderBg.setBackgroundResource(R.drawable.tab_right_selected);
            }
            Fragment fragment = (Fragment) object;
            if (currentFragment != fragment) {
                currentFragment = fragment;
            }
        }

        @Override
        public int getCount() {
            return TAB_INDEX_COUNT;
        }

        @Override
        public int getItemPosition(Object object) {
            return FragmentPagerAdapter.POSITION_NONE;
        }
    }

    public class PageChangeListener implements OnPageChangeListener {
        private int mCurrentPosition = -1;
        private int mNextPosition = -1;

        @Override
        public void onPageScrolled(
                int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            if (DEBUG) Log.d(TAG, "onPageSelected: position: " + position);
            if (position == CATEGORY) {
                onClick(mNavCategory);
            } else if (position == FOLDER) {
                onClick(mNavFolder);
            }
            mNextPosition = position;

            manageFeatureBar(position);
        }

        public void setCurrentPosition(int position) {
            mCurrentPosition = position;
        }

        public int getCurrentPosition() {
            return mCurrentPosition;
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            //if the third app select directory path, then return
            if (ACTION_DIR_SEL.equals(getIntent().getAction()) ||
                    Intent.ACTION_GET_CONTENT.equals(getIntent().getAction()))
                return;

            switch (state) {
                case ViewPager.SCROLL_STATE_IDLE: {
                    if (mNextPosition == -1) {
                        // This happens when the user drags the screen just after launching the
                        // application, and settle down the same screen without actually swiping it.
                        // At that moment mNextPosition is apparently -1 yet, and we expect it
                        // being updated by onPageSelected(), which is *not* called if the user
                        // settle down the exact same tab after the dragging.
                        mNextPosition = mViewPager.getCurrentItem();
                    }

                    invalidateOptionsMenu();
                    mCurrentPosition = mNextPosition;
                    mCurrentTab = mNextPosition;
                    break;
                }
                case ViewPager.SCROLL_STATE_DRAGGING: {
                    if ((currentFragment != null) && (currentFragment instanceof FolderFragment)) {
                        FolderFragment f = (FolderFragment)currentFragment;
                        f.leaveSelectionMode();
                    }
                    break;
                }
                case ViewPager.SCROLL_STATE_SETTLING: {
                    if (DEBUG) Log.d(TAG, "onPageScrollStateChanged() with SCROLL_STATE_SETTLING");
                    break;
                }
                default:
                    break;
            }
        }

         private void manageFeatureBar(int pagePos) {
            if (DEBUG) Log.d(TAG, "manageFeatureBar, pagePos=" + pagePos);
            switch (pagePos) {
                case CATEGORY:
                    setSoftKey(CATEGORY,true, false, true);
                    break;
                case FOLDER:
                    setSoftKey(FOLDER,true, false, true);
                    break;
            }
        }

        private void reflashFeatureBar() {
            if (DEBUG) Log.d(TAG, "reflashFeatureBar, mNextPosition=" + mNextPosition);
            switch (mNextPosition) {
                case -1:
                case CATEGORY:
                    setSoftKey(CATEGORY,true, false, true);
                    break;
                case FOLDER:
                    setSoftKey(FOLDER,true, false, true);
                    break;
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
		Log.d(TAG, "keyCode, ***********************=" + keyCode); 
        if ((keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT) && mRestrictChoosingCategory) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    // first,let the fdfragment deal key,such as go back;then if the root
    // path,finish();last,if category path,hide the current fragment,and pop up
    // back stack
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
		 
        if (keyCode == KeyEvent.KEYCODE_BACK ||
            keyCode == KeyEvent.KEYCODE_SOFT_RIGHT) {
            if (currentFragment != null) {
			 
				
                if (currentFragment instanceof FolderFragment) {
                    FolderFragment fg = (FolderFragment) currentFragment;
                    if (fg.handleKeyDown(keyCode)) {
                        return true;
                    }
                    else {
                        boolean rootf = fg.isRootPath();
                        if (rootf) {
                            finish();
                            return true;
                        }
                        if (getFragmentManager().getBackStackEntryCount() == 0) {
                            // nothing in back stack do close app
                            finish();
                            return true;
                        }
                        getFragmentManager().popBackStack();
                        mViewPagerAdapter.notifyDataSetChanged();
                        return true;
                    }
                }
                else if (currentFragment instanceof CategoryFragment) {
				 
				 
					
                    finish();
                    return true;
                }
            }
        }
        else {
		 
            if (keyCode == KeyEvent.KEYCODE_MENU ||
                keyCode == KeyEvent.KEYCODE_SOFT_LEFT) {
				 if (currentFragment != null&&(currentFragment instanceof CategoryFragment)) { 		 
				   Intent intent = new Intent();
				   intent.setClass(MainActivity.this, ConnectActivity.class);  
                   startActivity(intent);  
				 }else{
					 invalidateOptionsMenu();
				 }
            }
			
        }
        return super.onKeyDown(keyCode, event);
    }

    public int getCurrentTab() {
        return mCurrentTab;
    }

    @Override
    public void onDestroy() {
        if (mPowerOffReceiver != null) {
            unregisterReceiver(mPowerOffReceiver);
            mPowerOffReceiver = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
    }

    private void goToSelectedCategory(int category) {
        switch (category) {
            case CategoryFragment.MUSIC:
                onCategoryFileSelected(musicFile, CategoryFragment.MUSIC, false);
                break;
            case CategoryFragment.VIDEO:
                onCategoryFileSelected(videoFile, CategoryFragment.VIDEO, false);
                break;
            case CategoryFragment.IMAGE:
                onCategoryFileSelected(imageFile, CategoryFragment.IMAGE, false);
                break;
            case CategoryFragment.APK:
                onCategoryFileSelected(apkFile, CategoryFragment.APK, false);
                break;
        }
    }

    private void onCategoryFileSelected(ArrayList<String> fileList, int type, boolean addBack) {
        fileType = type;
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(FileUtils.CATEGORY_FILE, fileList);
        bundle.putString(FileUtils.OPEN_PATH, FileUtils.CATEGORY_PATH);
        bundle.putInt(FileUtils.START_MODE, FolderFragment.CATEGORY_SEL);
        bundle.putInt(FileUtils.CATEGORY_TYPE, type);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        FolderFragment foldfragment = new FolderFragment();
        foldfragment.setArguments(bundle);
        String name = makeFragmentName(R.id.pager, CATEGORY);
        transaction.add(R.id.pager, foldfragment, name);
        if (addBack) {
            transaction.addToBackStack(name);
        }
        transaction.commit();
        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                mViewPagerAdapter.notifyDataSetChanged();
            }
        });

        // User choose any category, FolderFragement will be launched
        mPageChangeListener.manageFeatureBar(FOLDER);
    }

    // display category file list
    public void onCategoryFileSelected(ArrayList<String> fileList, int type) {
        onCategoryFileSelected(fileList, type, true);
    }

    // display the folder tab after select the dst directory,dir is the display
    // path
    public void onDirectorySelectedOk(String dir, int request, int mode, int result) {
        if (ACTION_DIR_SEL.equals(getIntent().getAction())) {
            Intent intent = new Intent();
            intent.putExtra(RESULT_DIR_SEL, dir);
            setResult(RESULT_OK, intent);
            finish();
        }
        Bundle bundle = new Bundle();
        bundle.putString(FileUtils.OPEN_PATH, dir);
        bundle.putInt(FileUtils.REQUEST, request);
        bundle.putInt(FileUtils.OPERATE_MODE, mode);
        bundle.putInt(FileUtils.RESULT, result);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        FolderFragment foldfragment = new FolderFragment();
        foldfragment.setArguments(bundle);
        String name = makeFragmentName(R.id.pager, FOLDER);
        transaction.add(R.id.pager, foldfragment, name);
        transaction.commit();
        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                mViewPager.setCurrentItem(FOLDER, true);
                mViewPagerAdapter.notifyDataSetChanged();
            }
        });
    }

    // after select the multi file,start the folder to select the dst directory
    public void onFileOperationSelected(int mode, ArrayList<String> files) {
        if (mode != FolderFragment.MODE_INVALID) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            Bundle bundle = new Bundle();
            FolderFragment foldfragment = new FolderFragment();
            if (mode == FolderFragment.MODE_COPY ||
                    mode == FolderFragment.MODE_MOVE ||
                    mode == FolderFragment.MODE_DIR_SELECT) {
                // if the mode is copy and move,jump to the folder tab
                bundle.putInt(FileUtils.START_MODE, FolderFragment.DIR_SEL);
                String name = makeFragmentName(R.id.pager, FOLDER);
                //transaction.addToBackStack(name);
                bundle.putInt(FileUtils.RESULT, FolderFragment.RESULT_OK);
                bundle.putInt(FileUtils.OPERATE_MODE, mode);
                bundle.putStringArrayList(Intent.EXTRA_STREAM, files);
                foldfragment.setArguments(bundle);
                transaction.add(R.id.pager, foldfragment, name);
                transaction.commit();
                mViewPager.setCurrentItem(FOLDER, true);
                mViewPagerAdapter.notifyDataSetChanged();
            }
         } else {
            if (files != null && getIntent().getAction()
                    .equals(Intent.ACTION_GET_CONTENT)) {
                Intent intent = new Intent();
                //intent.setData(Uri.fromFile(new File(file)));
                intent.putStringArrayListExtra(Intent.EXTRA_STREAM, files);
                setResult(RESULT_OK, intent);
                finish();
            }
        }
    }

    // after select the multi file,start the folder to select the dst directory
    public void onFileSelectedOk(int mode, String file) {
        if (mode != FolderFragment.MODE_INVALID) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            Bundle bundle = new Bundle();
            FolderFragment foldfragment = new FolderFragment();
            if (mode == FolderFragment.MODE_COPY || mode == FolderFragment.MODE_MOVE) {
               // if the mode is copy and move,jump to the folder tab
                bundle.putInt(FileUtils.START_MODE, FolderFragment.DIR_SEL);
                String name = makeFragmentName(R.id.pager, FOLDER);
                transaction.addToBackStack(name);
                bundle.putInt(FileUtils.RESULT, FolderFragment.RESULT_OK);
                bundle.putInt(FileUtils.OPERATE_MODE, mode);
                foldfragment.setArguments(bundle);
                transaction.add(R.id.pager, foldfragment, name);
                transaction.commit();
                mViewPager.setCurrentItem(FOLDER, true);
                mViewPagerAdapter.notifyDataSetChanged();
            }
        } else {
            if (file != null && getIntent().getAction()
                    .equals(Intent.ACTION_GET_CONTENT)) {
                Intent intent = getIntent();
                String crop = intent.getStringExtra(CROP);
                if(crop != null && crop.equals("true")){
                    if(ACTION_CROP_TYPE.equals(intent.getType())){
                        doCropPhoto(file);
                    }
                }
                else{
                    intent = new Intent();
                    intent.setData(Uri.fromFile(new File(file)));
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        }
    }

    private void doCropPhoto(String fileName) {
        try{
            Uri uri = Uri.fromFile(new File(fileName));
            Intent intent = new Intent(ACTION_CROP, uri)
                    .putExtras(getIntent());
            intent.setDataAndType(uri, ACTION_CROP_TYPE);
            startActivityForResult(intent, REQUEST_CODE_PHOTO_PICKED_WITH_DATA);
        }
        catch(Exception e){
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Activity.RESULT_OK){
            switch(requestCode){
                case REQUEST_CODE_PHOTO_PICKED_WITH_DATA:
                    setResult(Activity.RESULT_OK, data);
                    finish();
            }
        }
    }

    public void onBacktoCategoryHome() {
        if (mCurrentTab == FOLDER)
            return;
        getFragmentManager().popBackStack();
        mViewPagerAdapter.notifyDataSetChanged();
    }

    // in the file_sel and dir_sel,this will back to the former fragment
    public void onSelectedCancel(String dir) {
        if (ACTION_DIR_SEL.equals(getIntent().getAction())) {
            finish();
            return;
        }
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Bundle bundle = new Bundle();
        FolderFragment foldfragment = new FolderFragment();
        bundle.putString(FileUtils.OPEN_PATH, dir);
        foldfragment.setArguments(bundle);
        String name = makeFragmentName(R.id.pager, FOLDER);
        transaction.replace(R.id.pager, foldfragment, name);
        transaction.commit();
        mViewPagerAdapter.notifyDataSetChanged();
    }

    public boolean addToFavorite(String path) {
        return catefragment.addToFavorite(path);
    }

    public boolean removeFavorite(String path) {
        return catefragment.removeFavorite(path);
    }

    private boolean handleGetContent() {
        Intent intent = getIntent();
        if (Intent.ACTION_GET_CONTENT.equals(intent.getAction())) {
            String type = intent.getType();
            Log.d(TAG, "ACTION_GET_CONTENT "+type);
            int categoryType = Common.getFileType(type);
            if (categoryType != CategoryFragment.INVALID_TYPE) {
                goToSelectedCategory(categoryType);
                return true;
            }
        }
        return false;
    }

    class CategoryUpdateThread extends Thread {
        public void run() {
            synchronized(LOCK) {
                clear();
                Log.d(FileUtils.TAG, "start to find files");

                // retrieve all the mounted external volumes' paths
                ArrayList<String> mountPoints = FileUtils
                        .getExternalMountedVolumes(MainActivity.this);
                int size = mountPoints.size();
                int loopCount = 0;

                while (loopCount < size) {
                    findAllFiles(mountPoints.get(loopCount));
                    loopCount++;
                }
                // Add the internal storage path explicitly
                findAllFiles(FileUtils.getInternalPath());
                sortList();
            }
            // Send response message to UI thread
            Message msg = Message.obtain(handler, UPDATE_DONE);
            handler.sendMessage(msg);
        }
    }

    private final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == UPDATE_DONE) {
                updateCategoryList();
                handleGetContent();
            }
        }
    };

    private void updateCategoryList() {
        if (currentFragment != null && currentFragment instanceof CategoryFragment) {
            CategoryFragment cg = (CategoryFragment) currentFragment;
            cg.updateView();
        }
    }

    private void sortList() {
        Collections.sort(musicFile, String.CASE_INSENSITIVE_ORDER);
        Collections.sort(videoFile, String.CASE_INSENSITIVE_ORDER);
        Collections.sort(imageFile, String.CASE_INSENSITIVE_ORDER);
        Collections.sort(docFile, String.CASE_INSENSITIVE_ORDER);
        Collections.sort(archiveFile, String.CASE_INSENSITIVE_ORDER);
        Collections.sort(apkFile, String.CASE_INSENSITIVE_ORDER);
    }

    private void clear() {
        musicFile.clear();
        videoFile.clear();
        imageFile.clear();
        docFile.clear();
        archiveFile.clear();
        apkFile.clear();
        mNeedLoadingSet.clear();
    }

    private void findAllFiles(String path) {
        File f = new File(path);

        if (!f.exists()) {
            return;
        }

        Queue<File> queue = new LinkedList<File>();
        queue.offer(f);

        while (null != (f = queue.poll())) {
            if (FileUtils.mMoveAndDeleteFlag != 0) {
                break;
            }
            //reduce listFiles call, it will cause performance issue.
            File[] entries = f.listFiles();
            if (entries == null)
                continue;

            if (entries.length > mMaxFileNumber) {
                mNeedLoadingSet.add(f);
                Log.v(TAG, "add " + f.toString() + " to mNeedLoadingSet");
            }

            for (File file : entries) {
                if (FileUtils.mMoveAndDeleteFlag != 0) {
                    break;
                }

                if (file.isDirectory()) {
                    queue.offer(file);

                } else {
                    switch (Common.getFileType(file)) {
                        case CategoryFragment.MUSIC:
                            musicFile.add(file.getPath());
                            break;

                        case CategoryFragment.VIDEO:
                            videoFile.add(file.getPath());
                            break;

                        case CategoryFragment.IMAGE:
                            imageFile.add(file.getPath());
                            break;

                        case CategoryFragment.DOC:
                            docFile.add(file.getPath());
                            break;

                        case CategoryFragment.ARCHIVE:
                            archiveFile.add(file.getPath());
                            break;

                        case CategoryFragment.APK:
                            apkFile.add(file.getPath());
                            break;

                        case CategoryFragment.MUSICandVIDEO:
                            musicFile.add(file.getPath());
                            videoFile.add(file.getPath());
                            break;

                        default:
                            break;
                    }
                }
            }
        }
    }

    public void onFileUpdate() {
        if (FileUtils.mMoveAndDeleteFlag == 0) {
            new CategoryUpdateThread().start();
        }
    }

    public void notifyListChanged() {
        if (mViewPagerAdapter != null)
            mViewPagerAdapter.notifyDataSetChanged();
    }

    private String makeFragmentName(int containId, int itemId) {
        return "android:switcher:" + containId + ":" + itemId;
    }

    @Override
    public void onPageShowing() {
        mPageChangeListener.reflashFeatureBar();
    }
}
