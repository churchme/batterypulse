package com.squarefist.batterypulse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class GraphView extends View {
    String msg = "GraphView : ";

    private Paint testPaint;
    private Paint sensPaint;
    private Path testPath;
    private Path sensPath;

    static class Pt {
        float x, y;
        Pt(float _x, float _y){
            x = _x;
            y = _y;
        }
    }

    List<Pt> myPath2 = new ArrayList<>();

    public GraphView(Context context, int sensitivityLine) {
        super(context);

        testPaint = new Paint();
        sensPaint = new Paint();
        testPaint.setColor(Color.GREEN);
        sensPaint.setColor(Color.BLACK);
        testPaint.setStrokeWidth(3);
        sensPaint.setStrokeWidth(3);
        testPaint.setStyle(Paint.Style.STROKE);
        sensPaint.setStyle(Paint.Style.STROKE);
        testPath = new Path();
        sensPath = new Path();

        updateSensPath(sensitivityLine);
    }

    private int scaleWidth(int sample, int width) {
        int scaled = ((width) * (sample - TestReceiver.MIN_SAMPLES)) / (TestReceiver.MAX_SAMPLES - TestReceiver.MIN_SAMPLES);
        return scaled;
    }

    private int scaleHeight(float reading, int height) {
        float scaled = ((height) * (reading - TestReceiver.MIN_ACCEL)) / (TestReceiver.MAX_ACCEL - TestReceiver.MIN_ACCEL);
        return (int)scaled;
    }

    public void resetPaths() {
        testPath.reset();
        myPath2.clear();
    }

    public void addPathPoint(int x, float y) {
        Pt newPoint = new Pt(scaleWidth(x, this.getWidth()), this.getHeight() - scaleHeight(y, this.getHeight()));
        myPath2.add(newPoint);
        this.invalidate();
    }

    public void updateSensPath(int sensitivityLine) {
        sensPath.reset();
        sensPath.moveTo(0, this.getHeight() - scaleHeight(sensitivityLine, this.getHeight()));
        sensPath.lineTo(this.getWidth(), this.getHeight() - scaleHeight(sensitivityLine, this.getHeight()));
        this.invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        testPath.moveTo(0, scaleHeight(9, this.getHeight()));

        for (int i = 0; i < myPath2.size(); i++) {
            testPath.lineTo(myPath2.get(i).x, myPath2.get(i).y);
        }

        canvas.drawPath(testPath, testPaint);
        canvas.drawPath(sensPath, sensPaint);
    }
}
