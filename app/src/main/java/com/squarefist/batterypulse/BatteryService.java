package com.squarefist.batterypulse;

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
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

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
    private SharedPreferences settings;
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    private float mAccelLast;
    private float mAccelCurrent;
    private boolean isVibrating = false;
    private int checkBatteryInterval;
    private int accelSensitvity;

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

            Toast.makeText(context, "Battery Levels Checked", Toast.LENGTH_SHORT).show();
            currentBatteryLevel = ((float)level / (float)scale) * 100.0f;
            Log.d(msg, "Battery is :" + currentBatteryLevel);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        currentBatteryLevel = getBatteryLevel();
        checkBatteryInterval = settings.getInt("battery_check_interval", 300);
        accelSensitvity = settings.getInt("accel_sensitivity", 11);
        sensorMan = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        sensorMan.registerListener(this, accelerometer, SensorManager.SENSOR_STATUS_ACCURACY_HIGH);

        batteryHandler = new Handler();
        batteryHandler.postDelayed(checkBatteryStatusRunnable, checkBatteryInterval);
        registerReceiver(batInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        Toast.makeText(this, "Service Started", Toast.LENGTH_SHORT).show();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        sensorMan.unregisterListener(this, accelerometer);
        unregisterReceiver(batInfoReceiver);
        batteryHandler.removeCallbacks(checkBatteryStatusRunnable);
        super.onDestroy();
        Toast.makeText(this, "Service Destroyed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] mGravity;
        float mProximity;
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
                mAccel = mAccel * 0.9f + delta;

                intentMessage(BatteryReceiver.SENSOR_RESP, SENSOR_OUT_MSG, Float.toString(zAbs));
                if (zAbs > accelSensitvity && !isVibrating) {
                    Log.d(msg, "DOING THE THING!");
                    isVibrating = true;
                    batteryHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            vibe.vibrate(10 * (long)currentBatteryLevel);
                            Log.d(msg, "Sleeping");
                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Log.d(msg, "Done sleeping");
                            isVibrating = false;
                        }
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void setBatteryInterval(int batteryInterval) {
        checkBatteryInterval = batteryInterval * 60;

        Toast.makeText(this, "Checking battery every "
                + batteryInterval + " minutes", Toast.LENGTH_SHORT).show();
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

    public float getBatteryLevel() {
        return currentBatteryLevel;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        //Do nothing
    }
}
