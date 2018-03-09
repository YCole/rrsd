/**
 * Copyright (c) 2013, Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Qualcomm Technologies Proprietary and Confidential.
 */

package com.android.qrdfileexplorer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.text.format.DateFormat;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.os.storage.StorageVolume;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Stack;

import com.android.qrdfileexplorer.ActionModeHandler.ActionModeListener;
import com.android.qrdfileexplorer.MainActivity;

public class FolderFragment extends Fragment implements SelectionManager.SelectionListener,
        MenuExecutor.FileSelectionListener {
    // Operating event
    static final int EVENT_FAILED = 0;
    static final int EVENT_DELETE_FILE_OK = 1;
    static final int EVENT_MOVE_FILE_OK = 2;
    static final int EVENT_COPY_FILE_OK = 3;
    static final int EVENT_COPY_FILE_OVERWRITE = 4;
    static final int EVENT_SHARE_OK = 5;
    static final int EVENT_ADD_FAVORITE_OK = 6;
    static final int EVENT_REMOVE_FAVORITE_OK = 7;
    static final int EVENT_PROGRESS_EVENT = 8;
    static final int EVENT_COPY_FALED_MEMORY_FULL = 9;
    static final int EVENT_LOAD_FILE_OK = 10;

    // System running mode
    public static final int MODE_INVALID = -1;
    public static final int MODE_DEFAULT = 0;
    public static final int MODE_COPY = 1;
    public static final int MODE_MOVE = 2;
    public static final int MODE_DELETE = 3;
    public static final int MODE_SHARE = 4;
    public static final int MODE_DIR_SELECT = 5;

    // Activity request code
    static final int REQ_CODE_FILE_SEL = 1;
    static final int REQ_CODE_DIR_SEL = 2;
    static final int REQ_CODE_DELETE = 3;
    static final int REQ_CODE_SHARE = 4;

    static final int OVERWRITE_FLAG_DISABLE = 0;
    static final int OVERWRITE_FLAG_ENABLE = 1;
    static final int OVERWRITE_FLAG_ALWAYS = 2;

    static int multimedia = 0;

    // Max file name length
    private static final int FILE_NAME_MAX_LENGTH = 255;

    // System current running mode
    public int mMode = MODE_DEFAULT;

    private static final String RESULT_DIR_SEL = "result_dir_sel";
    public static final int FILE_SEL = 1;
    public static final int DIR_SEL = 2;
    public static final int CATEGORY_SEL = 3;
    public static final int DEFAULT = 0;
    public static final int PROGRESS_MAX_VALUE = 100;

    private static final String ACTION_MEDIA_SCANNER_SCAN_ALL =
            "com.android.fileexplorer.action.MEDIA_SCANNER_SCAN_ALL";
    private static final String ACTION_MEDIA_SCANNER_SCAN_AUDIO =
            "com.android.fileexplorer.action.MEDIA_SCANNER_SCAN_AUDIO";
    private static final String ACTION_MEDIA_SCANNER_SCAN_AUDIOFILE =
            "com.android.fileexplorer.action.MEDIA_SCANNER_SCAN_AUDIOFILE";
    private static final String ACTION_DELETE_MUSIC =
            "com.android.fileexplorer.action.DELETE_MUSIC";
    private static String EXTRA_ALL_VIDEO_FOLDER =
            "org.codeaurora.intent.extra.ALL_VIDEO_FOLDER";
    private static final String KEY_PATH = "path";
    private static final String KEY_ROOT_PATH = "root_path";
    private static final String KEY_SAVED_PATH = "saved_path";
    private static final String EXTRA_STRING_ISDELETE = "is_delete";
    private static final String SUFFIX_LEFT = "(";
    private static final String SUFFIX_RIGHT = ")";
    private static final String SEPARATE_STRING = ".";

    private static final int TYPE_TEXT = 0;
    private static final int TYPE_AUDIO = 1;
    private static final int TYPE_VIDEO = 2;
    private static final int TYPE_IMAGE = 3;

    public static final int RESULT_OK = 1;
    public static final int RESULT_CANCEL = 2;

    private static final int MENU_SORT_NAME = 0;
    private static final int MENU_SORT_TIME = 1;
    private static final int MENU_SORT_TYPE = 2;
    private static final int MENU_SORT_SIZE = 3;
    private int mViewItem = 0;

    private String mRootPath = "/storage";
    private String mDefaultPath = null;
    private String mCurrentPath = null;
    private TextView mPath;
    private View space;
    private List<String> mPathItems = null;
    private List<String> mPathItemsCache = null;
    private FilesAdapter mFileAdapter = null;
    private CustomAlertDialog mProgressDialog = null;
    private CustomProgressDialog mLoadingDialog = null;
    private ImageButton mBtnUp = null;
    private ImageButton mBtnSD = null;
    private Button mBtnOK = null;
    private Button mBtnCancel = null;
    public static int category = -1;

    private static ArrayList<String> categoryItems = null;
    private static ArrayList<String> mSrcFiles = null;
    private static ArrayList<String> mSelectedFiles = null;
    private ArrayList<Integer> mSaveState = new ArrayList<Integer>();
    private String mDstDir = null;
    private int mOverwriteFlag = OVERWRITE_FLAG_DISABLE;
    private BroadcastReceiver mReceiver;
    private IntentFilter filter;

    private boolean hasCustomRoot = false;

    private Locale mLocale;

    private ListView listview = null;
    private View mView;
    private Context context;
    private Bundle dataBundle = null;
    private String path;
    private int startMode;
    private int operateMode;
    private int result;
    private int request;
    private AlertDialog mAlertDialog = null;
    private StorageManager mStorageManager = null;
    private ActionMode mSelectionMode;
    private SelectionManager mSelectionManager;
    private ActionModeHandler mActionModeHandler;

    private FileNameComparator mFileNameAscComp = new FileNameComparator(Common.ID_SORT_ASC);
    private FileTypeComparator mFileTypeAscComp = new FileTypeComparator(Common.ID_SORT_ASC);
    private FileSizeComparator mFileSizeAscComp = new FileSizeComparator(Common.ID_SORT_ASC);
    private FileTimeComparator mFileTimeAscComp = new FileTimeComparator(Common.ID_SORT_ASC);
    private FileNameComparator mFileNameDecComp = new FileNameComparator(Common.ID_SORT_DEC);
    private FileTypeComparator mFileTypeDecComp = new FileTypeComparator(Common.ID_SORT_DEC);
    private FileSizeComparator mFileSizeDecComp = new FileSizeComparator(Common.ID_SORT_DEC);
    private FileTimeComparator mFileTimeDecComp = new FileTimeComparator(Common.ID_SORT_DEC);
    private Object mLock = new Object();
    private ArrayList<File> mFileList = new ArrayList<File>();

    private final int mMaxFileNumber = 500;
    private boolean mIsLoading = false;

    private class Pos{
        int index = 0;
        int top = 0;
    };
    private Stack<Pos> mPosStack = new Stack<Pos>();
    private boolean mIsBack = false;

    private SharedPreferences prefs;

    OnFolderListener mCallback;
    private static int mSortType = Common.ID_SORT_TIME_ASC;
    //Remember the current show/hide status, false:show, true:hide
    private static boolean hiddenFlag = false;
    private static final String HIDDEN_FLAG_PREF = "hidden_flag_prefrence";
    private static final String TAG = "FolderFragment";

    public FolderFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        path = "";
        dataBundle = getArguments();
        if (dataBundle != null) {
            path = dataBundle.getString(FileUtils.OPEN_PATH, "");
            startMode = dataBundle.getInt(FileUtils.START_MODE, 0);
            operateMode = dataBundle.getInt(FileUtils.OPERATE_MODE, 0);
            result = dataBundle.getInt(FileUtils.RESULT, -1);
            request = dataBundle.getInt(FileUtils.REQUEST, -1);
            if (startMode == CATEGORY_SEL) {
                if (dataBundle.getInt(FileUtils.CATEGORY_TYPE, -1) != -1) {
                    category = dataBundle.getInt(FileUtils.CATEGORY_TYPE, -1);
                }
                if (dataBundle.getStringArrayList(FileUtils.CATEGORY_FILE) != null) {
                    categoryItems = dataBundle.getStringArrayList(FileUtils.CATEGORY_FILE);
                }
            }
        }
        // Get source file list
        if (mSrcFiles == null) {
            mSrcFiles = new ArrayList<String>();
        }
        if ((operateMode == MODE_COPY || operateMode == MODE_MOVE) &&
                startMode == DIR_SEL) {
            mSelectedFiles = dataBundle.getStringArrayList(Intent.EXTRA_STREAM);
        }
        mSelectionManager = new SelectionManager();
        mSelectionManager.setSelectionListener(this);
        mActionModeHandler = new ActionModeHandler(getActivity(), mSelectionManager);
        mActionModeHandler.setActionModeListener(new ActionModeListener() {
            @Override
            public boolean onActionItemClicked(MenuItem item) {
                return true;
            }
        });

        prefs = context.getSharedPreferences(Common.PREF_FILE_EXPLORER, Context.MODE_PRIVATE);
        hiddenFlag = prefs.getBoolean(HIDDEN_FLAG_PREF, false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        if (startMode == DIR_SEL)
            mView = inflater.inflate(R.layout.dir_sel, container, false);
        else {
            mView = inflater.inflate(R.layout.main, container, false);
        }

        listview = (ListView) mView.findViewById(com.android.internal.R.id.list);
        mBtnUp = (ImageButton) mView.findViewById(R.id.btn_up);
        mPath = (TextView) mView.findViewById(R.id.tv_path);
        space = (View) mView.findViewById(R.id.space);
        
        //mBtnUp.setBackgroundColor(Color.TRANSPARENT);
        mBtnUp.setVisibility(View.INVISIBLE);
        space.setVisibility(View.INVISIBLE);
        //Commented to remove the functionality of buttonup
        /*mBtnUp.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                gotoBackspace();
                mSelectionManager.leaveSelectionMode();
            }
        });

        mBtnUp.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    v.setBackgroundColor(Color.GRAY);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    v.setBackgroundColor(Color.TRANSPARENT);
                }
                return false;
            }
        });*/
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                mViewItem = arg2;
                if (mSelectionManager.inSelectionMode()) {
                    mSelectionManager.toggle(mFileAdapter.getItem(arg2).toString());
                } else if (startMode == CATEGORY_SEL && category == CategoryFragment.FAVORITE) {
                    String path = mPathItems.get(arg2);
                    File f = new File(path);
                    listview.setItemChecked(arg2, false);
                    if (f.exists()) {
                        if (f.isDirectory())
                            mCallback.onDirectorySelectedOk(path, -1, 0, -1);
                        else
                            openFile(f);
                    } else
                        Toast.makeText(context, R.string.toast_file_not_exists,
                                Toast.LENGTH_LONG).show();
                } else if (getActivity().getIntent().getAction() != null &&
                        getActivity().getIntent().getAction().equals(Intent.ACTION_GET_CONTENT)
                        && (startMode == DEFAULT || startMode == CATEGORY_SEL)) {
                    File f = new File(mPathItems.get(arg2));
                    listview.setItemChecked(arg2, false);
                    if (f.isDirectory()) {
                        loadDir(mPathItems.get(arg2));
                    } else {
                        mCallback.onFileSelectedOk(MODE_INVALID, mPathItems.get(arg2));
                    }
                } else {
                    listview.setItemChecked(arg2, false);
                    File f = new File(mPathItems.get(arg2));
                    if (f.exists()) {
                        if (f.isDirectory()) {
                            StoreLastListPos();
                            loadDir(mPathItems.get(arg2));
                        } else {
                            openFile(f);
                        }
                    } else {
                        // The file user clicked does not exist any more(this
                        // may happen
                        // when the file is deleted/moved via MTP on PC or by
                        // another
                        // program), pop up an toast to tell the user about this
                        // info
                        // and reload current directory.
                        Toast.makeText(context, R.string.toast_file_not_exists,
                                Toast.LENGTH_LONG).show();
                        loadDir(mCurrentPath);
                    }
                }
            }
        });

        listview.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listview.setOnItemLongClickListener(new OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                String path = mFileAdapter.getItem(pos).toString();

                if (FileUtils.getExternalMountedVolumes(context).contains(path)
                        || FileUtils.getInternalPath().equals(path)) {
                    return false;
                }
                // Store the current position
                StoreLastListPos();
                mSelectionManager.setTotalCount(mPathItems.size());
                mSelectionManager.setSourceFileList(mPathItems);
                FolderFragment.this.onLongTap(pos);
                listview.setItemChecked(pos, true);
                listview.invalidate();
                return true;
            }
        });

        initSelMode();
        setHasOptionsMenu(true);
        // Monitor SD card status
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(FileUtils.TAG, "Media mount state changed : " + intent.getAction());

                StorageVolume volumeInfo = intent
                        .getParcelableExtra(StorageVolume.EXTRA_STORAGE_VOLUME);

                if (!intent.getAction().equalsIgnoreCase(Intent.ACTION_MEDIA_MOUNTED)) {
                    // if any volume underwent unmount / bad_removal, dismiss
                    // the alert dialog showing the storage volumes' properties
                    if (mAlertDialog != null) {
                        mAlertDialog.dismiss();
                        mAlertDialog = null;
                    }

                    // show an appropriate Toast
                    Toast.makeText(context,
                            getString(R.string.toast_no) + volumeInfo.getDescription(context),
                            Toast.LENGTH_LONG).show();
                }
                else {
                    // update the AlertDialog with appropriate info, as new
                    // volume is mounted
                    if (mAlertDialog != null) {
                        mAlertDialog.setMessage(getVolumesInfo());
                    }
                }

                mCallback.onFileUpdate();
                loadDir(mCurrentPath);
            }
        };

        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        filter.addDataScheme("file");
        return mView;
    }

    private void StoreLastListPos()
    {
        mIsBack = false;

        int index = listview.getFirstVisiblePosition();
        View v = listview.getChildAt(0);
        int top  = (v == null) ? 0 : v.getTop();
        Pos lastPos = new Pos();
        lastPos.index = mViewItem;
        lastPos.top = top;
        mPosStack.push(lastPos);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (startMode == CATEGORY_SEL) {
            loadCacheDir();
            mPathItemsCache = null;
        }
        // if SDCard has unmounted, alertdialog should dismiss
        if (mCurrentPath != null && mCurrentPath.contains(FileUtils.getSDPath(context))
                && !FileUtils.ExternalMountableDiskExist(context)) {
            if (mAlertDialog != null) {
                mAlertDialog.dismiss();
                mAlertDialog = null;
            }
        }
        // very whether the tab and fragment is match

        // the path will not change after loadDir(),so we should get the correct
        // path from mPath.
        String rPath = mCurrentPath;
        Log.d(FileUtils.TAG, "rpath is:" + rPath);
        if (TextUtils.isEmpty(rPath)) {
            mDefaultPath = TextUtils.isEmpty(path) ? mRootPath : path;
        } else {
            mDefaultPath = rPath;
        }
        // If the listview has been set already, we don't need to loadDir again.
        // This will avoid re-load everytime when we resume from lock <-> unlock screen.
        if (listview.getAdapter() == null || listview.getCount() == 0) {
            mIsBack = true;
            loadDir(mDefaultPath);
        }
        context.registerReceiver(mReceiver, filter);
        if (operateMode != MODE_DEFAULT)
            executeFileOperation(path, operateMode, request, result);

        listview.post(new Runnable() {
            @Override
            public void run() {
                listview.requestFocus();
            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        try {
            mCallback = (OnFolderListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFolderListener");
        }
    }

    public void onStop() {
        super.onStop();
        StoreLastListPos();
        mSaveState.clear();

        // Save all the checked Item
        for (int pos = 0; pos < listview.getCount(); pos++) {

            if (listview.isItemChecked(pos)) {
                mSaveState.add(pos);
            }
        }
    }

    private void initSelMode() {
        mBtnOK = (Button) mView.findViewById(R.id.btn_ok);
        mBtnCancel = (Button) mView.findViewById(R.id.btn_cancel);

        if (mBtnOK == null || mBtnCancel == null) {
            return;
        }

        if (operateMode == MODE_DIR_SELECT)
            mBtnOK.setText(R.string.btn_ok);
        mBtnOK.setEnabled(false);
        mBtnOK.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (startMode == DIR_SEL) {
                    String dir = mCurrentPath;
                    mCallback.onDirectorySelectedOk(dir, REQ_CODE_DIR_SEL, operateMode, RESULT_OK);
                }
            }
        });

        mBtnCancel.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                String dir = mCurrentPath;
                mCallback.onSelectedCancel(dir);
            }
        });
    }

    private void loadCacheDir() {
        if (mPathItemsCache == null)
            return;
        if (mPathItemsCache.size() != mPathItems.size())
            return;
        for (int i = 0; i < mPathItems.size(); i++) {
            if (!mPathItems.get(i).equals(mPathItemsCache.get(i))) {
                return;
            }
        }
    }

    private void setCategoryTitle() {
        String strTitle = null;
        switch (category) {
            case CategoryFragment.MUSIC:
                strTitle = "/" + context.getString(R.string.music);
                break;
            case CategoryFragment.VIDEO:
                strTitle = "/" + context.getString(R.string.video);
                break;
            case CategoryFragment.IMAGE:
                strTitle = "/" + context.getString(R.string.image);
                break;
            case CategoryFragment.DOC:
                strTitle = "/" + context.getString(R.string.doc);
                break;
            case CategoryFragment.ARCHIVE:
                strTitle = "/" + context.getString(R.string.archive);
                break;
            case CategoryFragment.APK:
                strTitle = "/" + context.getString(R.string.apk);
                break;
            case CategoryFragment.FAVORITE:
                strTitle = "/" + context.getString(R.string.favorite);
                break;
            default:
                strTitle = "/Unknow";
                break;
        }
        mPath.setText(strTitle);
    }

    class LoadFileThread extends Thread {
        private String mCurrentPath = null;
        private int event = EVENT_LOAD_FILE_OK;

        public void run() {
            mPathItems = new ArrayList<String>();
            List<String> fileList = new ArrayList<String>();
            List<String> folderList = new ArrayList<String>();
            mFileList.clear();
            // if the path is a virtual path:/category,load the files from
            // categoryItems.
            if (FileUtils.CATEGORY_PATH.equals(mCurrentPath) && categoryItems != null) {
                if (category == CategoryFragment.FAVORITE) {
                    mSortType = Common.getSortType(Common.KEY_SORT_TYPE_FOR_FAVORITE);
                    ArrayList<String> favoriteList = CategoryFragment.getFavoriteList();
                    for (int i = 0; i < favoriteList.size(); i++) {
                        File file = new File(favoriteList.get(i));
                         //if this file is hide file and this path is in hide status, not show it
                        if (file.isHidden() && hiddenFlag) {
                            continue;
                        }
                        mFileList.add(file);
                    }
                    // mFiileList will be sorted after sort.
                    sort(mSortType);
                    // Get folder list and file list
                    for (int i = 0; i < mFileList.size(); i++) {
                        File file = mFileList.get(i);
                        if (file.isDirectory()) {
                            folderList.add(file.getPath());
                        } else {
                            fileList.add(file.getPath());
                        }
                    }
                    mPathItems.addAll(folderList);
                    mPathItems.addAll(fileList);
                } else {
                    // Add lock here to avoid multi-thread operate the same parameters,
                    // for example, when delete files in Category fragment.
                    synchronized (MainActivity.LOCK) {
                        for (int i = 0; i < categoryItems.size(); i++) {
                            String path = categoryItems.get(i);
                            if (path != null) {
                                File file = new File(categoryItems.get(i));
                                // if this file is hide file and this path is in
                                // hide status, not show it
                                if (file.isHidden() && hiddenFlag) {
                                    continue;
                                }
                                mFileList.add(file);
                            }
                        }
                    }
                    mSortType = Common.getSortType(Common.KEY_SORT_TYPE_FOR_CATEGORY);
                    sort(mSortType);
                    for (int i = 0; i < mFileList.size(); i++) {
                        File file = mFileList.get(i);
                        mPathItems.add(file.getPath());
                    }
                }
            } else {
                File f = new File(mCurrentPath);
                if (!f.exists()) {
                    mCurrentPath = mRootPath;
                    f = new File(mCurrentPath);
                }
                File[] files = f.listFiles();
                if (files != null) {
                    List<String> items = new ArrayList<String>();
                    if (mCurrentPath.equals(mRootPath)) {

                        // get combined list of external disks mounted
                        if (FileUtils.ExternalMountableDiskExist(context)) {
                            ArrayList<String> externalMountPoints = FileUtils
                                    .getExternalMountedVolumes(context);

                            int size = externalMountPoints.size();
                            int loopCount = 0;

                            while (loopCount < size) {
                                items.add(externalMountPoints.get(loopCount));
                                loopCount++;
                            }
                        }

                        // add the internal storage explicitly
                        if (FileUtils.internalStorageExist()) {
                            items.add(FileUtils.getInternalPath());
                        }
                    } else {
                        for (int i = 0; i < files.length; i++) {
                            File file = files[i];
                            items.add(file.getPath());
                        }
                    }
                    for (int i = 0; i < items.size(); i++) {
                        File file = new File(items.get(i));
                        //if this file is hide file and this path is in hide status, not show it
                        if (file.isHidden() && hiddenFlag) {
                            continue;
                        }
                        mFileList.add(file);
                    }
                    mSortType = Common.getSortType(Common.KEY_SORT_TYPE_FOR_FOLDER);
                    sort(mSortType);
                    // Get folders list and file list
                    for (int i = 0; i < mFileList.size(); i++) {
                        File file = mFileList.get(i);
                        if (file.isDirectory()) {
                            folderList.add(file.getPath());
                        } else {
                            fileList.add(file.getPath());
                        }
                    }
                    mPathItems.addAll(folderList);
                    mPathItems.addAll(fileList);
                }
            }
            // Send response message to UI thread
            Message msg = handler.obtainMessage();
            Bundle b = new Bundle();
            b.putInt("event", event);
            b.putString("path",mCurrentPath);
            msg.setData(b);
            handler.sendMessage(msg);
        }

        public LoadFileThread(String path) {
            mCurrentPath = path;
        }

    }

    private void loadDir(String path) {
        //Do not load again if we are loading.
        if (!mIsLoading) {
            mIsLoading = true;
            if (path.equals(FileUtils.CATEGORY_PATH) && categoryItems != null) {
                // before we load the file, change the tile, and set the list as null,
                // will update after load.
                listview.setAdapter(null);
                mCurrentPath = path;
                mPath.setText(FileUtils.fromPathtoText(context, path));
                setCategoryTitle();
                if (category != CategoryFragment.FAVORITE
                        && categoryItems != null && categoryItems.size() > mMaxFileNumber) {
                    showLoadingDialog(R.string.progress_title);
                }
            } else {
                listview.setAdapter(null);
                mCurrentPath = path;
                mPath.setText(FileUtils.fromPathtoText(context, path));
                File f = new File(path);
                if (!f.exists()) {
                    path = mRootPath;
                    f = new File(path);
                }

                if (MainActivity.mNeedLoadingSet.contains(f)) {
                    Log.v(TAG, "show loding dialog when load file:" + f.toString());
                    showLoadingDialog(R.string.progress_title);
                }
            }
            new LoadFileThread(path).start();
        }
    }

    private void sort(int sortType) {

        Comparator<? super File> comparator = mFileNameAscComp;
        switch (sortType) {

            case Common.ID_SORT_NAME_ASC:
                comparator = mFileNameAscComp;
                break;
            case Common.ID_SORT_NAME_DEC:
                comparator = mFileNameDecComp;
                break;
            case Common.ID_SORT_SIZE_ASC:
                comparator = mFileSizeAscComp;
                break;
            case Common.ID_SORT_SIZE_DEC:
                comparator = mFileSizeDecComp;
                break;
            case Common.ID_SORT_TIME_ASC:
                comparator = mFileTimeAscComp;
                break;
            case Common.ID_SORT_TIME_DEC:
                comparator = mFileTimeDecComp;
                break;
            case Common.ID_SORT_TYPE_ASC:
                comparator = mFileTypeAscComp;
                break;
            case Common.ID_SORT_TYPE_DEC:
                comparator = mFileTypeDecComp;
                break;
            default:
                comparator = mFileTimeAscComp;
                break;
        }

        synchronized (mLock) {
            Collections.sort(mFileList, comparator);
        }
        return;
    }

    /**
     * sort file and folder
     */
    private void sort(ArrayList<File> list, int sortType) {

        Comparator<? super File> comparator = mFileNameAscComp;
        switch (sortType) {

            case Common.ID_SORT_NAME_ASC:
                comparator = mFileNameAscComp;
                break;
            case Common.ID_SORT_NAME_DEC:
                comparator = mFileNameDecComp;
                break;
            case Common.ID_SORT_SIZE_ASC:
                comparator = mFileSizeAscComp;
                break;
            case Common.ID_SORT_SIZE_DEC:
                comparator = mFileSizeDecComp;
                break;
            case Common.ID_SORT_TIME_ASC:
                comparator = mFileTimeAscComp;
                break;
            case Common.ID_SORT_TIME_DEC:
                comparator = mFileTimeDecComp;
                break;
            case Common.ID_SORT_TYPE_ASC:
                comparator = mFileTypeAscComp;
                break;
            case Common.ID_SORT_TYPE_DEC:
                comparator = mFileTypeDecComp;
                break;
            default:
                comparator = mFileTimeAscComp;
                break;
        }

        synchronized (mLock) {
            Collections.sort(list, comparator);
        }
        return;
    }

    private void openFile(File f) {
        // We will first guess the MIME type of this file
        final Uri fileUri = Uri.fromFile(f);
        final Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        // Send file name to system application for sometimes it will be used.
        intent.putExtra(Intent.EXTRA_TITLE, f.getName());
        intent.putExtra(EXTRA_ALL_VIDEO_FOLDER, true);
        Uri contentUri = null;

        String type = getMIMEType(f);
        // For image file, content uri was needed in Gallery,
        // in order to know the pre/next image.
        if (type != null && type.contains("image/")) {
            String path = fileUri.getEncodedPath();
            if (path != null) {
                path = Uri.decode(path);
                ContentResolver cr = context.getContentResolver();
                StringBuffer buff = new StringBuffer();
                buff.append("(")
                        .append(MediaStore.Images.ImageColumns.DATA)
                        .append("=")
                        .append("'" + path + "'")
                        .append(")");
                Cursor cur = cr.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        new String[] { MediaStore.Images.ImageColumns._ID },
                        buff.toString(), null, null);
                int index = 0;
                if (cur != null){
                    for (cur.moveToFirst(); !cur.isAfterLast(); cur.moveToNext()) {
                        index = cur.getColumnIndex(MediaStore.Images.ImageColumns._ID);
                        index = cur.getInt(index);
                    }
                }
                if (index == 0) {
                    //not found, do nothing
                    Log.d(TAG,"file not found");
                } else {
                    contentUri = Uri.parse("content://media/external/images/media/"+ index);
                }

            }
        }
        if (!"application/octet-stream".equals(type)) {
            if (contentUri != null) {
                intent.setDataAndType(contentUri, type);
            } else {
                intent.setDataAndType(fileUri, type);
            }
            // If no activity can handle the intent then
            // give user a chooser dialog.
            try {
                startActivitySafely(intent);

            } catch (ActivityNotFoundException e) {
                showChooserDialog(fileUri, intent);
            }
        } else {
            showChooserDialog(fileUri, intent);
        }
    }

    private void showChooserDialog(final Uri fileUri, final Intent intent) {
        // If this file type can not be recognized then give user a chooser
        // dialog, providing 4 alternative options to open this file: open
        // as plain text, audio, video or image. And the corresponding MIME
        // type will be set to "text/plain", "audio/*", "video/*" and
        // "image/*". By this we can avoid many alternative activity entries
        // from one package in the action chooser dialog.
        mAlertDialog = new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.open_as))
                .setItems(
                        new String[]{
                                getString(R.string.type_text),
                                getString(R.string.type_audio),
                                getString(R.string.type_video),
                                getString(R.string.type_image)
                        },
                        new OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                switch (which) {
                                    case TYPE_TEXT:
                                        intent.setDataAndType(fileUri,
                                                "text/plain");
                                        break;

                                    case TYPE_AUDIO:
                                        intent.setDataAndType(fileUri,
                                                "audio/*");
                                        break;

                                    case TYPE_VIDEO:
                                        intent.setDataAndType(fileUri,
                                                "video/*");
                                        break;

                                    case TYPE_IMAGE:
                                        intent.setDataAndType(fileUri,
                                                "image/*");
                                        break;

                                    default:
                                        break;
                                }
                                startActivitySafely(intent);
                            }
                        }).create();
        mAlertDialog.show();
    }

    /**
     * Start activity safely with try-catch
     */
    private void startActivitySafely(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context,
                    R.string.toast_activity_not_found,
                    Toast.LENGTH_LONG).show();
        }
    }

    private String getMIMEType(File f) {
        String type = "";
        String fileName = f.getName();
        String ext = fileName.substring(fileName.lastIndexOf(".")
                + 1, fileName.length()).toLowerCase();

        if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("gif")
                || ext.equals("png") || ext.equals("bmp")) {
            type = "image/*";
        } else if (ext.equals("mp3") || ext.equals("amr") || ext.equals("wma")
                || ext.equals("aac") || ext.equals("m4a") || ext.equals("mid")
                || ext.equals("xmf") || ext.equals("ogg") || ext.equals("wav")
                || ext.equals("qcp") || ext.equals("awb") || ext.equals("flac")) {
            type = "audio/*";
        } else if (ext.equals("3gp") || ext.equals("avi") || ext.equals("mp4")
                || ext.equals("3g2") || ext.equals("wmv") || ext.equals("divx")
                || ext.equals("mkv") || ext.equals("webm") || ext.equals("ts")
                || ext.equals("asf") || ext.equals("3gpp")) {
            type = "video/*";
        } else if (ext.equals("apk")) {
            type = "application/vnd.android.package-archive";
        } else if (ext.equals("vcf")) {
            type = "text/x-vcard";
        } else if (ext.equals("txt")) {
            type = "text/plain";
        } else if (ext.equals("doc") || ext.equals("docx")) {
            type = "application/msword";
        } else if (ext.equals("xls") || ext.equals("xlsx")) {
            type = "application/vnd.ms-excel";
        } else if (ext.equals("ppt") || ext.equals("pptx")) {
            type = "application/vnd.ms-powerpoint";
        } else if (ext.equals("pdf")) {
            type = "application/pdf";
        } else if (ext.equals("xml")) {
            type = "text/xml";
        } else if (ext.equals("html")) {
            type = "text/html";
        } else if (ext.equals("zip")) {
            type = "application/zip";
        } else {
            type = "application/octet-stream";
        }

        return type;
    }

    private boolean gotoBackspace() {
        if (mPath == null) {
            return false;
        }
        mIsBack = true;
        String path = mCurrentPath;
        if (path.equals(FileUtils.CATEGORY_PATH)) {
            mCallback.onBacktoCategoryHome();
            return true;
        }
        if ((operateMode == MODE_COPY || operateMode == MODE_MOVE) &&
                   startMode == DIR_SEL && mRootPath.equals(path)) {
             String dir = path;
             mCallback.onSelectedCancel(dir);
        }
        if (!mRootPath.equals(path)) {
            File f = new File(path);
            if (path.equals(FileUtils.getInternalPath()))
                loadDir(mRootPath);
            else
                loadDir(f.getParent());
            return true;
        }
        return false;
    }

    public boolean isRootPath() {
        String path = mCurrentPath;
        if (path.equals(mRootPath) && (startMode == 0 || operateMode == MODE_DIR_SELECT))
            return true;
        else
            return false;
    }

    public boolean handleKeyDown(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_BACK ||
            keyCode == KeyEvent.KEYCODE_SOFT_RIGHT) {
            if (mSelectionManager.inSelectionMode()) {
                leaveSelectionMode();
                return true;
            }
            String path = mCurrentPath;
            if (path.equals(FileUtils.CATEGORY_PATH) || isRootPath())
                return false;
            if (startMode != FILE_SEL) {
                if (gotoBackspace()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Show delete verify dialog
     */
    private void showDeleteConfirmDialog(final DialogInterface.OnClickListener confirmListener) {
        View contents = View.inflate(context, R.layout.delete_dialog_view, null);
        TextView msg = (TextView)contents.findViewById(R.id.message);
        msg.setText(R.string.dialog_deleting_confirm);
        mAlertDialog = new AlertDialog.Builder(context)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(android.R.string.dialog_alert_title)
                .setPositiveButton(android.R.string.ok, confirmListener)
                .setNegativeButton(android.R.string.cancel, null)
                .setView(contents)
                .create();
        mAlertDialog.show();
    }

    /**
     * Rename file
     */
    private void renameSelectedFile(String filename) {
        final File f = new File(filename);
        final EditText ed = new EditText(context);
        // The original file name of user selected
        final String originalName = f.getName();
        final String fullFileName = filename;

        // Set single line for file rename edit text.
        ed.setSingleLine();
        ed.setText(f.getName());
        String type = getMIMEType(f);
        ed.setFilters(new InputFilter[]{new CustomLengthFilter(context,
                FILE_NAME_MAX_LENGTH/3)});
        if (Common.ID_TYPE_AUDIO == Common.getFileTypeId(f)) {
            multimedia = 1;
        }

        mAlertDialog = new AlertDialog.Builder(context)
                // .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.dialog_rename_title)
                .setView(ed)
                        // .setMessage(R.string.dialog_rename_title)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {

                                mIsBack = true;
                                if (TextUtils.isEmpty(ed.getText().toString())) {
                                    // If the file name inputed is empty, we
                                    // need
                                    // to alert user that he has inputed an
                                    // empty
                                    // file name.
                                    Toast.makeText(context,
                                            R.string.toast_filename_empty,
                                            Toast.LENGTH_LONG).show();
                                } else if (ed.getText().toString().getBytes().length
                                        > FILE_NAME_MAX_LENGTH) {
                                    // Restrict file name length
                                    Toast.makeText(context,
                                            R.string.toast_filename_too_long,
                                            Toast.LENGTH_LONG).show();
                                } else if (FileUtils.fileExist(f.getParent() + "/" + ed.getText())) {
                                    // If the file name is not change, we do not
                                    // show file exists toast message.
                                    if (originalName != null
                                            && (!originalName.equals(ed.getText().toString()))) {
                                        Toast.makeText(context, R.string.toast_file_exists,
                                                Toast.LENGTH_LONG).show();
                                    }
                                } else {
                                    Uri uri;
                                    if (startMode == CATEGORY_SEL) {
                                        String newName = f.getParent() + "/" +
                                                ed.getText();
                                        if (!f.renameTo(new File(newName))) {
                                            Toast.makeText(context,
                                                    R.string.toast_operate_failed,
                                                    Toast.LENGTH_LONG).show();
                                        } else {// refresh the category item after
                                            // rename
                                            if (mCurrentPath.equals(FileUtils.CATEGORY_PATH)) {

                                                if (CategoryFragment.getFavoriteList().contains(fullFileName)) {
                                                    mCallback.removeFavorite(fullFileName);
                                                    mCallback.addToFavorite(newName);
                                                }

                                                for (String cat : categoryItems) {
                                                    Log.e(TAG, "cat = " + cat);
                                                }

                                                int pos = categoryItems.indexOf(fullFileName);

                                                //If new file type changed, remove it from this category
                                                if (Common.getFileType(new File(newName)) == category &&
                                                        pos >= 0)
                                                    categoryItems.set(pos, newName);
                                                else
                                                    categoryItems.remove(fullFileName);

                                                mCallback.onFileUpdate();
                                                mCallback.notifyListChanged();
                                            }
                                        }
                                        loadDir(mCurrentPath);
                                        uri = Uri.parse("file://" + f.getParent() + "/"
                                                + ed.getText());
                                    } else {
                                        String newName = mCurrentPath + "/" + ed.getText();
                                        if (!f.renameTo(new File(newName))) {
                                            Toast.makeText(context,
                                                    R.string.toast_operate_failed,
                                                    Toast.LENGTH_LONG).show();
                                        } else {
                                            if (CategoryFragment.getFavoriteList().contains(fullFileName)) {
                                                mCallback.removeFavorite(fullFileName);
                                                mCallback.addToFavorite(newName);
                                            }

                                            if (categoryItems != null) {
                                                int pos = categoryItems.indexOf(fullFileName);
                                                if (pos >= 0)
                                                    categoryItems.set(pos, f.getParent()
                                                            + "/" + ed.getText());
                                            }

                                            mCallback.onFileUpdate();
                                            mCallback.notifyListChanged();
                                        }
                                        loadDir(mCurrentPath);
                                        // if operate the media file, only
                                        // update this file in the media
                                        // library.
                                        uri = Uri.parse("file://" + mCurrentPath + "/" +
                                                ed.getText());
                                    }

                                    if (multimedia == 1) {
                                        context.sendBroadcast(new Intent(
                                                ACTION_MEDIA_SCANNER_SCAN_AUDIOFILE, uri));
                                    } else {
                                        context.sendBroadcast(new Intent(
                                                ACTION_MEDIA_SCANNER_SCAN_ALL, uri));
                                    }
                                    multimedia = 0;
                                }
                            }
                        }
                )
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        mAlertDialog.show();
    }

    /**
     * Show the file property dialog
     */
    private class ShowSelectedDetailTask extends AsyncTask<String, Long, Long> {

        private String mFilePath;

        private String mFileName;
        private String mDateTime;
        private String mPathText;

        public ShowSelectedDetailTask(String filepath) {
            this.mFilePath = filepath;
        }

        @Override
        protected void onPreExecute() {
            final File file = new File(mFilePath);

            mFileName = file.getName();
            mDateTime = getDateText(file);
            mPathText = FileUtils.fromPathtoText(context, file.getParent());

            showDetailDialog();

            super.onPreExecute();
        }

        @Override
        protected Long doInBackground(String... arg0) {
            if (null == arg0[0]) return null;

            File file = new File(arg0[0]);

            if (!file.isDirectory()) {
                return file.length();
            }

            long size = 0;
            int updateCount = 0;

            Queue<File> queue = new LinkedList<File>();
            queue.offer(file);

            while (null != (file = queue.poll())) {

                for (File fileTemp : file.listFiles()) {
                    if (!fileTemp.isDirectory()) {
                        size += fileTemp.length();
                        updateCount++;

                        // each 50 files update once
                        if ((updateCount % 50) == 0) publishProgress(size);
                    } else {
                        queue.offer(fileTemp);
                    }
                }
            }

            return size;
        }


        @Override
        protected void onProgressUpdate(Long... values) {
            updateDialogInfo(values[0]);

            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Long result) {
            updateDialogInfo(result);

            super.onPostExecute(result);
        }

        private void showDetailDialog() {
            String info = String.format(getString(R.string.format_file_info),
                    mFileName, mPathText, mDateTime, 0);

            mAlertDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.menu_property)
                .setPositiveButton(android.R.string.ok, null)
                .setMessage(info)
                .create();

            mAlertDialog.show();
        }

        private void updateDialogInfo(Long size) {
            String info = String.format(getString(R.string.format_file_info),
                    mFileName, mPathText, mDateTime, FilesAdapter.getSizeString(size));

            mAlertDialog.setMessage(info);
        }

        private String getDateText(File file) {
            Date date = new Date(file.lastModified());

            String dateText;

            try {
                // get date format.
                java.text.DateFormat formater = DateFormat.getMediumDateFormat(
                        context.getApplicationContext());
                dateText = formater.format(date);
                // get time format.
                formater = DateFormat.getTimeFormat(context.getApplicationContext());
                // to show time, such as: "Dec.12,1999 18:00" in 24hourformat.
                dateText = dateText + " " + formater.format(date);
            } catch (Exception e) {
                Log.e(FileUtils.TAG, "Handled NULL pointer Exception");
                dateText = date.toLocaleString();
            }

            return dateText;
        }
    }

    /**
     * Load the options menu from XML layout file
     */
    public void onCreateOptionsMenu(Menu menu, final MenuInflater inflater) {

        inflater.inflate(R.layout.list, menu);

        if (startMode == CATEGORY_SEL) {
            // the category path is a virtual path,so it can't create file under
            // this path
            menu.removeItem(R.id.menu_new_folder);
            return;
        }

        // Remove the extra menu item if the directory is "/" or "/storage"
        if (mCurrentPath.equals(Environment.getRootDirectory().getParent())
                || mCurrentPath.equals(mRootPath)) {
            menu.removeItem(R.id.menu_new_folder);
            menu.removeItem(R.id.menu_hide);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (mProgressDialog != null) {
            mProgressDialog.invalidate();
        }
        if (newConfig.locale != null) {
            mPath.setText(FileUtils.fromPathtoText(context, mCurrentPath));
        }

        super.onConfigurationChanged(newConfig);
     }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        getActivity().getWindow().invalidatePanelMenu(Window.FEATURE_OPTIONS_PANEL);
        String path = mCurrentPath;
        if (hiddenFlag && menu.findItem(R.id.menu_hide) != null) {
            menu.findItem(R.id.menu_hide).setTitle(R.string.menu_show);
            return;
         }
         if (menu.findItem(R.id.menu_hide) != null)
             menu.findItem(R.id.menu_hide).setTitle(R.string.menu_hide);
    }

    /**
     * Process the options item selected message
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_new_folder: {
                onOptionsItemNewFolder();
                return true;
            }

            case R.id.menu_sort: {
                //show sort dialog
                showSortDialog();
                return true;
            }

            case R.id.menu_hide: {
                String path = mCurrentPath;
                hiddenFlag = !hiddenFlag;

                SharedPreferences.Editor ed = prefs.edit();
                ed.putBoolean(HIDDEN_FLAG_PREF, hiddenFlag);
                ed.commit();

                loadDir(path);
                return true;
            }

            case R.id.menu_property: {
                showSystemProperty();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the current path.
        if (mPath != null) {
            outState.putString(KEY_SAVED_PATH, mCurrentPath);
        }
    }

    /*
     * @Override public void onRestoreInstanceState(Bundle state) {
     * super.onRestoreInstanceState(state); // Get the saved path and reload the
     * dir. String path = state.getString(KEY_SAVED_PATH); if
     * (!TextUtils.isEmpty(path)) { loadDir(path); } }
     */

    private void onOptionsItemNewFolder() {
        final EditText ed = new EditText(context);
        ed.setText("");
        ed.setSingleLine();
        ed.setFilters(new InputFilter[]{new CustomLengthFilter(context,
                FILE_NAME_MAX_LENGTH/3)});
        mAlertDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.new_folder)
                .setView(ed)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                // Filter the white space character in the name.
                                final String fileName = ed.getText().
                                        toString().trim();
                                final File f = new File(mCurrentPath + "/" + fileName);
                                // Restrict file name length;
                                if (fileName.getBytes().length > FILE_NAME_MAX_LENGTH) {
                                    Toast.makeText(context,
                                            R.string.toast_filename_too_long,
                                            Toast.LENGTH_LONG).show();
                                } else if ((! fileName.isEmpty()) && (f.exists())) {
                                    Toast.makeText(context, R.string.toast_file_exists,
                                            Toast.LENGTH_LONG).show();
                                } else if (!f.mkdir()) {
                                    Toast.makeText(context,
                                            R.string.toast_operate_failed,
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    // if create a new folder success, show the
                                    // toast.
                                    Toast.makeText(context,
                                            R.string.toast_new_success,
                                            Toast.LENGTH_LONG).show();
                                }
                                loadDir(mCurrentPath);
                                // if operate the media file, only update this
                                // file in the media library.
                                Uri uri = Uri.parse("file://" + mCurrentPath + "/"
                                        + ed.getText());
                                context.sendBroadcast(new Intent(ACTION_MEDIA_SCANNER_SCAN_ALL,
                                        uri));
                            }
                        }
                )
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        mAlertDialog.show();
    }

    /**
     * Pop up the sort dialog
     */
    private void showSortDialog() {
        mAlertDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.sort_title)
                .setItems(R.array.sort_menu_items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String path = mCurrentPath;
                        if (path.equals(FileUtils.CATEGORY_PATH) && categoryItems != null) {
                            if (category == CategoryFragment.FAVORITE) {
                                mSortType = Common.getSortType(Common.KEY_SORT_TYPE_FOR_FAVORITE);
                            } else {
                                mSortType = Common.getSortType(Common.KEY_SORT_TYPE_FOR_CATEGORY);
                            }
                        } else {
                                mSortType = Common.getSortType(Common.KEY_SORT_TYPE_FOR_FOLDER);
                        }
                        switch (which) {
                            case MENU_SORT_NAME:  //by name
                                if (mSortType == Common.ID_SORT_NAME_ASC)
                                    mSortType = Common.ID_SORT_NAME_DEC;
                                else
                                    mSortType = Common.ID_SORT_NAME_ASC;
                                break;
                            case MENU_SORT_TIME:  //by time
                                if (mSortType == Common.ID_SORT_TIME_ASC)
                                    mSortType = Common.ID_SORT_TIME_DEC;
                                else
                                    mSortType = Common.ID_SORT_TIME_ASC;
                                break;
                            case MENU_SORT_TYPE:  //by type
                                if (mSortType == Common.ID_SORT_TYPE_ASC)
                                    mSortType = Common.ID_SORT_TYPE_DEC;
                                else
                                    mSortType = Common.ID_SORT_TYPE_ASC;
                                break;
                            case MENU_SORT_SIZE:  //by size
                                if (mSortType == Common.ID_SORT_SIZE_ASC)
                                    mSortType = Common.ID_SORT_SIZE_DEC;
                                else
                                    mSortType = Common.ID_SORT_SIZE_ASC;
                                break;
                        }

                        if (path.equals(FileUtils.CATEGORY_PATH) && categoryItems != null) {
                            if (category == CategoryFragment.FAVORITE) {
                                Common.setSortType(Common.KEY_SORT_TYPE_FOR_FAVORITE, mSortType);
                            } else {
                                Common.setSortType(Common.KEY_SORT_TYPE_FOR_CATEGORY, mSortType);
                            }
                        } else {
                                Common.setSortType(Common.KEY_SORT_TYPE_FOR_FOLDER, mSortType);
                        }
                        loadDir(mCurrentPath);
                        dialog.dismiss();
                    }
                })
                .create();
        mAlertDialog.show();
    }

    /**
     * Pop up the system information dialog
     */
    private void showSystemProperty() {
        mAlertDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.menu_property)
                .setMessage(getVolumesInfo())
                .setPositiveButton(android.R.string.ok, null)
                .create();
        mAlertDialog.show();
    }

    private String formatSize(long size) {
        return Formatter.formatFileSize(getActivity(), size);
    }

    private String getVolumesInfo() {
        StringBuilder memInfo = new StringBuilder(getString(R.string.internal_storage));

        StatFs sysFs = new StatFs(FileUtils.getInternalStoragePath());
        long sysFree = sysFs.getBlockSizeLong() * sysFs.getAvailableBlocksLong();
        long sysTotal = sysFs.getBlockSizeLong() * sysFs.getBlockCountLong();

        memInfo.append("\r\n");
        memInfo.append(getString(R.string.property_info_total));
        memInfo.append(formatSize(sysTotal));
        memInfo.append("\r\n");
        memInfo.append(getString(R.string.property_info_available));
        memInfo.append(formatSize(sysFree));

        ArrayList<String> extMountedVols = FileUtils.getExternalMountedVolumes(context);
        int size = extMountedVols.size();
        int loopCount = 0;

        if (size > 0)
            memInfo.append("\r\n------------------\r\n");

        while (loopCount < size) {
            try {
                String extVolume = extMountedVols.get(loopCount);
                File extVolFile = new File(extVolume);
                StatFs extVolFs = new StatFs(extVolFile.getPath());
                long extVolFree = extVolFs.getBlockSizeLong() * extVolFs.getAvailableBlocksLong();
                long extVolTotal = extVolFs.getBlockSizeLong() * extVolFs.getBlockCountLong();

                memInfo.append(FileUtils.getPathTitle(context, new File(extVolume)));
                memInfo.append("\r\n");
                memInfo.append(getString(R.string.property_info_total));
                memInfo.append(formatSize(extVolTotal));
                memInfo.append("\r\n");
                memInfo.append(getString(R.string.property_info_available));
                memInfo.append(formatSize(extVolFree));
                memInfo.append("\r\n");

            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }

            loopCount++;

            if (loopCount != size)
                memInfo.append("------------------\r\n");
        }

        return memInfo.toString();
    }

    /**
     * Show the waiting dialog
     */
    private void showProgressDialog(final int title, final int totalCount) {
        mProgressDialog = new CustomAlertDialog(context);
        mProgressDialog.setTitle(title);
        mProgressDialog.setMax(totalCount);
        //mProgressDialog.setMessage(message);
        mProgressDialog.show();
    }

    /**
     * Show the loading dialog
     */
    private void showLoadingDialog(final int title) {
        mLoadingDialog = new CustomProgressDialog(context, true);
        mLoadingDialog.setTitle(title);
        mLoadingDialog.setMessage(R.string.load_warning);
        mLoadingDialog.show();
    }


    protected void executeFileOperation(String dir, int mode, int requestCode, int resultCode) {
        Log.d(FileUtils.TAG, "request is:" + requestCode + " resultCode is:" + resultCode
                + " mode is:" + mode);
        if (resultCode != RESULT_OK) {
            return;
        }

        switch (mode) {
            case MODE_COPY:
                if (requestCode == REQ_CODE_DIR_SEL) {
                    // Get destination folder
                    mDstDir = dir;
                    copyFiles();
                }
                break;

            case MODE_MOVE:
                if (requestCode == REQ_CODE_DIR_SEL) {
                    // Get destination folder
                    mDstDir = dir;
                    // Get source files
                    showProgressDialog(R.string.progress_move_title,
                            PROGRESS_MAX_VALUE);
                    loadDir(mDstDir);
                    new MoveFileThread(mSelectedFiles, mDstDir).start();
                }
                break;

            case MODE_DELETE:
                showProgressDialog(R.string.progress_delete_title,
                        PROGRESS_MAX_VALUE);
                new DeleteFileThread(mSrcFiles).start();
                break;

            case MODE_SHARE:
                // share files.
                new ShareFileThread(mSrcFiles).start();
                break;
        }
    }

    /**
     * Copy multiple files
     */
    private void copyFiles() {
        mOverwriteFlag = OVERWRITE_FLAG_DISABLE;
        // Get source files
        showProgressDialog(R.string.progress_copy_title,
                PROGRESS_MAX_VALUE);
        loadDir(mDstDir);
        new CopyFileThread(mSelectedFiles, mDstDir).start();
    }

    /**
     * Generate the auto-suffix file when have the same file name.For example, a
     * file which named AAA.txt will be generated as AAA(1).txt and if the
     * AAA(1).txt is exist, it will be generated as AAA(2).txt, and so on...
     */
    private File generateNewFileWithSuffix(File dstFile) {
        int repeatCount = 1;
        String originalName = dstFile.getName();
        while (dstFile.exists()) {
            StringBuffer dstFileName = new StringBuffer();
            int index = originalName.lastIndexOf(SEPARATE_STRING);
            if (index == -1) {
                dstFileName.append(originalName).append(SUFFIX_LEFT).append(repeatCount)
                        .append(SUFFIX_RIGHT);
            } else {
                String nameWithoutExt = originalName.subSequence(0, index).toString();
                String extension = originalName.subSequence(index, originalName.length())
                        .toString();
                dstFileName.append(nameWithoutExt).append(SUFFIX_LEFT)
                        .append(repeatCount).append(SUFFIX_RIGHT).append(extension);
            }
            dstFile = new File(mDstDir, dstFileName.toString());
            repeatCount++;
        }
        return dstFile;
    }

    /**
     * Process the message from child threads
     */
    private final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            int event = msg.getData().getInt("event");
            int number = msg.getData().getInt("number");
            switch (event) {
                case EVENT_DELETE_FILE_OK:
                    mIsBack = true;
                    mProgressDialog.dismiss();
                    Toast.makeText(context, R.string.toast_delete_success,
                            Toast.LENGTH_LONG).show();
                    loadDir(mCurrentPath);
                    mCallback.onFileUpdate();
                    mCallback.notifyListChanged();
                    mMode = MODE_DEFAULT;
                    operateMode = MODE_DEFAULT;
                    break;

                case EVENT_COPY_FILE_OK:
                    mProgressDialog.dismiss();
                    loadDir(msg.getData().getString("dst_dir"));
                    mCallback.onFileUpdate();
                    mCallback.notifyListChanged();
                    mMode = MODE_DEFAULT;
                    operateMode = MODE_DEFAULT;
                    break;

                case EVENT_COPY_FALED_MEMORY_FULL:
                    if (number != 0)
                        mCallback.onFileUpdate();
                    // If one file operate faild, it doesn't mean no file
                    // operate
                    // success, so we need loadDir too.
                    if (mMode == MODE_COPY || mMode == MODE_MOVE) {
                        loadDir(msg.getData().getString("dst_dir"));
                    }
                    Toast.makeText(context, R.string.no_space,
                            Toast.LENGTH_LONG).show();
                    if (mProgressDialog != null)
                        mProgressDialog.dismiss();
                    mMode = MODE_DEFAULT;
                    operateMode = MODE_DEFAULT;
                    break;

                case EVENT_PROGRESS_EVENT:
                    Log.e(TAG, "EVENT_PROGRESS_EVENT number=" + number);
                    mProgressDialog.setProgress(number);
                    return;
                case EVENT_MOVE_FILE_OK:
                    mProgressDialog.dismiss();
                    loadDir(msg.getData().getString("dst_dir"));
                    mCallback.onFileUpdate();
                    mCallback.notifyListChanged();
                    mMode = MODE_DEFAULT;
                    operateMode = MODE_DEFAULT;
                    break;
                case EVENT_ADD_FAVORITE_OK:
                    mIsBack = true;
                    mFileAdapter.notifyDataSetChanged();
                    mCallback.notifyListChanged();
                    break;
                case EVENT_REMOVE_FAVORITE_OK:
                    mIsBack = true;
                    loadDir(FileUtils.fromTexttoPath(context,
                            mPath.getText().toString()));
                    mCallback.onFileUpdate();
                    mCallback.notifyListChanged();
                    break;
                case EVENT_FAILED:
                    if (number != 0)
                        mCallback.onFileUpdate();
                    // If one file operate faild, it doesn't mean no file
                    // operate
                    // success, so we need loadDir too.
                    if (mMode == MODE_DELETE) {
                        loadDir(mCurrentPath);
                    } else if (mMode == MODE_COPY || mMode == MODE_MOVE) {
                        loadDir(msg.getData().getString("dst_dir"));
                    }
                    Toast.makeText(context, R.string.toast_operate_failed,
                            Toast.LENGTH_LONG).show();
                    if (mProgressDialog != null)
                        mProgressDialog.dismiss();
                    mMode = MODE_DEFAULT;
                    operateMode = MODE_DEFAULT;
                    break;

                case EVENT_COPY_FILE_OVERWRITE:
                    // Show the overwrite confirm dialog
                    mAlertDialog = new AlertDialog.Builder(context)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle(android.R.string.dialog_alert_title)
                            .setMessage(String.format(getString(R.string.dialog_overwrite_confirm),
                                    msg.getData().getString("dst_file")))
                            .setPositiveButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface arg0, int arg1) {
                                            synchronized (handler) {
                                                // Send the overwrite files
                                                // acknowledge
                                                mOverwriteFlag = OVERWRITE_FLAG_ENABLE;
                                                handler.notify();
                                            }
                                        }
                                    })
                            .setNegativeButton(android.R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface arg0, int arg1) {
                                            synchronized (handler) {
                                                // Don't overwrite the files
                                                mOverwriteFlag = OVERWRITE_FLAG_DISABLE;
                                                handler.notify();
                                            }
                                        }
                                    })
                                    // If user presses "back" key when dialog is on the
                                    // top, we should also handle
                                    // this event as user presses "cancel" button. It is
                                    // important to notify the
                                    // CopyFileThread that the user do not want to
                                    // overwrite the file.
                            .setOnKeyListener(new DialogInterface.OnKeyListener() {
                                @Override
                                public boolean onKey(DialogInterface dialog, int keyCode,
                                                     KeyEvent event) {
                                    if ((keyCode == KeyEvent.KEYCODE_BACK
                                            || keyCode == KeyEvent.KEYCODE_SOFT_RIGHT)
                                            && event.getRepeatCount() == 0) {
                                        synchronized (handler) {
                                            // Don't overwrite the files
                                            mOverwriteFlag = OVERWRITE_FLAG_DISABLE;
                                            handler.notify();
                                        }
                                        // After user pressed "back" key,
                                        // dismiss the alert dialog.
                                        dialog.dismiss();
                                        return true;
                                    }
                                    return false;
                                }
                            }).create();
                    mAlertDialog.show();
                    break;
                case EVENT_LOAD_FILE_OK:
                    // Dismiss the progressbar if we have shown one.
                    String currentPath = msg.getData().getString("path");
                    if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
                        Log.e(TAG,"EVENT_LOAD_FILE_OK dismiss the dialog");
                        mLoadingDialog.dismiss();
                    }
                    if (mBtnOK != null) {
                        if (mRootPath.equals(currentPath))
                            mBtnOK.setEnabled(false);
                        else
                            mBtnOK.setEnabled(true);
                    }
                    if (FileUtils.CATEGORY_PATH.equals(currentPath) && categoryItems != null) {
                        mFileAdapter = new FilesAdapter(context, mPathItems, startMode, category);
                        listview.setAdapter(mFileAdapter);
                        mFileAdapter.setListView(listview);
                        listview.requestFocus();

                    } else {
                        mFileAdapter = new FilesAdapter(context, mPathItems, startMode, -1);
                        listview.setAdapter(mFileAdapter);
                        mFileAdapter.setListView(listview);
                        listview.requestFocus();
                        //Commented to remove the functionality of buttonup
                        /*if (mRootPath.equals(currentPath)) {
                            mBtnUp.setVisibility(View.INVISIBLE);
                            space.setVisibility(View.INVISIBLE);
                        } else {
                            mBtnUp.setVisibility(View.VISIBLE);
                            space.setVisibility(View.VISIBLE);
                        }*/
                    }
                    if (mIsBack && !mPosStack.empty()) {
                        Pos lastPos = mPosStack.pop();
                        listview.setSelectionFromTop(lastPos.index, lastPos.top);
                    }

                    if (mSelectionManager.inSelectionMode()) {

                        for (int i = 0; i < mSaveState.size(); i++) {
                            listview.setItemChecked(mSaveState.get(i), true);
                        }
                    }
                    // load file complete.
                    mIsLoading = false;

                    break;
            }

            if (event == EVENT_SHARE_OK) {
                mMode = MODE_DEFAULT;
                operateMode = MODE_DEFAULT;
            } else if (multimedia == 1) {
                // if operate the media file, need update the media library.
                Uri uri = Uri.parse("file://" + Environment.getExternalStorageDirectory());
                Intent intent = new Intent(ACTION_MEDIA_SCANNER_SCAN_AUDIOFILE, uri);
                context.sendBroadcast(intent);
            } else {
                Uri uri = Uri.parse("file://" + Environment.getExternalStorageDirectory());
                Intent intent = new Intent(ACTION_MEDIA_SCANNER_SCAN_ALL, uri);
                context.sendBroadcast(intent);
            }
            multimedia = 0;
        }
    };

    /**
     * Thread for deleting files
     */
    class DeleteFileThread extends Thread {
        public Handler mHandler;
        private ArrayList<String> mSrcFiles = null;
        private int event = EVENT_DELETE_FILE_OK;
        private int processEvent = EVENT_PROGRESS_EVENT;
        private int j = 0;
        private long totalFileNum = 0;

        public void run() {
            FileUtils.mMoveAndDeleteFlag++;
            totalFileNum = getTotalFileNum(mSrcFiles);
            for (int i = 0; i < mSrcFiles.size(); i++) {
                File srcFile = new File(mSrcFiles.get(i));
                String type = getMIMEType(srcFile);
                if (Common.ID_TYPE_AUDIO == Common.getFileTypeId(srcFile)) {
                    multimedia = 1;
                    NotificateMusicDelete(mSrcFiles.get(i));
                }
                if (FileUtils.deleteFiles(context, mSrcFiles.get(i), handler, totalFileNum)
                    == false) {
                    event = EVENT_FAILED;
                    j = i;
                } else {
                    if (mCurrentPath.equals(FileUtils.CATEGORY_PATH)) {
                        categoryItems.remove(mSrcFiles.get(i));
                    }
                }

                //if the deleted file belongs to favorite, remove it from favorite list.
                if (event != EVENT_FAILED && CategoryFragment.getFavoriteList().
                        contains(mSrcFiles.get(i))) {
                    mCallback.removeFavorite(mSrcFiles.get(i));
                }

            }
            FileUtils.mMoveAndDeleteFlag--;
            // Send response message to UI thread
            Message msg = handler.obtainMessage();
            Bundle b = new Bundle();
            b.putInt("event", event);
            b.putInt("number", j);
            msg.setData(b);
            handler.sendMessage(msg);
        }

        public DeleteFileThread(ArrayList<String> srcFiles) {
            mSrcFiles = srcFiles;
        }
    }

    private long getTotalFileNum(ArrayList<String> srcFiles) {
        int srcFileNum = 0;
        File fileToDelete;

        Queue<File> queue = new LinkedList<File>();

        for (String file : srcFiles) {
            fileToDelete = new File(file);

            if (fileToDelete.isDirectory()) {
                queue.offer(fileToDelete);

            } else {
                srcFileNum++;
            }
        }

        while ((fileToDelete = queue.poll()) != null) {

            if (! fileToDelete.isDirectory()) {
                srcFileNum++;

                continue;
            }

            for (File file : fileToDelete.listFiles()) {

                if (file.isDirectory()) {
                    queue.offer(file);

                } else {
                    srcFileNum++;
                }
            }
        }

        return srcFileNum;
    }

    /**
     * Thread for copying files
     */
    class CopyFileThread extends Thread {
        public Handler mHandler;
        private ArrayList<String> mSrcFiles = null;
        private String mDstDir = null;
        private int event = EVENT_COPY_FILE_OK;
        private int processEvent = EVENT_PROGRESS_EVENT;
        private int j = 0;
        private long totalSize = 0;

        public void run() {
            Common.setWakelock(context);
            for (int i = 0; i < mSrcFiles.size(); i++) {
                File sourceFile = new File(mSrcFiles.get(i));
                totalSize = totalSize + FileUtils.getFileSize(sourceFile);
            }
            for (int i = 0; i < mSrcFiles.size(); i++) {
                File srcFile = new File(mSrcFiles.get(i));
                File dstFile = new File(mDstDir, srcFile.getName());
                String type = getMIMEType(srcFile);
                if (Common.ID_TYPE_AUDIO == Common.getFileTypeId(srcFile)) {
                    multimedia = 1;
                }
                // Check the destination files, if existed, add the suffix.
                if (dstFile.exists() && mOverwriteFlag != OVERWRITE_FLAG_ALWAYS) {
                    dstFile = generateNewFileWithSuffix(dstFile);
                }

                int result = FileUtils.copyFiles(context, srcFile, dstFile, handler, totalSize);
                if (result != FileUtils.FILE_OPERATION_OK) {
                    if (result == FileUtils.FILE_OP_ERR_MEMORY_FULL) {
                        event = EVENT_COPY_FALED_MEMORY_FULL;
                    } else {
                        event = EVENT_FAILED;
                    }
                    j = i;
                    //break out?
                }
            }
            Common.unlock();
            // Send response message to UI thread
            Message msg = handler.obtainMessage();
            Bundle b = new Bundle();
            b.putInt("event", event);
            b.putInt("number", j);
            b.putString("dst_dir", mDstDir);
            msg.setData(b);
            handler.sendMessage(msg);
        }

        public CopyFileThread(ArrayList<String> srcFiles, String dstDir) {
            mSrcFiles = srcFiles;
            mDstDir = dstDir;
        }
    }

    /**
     * Thread for moving files
     */
    class MoveFileThread extends Thread {
        public Handler mHandler;
        private ArrayList<String> mSrcFiles = null;
        private String mDstDir = null;
        private int event = EVENT_MOVE_FILE_OK;
        private int processEvent = EVENT_PROGRESS_EVENT;
        // record the file number when operate failed
        private int j = 0;
        private long totalSize=0;

        public void run() {
            FileUtils.mMoveAndDeleteFlag++;
            Common.setWakelock(context);
            for (int i = 0; i < mSrcFiles.size(); i++) {
                File srcFile = new File(mSrcFiles.get(i));
                totalSize = totalSize + FileUtils.getFileSize(srcFile);
            }
            for (int i = 0; i < mSrcFiles.size(); i++) {
                File srcFile = new File(mSrcFiles.get(i));
                File dstFile = new File(mDstDir, srcFile.getName());
                String type = getMIMEType(srcFile);
                if (Common.ID_TYPE_AUDIO == Common.getFileTypeId(srcFile)) {
                    multimedia = 1;
                }

                // Check the destination files, if existed, add the suffix.
                // We will not add the suffix when the dstFile and srcFile are
                // in the same direction, because it is a invalid operation.
                if (dstFile.exists() && !dstFile.getParentFile().getAbsolutePath()
                        .equals(srcFile.getParentFile().getAbsolutePath())
                        && mOverwriteFlag != OVERWRITE_FLAG_ALWAYS) {
                    dstFile = generateNewFileWithSuffix(dstFile);
                }
                if (!FileUtils.moveFiles(context, srcFile, dstFile, handler, totalSize)) {
                    event = EVENT_FAILED;
                    j = i;
                    break;
                }
            }
            FileUtils.mMoveAndDeleteFlag--;
            Common.unlock();
            // Send response message to UI thread
            Message msg = handler.obtainMessage();
            Bundle b = new Bundle();
            b.putInt("event", event);
            b.putInt("number", j);
            b.putString("dst_dir", mDstDir);
            msg.setData(b);
            handler.sendMessage(msg);
        }

        public MoveFileThread(ArrayList<String> srcFiles, String dstDir) {
            mSrcFiles = srcFiles;
            mDstDir = dstDir;
        }
    }

    /**
     * Thread for share files
     */
    class ShareFileThread extends Thread {
        public Handler mHandler;
        private ArrayList<String> mSrcFiles = null;
        private int event = EVENT_SHARE_OK;

        public void run() {
            int count = mSrcFiles.size();
            Intent intent = new Intent();
            // Share single file
            if (count == 1) {
                String filePath = mSrcFiles.get(0);
                intent.setAction(Intent.ACTION_SEND);
                String mimeType = getMIMEType(new File(filePath));
                intent.setType(mimeType);
                Uri uri =  Uri.parse("file://" + getEncodePath(filePath));
                intent.putExtra(
                        Intent.EXTRA_STREAM, uri);
                intent.setType(mimeType);
            } else {
                // Share multiple files
                boolean audio = true;
                intent.setAction(Intent.ACTION_SEND_MULTIPLE);
                final ArrayList<Uri> value = new ArrayList<Uri>(count);
                for (int i = 0; i < count; i++) {
                    String path = mSrcFiles.get(i);
                    String encodePath = getEncodePath(path);
                    String mimeType = getMIMEType(new File(path));
                    if (!mimeType.equals("audio/*"))
                        audio = false;
                    value.add(Uri.parse("file://" + encodePath));
                }
                intent.setType(audio ? "audio/*" : "*/*");
                final Bundle b = new Bundle();
                b.putParcelableArrayList(Intent.EXTRA_STREAM, value);
                intent.putExtras(b);
                intent.setType(audio ? "audio/*" : "*/*");
            }
            context.startActivity(intent);
            // Send response message to UI thread
            Message msg = handler.obtainMessage();
            Bundle eventB = new Bundle();
            eventB.putInt("event", event);
            msg.setData(eventB);
            handler.sendMessage(msg);
        }

        public ShareFileThread(ArrayList<String> srcFiles) {
            mSrcFiles = srcFiles;
        }
    }

    /**
     * Thread for add file/folder to favorite
     */
    class AddFavoriteThread extends Thread {
        public Handler mHandler;
        private ArrayList<String> mSrcFiles = new ArrayList<String>();
        // record the file number when operate failed
        private int j = 0;

        public void run() {
            int count = mSrcFiles.size();
            int event = EVENT_ADD_FAVORITE_OK;

            for (int i = 0; i < count; i++) {
                if (mCallback.addToFavorite(mSrcFiles.get(i)) == false) {
                    event = EVENT_FAILED;
                    j = i;
                }
            }
            // Send response message to UI thread
            Message msg = handler.obtainMessage();
            Bundle b = new Bundle();
            b.putInt("event", event);
            b.putInt("number", j);
            msg.setData(b);
            handler.sendMessage(msg);
        }

        public AddFavoriteThread(ArrayList<String> srcFiles) {
            if (mSrcFiles != null)
                mSrcFiles.clear();
            ArrayList<String> favoriteList = CategoryFragment.getFavoriteList();
            for (int i = 0; i < srcFiles.size(); i++) {
                boolean isFavorite = favoriteList.contains(srcFiles.get(i));
                if (!isFavorite)
                    mSrcFiles.add(srcFiles.get(i));
            }
        }
    }

    /**
     * Thread for removing favorites
     */
    class RemoveFavoriteThread extends Thread {
        public Handler mHandler;
        private ArrayList<String> mSrcFiles = new ArrayList<String>();
        // record the file number when operate failed
        private int j = 0;

        public void run() {
            int event = EVENT_REMOVE_FAVORITE_OK;
            for (int i = 0; i < mSrcFiles.size(); i++) {
                // if remove favorite success, remove favorite icon, and update
                // list
                if (mCallback.removeFavorite(mSrcFiles.get(i)) == false) {
                    event = EVENT_FAILED;
                    j = i;
                }
            }
            // Send response message to UI thread
            Message msg = handler.obtainMessage();
            Bundle b = new Bundle();
            b.putInt("event", event);
            b.putInt("number", j);
            msg.setData(b);
            handler.sendMessage(msg);
        }

        public RemoveFavoriteThread(ArrayList<String> srcFiles) {
            if (mSrcFiles != null)
                mSrcFiles.clear();
            for (int i = 0; i < srcFiles.size(); i++) {
                mSrcFiles.add(srcFiles.get(i));
            }
        }
    }

    private String getEncodePath(String path) {
        File file = new File(path);
        String fileName = file.getName();
        String parentPath = file.getParent();
        String encodeFileName = URLEncoder.encode(fileName);
        encodeFileName = encodeFileName.replaceAll("\\+","%20");
        String encodePath = parentPath + "/" + encodeFileName;
        return encodePath;
    }

    @Override
    public void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        String filePath = mCurrentPath;

        context.unregisterReceiver(mReceiver);
        if (startMode == 1) {
            mPathItemsCache = mPathItems;
        }

        request = -1;
    }

    @Override
    public void onDestroy() {
        // Recycle all thumbnail in cache
        if (mFileAdapter != null) {
            mFileAdapter.clearThunmnailCache();
            mFileAdapter.onStop();
        }

        if (!mPosStack.empty()) {
            mPosStack.clear();
        }

        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        // When thread is stopped occassionaly,reset thread's counter.
        FileUtils.mCountNum = 0;
        FileUtils.mCountSize = 0;
        super.onDestroy();
    }

    private void NotificateMusicDelete(String path) {
        Cursor c = null;
        Uri uri = Uri.parse("file://" + Environment.getExternalStorageDirectory());
        Intent intent = new Intent(ACTION_DELETE_MUSIC, uri);
        try {
            ContentResolver resolver = context.getContentResolver();
            String[] cols = new String[]{
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ALBUM_ID
            };
            StringBuilder where = new StringBuilder();
            where.append(MediaStore.Audio.Media.DATA + " = '" + path + "'");
            c = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cols,
                    where.toString(), null, null);
            if (c != null) {
                c.moveToFirst();
                long id = c.getLong(0);
                long artIndex = c.getLong(2);
                intent.putExtra("mid", id);
                intent.putExtra("artindex", artIndex);
                context.sendBroadcast(intent);
            }
        } catch (Exception e) {
            Log.i(FileUtils.TAG, "Can't find this music in media database");
        } finally {
            if (c != null)
                c.close();
        }
    }

    private class CustomProgressDialog extends ProgressDialog {
        private int mTitleResId;
        private int mMsgResId;
        private int mTotalCount;
        private int mCurrentCount;

        public CustomProgressDialog(Context context) {
            super(context);
            //setProgressStyle(ProgressDialog.STYLE_SPINNER);
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            setCancelable(false);
        }

        public CustomProgressDialog(Context context, boolean loading) {
            super(context);
            if (loading) {
                setProgressStyle(ProgressDialog.STYLE_SPINNER);
                setCancelable(false);
            } else {
                Log.e(FileUtils.TAG, "Not support when loading is false");
            }
        }

        public void setTitle(int titleId) {
            if (titleId > 0) {
                super.setTitle(titleId);
                mTitleResId = titleId;
            }
        }

        public void setTotalCount(int total) {
            mTotalCount = total;
        }

        public void setMessage(int msgId) {
            if (msgId > 0) {
                super.setMessage(getContext().getString(msgId));
                mMsgResId = msgId;
            }
        }

        public void invalidate() {
            setTitle(mTitleResId);
            if (mMsgResId != 0)
                setMessage(mMsgResId);
        }
    }

    public interface OnFolderListener {
        // after select the dst directory and click ok,it will restart with
        // DEFAULT;
        public void onDirectorySelectedOk(String directory, int request, int mode, int result);

        // after click the context menu item and ok,it will restart with
        // DIR_SEL;
        public void onFileSelectedOk(int mode, String file);

        // return to the category home page
        public void onBacktoCategoryHome();

        // back to the former fragment
        public void onSelectedCancel(String dir);

        public void onFileUpdate();

        public void notifyListChanged();

        public boolean addToFavorite(String path);

        public boolean removeFavorite(String path);
    }

    @Override
    public void onSelectionChange(String path, boolean selected) {
        int count = mSelectionManager.getSelectedCount();
        String format = getActivity().getResources().getQuantityString(
                R.plurals.number_of_items_selected, count);
        mActionModeHandler.setTitle(String.format(format, count));
        mActionModeHandler.updateSupportedOperation(path, selected);
        mActionModeHandler.updateSelectionMenu();
    }

    @Override
    public void onSelectionModeChange(int mode) {
        switch (mode) {
            case SelectionManager.ENTER_SELECTION_MODE: {
                mActionModeHandler.startActionMode();
                //mVibrator.vibrate(100);
                break;
            }
            case SelectionManager.LEAVE_SELECTION_MODE: {
                mActionModeHandler.finishActionMode();
                for (int pos = 0; pos < listview.getCount(); pos++) {
                    listview.setItemChecked(pos, false);
                }
                listview.clearFocus();
                break;
            }
            case SelectionManager.SELECT_ALL_MODE: {
                mActionModeHandler.updateSelectionMenu();
                for (int pos = 0; pos < mFileAdapter.getCount(); pos++) {
                    listview.setItemChecked(pos, true);
                }
                break;
            }
            case SelectionManager.DESELECT_ALL_MODE: {
                //mActionModeHandler.updateSupportedOperation();
                for (int pos = 0; pos < mFileAdapter.getCount(); pos++) {
                    listview.setItemChecked(pos, false);
                }
                listview.clearFocus();
                mActionModeHandler.finishActionMode();
                break;
            }
        }

    }

    public void leaveSelectionMode() {
        if (mActionModeHandler != null) {
            mSelectionManager.leaveSelectionMode();
        }
    }

    public void onLongTap(int index) {
        String path = mFileAdapter.getItem(index).toString();
        if (mSelectionManager.isItemSelected(path)) return;
        mActionModeHandler.setMenuType(
                (startMode == CATEGORY_SEL && category == CategoryFragment.FAVORITE) ?
                        true : false);
        mSelectionManager.setAutoLeaveSelectionMode(true);
        mSelectionManager.toggle(path);

    }

    public void onFileSelectedDelete(ArrayList<String> fileList) {
        mSrcFiles.clear();
        mSrcFiles = fileList;
        showDeleteConfirmDialog(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                executeFileOperation(null, MODE_DELETE, -1, RESULT_OK);
            }
        });
    }

    public void onFileSelectedShare(ArrayList<String> fileList) {
        mSrcFiles.clear();
        mSrcFiles = fileList;
        executeFileOperation(null, MODE_SHARE, -1, RESULT_OK);
    }

    public void onFileSelectedAddFavorite(ArrayList<String> fileList) {
        mSrcFiles.clear();
        mSrcFiles = fileList;
        new AddFavoriteThread(mSrcFiles).start();
    }

    public void onFileSelectedRmFavorite(ArrayList<String> fileList) {
        mSrcFiles.clear();
        mSrcFiles = fileList;
        new RemoveFavoriteThread(mSrcFiles).start();
    }

    public void onFileSelectedRename(String file) {
        renameSelectedFile(file);
    }

    public void onFileSelectedDetail(String file) {
        new ShowSelectedDetailTask(file).execute(file);
    }

    /** This filter constrain edits not to make the length of the text greater than the specified length */
    private static class CustomLengthFilter implements InputFilter{
        private int mMax;
        private Context mContext;
        public CustomLengthFilter(Context context,int max){
            mMax = max;
            mContext = context;
        }

	@Override
	public CharSequence filter(CharSequence source, int start, int end,
	        Spanned dest, int dstart, int dend) {

	    int keep = mMax - (dest.length() - (dend -dstart));

            if (keep <= 0){
                showHint();
                return "";
            }else if (keep >= end - start){
                return null;
            }else{
                keep += start;
                if (Character.isHighSurrogate(source.charAt(keep - 1))){
                    --keep;
                    if (keep == start){
                        showHint();
                        return "";
                    }
                }
            }

            return source.subSequence(start, keep);
        }
        private void showHint(){
            Toast.makeText(mContext, mContext.getString(R.string.folder_name_reach_max_hint,mMax),
                    Toast.LENGTH_SHORT).show();
        }

    }
}
