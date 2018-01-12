package com.squarefist.batterypulse;

/**
 * Created by mchurch on 3/1/17.
 */

import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
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

public class BatteryActivity extends Activity implements TestDialogFragment.NoticeDialogListener {
    Context context;
    String msg = "Android : ";
    public static final String PREFS_NAME = "BatteryPrefs";
    public static boolean canRegister = true;

    private static int MAX_SENSITIVITY;
    private static int MIN_SENSITIVITY;
    private static int SENSITIVITY_INTERVAL;

    private TextView txt_delay;
    private TextView txt_sensitivity;
    private ToggleButton toggle_service;
    private RadioGroup radio_style;
    private SeekBar seekbar_delay;
    private SeekBar seekbar_sensitivity;
    private GraphView graph_lift;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.d(msg, "The onCreate() event");
        setContentView(R.layout.main);
        context = getApplicationContext();
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        Resources res = getResources();
        MAX_SENSITIVITY = res.getInteger(R.integer.MAX_SENSITIVITY);
        MIN_SENSITIVITY = res.getInteger(R.integer.MIN_SENSITIVITY);
        SENSITIVITY_INTERVAL = res.getInteger(R.integer.SENSITIVITY_INTERVAL);

        LinearLayout layout = findViewById(R.id.linearLayout);
        txt_delay = findViewById(R.id.textDelay);
        txt_sensitivity = findViewById(R.id.textSensitivity);
        seekbar_delay = findViewById(R.id.seekbarDelay);
        seekbar_sensitivity = findViewById(R.id.seekbarSensitivity);
        toggle_service = findViewById(R.id.toggleService);
        radio_style = findViewById(R.id.radioGroupStyle);

        seekbar_delay.setProgress((settings.getInt("screen_off_delay",
                res.getInteger(R.integer.DEFAULT_SCREEN_OFF_PROGRESS))));
        seekbar_sensitivity.setProgress(settings.getInt("accel_sensitivity",
                res.getInteger(R.integer.DEFAULT_SENSITIVITY_PROGRESS)));
        txt_delay.setText(String.valueOf(seekbar_delay.getProgress() + 1));
        txt_sensitivity.setText(String.valueOf(seekbar_sensitivity.getProgress() + 1));
        toggle_service.setChecked(settings.getBoolean("is_enabled", false));
        ((RadioButton) radio_style.getChildAt(settings.getInt("buzz_style", 0))).setChecked(true);

        graph_lift = new GraphView(this, seekbar_sensitivity.getProgress());
        graph_lift.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        layout.addView(graph_lift);

        seekbar_delay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                toggle_service.setChecked(true);
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
                toggle_service.setChecked(true);
                saveSettings();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                graph_lift.updateSensPath(scaleSensitivity(SENSITIVITY_INTERVAL - progress));
                txt_sensitivity.setText(String.valueOf((++progress)));
            }
        });

        radio_style.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                toggle_service.setChecked(true);
                saveSettings();
            }
        });
    }

    public static int scaleSensitivity(int input) {
        Log.d("Android---", "int: " + SENSITIVITY_INTERVAL + " MAX: " + MAX_SENSITIVITY);
        Log.d("Android---", "min: " + MIN_SENSITIVITY + " input: " + input);
        return (((MAX_SENSITIVITY - MIN_SENSITIVITY) * (input)) / (SENSITIVITY_INTERVAL)) + MIN_SENSITIVITY;
    }

    @Override
    public void onStop() {
        super.onStop();
        saveSettings();
    }

    private void saveSettings() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putInt("screen_off_delay", (seekbar_delay.getProgress()));
        Log.d(msg, "Screen off Delay: " + (seekbar_delay.getProgress() + 1));
        editor.putInt("accel_sensitivity", seekbar_sensitivity.getProgress());
        Log.d(msg, "Accel Sensitivity: " + scaleSensitivity(SENSITIVITY_INTERVAL - seekbar_sensitivity.getProgress()));
        editor.putBoolean("is_enabled", toggle_service.isChecked());
        Log.d(msg, "Is enabled: " + toggle_service.isChecked());
        editor.putInt("buzz_style", radio_style.indexOfChild(findViewById(radio_style.getCheckedRadioButtonId())));
        Log.d(msg, "Style: " + radio_style.indexOfChild(findViewById(radio_style.getCheckedRadioButtonId())));

        // Apply the edits!
        editor.apply();

        // Start or stop the service
        if (settings.getBoolean("is_enabled", false)) {
            Intent intent = new Intent(this, BatteryService.class);
            intent.putExtra("GoCode", getResources().getInteger(R.integer.STRT_INTENT));
            context.startService(intent);
            //context.stopService(new Intent(this, BatteryService.class));
            //context.startService(new Intent(this, BatteryService.class));
        } else {
            Intent intent = new Intent(this, BatteryService.class);
            intent.putExtra("GoCode", getResources().getInteger(R.integer.STOP_INTENT));
            context.stopService(intent);
        }
    }

    public void enableService(View view) {
        saveSettings();
    }

    public void resetSettings(View view) {
        seekbar_delay.setProgress(getResources().getInteger(R.integer.DEFAULT_SCREEN_OFF_PROGRESS));
        seekbar_sensitivity.setProgress(getResources().getInteger(R.integer.DEFAULT_SENSITIVITY_PROGRESS));
        toggle_service.setChecked(false);
        ((RadioButton) radio_style.getChildAt(0)).setChecked(true);

        saveSettings();
    }

    public void testSettings(View view) {
        TestDialogFragment dialog = new TestDialogFragment();
        dialog.show(getFragmentManager(), "test");
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        if (canRegister) {
            graph_lift.resetPaths();
            graph_lift.updateSensPath(scaleSensitivity(SENSITIVITY_INTERVAL - seekbar_sensitivity.getProgress()));

            /* For getting the accel readings to the graph view */
            IntentFilter filter1 = new IntentFilter(TestReceiver.SENSOR_RESP);
            filter1.addCategory(Intent.CATEGORY_DEFAULT);
            TestReceiver bReceiver = new TestReceiver(getResources().getInteger(R.integer.MAX_SAMPLES));
            registerReceiver(bReceiver, filter1);

            bReceiver.setGraphView(graph_lift);
            startService(new Intent(context, TestService.class));
            canRegister = false;
        }
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        //Do nothing
    }
}