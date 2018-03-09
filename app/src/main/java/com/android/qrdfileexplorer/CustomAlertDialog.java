/**
 * Copyright (c) 2014, Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Qualcomm Technologies Proprietary and Confidential.
 */

package com.android.qrdfileexplorer;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class CustomAlertDialog extends AlertDialog {
    private Context mContext;
    private ProgressBar mProgressBar;
    private TextView mProgressNumber;
    private String mProgressNumberFormat;
    private Handler mViewUpdateHandler;
    private int mMax;
    private int mTitleResId;
    private int mMsgResId;

    public CustomAlertDialog(Context context) {
        super(context);
        mContext = context;
        setCancelable(false);
        initFormats();
        initView();
    }

    private void initFormats() {
        mProgressNumberFormat = "%1d/%2d";
    }

    private void initView() {
        // TODO Auto-generated method stub
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.progress_dialog_view, null);
        setView(view);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mProgressNumber = (TextView) view.findViewById(R.id.progressNumber);
        mViewUpdateHandler = new Handler() {
            public void handleMessage(Message msg) {

                /* Update the number and percent */
                int progress = mProgressBar.getProgress();
                int max = mProgressBar.getMax();
                if (mProgressNumberFormat != null) {
                    String format = mProgressNumberFormat;
                    mProgressNumber.setText(String.format(format, progress, max));
                } else {
                    mProgressNumber.setText("");
                }
            }
        };
    }

    public void setTitle(int titleId) {
        if (titleId > 0) {
            super.setTitle(titleId);
            mTitleResId = titleId;
        }
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

    public void setMax(int max) {
        if (mProgressBar != null) {
            mProgressBar.setMax(max);
            onProgressChanged();
        } else {
            mMax = max;
        }
    }

    public void setProgress(int value) {
        mProgressBar.setProgress(value);
        onProgressChanged();
    }

    private void onProgressChanged() {
        if (mViewUpdateHandler != null && !mViewUpdateHandler.hasMessages(0)) {
            mViewUpdateHandler.sendEmptyMessage(0);
        }
    }

}
