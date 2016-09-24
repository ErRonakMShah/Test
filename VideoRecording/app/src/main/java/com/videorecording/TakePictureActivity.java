package com.videorecording;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.videorecording.fragment.CameraControlFragment;
import com.videorecording.fragment.CameraFragment;
import com.videorecording.fragment.VideoControlFragment;
import com.videorecording.fragment.VideoFragment;
import com.videorecording.util.GlobalClass;

public class TakePictureActivity extends FragmentActivity implements CameraFragment.OnCameraFragmentListener, CameraControlFragment.onCameraListener, CameraFragment.eventListener {

    private FragmentManager sfm;
    private CameraControlFragment cameraControlFragment;
    private CameraFragment.CameraControls control;
    private boolean isRecording = false;
    private GlobalClass globalClass;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_take_picture);
        globalClass = (GlobalClass) getApplicationContext();
        sfm = getSupportFragmentManager();

        cameraControlFragment = (CameraControlFragment) sfm.findFragmentById(R.id.cameraControlFragment);
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {

        }
    }

    @Override
    public void onBurstModeStart() {

    }

    @Override
    public void onTakePictureStart() {
        if (control != null) {
            control.takePicture();
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
    public void cameraFragmentReady(CameraFragment.CameraControls control) {
        this.control = control;
    }

    @Override
    public void enableTakeShot() {
        cameraControlFragment.enableTakeShot();
    }

    @Override
    public void stopBurstMode() {
        cameraControlFragment.stopBurstMode();
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode,
//                                           String permissions[], int[] grantResults) {
//        switch (requestCode) {
//            case GlobalClass.REQUEST_PERMISSION_CAMERA:
//                // If request is cancelled, the result arrays are empty.
//                if (grantResults.length == 0) {
//                    ActivityCompat.requestPermissions(TakePictureActivity.this,
//                            new String[]{Manifest.permission.CAMERA},
//                            GlobalClass.REQUEST_PERMISSION_CAMERA);
//                } else if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
//                    ActivityCompat.requestPermissions(TakePictureActivity.this,
//                            new String[]{Manifest.permission.CAMERA},
//                            GlobalClass.REQUEST_PERMISSION_CAMERA);
//                }
//                break;
//            case GlobalClass.REQUEST_PERMISSION_STORAGE:
//                // If request is cancelled, the result arrays are empty.
//                if (grantResults.length == 0) {
//                    ActivityCompat.requestPermissions(TakePictureActivity.this,
//                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                            GlobalClass.REQUEST_PERMISSION_STORAGE);
//                } else if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
//                    ActivityCompat.requestPermissions(TakePictureActivity.this,
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
//            ActivityCompat.requestPermissions(TakePictureActivity.this,
//                    new String[]{Manifest.permission.CAMERA},
//                    GlobalClass.REQUEST_PERMISSION_CAMERA);
//        }
//
//
//        if (ContextCompat.checkSelfPermission(this,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                != PackageManager.PERMISSION_GRANTED) {
//
//            ActivityCompat.requestPermissions(TakePictureActivity.this,
//                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                    GlobalClass.REQUEST_PERMISSION_STORAGE);
//        }

    }
}
