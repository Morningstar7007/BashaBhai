package com.example.bashabhai;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.widget.Toast;

public class NetworkChangeReceiver extends BroadcastReceiver {

    // Track the previous state so we only toast when RESTORING connection
    private static boolean wasOffline = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            boolean isConnected = NetworkUtils.isNetworkAvailable(context);
            
            if (isConnected) {
                if (wasOffline) {
                    Toast.makeText(context, "Internet connection restored", Toast.LENGTH_SHORT).show();
                    wasOffline = false;
                }
            } else {
                if (!wasOffline) {
                    // Only show this once when it drops
                    Toast.makeText(context, "No Internet Connection. You can still view offline favorites.", Toast.LENGTH_LONG).show();
                    wasOffline = true;
                }
            }
        }
    }
}
