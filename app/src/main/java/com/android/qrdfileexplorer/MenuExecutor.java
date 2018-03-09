/**
 * Copyright (c) 2013, Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Qualcomm Technologies Proprietary and Confidential.
 */

package com.android.qrdfileexplorer;

import android.app.Activity;
import android.app.Fragment;
import java.util.ArrayList;


public class MenuExecutor {
    private final Activity mActivity;
    private final SelectionManager mSelectionManager;
    private FileOperationListener mFileOperationCallback;
    private FileSelectionListener mSelectionListenerCallback;

    public MenuExecutor(
            Activity activity, SelectionManager selectionManager) {
        mActivity = activity;
        mFileOperationCallback = (FileOperationListener) activity;
        if ((Fragment) mActivity.getFragmentManager().findFragmentById(R.id.pager) instanceof FolderFragment) {
            mSelectionListenerCallback =
                    (FileSelectionListener) mActivity.getFragmentManager().findFragmentById(
                            R.id.pager);
        }
        mSelectionManager = selectionManager;
    }

    public boolean onMenuClicked(int action) {
        if ((mFileOperationCallback == null)
                || (mSelectionListenerCallback == null)
                || (mSelectionManager == null)) {
            return false;
        }

        switch (action) {
            case 1:
                if (mSelectionManager.inSelectAllMode()) {
                    mSelectionManager.deSelectAll();
                } else {
                    mSelectionManager.selectAll();
                }
                return true;
            case R.id.action_move: {
                mFileOperationCallback.onFileOperationSelected(
                        FolderFragment.MODE_MOVE,mSelectionManager.getSelected());
                return true;
            }
            case R.id.action_copy: {
                mFileOperationCallback.onFileOperationSelected(
                        FolderFragment.MODE_COPY, mSelectionManager.getSelected());
                return true;
            }
            case R.id.action_delete:
                mSelectionListenerCallback.onFileSelectedDelete(
                        mSelectionManager.getSelected());
                return true;
            case R.id.action_share:
                mSelectionListenerCallback.onFileSelectedShare(
                        mSelectionManager.getSelected());
                return true;
            case R.id.action_add_favorite:
                mSelectionListenerCallback.onFileSelectedAddFavorite(
                        mSelectionManager.getSelected());
                return true;
            case R.id.action_rm_favorite:
                mSelectionListenerCallback.onFileSelectedRmFavorite(
                        mSelectionManager.getSelected());
                return true;
            case R.id.action_rename:
                mSelectionListenerCallback.onFileSelectedRename(
                        mSelectionManager.getSelected().get(0));
                return true;
            case R.id.action_detail:
                mSelectionListenerCallback.onFileSelectedDetail(
                        mSelectionManager.getSelected().get(0));
                return true;
            default:
                return false;
        }
    }

    public interface FileOperationListener {
        public void onFileOperationSelected(int mode,ArrayList<String> fileList);
    }

    public interface FileSelectionListener {
        public void onFileSelectedDelete(ArrayList<String> fileList);
        public void onFileSelectedShare(ArrayList<String> fileList);
        public void onFileSelectedAddFavorite(ArrayList<String> fileList);
        public void onFileSelectedRmFavorite(ArrayList<String> fileList);
        public void onFileSelectedRename(String file);
        public void onFileSelectedDetail(String file);
    }

}
