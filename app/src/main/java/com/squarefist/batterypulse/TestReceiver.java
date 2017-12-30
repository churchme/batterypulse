package com.squarefist.batterypulse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class TestReceiver extends BroadcastReceiver {
    String msg = "Battery Receiver : ";
    public static final String SENSOR_RESP =
            "com.squarefist.batterybuzzer.action.SENSOR_PROCESSED";
    public static final int MAX_SAMPLES = 100;
    public static final int MIN_SAMPLES = 1;
    public static final int MAX_ACCEL = 20;
    public static final int MIN_ACCEL = 1;

    private GraphView sensor;
    public int sample;

    public void setGraphView(GraphView graphView) {
        sample = 1;
        sensor = graphView;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(TestReceiver.SENSOR_RESP)) {
            float reading = Float.valueOf(intent.getStringExtra(BatteryService.SENSOR_OUT_MSG));
            sensor.addPathPoint(sample, reading);
            if (sample++ > MAX_SAMPLES) {
                //context.stopService(new Intent(context, BatteryService.class));
                context.unregisterReceiver(this);
                context.stopService(new Intent(context, BatteryService.class));
                Log.d(msg, "Enough, thanks");
            }
        }
    }
}
