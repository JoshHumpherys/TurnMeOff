package com.android.cubemaster.turnmeoff;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final boolean MOBILE_DATA_TOGGLABLE =
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT;
    private static final String SHARED_PREFERENCES_NAME = "SHARED_PREFERENCES_NAME";
    private static final String SHARED_PREFERENCES_IS_CONNECTIVITY_DISABLED_NAME =
            "SHARED_PREFERENCES_IS_CONNECTIVITY_DISABLED_NAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void sendMessage(View view) {
        disableConnection(this);
    }

    private static void disableConnection(Context context) {
        String message = "Wi-Fi has been disabled";
        disableWifi(context);
        if(MOBILE_DATA_TOGGLABLE) {
            disableMobileData(context);
            message = "Wi-Fi and cellular data have been disabled";
        }
//        Only works up to API 16, but we can disable mobile data up to API 20, which is preferable
//        activateAirplaneMode(context);

        Toast.makeText(context, message, Toast.LENGTH_LONG).show();

        SharedPreferences.Editor editor =
                context.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(SHARED_PREFERENCES_IS_CONNECTIVITY_DISABLED_NAME, true);
        editor.commit();
    }

    private static boolean isWifiEnabled(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return wifiManager.isWifiEnabled();
    }

    private static void disableWifi(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(false);
    }

    private static boolean isMobileDataEnabled(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            Class cmClass = Class.forName(cm.getClass().getName());
            Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
            method.setAccessible(true);
            return (boolean) method.invoke(cm);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return true; // defaults to true
    }

    private static void disableMobileData(Context context) {
        final ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            final Class conmanClass = Class.forName(cm.getClass().getName());
            final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
            iConnectivityManagerField.setAccessible(true);
            final Object iConnectivityManager = iConnectivityManagerField.get(cm);
            final Class iConnectivityManagerClass = Class.forName(
                    iConnectivityManager.getClass().getName());
            final Method setMobileDataEnabledMethod = iConnectivityManagerClass
                    .getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
            setMobileDataEnabledMethod.setAccessible(true);

            setMobileDataEnabledMethod.invoke(iConnectivityManager, false);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private static void activateAirplaneMode(Context context) {
        boolean successful = Settings.System.putInt(
                context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 1);
        Log.d(LOG_TAG, String.valueOf(successful));
    }

    public static class NetworkChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences sharedPreferences =
                    context.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
            if(sharedPreferences.getBoolean(
                    SHARED_PREFERENCES_IS_CONNECTIVITY_DISABLED_NAME, false)) {
                if(isWifiEnabled(context) ||
                        (MOBILE_DATA_TOGGLABLE && isMobileDataEnabled(context))) {
                    disableConnection(context);
                }
            }
        }
    }
}