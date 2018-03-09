/**
 * Copyright (c) 2013 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Qualcomm Technologies Confidential and Proprietary.
 *
 */

package com.android.qrdfileexplorer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class SelectionManager {

    public static final int ENTER_SELECTION_MODE = 1;
    public static final int LEAVE_SELECTION_MODE = 2;
    public static final int SELECT_ALL_MODE = 3;
    public static final int DESELECT_ALL_MODE = 4;
    private Set<String> mClickedPath;
    private List<String> mSourceFileList;
    private SelectionListener mListener;
    private boolean mInverseSelection;
    private boolean mInSelectionMode;
    private boolean mAutoLeave = true;
    private int mTotal;

    public SelectionManager() {
        mClickedPath = new HashSet<String>();
        mTotal = -1;
    }

    // Whether we will leave selection mode automatically once the number of
    // selected items is down to zero.
    public void setAutoLeaveSelectionMode(boolean enable) {
        mAutoLeave = enable;
    }

    public void setSelectionListener(SelectionListener listener) {
        mListener = listener;
    }

    public void selectAll() {
        mInverseSelection = true;
        enterSelectionMode();
        for (String file : mSourceFileList) {
            if (!mClickedPath.contains(file)) {
                mClickedPath.add(file);
            }
        }
        if (mListener != null) mListener.onSelectionModeChange(SELECT_ALL_MODE);
    }

    public void deSelectAll() {
        mInSelectionMode = false;
        mInverseSelection = false;
        mClickedPath.clear();
        if (mListener != null) mListener.onSelectionModeChange(DESELECT_ALL_MODE);
    }

    public boolean inSelectAllMode() {
        return mInverseSelection;
    }

    public boolean inSelectionMode() {
        return mInSelectionMode;
    }

    public void enterSelectionMode() {
        if (mInSelectionMode) return;
        mInSelectionMode = true;
        if (mListener != null) mListener.onSelectionModeChange(ENTER_SELECTION_MODE);
    }

    public void leaveSelectionMode() {
        if (!mInSelectionMode) return;

        mInSelectionMode = false;
        mInverseSelection = false;
        mClickedPath.clear();
        if (mListener != null) mListener.onSelectionModeChange(LEAVE_SELECTION_MODE);
    }

    public boolean isItemSelected(String itemId) {
        return mClickedPath.contains(itemId);
    }

    private int getTotalCount() {
        return mTotal;
    }

    public void setTotalCount(int totalCount) {
        mTotal = totalCount;
    }

    public int getSelectedCount() {
        int count = mClickedPath.size();

        return count;
    }

    public void toggle(String path) {
        if (mClickedPath.contains(path)) {
            mClickedPath.remove(path);
            mInverseSelection = false;
        } else {
            enterSelectionMode();
            mClickedPath.add(path);
        }

        // Convert to inverse selection mode if everything is selected.
        int count = getSelectedCount();
        if (count == getTotalCount()) {
            selectAll();
        }

        if (mListener != null) mListener.onSelectionChange(path, isItemSelected(path));

        if (count == 0 && mAutoLeave) {
            leaveSelectionMode();
        } else if (count == 0) {
            deSelectAll();
        }

    }

    public ArrayList<String> getSelected() {
        ArrayList<String> selected = new ArrayList<String>();

        for (String path : mClickedPath) {
            selected.add(path);
        }

        return selected;
    }

    public void setSourceFileList(List<String> fileList) {
        mSourceFileList = fileList;
    }

    public interface SelectionListener {
        public void onSelectionModeChange(int mode);

        public void onSelectionChange(String path, boolean selected);
    }
}
