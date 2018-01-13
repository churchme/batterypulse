package com.squarefist.batterypulse;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;

public class BatteryService extends Service implements SensorEventListener {
    String msg = "Battery Service : ";
    public static final String PREFS_NAME = "BatteryPrefs";

    private static VibeTask vibe_task;
    private static AsyncTask.Status vibe_status;

    private BatteryReceiver sReceiver;
    private BatteryService mService;
    private SensorManager sensorMan;
    private Sensor accelerometer;
    private static Vibrator vibe;
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    private int MAX_SENSITIVITY;
    private int MIN_SENSITIVITY;
    private int SENSITIVITY_INTERVAL;
    private float mAccelCurrent;
    private int accelSensitvity;
    private int pattern;
    private static int onTheMove;
    private static int maxMove;
    private static PowerManager.WakeLock mWakeLock;

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
        mService = this;
        int goCode = 0;
        goCode = intent.getIntExtra("GoCode", goCode);
        if (goCode == getResources().getInteger(R.integer.STOP_INTENT)) {
            tearDown();
            stopSelf();
            return START_REDELIVER_INTENT;
        } else {
            return setUp();
        }
    }

    private int setUp() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        Resources res = getResources();
        MAX_SENSITIVITY = res.getInteger(R.integer.MAX_SENSITIVITY);
        MIN_SENSITIVITY = res.getInteger(R.integer.MIN_SENSITIVITY);
        SENSITIVITY_INTERVAL = res.getInteger(R.integer.SENSITIVITY_INTERVAL);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BatteryPulse");
        mWakeLock.acquire();

        sensorMan = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // REGISTER RECEIVER THAT HANDLES SCREEN ON AND SCREEN OFF LOGIC
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        sReceiver = new BatteryReceiver();
        registerReceiver(sReceiver, filter);

        accelSensitvity = scaleSensitivity(
                res.getInteger(R.integer.SENSITIVITY_INTERVAL)
                        - settings.getInt("accel_sensitivity", R.integer.DEFAULT_SENSITIVITY_PROGRESS));
        pattern = settings.getInt("buzz_style", 0);


        maxMove = (settings.getInt("screen_off_delay",
                res.getInteger(R.integer.DEFAULT_SCREEN_OFF_PROGRESS)) + 1)
                * 60 * (1000000 / res.getInteger(R.integer.SAMPLE_US));
        //maxMove = 20;
        onTheMove = maxMove;

        vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        startForeground(1337, getNotification());

        return START_STICKY;
    }

    private void tearDown() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("is_enabled", false);
        editor.apply();
        if (mWakeLock != null && mWakeLock.isHeld())
            mWakeLock.release();
        try {
            unregisterReceiver(sReceiver);
        } catch (Exception RuntimeException) {
            Log.d(msg, "Shit");
        }
        if (vibe_task != null)
            vibe_task.cancel(true);
    }

    private Notification getNotification() {
        Intent notificationIntent = new Intent(getApplication(), BatteryActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Intent i = new Intent(mService, BatteryService.class);
        i.putExtra("GoCode", getResources().getInteger(R.integer.STOP_INTENT));
        PendingIntent resultPendingIntent = PendingIntent.getService(mService, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        return new Notification.Builder(mService)
                .setSmallIcon(R.mipmap.bp_launcher)
                .setContentTitle("Battery Pulse")
                .setContentIntent(pendingIntent)
                .addAction(0, "Disable", resultPendingIntent)
                .build();
    }

    private int scaleSensitivity(int input) {
        Log.d(msg, "int: " + SENSITIVITY_INTERVAL + " MAX: " + MAX_SENSITIVITY);
        Log.d(msg, "min: " + MIN_SENSITIVITY + " input: " + input);
        return (((MAX_SENSITIVITY - MIN_SENSITIVITY) * (input)) / (SENSITIVITY_INTERVAL)) + MIN_SENSITIVITY;
    }

    @Override
    public void onDestroy() {
        tearDown();
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

        if (onTheMove < 20) {
            vibe_status = vibe_task.getStatus();
            switch (vibe_status) {
                case FINISHED:
                    vibe_task = new VibeTask();
                    Log.d(msg, "Recreating task");
                    break;
                case PENDING:
                    if (zAbs > accelSensitvity) {
                        try {
                            Log.d(msg, "DOING THE THING! Acc: " + accelSensitvity + " aAbs: " + zAbs);
                            vibe_task.execute(pattern, (int)Math.ceil(getBatteryLevel()));
                            onTheMove = maxMove;
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

    public class BatteryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                Log.d(msg, "Start listening for lift...");
                onTheMove = maxMove;
                vibe_task = new VibeTask();
                sensorMan.registerListener(mService, accelerometer, getResources().getInteger(R.integer.SAMPLE_US));
            } else {
                Log.d(msg, "Stop listening for lift...");
                sensorMan.unregisterListener(mService, accelerometer);
                if (vibe_task != null)
                    vibe_task.cancel(true);
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
