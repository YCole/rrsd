/**
 * Copyright (c) 2013, Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Qualcomm Technologies Proprietary and Confidential.
 */
package com.android.qrdfileexplorer;

import android.app.Activity;

import android.content.Context;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Button;


import java.io.File;
import java.util.ArrayList;


public class ActionModeHandler implements Callback, PopupList.OnPopupItemClickListener {

    @SuppressWarnings("unused")
    private static final String TAG = "ActionModeHandler";

    @Override
    public boolean onPopupItemClick(int itemId) {
        if (itemId == 1) {
            mMenuExecutor.onMenuClicked(itemId);
            updateSelectedSupportOperation();
        }
        return true;
    }

    public interface ActionModeListener {
        public boolean onActionItemClicked(MenuItem item);
    }

    private Menu mMenu;
    private MenuItem mCopy;
    private MenuItem mMove;
    private MenuItem mDelete;
    private MenuItem mShare;
    private MenuItem mAddFavorite;
    private MenuItem mRemoveFavorite;
    private MenuItem mRename;
    private MenuItem mDetail;
    private ActionModeListener mListener;
    private ActionMode mActionMode;
    private final Activity mActivity;
    private final SelectionManager mSelectionManager;
    private SelectionMenu mSelectionMenu;
    private final MenuExecutor mMenuExecutor;
    private boolean mIsCategoryFavorite;

    public ActionModeHandler(Activity activity, SelectionManager selectionManager) {
        mActivity = activity;
        mSelectionManager = selectionManager;
        mMenuExecutor = new MenuExecutor(activity, mSelectionManager);

    }

    public void startActionMode() {
        Activity a = mActivity;
        mActionMode = a.startActionMode(this);
        View customView = LayoutInflater.from(a).inflate(R.layout.action_mode, null);
        mActionMode.setCustomView(customView);
        mSelectionMenu = new SelectionMenu(a,
                (Button) customView.findViewById(R.id.selection_menu), this);
        updateSelectionMenu();

    }

    public void setMenuType(boolean favorite) {
        mIsCategoryFavorite = favorite;
    }

    public void finishActionMode() {
        mActionMode.finish();
        mSelectionMenu.dismiss();
    }

    public void setTitle(String title) {
        mSelectionMenu.setTitle(title);

    }

    public void setActionModeListener(ActionModeListener listener) {
        mListener = listener;
    }

    public void updateSelectionMenu() {
        // update title
        int count = mSelectionManager.getSelectedCount();
        String format = mActivity.getResources().getQuantityString(
                R.plurals.number_of_items_selected, count);
        setTitle(String.format(format, count));
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        mActionMode = actionMode;
        MenuInflater inflater = actionMode.getMenuInflater();
        inflater.inflate(R.menu.select_action_menu, menu);
        mCopy = menu.findItem(R.id.action_copy);
        mMove = menu.findItem(R.id.action_move);
        mDelete = menu.findItem(R.id.action_delete);
        mShare = menu.findItem(R.id.action_share);
        mAddFavorite = menu.findItem(R.id.action_add_favorite);
        mRemoveFavorite = menu.findItem(R.id.action_rm_favorite);
        mRename = menu.findItem(R.id.action_rename);
        mDetail = menu.findItem(R.id.action_detail);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        updateSelectedSupportOperation();
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        int action = menuItem.getItemId();
        boolean bValue = mMenuExecutor.onMenuClicked(action);
        mSelectionManager.leaveSelectionMode();
        return bValue;
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        mSelectionManager.leaveSelectionMode();

    }

    private void updateSelectedSupportOperation() {
        if (mIsCategoryFavorite) {
            mCopy.setVisible(false);
            mMove.setVisible(false);
            mDelete.setVisible(false);
            mRename.setVisible(false);
            if (mSelectionManager.getSelectedCount() > 1) {
                mDetail.setVisible(false);
            } else {
                mDetail.setVisible(true);
            }
        } else if (mSelectionManager.getSelectedCount() > 1) {
            mDetail.setVisible(false);
            mRename.setVisible(false);
        } else {
            mDetail.setVisible(true);
            mRename.setVisible(true);
        }
        for (String filename : mSelectionManager.getSelected()) {
            File f = new File(filename);
            if (f.isDirectory()) {
                mShare.setVisible(false);
                break;
            } else {
                mShare.setVisible(true);
            }
        }

        ArrayList<String> favoriteList = CategoryFragment.getFavoriteList();
        mAddFavorite.setVisible(false);
        mRemoveFavorite.setVisible(true);
        for (String filename : mSelectionManager.getSelected()) {
            if (!favoriteList.contains(filename)) {
                mAddFavorite.setVisible(true);
                mRemoveFavorite.setVisible(false);
                break;
            }
        }
    }

    public void updateSupportedOperation(String path, boolean selected) {
        File file = new File(path);
        if (file.isDirectory()) {
            if (selected) {
                mShare.setVisible(false);
            } else {
                updateSelectedSupportOperation();
            }
        }
        updateSelectedSupportOperation();
    }

    private class SelectionMenu implements View.OnClickListener {

        private final Context mContext;
        private final Button mButton;
        private final PopupList mPopupList;

        public SelectionMenu(Context context,
                             Button button,
                             PopupList.OnPopupItemClickListener listener) {
            mContext = context;
            mButton = button;
            mPopupList = new PopupList(context, mButton);
            mPopupList.addItem(1,
                    context.getString(R.string.select_all));
            mPopupList.setOnPopupItemClickListener(listener);
            mButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            updateSelectAllMode(mSelectionManager.inSelectAllMode());
            mPopupList.show();
        }

        public void dismiss() {
            mPopupList.dismiss();
        }

        public void updateSelectAllMode(boolean inSelectAllMode) {
            PopupList.Item item = mPopupList.findItem(1);
            if (item != null) {
                item.setTitle(mContext.getString(
                        inSelectAllMode ? R.string.deselect_all : R.string.select_all));
            }
        }

        public void setTitle(CharSequence title) {
            mButton.setText(title);
        }
    }

}
