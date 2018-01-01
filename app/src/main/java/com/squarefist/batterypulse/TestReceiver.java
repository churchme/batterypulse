package com.squarefist.batterypulse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;

public class TestReceiver extends BroadcastReceiver {
    String msg = "Test Receiver : ";
    public static final String SENSOR_RESP =
            "com.squarefist.batterybuzzer.action.SENSOR_PROCESSED";
    private static int MAX_SAMPLES;

    private GraphView sensor;
    private int sample;

    public TestReceiver() {
        MAX_SAMPLES = 100;
    }

    public TestReceiver(int max) {
        MAX_SAMPLES = max;
    }

    public void setGraphView(GraphView graphView) {
        sample = 1;
        sensor = graphView;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(TestReceiver.SENSOR_RESP)) {
            if (sample++ > MAX_SAMPLES) {
                BatteryActivity.canRegister = true;
                context.unregisterReceiver(this);
                context.stopService(new Intent(context, TestService.class));
                Log.d(msg, "Enough, thanks");
            } else {
                sensor.addPathPoint(sample, Float.valueOf(intent.getStringExtra(TestService.SENSOR_OUT_MSG)));
            }
        }
    }
}
