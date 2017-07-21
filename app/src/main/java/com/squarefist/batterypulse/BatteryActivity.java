package com.squarefist.batterypulse;

/**
 * Created by mchurch on 3/1/17.
 */

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.app.Activity;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

public class BatteryActivity extends Activity {
    BatteryService mService;
    boolean mBound = false;
    Context context;
    String msg = "Android : ";
    public static final String PREFS_NAME = "BatteryPrefs";

    private TextView txt_interval;
    private TextView txt_delay;
    private TextView txt_sensitivity;
    private SeekBar seekbar_battery;
    private SeekBar seekbar_delay;
    private SeekBar seekbar_sensitivity;
    private ToggleButton toggle_service;

    BatteryReceiver mReceiver;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(msg, "The onCreate() event");
        setContentView(R.layout.main);
        context = getApplicationContext();
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        // REGISTER RECEIVER THAT HANDLES SCREEN ON AND SCREEN OFF LOGIC
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        BroadcastReceiver mReceiver = new ScreenReceiver();
        registerReceiver(mReceiver, filter);

        TextView txt_sensor = (TextView) findViewById(R.id.textAccelerometer);
        txt_interval = (TextView)findViewById(R.id.textInterval);
        txt_delay = (TextView)findViewById(R.id.textDelay);
        txt_sensitivity = (TextView)findViewById(R.id.textSensitivity);
        seekbar_delay = (SeekBar) findViewById(R.id.seekbarDelay);
        seekbar_battery = (SeekBar) findViewById(R.id.seekbarInterval);
        seekbar_sensitivity = (SeekBar) findViewById(R.id.seekbarSensitivity);
        seekbar_delay.setProgress(settings.getInt("screen_off_delay", 10));
        seekbar_battery.setProgress(settings.getInt("battery_check_interval", 4));
        seekbar_sensitivity.setProgress(settings.getInt("accel_sensitivity", 11));
        toggle_service = (ToggleButton) findViewById(R.id.toggleService);

        seekbar_battery.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveSettings();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txt_interval.setText(String.valueOf(++progress));
            }
        });

        seekbar_delay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveSettings();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txt_delay.setText(String.valueOf(++progress));
            }
        });

        seekbar_sensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveSettings();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txt_sensitivity.setText(String.valueOf(++progress));
            }
        });


        /* For getting the accel readings to the text view */
        //IntentFilter filter1 = new IntentFilter(BatteryReceiver.SENSOR_RESP);
        //filter1.addCategory(Intent.CATEGORY_DEFAULT);

        //mReceiver = new BatteryReceiver();
        //registerReceiver(mReceiver, filter1);
        //mReceiver.setTextView(txt_sensor);

    }

    @Override
    public void onStop() {
        super.onStop();
        saveSettings();
    }

    private void saveSettings() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("battery_check_interval", seekbar_battery.getProgress());
        editor.putInt("screen_off_delay", seekbar_delay.getProgress());
        editor.putInt("accel_sensitivity", seekbar_sensitivity.getProgress());
        editor.putBoolean("is_enabled", toggle_service.isChecked());

        // Commit the edits!
        editor.commit();
        Log.d(msg, "Saving settings");
    }

    public void toggleService(View view) {
        saveSettings();
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to BatteryService, cast the IBinder and get BatteryService instance
            BatteryService.LocalBinder binder = (BatteryService.LocalBinder) service;
            mService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };
}
