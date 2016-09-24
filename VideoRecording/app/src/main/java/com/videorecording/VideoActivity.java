package com.videorecording;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;

import com.videorecording.fragment.VideoControlFragment;
import com.videorecording.fragment.VideoFragment;
import com.videorecording.util.GlobalClass;

public class VideoActivity extends FragmentActivity implements VideoFragment.OnVideoFragmentListener, VideoControlFragment.OnVideoControlFragmentListener {

    private FragmentManager sfm;

    private VideoControlFragment videoControlFragment;

    private VideoFragment.VideoControls control;
    private boolean isRecording = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);

        sfm = getSupportFragmentManager();

        videoControlFragment = (VideoControlFragment) sfm.findFragmentById(R.id.videoControlFragment);

    }

    @Override
    public void videoFragmentReady(VideoFragment.VideoControls control) {
        this.control = control;
    }

    @Override
    public void onRecordButtonPress() {
        if(control != null) {
            isRecording = true;
            control.startRecording();
        }
    }

    @Override
    public void onStopButtonPress() {
        if(control != null) {
            isRecording = false;
            control.stopRecording();
        }
    }

    @Override
    public void onPauseButtonPress() {
        if(control != null) {
            if(isRecording) {
                control.pauseRecording();
            }
        }
    }

    @Override
    public void onResumeButtonPress() {
        if(control != null) {
            if(isRecording) {
                control.resumeRecording();
            }
        }
    }

    @Override
    public void turnOnFlash() {
        if (control != null) {
            control.turnOnFlash();
        }
    }

    @Override
    public void turnOffFlash() {
        if (control != null) {
            control.turnOffFlash();
        }
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode,
//                                           String permissions[], int[] grantResults) {
//        switch (requestCode) {
//            case GlobalClass.REQUEST_PERMISSION_CAMERA:
//                // If request is cancelled, the result arrays are empty.
//                if (grantResults.length == 0) {
//                    ActivityCompat.requestPermissions(VideoActivity.this,
//                            new String[]{Manifest.permission.CAMERA},
//                            GlobalClass.REQUEST_PERMISSION_CAMERA);
//                } else if(grantResults[0] != PackageManager.PERMISSION_GRANTED) {
//                    ActivityCompat.requestPermissions(VideoActivity.this,
//                            new String[]{Manifest.permission.CAMERA},
//                            GlobalClass.REQUEST_PERMISSION_CAMERA);
//                }
//                break;
//            case GlobalClass.REQUEST_PERMISSION_RECORD_AUDIO:
//                    // If request is cancelled, the result arrays are empty.
//                    if (grantResults.length == 0) {
//                        ActivityCompat.requestPermissions(VideoActivity.this,
//                                new String[]{Manifest.permission.RECORD_AUDIO},
//                                GlobalClass.REQUEST_PERMISSION_RECORD_AUDIO);
//                    } else if(grantResults[0] != PackageManager.PERMISSION_GRANTED) {
//                        ActivityCompat.requestPermissions(VideoActivity.this,
//                                new String[]{Manifest.permission.RECORD_AUDIO},
//                                GlobalClass.REQUEST_PERMISSION_RECORD_AUDIO);
//                    }
//                    break;
//            case GlobalClass.REQUEST_PERMISSION_STORAGE:
//                // If request is cancelled, the result arrays are empty.
//                if (grantResults.length == 0) {
//                    ActivityCompat.requestPermissions(VideoActivity.this,
//                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                            GlobalClass.REQUEST_PERMISSION_STORAGE);
//                } else if(grantResults[0] != PackageManager.PERMISSION_GRANTED) {
//                    ActivityCompat.requestPermissions(VideoActivity.this,
//                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                            GlobalClass.REQUEST_PERMISSION_STORAGE);
//                }
//                break;
//        }
//    }

    @Override
    protected void onResume() {
        super.onResume();

//        if (ContextCompat.checkSelfPermission(this,
//                Manifest.permission.CAMERA)
//                != PackageManager.PERMISSION_GRANTED) {
//
//            ActivityCompat.requestPermissions(VideoActivity.this,
//                    new String[]{Manifest.permission.CAMERA},
//                    GlobalClass.REQUEST_PERMISSION_CAMERA);
//        }
//
//        if (ContextCompat.checkSelfPermission(this,
//                Manifest.permission.RECORD_AUDIO)
//                != PackageManager.PERMISSION_GRANTED) {
//
//            ActivityCompat.requestPermissions(VideoActivity.this,
//                    new String[]{Manifest.permission.RECORD_AUDIO},
//                    GlobalClass.REQUEST_PERMISSION_RECORD_AUDIO);
//        }
//
//        if (ContextCompat.checkSelfPermission(this,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                != PackageManager.PERMISSION_GRANTED) {
//
//            ActivityCompat.requestPermissions(VideoActivity.this,
//                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                    GlobalClass.REQUEST_PERMISSION_STORAGE);
//        }


    }
}
