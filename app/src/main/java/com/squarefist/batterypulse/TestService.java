package com.squarefist.batterypulse;

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
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;

public class TestService extends Service implements SensorEventListener {
    public static String SENSOR_OUT_MSG = "sens_msg";
    String msg = "Test Service : ";
    public static final String PREFS_NAME = "BatteryPrefs";

    private static VibeTask vibe_task;
    private static AsyncTask.Status status;

    private SensorManager sensorMan;
    private Sensor accelerometer;
    private static Vibrator vibe;
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    private float mAccelCurrent;
    private int accelSensitvity;
    private int pattern;

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    class LocalBinder extends Binder {
        TestService getService() {
            // Return this instance of LocalService so clients can call public methods
            return TestService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        vibe_task = new VibeTask();
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        accelSensitvity = BatteryActivity.scaleSensitivity(
                getResources().getInteger(R.integer.SENSITIVITY_INTERVAL)
                        - settings.getInt("accel_sensitivity", R.integer.DEFAULT_SENSITIVITY_PROGRESS));
        pattern = settings.getInt("buzz_style", 0);

        sensorMan = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorMan.registerListener(this, accelerometer, getResources().getInteger(R.integer.SAMPLE_US));

        vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        sensorMan.unregisterListener(this, accelerometer);
        if (!vibe_task.getStatus().equals(AsyncTask.Status.FINISHED))
            vibe_task.cancel(true);
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] mGravity;
        //float mAccel = 0;

        mGravity = event.values.clone();
        // Shake detection
        float x = mGravity[0];
        float y = mGravity[1];
        float z = mGravity[2];

        float zAbs = Math.abs(mGravity[2]);

        float mAccelLast = mAccelCurrent;
        mAccelCurrent = (float)Math.sqrt(x * x + y * y + z * z);
        //float delta = mAccelCurrent - mAccelLast;
        //mAccel = Math.abs(mAccel * 0.9f + delta);

        intentMessage(TestReceiver.SENSOR_RESP, SENSOR_OUT_MSG, zAbs);
        status = vibe_task.getStatus();
        switch (status) {
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
                        Log.d(msg, "Somehow too fast");
                    }
                }
                break;
            case RUNNING:
                Log.d(msg, "Vibrator busy");
                break;
        }
    }

    public float getBatteryLevel() {
        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        assert batteryIntent != null;
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        // Error checking that probably isn't needed but I added just in case.
        if(level == -1 || scale == -1) {
            return 50.0f;
        }

        return ((float)level / (float)scale) * 100.0f;
    }

    private void intentMessage(String receiverAction, String messageType, Object thing) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(receiverAction);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(messageType, String.valueOf(thing));
        sendBroadcast(broadcastIntent);
    }

    private static class VibeTask extends AsyncTask<Integer, Object, Void> {
        //input = [pattern style, battery level]
        protected Void doInBackground(Integer... input) {
            double currentBatteryLevel = input[1];
            if (input[0].equals(1)) {
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
