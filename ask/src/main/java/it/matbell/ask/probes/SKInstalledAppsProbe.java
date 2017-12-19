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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import it.matbell.ask.commons.Utils;

/**
 * This probe monitors the installed applications. For each application, it reports the package
 * name.
 *
 */
@SuppressWarnings("unused")
class SKInstalledAppsProbe extends SKContinuousProbe {

    private BroadcastReceiver appReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent.getAction() != null &&
                    (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED) ||
                            intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED))){
                Log.d(Utils.TAG, "New app installation / uninstall");
                fetchPackages();
            }
        }
    };

    @Override
    public void init() {

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme("package");

        getContext().registerReceiver(appReceiver, intentFilter);
    }

    @Override
    public void onFirstRun() {

    }

    @Override
    void onStop() {

        getContext().unregisterReceiver(appReceiver);
    }

    @Override
    public void exec() {
        fetchPackages();
    }

    private void fetchPackages(){

        final PackageManager pm = getContext().getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        List<String> packagesNames = new ArrayList<>();
        for (ApplicationInfo packageInfo : packages) packagesNames.add(packageInfo.packageName);

        logOnFile(true, packagesNames);
    }
}
