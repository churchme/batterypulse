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
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
    private ToggleButton toggle_service;
    private RadioGroup radio_style;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        SeekBar seekbar_battery;
        SeekBar seekbar_delay;
        SeekBar seekbar_sensitivity;

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

        txt_interval = (TextView)findViewById(R.id.textInterval);
        txt_delay = (TextView)findViewById(R.id.textDelay);
        txt_sensitivity = (TextView)findViewById(R.id.textSensitivity);
        seekbar_delay = (SeekBar) findViewById(R.id.seekbarDelay);
        seekbar_battery = (SeekBar) findViewById(R.id.seekbarInterval);
        seekbar_sensitivity = (SeekBar) findViewById(R.id.seekbarSensitivity);
        toggle_service = (ToggleButton) findViewById(R.id.toggleService);
        radio_style = (RadioGroup) findViewById(R.id.radioGroupStyle);

        //TODO Add reset button
        seekbar_delay.setProgress((settings.getInt("screen_off_delay", 9)));
        seekbar_battery.setProgress((settings.getInt("battery_check_interval", 3)));
        seekbar_sensitivity.setProgress(settings.getInt("accel_sensitivity", 10));
        txt_delay.setText(String.valueOf(settings.getInt("screen_off_delay", 10)));
        txt_interval.setText(String.valueOf(settings.getInt("battery_check_interval", 4)));
        txt_sensitivity.setText(String.valueOf(settings.getInt("accel_sensitivity", 11)));
        toggle_service.setChecked(settings.getBoolean("is_enabled", false));
        ((RadioButton)radio_style.getChildAt(settings.getInt("buzz_style", 0))).setChecked(true);

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

        radio_style.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                saveSettings();
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

        editor.putInt("screen_off_delay", Integer.parseInt(txt_delay.getText().toString()));
        Log.d(msg, "Screen off Delay: " + Integer.parseInt(txt_delay.getText().toString()));
        editor.putInt("accel_sensitivity", Integer.parseInt(txt_sensitivity.getText().toString()));
        Log.d(msg, "Accel Sensitivity: " + Integer.parseInt(txt_sensitivity.getText().toString()));
        editor.putInt("battery_check_interval", Integer.parseInt(txt_interval.getText().toString()));
        Log.d(msg, "Battery Check interval: " + Integer.parseInt(txt_interval.getText().toString()));
        editor.putBoolean("is_enabled", toggle_service.isChecked());
        Log.d(msg, "Is enabled: " + toggle_service.isChecked());
        editor.putInt("buzz_style", radio_style.indexOfChild(findViewById(radio_style.getCheckedRadioButtonId())));
        Log.d(msg, "Style: " + radio_style.indexOfChild(findViewById(radio_style.getCheckedRadioButtonId())));

        // Apply the edits!
        editor.apply();
    }

    public void toggleService(View view) {
        saveSettings();
    }

    public void setToggleButton(boolean bool) {
        toggle_service.setChecked(bool);
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
