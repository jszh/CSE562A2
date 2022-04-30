package com.example.imu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor gravitySensor;
    private TextView textView;
    private Sensor linearAcclerationSensor;
    private LineChart lineChart;
    private Sensor magnetometer;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private double gyroIntegratedX;
    private double gyroIntegratedY;
    private long lastGyroTime;

    private int grantResults[];
    private TiltBuffer tiltBuffer;
    List<Entry> lineDataX;
    List<Entry> lineDataY;
    List<Entry> lineDataZ;
    int counter=0;
    int lim=500;
    Activity av;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        av = this;

        // get permissions
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        onRequestPermissionsResult(1,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},grantResults);

        // ui logic
        Constants.startButton = (Button)findViewById(R.id.button);
        Constants.stopButton = (Button)findViewById(R.id.button2);
        lineChart = (LineChart)findViewById(R.id.linechart);
        textView = (TextView)findViewById(R.id.textView);

        Constants.startButton.setEnabled(true);
        Constants.stopButton.setEnabled(false);

        // defining sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        linearAcclerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, linearAcclerationSensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);

        // on click listeners
        Constants.startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Constants.accx=new ArrayList<>();
                Constants.accy=new ArrayList<>();
                Constants.accz=new ArrayList<>();
                Constants.gravx=new ArrayList<>();
                Constants.gravy=new ArrayList<>();
                Constants.gravz=new ArrayList<>();
                Constants.magx=new ArrayList<>();
                Constants.magy=new ArrayList<>();
                Constants.magz=new ArrayList<>();
                Constants.rawaccx=new ArrayList<>();
                Constants.rawaccy=new ArrayList<>();
                Constants.rawaccz=new ArrayList<>();
                Constants.gyrx=new ArrayList<>();
                Constants.gyry=new ArrayList<>();
                Constants.gyrz=new ArrayList<>();
                Constants.tiltAcc=new ArrayList<>();
                Constants.tiltGyr=new ArrayList<>();
                Constants.tiltComp=new ArrayList<>();
                Constants.startButton.setEnabled(false);
                Constants.stopButton.setEnabled(true);
                resetTiltState();

                lineDataX=new ArrayList<>();
                lineDataY=new ArrayList<>();
                lineDataZ=new ArrayList<>();
                counter=0;
                Constants.start=true;
            }
        });
        Constants.stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Constants.startButton.setEnabled(true);
                Constants.stopButton.setEnabled(false);
                Constants.start=false;
                String fname = System.currentTimeMillis()+"";
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText(fname);
                    }
                });
                FileOperations.writetofile(av,fname);
            }
        });

        // tilt calculation
        tiltBuffer = new TiltBuffer();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (Constants.start) {
            float[] arr = new float[3];
            if (sensorEvent.sensor.equals(linearAcclerationSensor)) {
                Constants.accx.add(sensorEvent.values[0]);
                Constants.accy.add(sensorEvent.values[1]);
                Constants.accz.add(sensorEvent.values[2]);
            }
            else if (sensorEvent.sensor.equals(gravitySensor)) {
                Constants.gravx.add(sensorEvent.values[0]);
                Constants.gravy.add(sensorEvent.values[1]);
                Constants.gravz.add(sensorEvent.values[2]);
            }
            else if (sensorEvent.sensor.equals(magnetometer)) {  // magnetometer
                Constants.magx.add(sensorEvent.values[0]);
                Constants.magy.add(sensorEvent.values[1]);
                Constants.magz.add(sensorEvent.values[2]);
            }
            else if (sensorEvent.sensor.equals(accelerometer)) {
                Constants.rawaccx.add(sensorEvent.values[0]);
                Constants.rawaccy.add(sensorEvent.values[1]);
                Constants.rawaccz.add(sensorEvent.values[2]);

                //graphing logic
                float tilt = getTiltFromRawAcc(sensorEvent.values);
                if (tiltBuffer.updateAccTilt(tilt)) {
                    graphData(tiltBuffer.getGraphData());
                }
            }
            else if (sensorEvent.sensor.equals(gyroscope)) {
                Constants.gyrx.add(sensorEvent.values[0]);
                Constants.gyry.add(sensorEvent.values[1]);
                Constants.gyrz.add(sensorEvent.values[2]);

                Pair<Float, Float> tiltPair = getTiltFromGyro(System.nanoTime());
                if (tiltBuffer.updateGyrTilt(tiltPair.first, tiltPair.second)) {
                    graphData(tiltBuffer.getGraphData());
                }
            }

//            Log.e("log",String.format("%s %.2f %.2f %.2f",sensorEvent.sensor.getName(),sensorEvent.values[0],sensorEvent.values[1],sensorEvent.values[2]));
        }
    }


    private float getTiltFromRawAcc(float[] values) {
        double sum = values[0] * values[0] + values[1] * values[1] + values[2] * values[2];
        float tilt = (float) (Math.abs(Math.acos(values[2] / Math.sqrt(sum))));
        return tilt;
    }

    private Pair getTiltFromGyro(long time) {
        if (lastGyroTime == 0) {
            lastGyroTime = time;
            return new Pair(0f, 0f);
        }

        double dt = (time - lastGyroTime) / 1e9d;

        int len = Constants.gyrx.size();
        float lastX = Constants.gyrx.get(len-2);
        float lastY = Constants.gyrx.get(len-2);
        float currX = Constants.gyry.get(len-1);
        float currY = Constants.gyry.get(len-1);

        double gyroDeltaX = (currX - lastX) * dt;
        double gyroDeltaY = (currY - lastY) * dt;
        gyroIntegratedX += gyroDeltaX;
        gyroIntegratedY += gyroDeltaY;

        float tilt = (float) Math.sqrt( gyroIntegratedX * gyroIntegratedX
                                      + gyroIntegratedY * gyroIntegratedY );
        lastGyroTime = time;

        float compTilt = getComplementaryTilt(gyroDeltaX, gyroDeltaY);
        return new Pair(tilt, compTilt);
    }

    private float getComplementaryTilt(double gyroDeltaX, double gyroDeltaY) {
        double pitch = tiltBuffer.complementaryPitch - gyroDeltaX;
        double roll = tiltBuffer.complementaryRoll - gyroDeltaY;

        int len = Constants.rawaccx.size();
        if (len > 0) {
            double pitchAcc = Math.atan2(Constants.rawaccy.get(len - 1), Constants.rawaccz.get(len - 1));
            double rollAcc = Math.atan2(Constants.rawaccx.get(len - 1), Constants.rawaccz.get(len - 1));
            pitch = pitch * 0.98 + pitchAcc * 0.02;
            roll = roll * 0.98 + rollAcc * 0.02;
        }

        tiltBuffer.complementaryPitch = pitch;
        tiltBuffer.complementaryRoll = roll;
        float tilt = (float) Math.sqrt( pitch * pitch + roll * roll );
        return tilt;
    }

    public void graphData(float[] values) {
        lineDataX.add(new Entry(counter,values[0]));
        lineDataY.add(new Entry(counter,values[1]));
        lineDataZ.add(new Entry(counter,values[2]));
        if (lineDataX.size()>lim) {
            lineDataX.remove(0);
            lineDataY.remove(0);
            lineDataZ.remove(0);
        }
        counter+=1;

        LineDataSet data1 = new LineDataSet(lineDataX, "Acc");
        LineDataSet data2 = new LineDataSet(lineDataY, "Gyro");
        LineDataSet data3 = new LineDataSet(lineDataZ, "Complementary");
        data1.setDrawCircles(false);
        data2.setDrawCircles(false);
        data3.setDrawCircles(false);
        data1.setColor(((MainActivity)this).getResources().getColor(R.color.red));
        data2.setColor(((MainActivity)this).getResources().getColor(R.color.green));
        data3.setColor(((MainActivity)this).getResources().getColor(R.color.blue));
        List<ILineDataSet> data = new ArrayList<>();
        data.add(data1);
        data.add(data2);
        data.add(data3);

        LineData lineData = new LineData(data);
        lineChart.setData(lineData);
        lineChart.notifyDataSetChanged();
        lineChart.invalidate();
    }

    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, linearAcclerationSensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        resetTiltState();
    }

    private void resetTiltState() {
        lastGyroTime = 0;
        gyroIntegratedX = 0;
        gyroIntegratedY = 0;
        tiltBuffer.clear();
    }

    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}