package com.example.masayuki.throwcamera;

import com.example.masayuki.throwcamera.RecorderService;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;




public class MainActivity extends Activity implements SensorEventListener {

    ToneGenerator mToneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM, ToneGenerator.MAX_VOLUME);



    private SensorManager mSensorManager = null;



    private Boolean mTriggerEnableFlag = false;
    private Boolean mIsRecording = false;

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


    protected static final String TAG = "masayuki";


    private Button buttonTake;


    private SensorAdapter mSensorAdapter;

    //取得したServiceの保存
    private RecorderService mRecordeService;
    private Boolean mIsBound;
    private RecorderService rs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // construct sensor
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);

        if (sensors.size() > 0) {
            Sensor s = sensors.get(0);
            sensorManager.registerListener(this, s, SensorManager.SENSOR_DELAY_UI);
        }


        buttonTake = (Button) findViewById(R.id.btn_take);
        buttonTake.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                doBindService();

                mIsRecording = true;
            }
        });




    }

   @Override
    protected void onPause() {
       doUnbindService();
        super.onPause();
    }



    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {

            mRecordeService = ((RecorderService.MyServiceLocalBinder)service).getService();
            mIsBound = true;

            mRecordeService.startRecording();
            //必要であればmBoundServiceを使ってバインドしたサービスへの制御を行う
        }

        public void onServiceDisconnected(ComponentName className) {
            // サービスとの切断(異常系処理)
            // プロセスのクラッシュなど意図しないサービスの切断が発生した場合に呼ばれる。
            mRecordeService = null;
            mIsBound = false;
        }

    };

    void doBindService() {
        //サービスとの接続を確立する。明示的にServiceを指定
        //(特定のサービスを指定する必要がある。他のアプリケーションから知ることができない = ローカルサービス)
        bindService(new Intent(this,
                RecorderService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            // コネクションの解除
            unbindService(mConnection);
            mIsBound = false;
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

        if( mIsRecording == false ){
            return;
        }

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
                        //mToneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);
                        if( mRecordeService == null ){
                            Log.d(TAG, "mRecordeService is null");
                        }
                        if( mIsBound == true ){
                            mConnection.stopRecording();
                            mRecordeService.stopRecording();
                        }
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