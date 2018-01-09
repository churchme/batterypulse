package com.squarefist.batterypulse;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
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

    private static VibeTask vibe_task;
    private static AsyncTask.Status vibe_status;

    private SensorManager sensorMan;
    private Sensor accelerometer;
    private static Vibrator vibe;
    private float currentBatteryLevel;
    private Handler batteryHandler;
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    private float mAccelCurrent;
    private int checkBatteryInterval;
    private int accelSensitvity;
    private int pattern;
    private static int onTheMove;
    private static int maxMove;
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        vibe_task = new VibeTask();
        vibe_status = vibe_task.getStatus();
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BatteryPulse");
        mWakeLock.acquire();

        accelSensitvity = BatteryActivity.scaleSensitivity(
                getResources().getInteger(R.integer.SENSITIVITY_INTERVAL)
                        - settings.getInt("accel_sensitivity", R.integer.DEFAULT_SENSITIVITY_PROGRESS));
        pattern = settings.getInt("buzz_style", 0);


        maxMove = (settings.getInt("screen_off_delay",
                getResources().getInteger(R.integer.DEFAULT_SCREEN_OFF_PROGRESS)) + 1)
                * 60 * (1000000 / getResources().getInteger(R.integer.SAMPLE_US));
        onTheMove = maxMove;

        sensorMan = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorMan.registerListener(this, accelerometer, getResources().getInteger(R.integer.SAMPLE_US));

        vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);


        Intent notificationIntent = new Intent(this, BatteryActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_info_black_24dp)
                .setContentTitle("Battery Pulse")
                .setContentText("Catching a lift...")
                .setContentIntent(pendingIntent).build();

        startForeground(1337, notification);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        sensorMan.unregisterListener(this, accelerometer);
        mWakeLock.release();
        if (vibe_task != null)
            vibe_task.cancel(true);
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] mGravity;
        float mAccel = 0;

        mGravity = event.values.clone();
        // Shake detection
        float x = mGravity[0];
        float y = mGravity[1];
        float z = mGravity[2];

        float zAbs = Math.abs(mGravity[2]);

        float mAccelLast = mAccelCurrent;
        mAccelCurrent = (float)Math.sqrt(x * x + y * y + z * z);
        float delta = mAccelCurrent - mAccelLast;
        mAccel = Math.abs(mAccel * 0.9f + delta);

        if (mAccel > 0.5) {
            onTheMove = Math.min(++onTheMove, maxMove);
        } else {
            onTheMove = Math.max(--onTheMove, 0);
        }
        Log.d(msg, "onTheMove: " + onTheMove);

        if (onTheMove < 10) {
            vibe_status = vibe_task.getStatus();
            switch (vibe_status) {
                case FINISHED:
                    vibe_task = new VibeTask();
                    Log.d(msg, "Recreating task");
                    break;
                case PENDING:
                    if (zAbs > accelSensitvity) {
                        try {
                            Log.d(msg, "DOING THE THING!");
                            vibe_task.execute(pattern, (int)Math.ceil(getBatteryLevel()));
                        } catch (Exception IllegalStateException) {
                            Log.d(msg, IllegalStateException.getMessage());
                        }
                    }
                    break;
                case RUNNING:
                    Log.d(msg, "Vibrator busy");
                    break;
            }
        }
    }

    public float getBatteryLevel() {
        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        // Error checking that probably isn't needed but I added just in case.
        if(level == -1 || scale == -1) {
            return 50.0f;
        }

        return ((float)level / (float)scale) * 100.0f;
    }

    private static class VibeTask extends AsyncTask<Integer, Object, Void> {
        protected Void doInBackground(Integer... inputs) {
            double currentBatteryLevel = inputs[1];
            if (inputs[0].equals(1)) {
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
