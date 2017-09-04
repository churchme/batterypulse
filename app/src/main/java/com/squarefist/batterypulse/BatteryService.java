package com.squarefist.batterypulse;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

public class BatteryService extends Service implements SensorEventListener {
    public static String STATUS_OUT_MSG = "stat_msg";
    public static String SENSOR_OUT_MSG = "sens_msg";
    public static String OTHER_OUT_MSG = "other_msg";
    String msg = "Battery Service : ";
    public static final String PREFS_NAME = "BatteryPrefs";

    private SensorManager sensorMan;
    private Sensor accelerometer;
    private Vibrator vibe;
    private float currentBatteryLevel;
    private Handler batteryHandler;
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    private float mAccelLast;
    private float mAccelCurrent;
    private int checkBatteryInterval;
    private int accelSensitvity;
    private int pattern;
    private int onTheMove;
    private int maxMove;
    private PowerManager.WakeLock mWakeLock;

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    class LocalBinder extends Binder {
        BatteryService getService() {
            // Return this instance of LocalService so clients can call public methods
            return BatteryService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private BroadcastReceiver batInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent batteryIntent) {
            int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            // Error checking that probably isn't needed but I added just in case.
            if(level == -1 || scale == -1) {
                currentBatteryLevel = 50.0f;
            }

            currentBatteryLevel = ((float)level / (float)scale) * 100.0f;
            Log.d(msg, "Battery is :" + currentBatteryLevel);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Context context = getApplicationContext();
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
        mWakeLock.acquire();

        checkBatteryInterval = (settings.getInt("battery_check_interval", 240000) * 60000);
        accelSensitvity = settings.getInt("accel_sensitivity", 11);
        pattern = settings.getInt("buzz_style", 0);

        // Accelerometer checked very .2 seconds
        maxMove = settings.getInt("screen_off_delay", 10) * 60 * 5;
        onTheMove = maxMove;

        sensorMan = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorMan.registerListener(this, accelerometer, SensorManager.SENSOR_STATUS_ACCURACY_HIGH);

        batteryHandler = new Handler();
        batteryHandler.postDelayed(checkBatteryStatusRunnable, checkBatteryInterval);
        registerReceiver(batInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);


        Intent notificationIntent = new Intent(this, BatteryActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_info_black_24dp)
                .setContentTitle("My Awesome App")
                .setContentText("Doing some work...")
                .setContentIntent(pendingIntent).build();

        startForeground(1337, notification);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        sensorMan.unregisterListener(this, accelerometer);
        unregisterReceiver(batInfoReceiver);
        batteryHandler.removeCallbacks(checkBatteryStatusRunnable);
        mWakeLock.release();
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] mGravity;
        float mAccel = 0;

        try {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                mGravity = event.values.clone();
                // Shake detection
                float x = mGravity[0];
                float y = mGravity[1];
                float z = mGravity[2];

                float zAbs = Math.abs(mGravity[2]);

                mAccelLast = mAccelCurrent;
                mAccelCurrent = (float)Math.sqrt(x * x + y * y + z * z);
                float delta = mAccelCurrent - mAccelLast;
                mAccel = Math.abs(mAccel * 0.9f + delta);

                //intentMessage(BatteryReceiver.SENSOR_RESP, SENSOR_OUT_MSG, Float.toString(zAbs));
                if (zAbs > accelSensitvity && onTheMove <= 10) {
                    Log.d(msg, "DOING THE THING!");
                    new VibeTask().execute(pattern);
                }

                if (mAccel > 0.5) {
                    onTheMove = Math.min(++onTheMove, maxMove);
                } else {
                    onTheMove = Math.max(--onTheMove, 0);
                }
                Log.d(msg, "mAccel: " + mAccel);
                Log.d(msg, "onTheMove: " + onTheMove);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void intentMessage(String receiverAction, String messageType, Object thing) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(receiverAction);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(messageType, String.valueOf(thing));
        sendBroadcast(broadcastIntent);
    }

    private Runnable checkBatteryStatusRunnable = new Runnable() {
        @Override
        public void run() {
            //DO WHATEVER YOU WANT WITH LATEST BATTERY LEVEL STORED IN batteryLevel HERE...

            // schedule next battery check
            batteryHandler.postDelayed(checkBatteryStatusRunnable, checkBatteryInterval);
        }
    };

    private class VibeTask extends AsyncTask<Integer, Object, Void> {
        protected Void doInBackground(Integer... patternID) {
            if (patternID[0].equals(1)) {
                int n = 0, pos = 0;
                long[] pattern = { 0, 0, 0, 0, 0, 0, 0, 0, 0 };
                do {
                    pattern[++pos] = 50;
                    pattern[++pos] = 200;
                    n += 25;
                } while (n < currentBatteryLevel);
                vibe.vibrate(pattern, -1);
            } else {
                vibe.vibrate(10 * (long) currentBatteryLevel);
            }
            onTheMove = maxMove;
            return null;
        }

        protected void onProgressUpdate(Object... progress) {
            // Not needed
        }

        protected void onPostExecute(Void result) {
            // Not needed
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        //Do nothing
    }
}
