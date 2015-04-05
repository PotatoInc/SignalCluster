package ucup.tech.icons;
import ucup.tech.icons.R;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class SignalCluster extends LinearLayout{
    private FrameLayout mSignalCluster, mWifiCluster;
    private ImageView mDataIcon, mSignalIcon, mInOutIcon, mWifiSignalIcon, mWifiInOutIcon;
    private final LayoutParams mLayoutParam = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

    private IntentFilter mFilter;
    private Context mContext;
    private ServiceState mService;
    //    private Controller mControl;
    private Handler mWifiHandler;
    private Handler mHandler;
    
    private boolean mShowCmSignal;

    private int mDataState, mDataActivity, mSignalStrength;
    private long lastTx,lastRx;
    private boolean mIsWifiConnected = false;
    private boolean isWifiInUse;
    private final String TAG = "SignalCluster";

    private static final int[] sSignalImages = {
        R.drawable.stat_sys_signal_0,
        R.drawable.stat_sys_signal_1,
        R.drawable.stat_sys_signal_2,
        R.drawable.stat_sys_signal_3,
        R.drawable.stat_sys_signal_4
    };

    // wifi
    private static final int[] sWifiSignalImages = {
        R.drawable.stat_sys_wifi_signal_0,
        R.drawable.stat_sys_wifi_signal_1,
        R.drawable.stat_sys_wifi_signal_2,
        R.drawable.stat_sys_wifi_signal_3,
        R.drawable.stat_sys_wifi_signal_4 
    };

    public SignalCluster(Context arg0, AttributeSet arg1) {
        super(arg0, arg1);

        mContext = arg0;
        //        mControl = new Controller(arg0);
        mWifiHandler = new Handler();

        mSignalCluster = new FrameLayout(arg0); 
        mSignalCluster.setLayoutParams(mLayoutParam);
        
        mDataIcon = new ImageView(arg0); 
        mSignalIcon = new ImageView(arg0); 
        mInOutIcon = new ImageView(arg0);
        
        mDataIcon.setLayoutParams(mLayoutParam); 
        mSignalIcon.setLayoutParams(mLayoutParam); 
        mInOutIcon.setLayoutParams(mLayoutParam);
        
        mSignalCluster.addView(mDataIcon); 
        mSignalCluster.addView(mSignalIcon); 
        mSignalCluster.addView(mInOutIcon);

        mWifiCluster = new FrameLayout(arg0); 
        mWifiCluster.setLayoutParams(mLayoutParam);

        mWifiSignalIcon = new ImageView(arg0); 
        mWifiInOutIcon = new ImageView(arg0);
        
        mWifiSignalIcon.setLayoutParams(mLayoutParam); 
        mWifiInOutIcon.setLayoutParams(mLayoutParam);

        mWifiCluster.addView(mWifiSignalIcon); 
        mWifiCluster.addView(mWifiInOutIcon);

        mWifiInOutIcon.setImageDrawable(null);
        mWifiSignalIcon.setImageResource(sWifiSignalImages[3]);
        mWifiCluster.setVisibility(View.GONE);

        this.addView(mWifiCluster); 
        this.addView(mSignalCluster);

        mFilter = new IntentFilter();
        mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        mFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);

        arg0.registerReceiver(mReceiver, mFilter);

        ((TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE))
        .listen(mPhoneStateListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                | PhoneStateListener.LISTEN_DATA_ACTIVITY
                | PhoneStateListener.LISTEN_SERVICE_STATE
                | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        
        mHandler = new Handler();
        
        CMSignalIconObserver mSignalObserver = new CMSignalIconObserver(mHandler);
        mSignalObserver.observe();
        
        setSignalIconVisibility();
        
    }
    private void updateConnectivity(Intent intent){
        NetworkInfo info = (NetworkInfo)(intent.getParcelableExtra(
                ConnectivityManager.EXTRA_NETWORK_INFO));
        switch(info.getType()){
            case ConnectivityManager.TYPE_MOBILE:
                updateDataNetType(info.getSubtype());
                updateInOutIcon();
                updateSignalIcon();
                mWifiCluster.setVisibility(View.GONE);
                break;
            case ConnectivityManager.TYPE_WIFI:
                if (info.isConnected()) {
                    mIsWifiConnected = true;
                    mWifiCluster.setVisibility(View.VISIBLE);
                    mWifiHandler.post(WifiInOut);
                }else{
                    mWifiHandler.removeCallbacks(WifiInOut);
                }
                break;
            default:
                makeLog("TYPE: NOT MOBILE");
                this.mDataIcon.setImageDrawable(null);
                this.mInOutIcon.setImageDrawable(null);
                mWifiCluster.setVisibility(View.GONE);
                mWifiHandler.removeCallbacks(WifiInOut);
                break;
        }
    }
    private void updateDataNetType(int type){
        if (hasService() && mDataState == TelephonyManager.DATA_CONNECTED) {
            switch (type) {
                case TelephonyManager.NETWORK_TYPE_EDGE:
                    makeLog("DATAICON: EDGE");
                    this.mDataIcon.setImageResource(R.drawable.stat_sys_data_connected_e);
                    break;
                case  TelephonyManager.NETWORK_TYPE_UMTS:
                    makeLog("DATAICON: 3G");
                    this.mDataIcon.setImageResource(R.drawable.stat_sys_data_connected_3g);
                    break;
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                    makeLog("DATAICON: HSPA");
                    this.mDataIcon.setImageResource(R.drawable.stat_sys_data_connected_h);
                    break;
                case TelephonyManager.NETWORK_TYPE_GPRS:
                    makeLog("DATAICON: GPRS");
                    this.mDataIcon.setImageResource(R.drawable.stat_sys_data_connected_g);
                    break;
                default:
                    makeLog("DATAICON: NULL");
                    this.mDataIcon.setImageDrawable(null);
                    break;
            }
            return;
        }
        makeLog("DATAICON: NULL");
        this.mDataIcon.setImageDrawable(null);
    }
    private void updateInOutIcon(){
        if (hasService() && mDataState == TelephonyManager.DATA_CONNECTED) {
            switch (mDataActivity) {
                case TelephonyManager.DATA_ACTIVITY_IN:
                    //makeLog("INOUT: IN");
                    this.mInOutIcon.setImageResource(R.drawable.stat_sys_signal_in);
                    break;
                case TelephonyManager.DATA_ACTIVITY_OUT:
                    //makeLog("INOUT: OUT");
                    this.mInOutIcon.setImageResource(R.drawable.stat_sys_signal_out);
                    break;
                case TelephonyManager.DATA_ACTIVITY_INOUT:
                    //makeLog("INOUT: INOUT");
                    this.mInOutIcon.setImageResource(R.drawable.stat_sys_signal_inout);
                    break;
                default:
                    //makeLog("INOUT: NULL");
                    this.mInOutIcon.setImageDrawable(null);
                    break;
            }
            return;
        }
        makeLog("INOUT: NULL");
        this.mInOutIcon.setImageDrawable(null);
    }
    private void updateSignalIcon(){
        int level;

        if (mService == null || (!hasService()) ) {
            makeLog("SIGNAL: NO SERVICE");
            if (Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.AIRPLANE_MODE_ON, 0) == 1) {
                this.mSignalIcon.setImageResource(R.drawable.stat_sys_signal_flightmode);
            } else {
                this.mSignalIcon.setImageResource(R.drawable.stat_sys_signal_null);
            }
            return; 
        }

        if (mSignalStrength <= 0)
            level = 0;
        else if (mSignalStrength >= 1 && mSignalStrength <= 4)
            level = 1;
        else if (mSignalStrength >= 5 && mSignalStrength <= 7)
            level = 2;
        else if (mSignalStrength >= 8 && mSignalStrength <= 11)
            level = 3;
        else if (mSignalStrength >= 12)
            level = 4;
        else
            level = 0;
        //makeLog("LEVEL: " + String.valueOf(level));
        //this.mSignalIcon.setImageLevel(level);
        this.mSignalIcon.setImageResource(sSignalImages[level]);
        //this.mSignalIcon.setImageResource(mControl.getDrawableId("stat_sys_signal"));
    }

    private void updateWifiSignalIcon(Intent intent){
        final String action = intent.getAction();
        if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {

            final boolean enabled = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN) == WifiManager.WIFI_STATE_ENABLED;

            if (!enabled) {
                // If disabled, hide the icon. (We show icon when connected.)
                mWifiCluster.setVisibility(View.GONE);
            }

        } else if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
            final boolean enabled = intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED,
                    false);
            if (!enabled) {
                mWifiCluster.setVisibility(View.GONE);
            }
        } else if (action.equals(WifiManager.RSSI_CHANGED_ACTION)) {
            int iconId;
            final int newRssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, -200);
            int newSignalLevel = WifiManager.calculateSignalLevel(newRssi,
                    5);
            if (mIsWifiConnected) {
                iconId = newSignalLevel;
            } else {
                iconId = 0;
            }
            //mWifiSignalIcon.setImageLevel(iconId);
            mWifiSignalIcon.setImageResource(sWifiSignalImages[iconId]);
        }
    }
    private boolean hasService() {
        if (mService != null) {
            switch (mService.getState()) {
                case ServiceState.STATE_OUT_OF_SERVICE:
                case ServiceState.STATE_POWER_OFF:
                    return false;
                default:
                    return true;
            }
        } else {
            return false;
        }
    }
    private void makeLog(String log){
        Log.d(TAG, log);
    }
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            String action = arg1.getAction().toString();
            if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION) ||
                    action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION) ||
                    action.equals(WifiManager.RSSI_CHANGED_ACTION)) {
                updateWifiSignalIcon(arg1);
            }else if(action.equals(ConnectivityManager.CONNECTIVITY_ACTION)){
                updateConnectivity(arg1);
            }
        }
    };

    private Runnable WifiInOut = new Runnable() {

        public void run() {
            long cRx = TrafficStats.getTotalRxBytes() - TrafficStats.getMobileRxBytes();
            long cTx = TrafficStats.getTotalTxBytes() - TrafficStats.getMobileTxBytes();
            if (cTx - lastTx != 0 || cRx - lastRx != 0){
                if (!isWifiInUse){
                    isWifiInUse = true;
                    mWifiInOutIcon.setImageResource(R.drawable.stat_sys_wifi_inout);
                    if(cTx - lastTx !=0 && cRx - lastRx == 0){
                        mWifiInOutIcon.setImageResource(R.drawable.stat_sys_wifi_in);
                    }else if(cTx - lastTx ==0 && cRx - lastRx != 0){
                        mWifiInOutIcon.setImageResource(R.drawable.stat_sys_wifi_out);
                    }
                }
            }else if (isWifiInUse){
                isWifiInUse = false;
                mWifiInOutIcon.setImageDrawable(null);
            }
            lastRx = cRx;
            lastTx = cTx;
            mWifiHandler.postDelayed(this, 1000);
        }
    }; 
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onServiceStateChanged(ServiceState state) {
            mService = state;
            updateSignalIcon();
            updateInOutIcon();
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength paramSignalStrength)
        {
            mSignalStrength = paramSignalStrength.getGsmSignalStrength();
            updateSignalIcon();
        }
        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            mDataState = state;
            updateDataNetType(networkType);
            updateInOutIcon();
        }

        @Override
        public void onDataActivity(int direction) {
            mDataActivity = direction;
            updateInOutIcon();
        }
    };
    
    private void setSignalIconVisibility(){
        
        //If CM Signal text is enabled hide signal icons      
        mShowCmSignal = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_CM_SIGNAL_TEXT, 0) != 0;
        
        if(!mShowCmSignal)
            mSignalCluster.setVisibility(View.VISIBLE);
        else
            mSignalCluster.setVisibility(View.GONE); 
        
    }
    
    private class CMSignalIconObserver extends ContentObserver {
        CMSignalIconObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();

            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUS_BAR_CM_SIGNAL_TEXT), false, this);
            
            onChange(true);
        }

        @Override
        public void onChange(boolean selfChange) {
            setSignalIconVisibility();
        }
    }
}
