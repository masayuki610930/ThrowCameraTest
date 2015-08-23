package com.example.masayuki.throwcamera;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.os.Binder;
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

/**
 * Created by masayuki on 2015/08/23.
 */
public class RecorderService extends Service implements MediaRecorder.OnInfoListener, SurfaceHolder.Callback  {

    protected static final String TAG = "masayuki";

    private View mMainView;
    private SurfaceHolder mSurfaceHolder;
    private MediaRecorder mMediaRecorder;
    private final SimpleDateFormat mSimpleDateFormatVideo = new SimpleDateFormat("'Mov'_yyyyMMdd_HHmmss'.3gp'");
    private final SimpleDateFormat mSimpleDateFormatImg = new SimpleDateFormat("'IMG'_yyyyMMdd_HHmmss'.jpg'");
    private String mFileName;


    private Boolean mRecodingFlag = false;



    private final IBinder mBinder = new MyServiceLocalBinder();



    @Override
    public void onCreate() {
        super.onCreate();




        // xml����View�𐶐�
        mMainView = LayoutInflater.from(this).inflate(R.layout.camera, null);
        mSurfaceHolder = ((SurfaceView) mMainView.findViewById(R.id.surfaceView)).getHolder();
        mSurfaceHolder.addCallback(this);


        // �V�X�e���I�[�o�[���C���C���[��View��ǉ�
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                100, // �K���ȃT�C�Y
                100, // �K���ȃT�C�Y
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(mMainView, layoutParams);


    }





    public class MyServiceLocalBinder extends Binder {
        //�T�[�r�X�̎擾
        RecorderService getService() {
            return RecorderService.this;
        }
    }


    public void InitializeVideoSettings(){

        // �o�͐�
        String directoryPath = Environment.getExternalStorageDirectory().getPath();
        directoryPath += "/DCIM/ThrowCamera/";
        final File directory = new File(directoryPath);
        if( ! directory.exists() ){
            directory.mkdirs();
        }

        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);

        mFileName = mSimpleDateFormatVideo.format(new Date(System.currentTimeMillis()));
        mMediaRecorder.setOutputFile(directoryPath + mFileName);
        mMediaRecorder.setVideoFrameRate(60);
        mMediaRecorder.setVideoSize(1920, 1080);
        mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());

        try{
            mMediaRecorder.prepare();
        }catch (final Exception e){

        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // �^��J�n
        startRecording();

        // View���\���ɂ���
        //mMainView.setVisibility(View.INVISIBLE);
    }

    public void startRecording() {

        InitializeVideoSettings();
        mMediaRecorder.start();
        Log.d(TAG, "record start");
        mRecodingFlag = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // �^���~
        stopRecording();

        // View���폜
        ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(mMainView);
        mMainView = null;
    }

    public void stopRecording() {
        if( mRecodingFlag == true ){
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder = null;
        }


    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public IBinder onBind(final Intent intent ){
        return null;
    }



    public void onInfo( MediaRecorder mr, int what, int extra ) {
        // MediaRecorder�̘^�掞�Ԃ܂��͘^��T�C�Y�̐����ɒB������A�^���~���čēxView��\��
        if ( what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED || what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED ) {
            stopRecording();
            mMainView.setVisibility( View.VISIBLE );
        }
    }
}
