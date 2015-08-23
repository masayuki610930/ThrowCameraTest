package com.example.masayuki.throwcamera;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.IBinder;
import android.util.Log;

import java.util.List;

/**
 * Created by masayuki on 2015/08/23.
 */
public class SensorAdapter implements SensorEventListener {

    ToneGenerator mToneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM, ToneGenerator.MAX_VOLUME);

     private Boolean mTriggerEnableFlag = false;

    private SensorManager mSensorManager = null;


    //THRESHOLD ある値以上を検出するための閾値
    protected final static double THRESHOLD = 0.5;
    private final static double THRESHOLD_TRIGGER_START = 10;
    private final static double THRESHOLD_TRIGGER = 1;

    //low pass filter alpha ローパスフィルタのアルファ値
    protected final static float alpha = 0.1f;

    //端末が実際に取得した加速度値。重力加速度も含まれる。This values include gravity force.
    private float[] currentOrientationValues = {0.0f, 0.0f, 0.0f};
    //ローパス、ハイパスフィルタ後の加速度値 Values after low pass and high pass filter
    private float[] currentAccelerationValues = {0.0f, 0.0f, 0.0f};

    //diff 差分
    private float dx = 0.0f;
    private float dy = 0.0f;
    private float dz = 0.0f;

    //previous data 1つ前の値
    private float old_x = 0.0f;
    private float old_y = 0.0f;
    private float old_z = 0.0f;


    //一回目のゆれを省くカウントフラグ（一回の端末の揺れで2回データが取れてしまうのを防ぐため）
    //count flag to prevent aquiring data twice with one movement of a device
    boolean counted = false;


    //ノイズ対策
    boolean noiseFlag = true;

    // RecorderService
    private final RecorderService mRecordeService;

    private final MainActivity mMainActivity;


    protected static final String TAG = "masayuki";

    public SensorAdapter( MainActivity mainActivity, SensorManager sensorManager, RecorderService recordeService){
        mMainActivity = mainActivity;

        mRecordeService = recordeService;
        mRecordeService.startRecording();


        // construct sensor
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);

        if (sensors.size() > 0) {
            Sensor s = sensors.get(0);
            sensorManager.registerListener(this, s, SensorManager.SENSOR_DELAY_UI);
        }
    }

    public void stopSensor() {

        // センサー停止時のリスナ解除 Stopping Listener
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
        mSensorManager = null;

    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
        // TODO 自動生成されたメソッド・スタブ

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        //ベクトル量
        double vectorSize;


        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // 取得 Acquiring data

            // ローパスフィルタで重力値を抽出 Isolate the force of gravity with the low-pass filter.
            currentOrientationValues[0] = event.values[0] * alpha + currentOrientationValues[0] * (1.0f - alpha);
            currentOrientationValues[1] = event.values[1] * alpha + currentOrientationValues[1] * (1.0f - alpha);
            currentOrientationValues[2] = event.values[2] * alpha + currentOrientationValues[2] * (1.0f - alpha);

            // 重力の値を省くRemove the gravity contribution with the high-pass filter.
            currentAccelerationValues[0] = event.values[0] - currentOrientationValues[0];
            currentAccelerationValues[1] = event.values[1] - currentOrientationValues[1];
            currentAccelerationValues[2] = event.values[2] - currentOrientationValues[2];

            // ベクトル値を求めるために差分を計算　diff for vector
            dx = currentAccelerationValues[0] - old_x;
            dy = currentAccelerationValues[1] - old_y;
            dz = currentAccelerationValues[2] - old_z;

            vectorSize = Math.sqrt((double) (dx * dx + dy * dy + dz * dz));

            // 一回目はノイズになるから省く
            if (noiseFlag == true) {
                noiseFlag = false;
            } else {


                if (vectorSize > THRESHOLD) {

                    String str = dx + ", " + dy + ", " + dz + ", " + vectorSize;
                    Log.d(TAG, str);


                    if (vectorSize > THRESHOLD_TRIGGER_START) {
                        mTriggerEnableFlag = true;
                    }
                    if (vectorSize < THRESHOLD_TRIGGER && mTriggerEnableFlag == true) {
                        mToneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);
                        mTriggerEnableFlag = false;
                    }


                    counted = false;

                }
            }

            // 状態更新
            old_x = currentAccelerationValues[0];
            old_y = currentAccelerationValues[1];
            old_z = currentAccelerationValues[2];

        }

    }

}
