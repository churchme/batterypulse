package com.squarefist.batterypulse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class ScreenReceiver extends BroadcastReceiver {

    String msg = "Screen Reciever : ";
    public static final String PREFS_NAME = "BatteryPrefs";

    private boolean screenOn;
    private Timer mTimer = new Timer();
    private TimerTask mTimerTask;

    @Override
    public void onReceive(final Context context, Intent intent) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            screenOn = false;
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            screenOn = true;
        }
        Log.d(msg, "screenOn is :" + screenOn);

        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                context.startService(new Intent(context, BatteryService.class));
                Log.d(msg, "Ready to go");
            };
        };

        if (!screenOn && settings.getBoolean("is_enabled", false)) {
            Log.d(msg, "Waiting to go");
            mTimer.schedule(mTimerTask, 60000 * settings.getInt("screen_off_delay", 10000));
        } else {
            Log.d(msg, "Cancelled");
            mTimerTask.cancel();
            mTimer.purge();
            context.stopService(new Intent(context, BatteryService.class));
        }
    }
}
