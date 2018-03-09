/*******************************************************************************
 * Copyright (c) 2012-2013 Pieter Pareit.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * Contributors:
 * Pieter Pareit - initial API and implementation
 ******************************************************************************/

package com.android.qrdfileexplorer.ftp.gui;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.preference.EditTextPreference;
import android.preference.PreferenceScreen;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference;
import android.view.View;
import android.widget.Toast;

import com.android.qrdfileexplorer.ftp.App;
import com.android.qrdfileexplorer.ftp.Cat.Cat;
import com.android.qrdfileexplorer.ftp.FsService;
import com.android.qrdfileexplorer.ftp.FsSettings;
import com.android.qrdfileexplorer.R;

import java.io.File;
import java.net.InetAddress;


/**
 * This is the main activity for swiftp, it enables the user to start the server service
 * and allows the users to change the settings.
 */
public class PreferenceFragment extends android.preference.PreferenceFragment implements OnSharedPreferenceChangeListener {

    private EditTextPreference mPassWordPref;
    private Handler mHandler = new Handler();
    private View mView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());


        PreferenceScreen  total= findPref("preference_screen");
        Resources resources = getResources();

        startServer();

        /*Preference runningPref = findPref("running_switch");
        updateRunningState();
        runningPref.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if ((Boolean) newValue) {
                            startServer();
                        } else {
                            stopServer();
                        }
                        return true;
                    }
                }
        );
*/



        updateLoginInfo();
        EditTextPreference usernamePref = findPref("username");
        usernamePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                                                       @Override
                                                       public boolean onPreferenceChange(Preference preference, Object newValue) {
                                                           String newUsername = (String) newValue;
                                                           if (preference.getSummary().equals(newUsername))
                                                               return false;
                                                           if (!newUsername.matches("[a-zA-Z0-9]+")) {
                                                               Toast.makeText(getActivity(),
                                                                       R.string.username_validation_error, Toast.LENGTH_LONG).show();
                                                               return false;
                                                           }
                                                           stopServer();
                                                           return true;
                                                       }
                                                   }
        );

        mPassWordPref = findPref("password");
        mPassWordPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                                                        @Override
                                                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                                                            stopServer();
                                                            return true;
                                                        }
                                                    }
        );


        EditTextPreference portnum_pref = findPref("portNum");
        portnum_pref.setSummary(sp.getString("portNum",
                resources.getString(R.string.portnumber_default)));
        portnum_pref.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        String newPortnumString = (String) newValue;
                        if (preference.getSummary().equals(newPortnumString))
                            return false;
                        int portnum = 0;
                        try {
                            portnum = Integer.parseInt(newPortnumString);
                        } catch (Exception e) {
                            Cat.d("Error parsing port number! Moving on...");
                        }
                        if (portnum <= 0 || 65535 < portnum) {
                            Toast.makeText(getActivity(),
                                    R.string.port_validation_error, Toast.LENGTH_LONG).show();
                            return false;
                        }
                        preference.setSummary(newPortnumString);
                        stopServer();
                        return true;
                    }
                }

        );

        Preference chroot_pref = findPref("chrootDir");
        chroot_pref.setSummary(FsSettings.getChrootDirAsString());
        chroot_pref.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(final Preference preference) {
                        AlertDialog folderPicker = new FolderPickerDialogBuilder(getActivity(), FsSettings.getChrootDir())
                                .setSelectedButton(R.string.select,
                                        new FolderPickerDialogBuilder.OnSelectedListener() {
                                            @Override
                                            public void onSelected(String path) {
                                                if (preference.getSummary().equals(path))
                                                    return;
                                                if (!FsSettings.setChrootDir(path))
                                                    return;
                                                // TODO: this is a hotfix, create correct resources, improve UI/UX
                                                final File root = new File(path);
                                                if (!root.canRead()) {
                                                    Toast.makeText(getActivity(),
                                                            "Notice that we can't read/write in this folder.",
                                                            Toast.LENGTH_LONG).show();
                                                } else if (!root.canWrite()) {
                                                    Toast.makeText(getActivity(),
                                                            "Notice that we can't write in this folder, reading will work. Writing in subfolders might work.",
                                                            Toast.LENGTH_LONG).show();
                                                }

                                                preference.setSummary(path);
                                                stopServer();
                                            }
                                        }
                                )

                                .setNegativeButton(R.string.cancel, null)
                                .create();
                        folderPicker.show();
                        return true;
                    }
                }
        );



        total.removePreference(chroot_pref);
        total.removePreference(usernamePref);
        total.removePreference(mPassWordPref);
        total.removePreference(portnum_pref);

    }

    @Override
    public void onResume() {
        super.onResume();

        updateRunningState();

        Cat.d("onResume: Register the preference change listner");
        SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
        sp.registerOnSharedPreferenceChangeListener(this);

        Cat.d("onResume: Registering the FTP server actions");
        IntentFilter filter = new IntentFilter();
        filter.addAction(FsService.ACTION_STARTED);
        filter.addAction(FsService.ACTION_STOPPED);
        filter.addAction(FsService.ACTION_FAILEDTOSTART);
        getActivity().registerReceiver(mFsActionsReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();

        Cat.v("onPause: Unregistering the FTPServer actions");
        getActivity().unregisterReceiver(mFsActionsReceiver);

        Cat.d("onPause: Unregistering the preference change listner");
        SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
        sp.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        updateLoginInfo();
    }

    private void startServer() {
        getActivity().sendBroadcast(new Intent(FsService.ACTION_START_FTPSERVER));
    }

    private void stopServer() {
        getActivity().sendBroadcast(new Intent(FsService.ACTION_STOP_FTPSERVER));
    }

    private void updateLoginInfo() {

        String username = FsSettings.getUserName();
        String password = FsSettings.getPassWord();

        Cat.v("Updating login summary");


        EditTextPreference usernamePref = findPref("username");
        usernamePref.setSummary(username);

        EditTextPreference passWordPref = findPref("password");
        passWordPref.setSummary(transformPassword(password));
    }
    private void updateRunningState() {
        Resources res = getResources();
        Preference runningPref = findPref("running_switch");
        Cat.v("U**********START******************");
        if (FsService.isRunning()) {
            Cat.v("U**********FsService*******RUN***********"+FsService.isRunning());
            //  runningPref.setChecked(true);
            // Fill in the FTP server address
            InetAddress address = FsService.getLocalInetAddress();
            if (address == null) {
                Cat.v("Unable to retrieve wifi ip address");
                runningPref.setSummary(R.string.running_summary_failed_to_get_ip_address);
                return;
            }
            String iptext = "ftp://" + address.getHostAddress() + ":"
                    + FsSettings.getPortNumber() + "/";
            Cat.v("U****************************"+iptext);
            String summary = res.getString(R.string.running_summary_started, iptext);
            runningPref.setSummary(summary);
        } else {
            Cat.v("U**********FsService*********FAILED*********"+FsService.isRunning());
           // runningPref.setChecked(false);
            runningPref.setSummary(R.string.running_summary_stopped);
        }
    }

    /**
     * This receiver will check FTPServer.ACTION* messages and will update the button,
     * running_state, if the server is running and will also display at what url the
     * server is running.
     */
    BroadcastReceiver mFsActionsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Cat.v("action received: " + intent.getAction());
            // remove all pending callbacks
            mHandler.removeCallbacksAndMessages(null);
            // action will be ACTION_STARTED or ACTION_STOPPED
            updateRunningState();
            // or it might be ACTION_FAILEDTOSTART
            final Preference runningPref = findPref("running_switch");
            if (intent.getAction().equals(FsService.ACTION_FAILEDTOSTART)) {
                // runningPref.setChecked(false);
                mHandler.postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                runningPref.setSummary(R.string.running_summary_failed);
                            }
                        }, 100
                );
                mHandler.postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                runningPref.setSummary(R.string.running_summary_stopped);
                            }
                        }, 2000
                );
            }
        }
    };

    static private String transformPassword(String password) {
        Context context = App.getAppContext();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        Resources res = context.getResources();
        String showPasswordString = res.getString(R.string.show_password_default);
        boolean showPassword = showPasswordString.equals("true");
        showPassword = sp.getBoolean("show_password", showPassword);
        if (showPassword)
            return password;
        else {
            StringBuilder sb = new StringBuilder(password.length());
            for (int i = 0; i < password.length(); ++i)
                sb.append('*');
            return sb.toString();
        }
    }

    @SuppressWarnings({"unchecked", "deprecation"})
    protected <T extends Preference> T findPref(CharSequence key) {
        return (T) findPreference(key);
    }

}
