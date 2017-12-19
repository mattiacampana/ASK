/*
 * Copyright (c) 2017. Mattia Campana, m.campana@iit.cnr.it, campana.mattia@gmail.com
 *
 * This file is part of Android Sensing Kit (ASK).
 *
 * Android Sensing Kit (ASK) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Android Sensing Kit (ASK) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Android Sensing Kit (ASK).  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package it.matbell.ask.probes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import it.matbell.ask.commons.Utils;

/**
 * This probe performs a continuous Wi-Fi scan to discover the Wi-Fi Access Points in proximity.
 * For each Ap, this probe reports the following information:
 *
 *      - SSID	: The AP's SSID
 *      - BSSID	: The AP's BSSID
 *      - RSSI	: The RSSI level in the [0,4] range
 *      - SIG_LEVEL	: The raw RSSI level
 *      - CAPABILITIES	: Authentication, key management, and encryption schemes supported by the AP
 *      - FREQUENCY	The primary 20 MHz frequency (in MHz) of the channel over which the client is
 *      communicating with the access point
 *      - CONNECTED	: TRUE/FALSE if the device is currently connected to the Access Point
 *
 * Required permissions:
 *
 *      - "android.permission.ACCESS_WIFI_STATE"
 *      - "android.permission.CHANGE_WIFI_STATE"
 *      - "android.permission.ACCESS_COARSE_LOCATION"
 *      - "android.permission.ACCESS_NETWORK_STATE"
 *
 */
@SuppressWarnings("unused")
class SKWiFiProbe extends SKContinuousProbe {

    private static final String LOCK_KEY = SKWiFiProbe.class.getName();
    private static final int MAX_RSSI_LEVELS = 4;

    private WifiManager wifiManager;
    private ConnectivityManager connManager;
    private WifiManager.WifiLock wifiLock;
    private String connectedBSSID;

    private BroadcastReceiver scanResultsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {

                List<ScanResult> results = wifiManager.getScanResults();

                if (results != null) {

                    List<String> data = new ArrayList<>();

                    for (ScanResult result : results) {

                        boolean connected = result.BSSID.equals(connectedBSSID);

                        data.add(StringUtils.join(Arrays.asList(result.SSID, result.BSSID,
                                WifiManager.calculateSignalLevel(result.level, MAX_RSSI_LEVELS),
                                result.level, result.capabilities,
                                result.frequency, String.valueOf(connected)), ","));

                        Log.d(Utils.TAG, ""+Arrays.asList(result.SSID, result.BSSID,
                                WifiManager.calculateSignalLevel(result.level, MAX_RSSI_LEVELS),
                                result.level, result.capabilities,
                                result.frequency));
                    }

                    if(data.size() != 0) logOnFile(true, data);
                }

                stop();
            }
        }
    };

    @Override
    public void init() {
        wifiManager = (WifiManager) getContext().getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

        connManager = (ConnectivityManager) getContext().getSystemService(
                Context.CONNECTIVITY_SERVICE);
    }

    @Override
    public void onFirstRun() {}

    @Override
    void onStop() {
        getContext().unregisterReceiver(scanResultsReceiver);
        connectedBSSID = null;
        wifiLock = Utils.releaseWifiLock(wifiLock);
    }

    @Override
    public void exec() {
        if(wifiManager != null) {

            if(!wifiManager.isWifiEnabled()) wifiManager.setWifiEnabled(true);

            wifiLock = Utils.acquireWifiLock(wifiManager, wifiLock, LOCK_KEY);

            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            if(mWifi.isConnectedOrConnecting()){
                WifiInfo wInfo = wifiManager.getConnectionInfo();
                connectedBSSID = wInfo.getBSSID();
            }

            getContext().registerReceiver(scanResultsReceiver,
                    new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

            wifiManager.startScan();
        }else{
            Log.e(Utils.TAG, "WiFiManager is null in "+this.getClass().getName());
        }
    }
}
