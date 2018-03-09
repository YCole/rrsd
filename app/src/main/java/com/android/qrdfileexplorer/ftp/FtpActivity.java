package com.android.qrdfileexplorer.ftp;

import android.app.Activity;
import android.os.Bundle;
import com.android.qrdfileexplorer.R;
import com.android.qrdfileexplorer.ftp.gui.PreferenceFragment;

public class FtpActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ftp);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new PreferenceFragment())
                .commit();
    }
}
