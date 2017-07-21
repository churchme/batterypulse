package com.squarefist.batterypulse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.TextView;

public class BatteryReceiver extends BroadcastReceiver {
    public static final String SENSOR_RESP =
            "com.squarefist.batterybuzzer.action.SENSOR_PROCESSED";
    public static final String ENABLE_BUTTON =
            "com.squarefist.batterybuzzer.action.ENABLE_BUTTON";
    private TextView txt_sensor;

    public void setTextView(TextView view) {
        txt_sensor = view;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.hasExtra(BatteryService.SENSOR_OUT_MSG)) {
            String text = intent.getStringExtra(BatteryService.SENSOR_OUT_MSG);
            txt_sensor.setText(text);
        }
    }
}
