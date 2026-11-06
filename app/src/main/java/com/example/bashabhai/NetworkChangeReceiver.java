package com.example.bashabhai;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

public class NetworkChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            
            if (!isConnected) {
                Toast.makeText(context, "No Internet Connection. You can still view offline favorites.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
