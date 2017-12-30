package com.squarefist.batterypulse;

/**
 * Created by mchurch on 3/1/17.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.app.Activity;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
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

    public static final int MAX_SENSE = 18;
    public static final int MIN_SENSE = 10;

    private LinearLayout layout;
    private TextView txt_delay;
    private TextView txt_sensitivity;
    private ToggleButton toggle_service;
    private Button button_test;
    private RadioGroup radio_style;
    private SeekBar seekbar_delay;
    private SeekBar seekbar_sensitivity;
    private TestReceiver bReceiver;
    private GraphView graph_lift;

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
        BroadcastReceiver sReceiver = new ScreenReceiver();
        registerReceiver(sReceiver, filter);

        layout = findViewById(R.id.linearLayout);
        txt_delay = findViewById(R.id.textDelay);
        txt_sensitivity = findViewById(R.id.textSensitivity);
        seekbar_delay = findViewById(R.id.seekbarDelay);
        seekbar_sensitivity = findViewById(R.id.seekbarSensitivity);
        toggle_service = findViewById(R.id.toggleService);
        radio_style = findViewById(R.id.radioGroupStyle);
        button_test = findViewById(R.id.buttonTestLift);

        seekbar_delay.setProgress((settings.getInt("screen_off_delay", 9)));
        seekbar_sensitivity.setProgress(settings.getInt("accel_sensitivity", 10));
        txt_delay.setText(String.valueOf(seekbar_delay.getProgress()));
        txt_sensitivity.setText(String.valueOf(seekbar_sensitivity.getProgress()));
        toggle_service.setChecked(settings.getBoolean("is_enabled", false));
        ((RadioButton)radio_style.getChildAt(settings.getInt("buzz_style", 0))).setChecked(true);

        graph_lift = new GraphView(this, seekbar_sensitivity.getProgress());
        graph_lift.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        layout.addView(graph_lift);

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
                txt_sensitivity.setText(String.valueOf(scaleSensitivity(++progress)));
                graph_lift.updateSensPath(scaleSensitivity(progress));
            }
        });

        radio_style.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                saveSettings();
            }
        });
    }

    private int scaleSensitivity(int input) {
        int scaled = (((MAX_SENSE - MIN_SENSE) * (input)) / (20)) + MIN_SENSE;
        return scaled;
    }

    @Override
    public void onStop() {
        super.onStop();
        saveSettings();
    }

    private void saveSettings() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putInt("screen_off_delay", seekbar_delay.getProgress());
        Log.d(msg, "Screen off Delay: " + seekbar_delay.getProgress());
        editor.putInt("accel_sensitivity", scaleSensitivity(seekbar_sensitivity.getProgress() + 1));
        Log.d(msg, "Accel Sensitivity: " + scaleSensitivity(seekbar_sensitivity.getProgress() + 1));
        editor.putBoolean("is_enabled", toggle_service.isChecked());
        Log.d(msg, "Is enabled: " + toggle_service.isChecked());
        editor.putInt("buzz_style", radio_style.indexOfChild(findViewById(radio_style.getCheckedRadioButtonId())));
        Log.d(msg, "Style: " + radio_style.indexOfChild(findViewById(radio_style.getCheckedRadioButtonId())));

        // Apply the edits!
        editor.apply();
    }

    public void enableService(View view) {
        saveSettings();
    }

    public void resetSettings(View view) {
        seekbar_delay.setProgress(9);
        seekbar_sensitivity.setProgress(9);
        toggle_service.setChecked(false);
        ((RadioButton)radio_style.getChildAt(0)).setChecked(true);

        saveSettings();
    }

    public void testSettings(View view) {
        graph_lift.resetPaths();

        /* For getting the accel readings to the text view */
        IntentFilter filter1 = new IntentFilter(TestReceiver.SENSOR_RESP);
        filter1.addCategory(Intent.CATEGORY_DEFAULT);
        bReceiver = new TestReceiver();
        registerReceiver(bReceiver, filter1);

        bReceiver.setGraphView(graph_lift);
        startService(new Intent(context, BatteryService.class));
    }
}

