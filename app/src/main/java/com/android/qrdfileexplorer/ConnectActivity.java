package com.android.qrdfileexplorer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.qrdfileexplorer.ftp.Cat.Cat;
import java.net.InetAddress;
import android.view.KeyEvent;
import android.widget.CompoundButton;

import android.app.Fragment;
import android.util.AttributeSet;
import android.app.ActionBar;
import com.android.qrdfileexplorer.ftp.FsService;
import com.android.qrdfileexplorer.ftp.FsSettings;
import android.view.ViewGroup;
import com.qcom.android.support.featurebar.FeatureBarHelper;
import android.widget.CheckBox;
import com.android.qrdfileexplorer.ftp.gui.PreferenceFragment;

public class ConnectActivity extends Activity {

    private ActionBar actionbar;
    private WifiReceiver mWifiReceiver;
    private TextView tv_wifi_state;
    private ImageView iv_wifi;
    private CheckBox btn_start;
    private boolean isStart = false;
    
    private TextView tv_ftp_address;
    private Handler mHandler = new Handler();
    private NetworkInfo wifiNetworkInfo;
	private Fragment currentFragment;
	private FeatureBarHelper mFeatureBarHelper;
	private ViewGroup mBar;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
		actionbar = getActionBar();
        actionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionbar.setCustomView(R.layout.my_action_bar);
		mFeatureBarHelper = new FeatureBarHelper(this);
		mBar = (ViewGroup)mFeatureBarHelper.getFeatureBar();
		mBar.setVisibility(View.GONE);
		
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        wifiNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		
        mWifiReceiver = new WifiReceiver();
		
        initView();
		
		initReceiver();
		
		
		 
    }

 
 @Override
    public void onResume() {
        super.onResume();

       updateRunningState();

        /*Cat.d("onResume: Register the preference change listner");
        SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
        sp.registerOnSharedPreferenceChangeListener(this);*/

        Cat.d("onResume: Registering the FTP server actions");
        IntentFilter filter = new IntentFilter();
        filter.addAction(FsService.ACTION_STARTED);
        filter.addAction(FsService.ACTION_STOPPED);
        filter.addAction(FsService.ACTION_FAILEDTOSTART);
        registerReceiver(mFsActionsReceiver, filter);
		
		 
    }
	
 

    private void initView() {
        tv_wifi_state = ((TextView) findViewById(R.id.tv_wifi_state));
        iv_wifi = ((ImageView) findViewById(R.id.iv_wifi));
        btn_start = ((CheckBox) findViewById(R.id.btn_start));
        tv_ftp_address = (TextView) findViewById(R.id.tv_ftp_address);
        btn_start.setText(getResources().getString(R.string.btn_start));
        tv_ftp_address.setText(getResources().getString(R.string.start_instruction));
		
		
        btn_start.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (isChecked) {
						 if (wifiNetworkInfo.isConnected()) {
							startServer();
							//updateRunningState();
						} else {
							 tv_ftp_address.setText( R.string.open_wifi_connection);  
						}
						
					}else {
						stopServer();
						tv_ftp_address.setText( R.string.running_summary_stopped );
						 
					}
					
				}
			});
    }
	 

    private void initReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        IntentFilter ftpFilter = new IntentFilter();
        ftpFilter.addAction(FsService.ACTION_STARTED);
        ftpFilter.addAction(FsService.ACTION_STOPPED);
        ftpFilter.addAction(FsService.ACTION_FAILEDTOSTART);
        registerReceiver(mFsActionsReceiver, ftpFilter);
        registerReceiver(mWifiReceiver, filter);
    }


    
	
     @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mWifiReceiver);
        unregisterReceiver(mFsActionsReceiver);
    } 
 

    public class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            
            //is wifi connecting
            if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (info.getState().equals(NetworkInfo.State.DISCONNECTED)) {
                    tv_wifi_state.setText(getResources().getString(R.string.wifi_not_connected));
                    iv_wifi.setImageResource(R.drawable.wifi_off);
                } else if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
                    WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    tv_wifi_state.setText(String.format(
					getResources().getString(R.string.network_state),wifiInfo.getSSID()));
                    iv_wifi.setImageResource(R.drawable.wifi);
                }
            }
        }
    }

    private void startServer() {
        sendBroadcast(new Intent(FsService.ACTION_START_FTPSERVER));
    }

    private void stopServer() {
        sendBroadcast(new Intent(FsService.ACTION_STOP_FTPSERVER));
    }


    private synchronized void    updateRunningState() {
        Resources res = getResources();
        
        if (FsService.isRunning()) {
           
            //  runningPref.setChecked(true);
            // Fill in the FTP server address
            InetAddress address = FsService.getLocalInetAddress();
            if (address == null) {
                Cat.v("Unable to retrieve wifi ip address");
                tv_ftp_address.setText(res.getString(
                        R.string.running_summary_failed_to_get_ip_address));
                return;
            }
            String iptext = "ftp://" + address.getHostAddress() + ":"
                    + FsSettings.getPortNumber() + "/";
          
             String summary = res.getString(R.string.running_summary_started, iptext);
			 btn_start.setText( R.string.btn_close);
			     tv_ftp_address.setText(  summary );
			 
         
        } else {
            
           
            tv_ftp_address.setText(res.getString(R.string.running_summary_stopped));

			btn_start.setText( R.string.btn_start );
            
            
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
            if (intent.getAction().equals(FsService.ACTION_FAILEDTOSTART)) {
				
				  
                mHandler.postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                tv_ftp_address.setText(
                                        R.string.running_summary_failed);
										 
                            }
                        }, 1000
                );
                mHandler.postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
								 
                                tv_ftp_address.setText(R.string.running_summary_stopped);
                            }
                        }, 5000
                );
            }
        }
    };
}
