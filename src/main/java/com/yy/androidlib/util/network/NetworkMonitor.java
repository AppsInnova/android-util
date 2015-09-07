package com.yy.androidlib.util.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkMonitor {

    private static BroadcastReceiver networkStateReceiver;
    private static Map<NetworkChanged, Object> callbacks = new ConcurrentHashMap<NetworkChanged, Object>();
    private static boolean connected;

    public interface NetworkChanged {

        void onConnect();

        void onDisconnect();
    }

    public synchronized static void addMonitor(Context context, NetworkChanged callback) {
        callbacks.put(callback, true);

        if (networkStateReceiver == null) {
            context = context.getApplicationContext();
            connected = isConnected(context);
            networkStateReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    doBroadcast(context);
                }
            };
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            context.registerReceiver(networkStateReceiver, filter);
        }

        notifyConnect(callback);
    }

    public synchronized static void removeMonitor(NetworkChanged callback) {
        if (callbacks != null && !callbacks.isEmpty()) {
            callbacks.remove(callback);
        }
    }


    private synchronized static void notifyConnect(NetworkChanged callback) {
        if (connected) {
            callback.onConnect();
        } else {
            callback.onDisconnect();
        }
    }

    private synchronized static void doBroadcast(Context context) {
        boolean newConnected = isConnected(context);
        if (newConnected != connected) {
            connected = newConnected;
            for (NetworkChanged callback : callbacks.keySet()) {
                notifyConnect(callback);
            }
        }

    }

    public static boolean isConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}
