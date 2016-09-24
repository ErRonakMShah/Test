package com.videorecording.util;

import android.app.Application;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GlobalClass extends Application {
    public String TAG = "GlobalClass";
    public boolean isBurstModeStart = false;
    public int burstcount = 0;
    public boolean isFlashOn = false;
    public String latestimagename = "";
    public final String STORAGE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Meditab/Android/Images";
    public final String STORAGE_VIDEO_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Meditab/Android/Videos";

    public static final int REQUEST_PERMISSION_CAMERA = 101;
    public static final int REQUEST_PERMISSION_STORAGE = 102;
    public static final int REQUEST_PERMISSION_RECORD_AUDIO = 103;

    public String getCurrentTimeStamp() {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            String currentDateTime = dateFormat.format(new Date()); // Find todays date
            return currentDateTime;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void validateStoragePath() {
        try {
            File file = new File(STORAGE_PATH);
            Log.d(TAG, file.toString());
            if (!file.exists()) {
                file.mkdirs();
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in create directory");
            e.printStackTrace();
        }
    }


    public void validateStorageVideoPath() {
        try {
            File file = new File(STORAGE_VIDEO_PATH);
            Log.d(TAG, file.toString());
            if (!file.exists()) {
                file.mkdirs();
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in create directory");
            e.printStackTrace();
        }
    }
}
