package com.squarefist.batterypulse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class ScreenReceiver extends BroadcastReceiver {

    String msg = "Screen Reciever : ";
    public static final String PREFS_NAME = "BatteryPrefs";

    private boolean screenOn;

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            screenOn = false;
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            screenOn = true;
        }
        Log.d(msg, "screenOn is :" + screenOn);

        if (!screenOn && settings.getBoolean("is_enabled", false)) {
            context.startService(new Intent(context, BatteryService.class));
        } else {
            context.stopService(new Intent(context, BatteryService.class));
        }
    }

}
