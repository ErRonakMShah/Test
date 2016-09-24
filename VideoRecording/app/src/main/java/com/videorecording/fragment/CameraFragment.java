package com.videorecording.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.media.CamcorderProfile;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.almalence.plugins.capture.video.Mp4Editor;
import com.videorecording.R;
import com.videorecording.util.CameraHelper;
import com.videorecording.util.GlobalClass;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class CameraFragment extends Fragment {

    private static final String TAG = "CameraFragment";
    private final String ERROR_IMPLEMENT_ON_FRAGMENT_INTERACTION_LISTENER = " must implement OnCameraFragmentListener";
    private final int MAX_SUPPORTED_IMAGE_WIDTH = 1024;
    private final int MAX_SUPPORTED_IMAGE_HEIGHT = 768;
    private final boolean FRONT_CAMERA = true;
    private final boolean BACK_CAMERA = false;

    private CameraControls controls;

    //Change this to stream RMTP
    private String rtmpLink;
    private OnCameraFragmentListener listener;
    private eventListener eventlistener;
    private Context context;

    private FrameLayout root;
    private FrameLayout topLayout;
    private GlobalClass globalClass;
    // States for recorder
    long startTime;
    boolean recorderWhenReady = false;
    boolean isInitialized = false;
    boolean recording = false;

    private Camera cameraDevice;
    private CameraView cameraView;

    private MediaRecorder mMediaRecorder;
    private File mOutputFile;

    // Video and audio codec settings
    private String VIDEO_FORMAT = "flv";
    private int VIDEO_CODEC_H264 = 28;
    private int AUDIO_CODEC_AAC = 86018;
    private int VIDEO_FRAME_RATE = 30;

    // From javacpp-presets -> avutil.java
    private int AV_PIX_FMT_NV21 = 26;

    // Rotation constant
    final int ROTATION_90 = 90;
    private int mCameraOrientation;

    // Audio and video initial setting
    boolean isPreviewOn;
    int sampleAudioRateInHz = 44100;
    int imageWidth = 320;
    int imageHeight = 240;
    int pictureWidth=320;
    int pictureHeight=240;
    int frameRate = 30;
    private int actualHeight;

    private List<File> outputFiles = new ArrayList<>();
    private String startRecordingFileName = "";
    private int cameraId;

    public CameraFragment() {
    }

    OrientationEventListener orientationEventListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment StreamVideoFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CameraFragment newInstance() {
        CameraFragment fragment = new CameraFragment();
        return fragment;
    }

    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        this.context = context;
        if (context instanceof OnCameraFragmentListener) {
            listener = (OnCameraFragmentListener) context;
        }
        if (context instanceof eventListener) {
            eventlistener= (eventListener) context;
        }else {
            throw new RuntimeException(context.toString()
                    + ERROR_IMPLEMENT_ON_FRAGMENT_INTERACTION_LISTENER);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        // Get the layout id
        View view = inflater.inflate(R.layout.camera_fragment, container, false);
        globalClass=(GlobalClass)getActivity().getApplicationContext();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        isInitialized = false;
        isPreviewOn = false;
        root = (FrameLayout) getView();
        controls = new CameraControls();
        listener.cameraFragmentReady(controls);
        // Prevent the window from turning dark
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initializeLayout(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Remove the previous view and add a new one on resume
        ((ViewGroup) getView()).removeView(topLayout);
        destroyCamera();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // Compute the layout size and set it to displayDimensions
    // If isToggle is true, there will be no outside signal to start recording
    private void initializeLayout(final boolean isOrientationChange) {
        root.post(new Runnable() {
            public void run() {
                // Get actual height of the activity
                getActualHeight();
                // Start camera preview
                initializeCameraPreviewLayout(isOrientationChange);
            }
        });
    }

    private void getActualHeight() {
        Rect rect = new Rect();
        Window win = getActivity().getWindow();  // Get the Window
        win.getDecorView().getWindowVisibleDisplayFrame(rect);
        // Get the height of Status Bar
        int statusBarHeight = rect.top;
        // Get the height occupied by the decoration contents
        int contentViewTop = win.findViewById(Window.ID_ANDROID_CONTENT).getTop();
        // Calculate titleBarHeight by deducting statusBarHeight from contentViewTop
        int titleBarHeight = contentViewTop - statusBarHeight;

        // By now we got the height of titleBar & statusBar
        // Now lets get the screen size
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int screenHeight = metrics.heightPixels;
        int screenWidth = metrics.widthPixels;

        // Now calculate the height that our layout can be set
        // If you know that your application doesn't have statusBar added, then don't add here also. Same applies to application bar also
        int layoutHeight = screenHeight - (titleBarHeight + statusBarHeight);

        actualHeight = layoutHeight;
    }


    // Create the layout
    private void initializeCameraPreviewLayout(boolean isOrientationChange) {
        // Dimensions of the screen
        Display display;
        Point displayDimensions;

        // Set the width and height for the video preview
        int bg_screen_width;
        int bg_screen_height;

        // Set the width and height for the backdrop
        int bg_width;
        int bg_height;

        int live_width;
        int live_height;
        int screenWidth, screenHeight;

        // x border and y border
        final int bg_screen_bx = 0;
        final int bg_screen_by = 0;

        /* get size of screen */
        display = ((WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        displayDimensions = new Point();

        //Set the size of the screen, preview and its background
        display.getSize(displayDimensions);
        displayDimensions.set(displayDimensions.x, actualHeight);

        Log.i(TAG, "x: " + displayDimensions.x + " y: " + displayDimensions.y);

        screenWidth = displayDimensions.x;
        screenHeight = displayDimensions.y;
        bg_screen_width = displayDimensions.x;
        bg_screen_height = displayDimensions.y;
        bg_width = displayDimensions.x;
        bg_height = displayDimensions.y;
        live_width = displayDimensions.x;
        live_height = displayDimensions.y;

        //Initialize the frame layout
        FrameLayout.LayoutParams frameLayoutParam;

        if(!isOrientationChange) {
            topLayout = new FrameLayout(context);
            // Add topLayout into view
            ((ViewGroup) getView()).addView(topLayout);
        }


        /* add camera view */
        int display_width_d = (int) (1.0 * bg_screen_width * screenWidth / bg_width);
        int display_height_d = (int) (1.0 * bg_screen_height * screenHeight / bg_height);
        int prev_rw, prev_rh;
        if (1.0 * display_width_d / display_height_d > 1.0 * live_width / live_height) {
            prev_rh = display_height_d;
            prev_rw = (int) (1.0 * display_height_d * live_width / live_height);
        } else {
            prev_rw = display_width_d;
            prev_rh = (int) (1.0 * display_width_d * live_height / live_width);
        }
        frameLayoutParam = new FrameLayout.LayoutParams(prev_rw, prev_rh);
        frameLayoutParam.topMargin = (int) (1.0 * bg_screen_by * screenHeight / bg_height);
        frameLayoutParam.leftMargin = (int) (1.0 * bg_screen_bx * screenWidth / bg_width);

        if(!isOrientationChange) {
            //Set the camera to portrait mode
            cameraDevice = openCamera(BACK_CAMERA);
        }



        if(cameraDevice != null) {

            orientationEventListener = new OrientationEventListener(getActivity(), SensorManager.SENSOR_DELAY_NORMAL) {
                @Override
                public void onOrientationChanged(int orientation) {
                    if (orientation == ORIENTATION_UNKNOWN) return;
                    android.hardware.Camera.CameraInfo info =
                            new android.hardware.Camera.CameraInfo();
                    android.hardware.Camera.getCameraInfo(cameraId, info);
                    orientation = (orientation + 45) / 90 * 90;
                    int rotation = 0;
                    // back-facing camera
                    rotation = (info.orientation + orientation) % 360;
                    mCameraOrientation = rotation; // Store rotation for later use
                }
            };

            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                cameraDevice.setDisplayOrientation(ROTATION_90);
            } else {
                cameraDevice.setDisplayOrientation(0);
            }

            if(!isOrientationChange) {
                cameraView = new CameraView(context, cameraDevice);
                topLayout.addView(cameraView, frameLayoutParam);
                isInitialized = true;
            } else {
                cameraView.setLayoutParams(frameLayoutParam);
                topLayout.setLayoutParams(frameLayoutParam);
            }

        }


    }


    private Camera openCamera(boolean front) {
        int cameraCount = 0;
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT && front) {
                try {
                    cam = Camera.open(camIdx);
                } catch (RuntimeException e) {
                    Log.e(TAG, "Camera failed to open: " + e.getLocalizedMessage());
                }
            } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK && !front) {
                try {
                    cameraId = camIdx;
                    cam = Camera.open(camIdx);
                } catch (RuntimeException e) {
                    Log.e(TAG, "Camera failed to open: " + e.getLocalizedMessage());
                }
            }
        }

        return cam;
    }

    // CameraView class that contains thread to get and encode video data
    class CameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

        private SurfaceHolder holder;
        private Camera camera;

        public CameraView(Context context, Camera camera) {
            super(context);
            this.camera = camera;
            holder = getHolder();
            holder.addCallback(CameraView.this);
            holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public Camera getCamera() {
            return this.camera;
        }

        public void setCamera(Camera camera) {
            this.camera = camera;
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                stopCameraPreview();
                camera.setPreviewDisplay(holder);
                camera.setPreviewCallback(CameraView.this);

            } catch (IOException e) {
                e.printStackTrace();
                camera.release();
                camera = null;
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            startCameraPreview();
        }


        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            try {
                holder.addCallback(null);
                camera.setPreviewCallback(null);
            } catch (RuntimeException e) {
                // The camera has probably just been released, ignore.
            }
        }

        public void startCameraPreview() {

            try {
                Camera.Parameters camParams = camera.getParameters();
            } catch (Exception e) {
                this.camera = openCamera(BACK_CAMERA);
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    cameraDevice.setDisplayOrientation(ROTATION_90);
                } else {
                    cameraDevice.setDisplayOrientation(0);
                }
                this.camera.setPreviewCallback(CameraView.this);
            }

            Camera.Parameters camParams = camera.getParameters();

            // Enable auto focus
            if (camParams.getSupportedFocusModes().contains(
                    Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                camParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }

            // Sort the list in ascending order
            List<Camera.Size> sizes = camParams.getSupportedPreviewSizes();
            Collections.sort(sizes, new Comparator<Camera.Size>() {

                public int compare(final Camera.Size a, final Camera.Size b) {
                    return a.width * a.height - b.width * b.height;
                }
            });

            // Pick the first preview size that is equal or bigger, or pick the last (biggest) option if we cannot
            // reach the initial settings of imageWidth/imageHeight.
            for (int i = sizes.size() - 1; i >= 0; i--) {
                if ((sizes.get(i).width <= MAX_SUPPORTED_IMAGE_WIDTH && sizes.get(i).height <= MAX_SUPPORTED_IMAGE_HEIGHT) || i == 0) {
                    imageWidth = sizes.get(i).width;
                    imageHeight = sizes.get(i).height;
                    break;
                }
            }


            try {
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    camParams.set("orientation", "portrait");
                    camParams.set("rotation", 90);
                }
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    camParams.set("orientation", "landscape");
                    camParams.set("rotation", 0);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }


            camParams.setPreviewSize(imageWidth, imageHeight);


            camParams.setPreviewFrameRate(frameRate);


            // Sort the list in ascending order
            List<Camera.Size> picturesizes = camParams.getSupportedPictureSizes();
            Collections.sort(picturesizes, new Comparator<Camera.Size>() {

                public int compare(final Camera.Size a, final Camera.Size b) {
                    return a.width * a.height - b.width * b.height;
                }
            });

            // Pick the first preview size that is equal or bigger, or pick the last (biggest) option if we cannot
            // reach the initial settings of imageWidth/imageHeight.
            for (int i = picturesizes.size() - 1; i >= 0; i--) {
                if ((picturesizes.get(i).width <= MAX_SUPPORTED_IMAGE_WIDTH && picturesizes.get(i).height <= MAX_SUPPORTED_IMAGE_HEIGHT) || i == 0) {
                    pictureWidth = picturesizes.get(i).width;
                    pictureHeight = picturesizes.get(i).height;
                    break;
                }
            }

            camParams.setPictureSize(pictureWidth,pictureHeight);


            /*set parametes here...*/

            camera.setParameters(camParams);

            if (!isPreviewOn && camera != null) {
                isPreviewOn = true;
                camera.startPreview();
            }
        }

        public void stopCameraPreview() {
            if (isPreviewOn && camera != null) {
                isPreviewOn = false;
                camera.stopPreview();
            }
        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            try {
                if(globalClass!=null && globalClass.isBurstModeStart && globalClass.burstcount<=20){

                    ++globalClass.burstcount;
                    if(globalClass!=null){
                        globalClass.validateStoragePath();
                    }


                    Camera.Parameters parameters = camera.getParameters();
                    Camera.Size size = parameters.getPreviewSize();
                    Rect rectangle = new Rect();
                    rectangle.bottom = size.height;
                    rectangle.top = 0;
                    rectangle.left = 0;
                    rectangle.right = size.width;


                    int ROTATION=Integer.parseInt(parameters.get("rotation"));

                    YuvImage image = new YuvImage(data, parameters.getPreviewFormat(),
                            size.width, size.height, null);

                    Bitmap rotatedBitmap=rotateBitmap(image,ROTATION,rectangle);
                    String imageName=globalClass.getCurrentTimeStamp()+".jpg";
                    globalClass.latestimagename=imageName;
                    String filePath=globalClass.STORAGE_PATH+"/"+imageName;
                    File file =new File(filePath);
                    FileOutputStream outStream = new FileOutputStream(file);
                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                    outStream.flush();
                    outStream.close();
                    if(eventlistener!=null){
                        eventlistener.enableTakeShot();
                    }

                    if(globalClass.burstcount==20){
                        eventlistener.stopBurstMode();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG,"Exception in preview frame"+e.toString());
            }
        }

    }
    private void destroyPreviewFromFragment() {
        destroyCamera();
    }

    private void destroyCamera() {
        if (cameraDevice != null) {
            Log.d(TAG, "Camera released!");
            cameraDevice.stopPreview();
            cameraView.getHolder().removeCallback(cameraView);
            cameraDevice.setPreviewCallback(null);
            cameraView.getCamera().release();
            cameraView.setCamera(null);
            cameraDevice.release();
            cameraDevice = null;
        }
    }


    private void releaseCamera() {
        if (cameraDevice != null) {
            cameraDevice.release();
            cameraDevice = null;
        }
    }

    public void setRTMPLink(String rtmpLink) {

    }

    /**
     * This set of public functions control the camera preview within the fragment
     */
    public class CameraControls {

        public void takePicture() {
            if(cameraView!=null){
                cameraView.getCamera().takePicture(shutterCallback, rawCallback,
                        jpegCallback);
            }
        }

        public void startBurstMode() {
        }

        public void destroyCamera() {
            destroyPreviewFromFragment();
        }

        public void turnOnFlash(){
            turnOn();
        }

        public void turnOffFlash(){
            turnOff();
        }
    }

    public interface OnCameraFragmentListener {
        void cameraFragmentReady(CameraControls control);
    }

    public interface eventListener{
        void enableTakeShot();
        void stopBurstMode();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);


        initializeLayout(true);
    }

    private void reInitializeLayout() {
        ((ViewGroup) getView()).removeView(topLayout);

    }

    private void turnOn() {
        if(!globalClass.isFlashOn) {
            try{
                if(cameraView.getCamera()!= null){

                    Camera.Parameters params = cameraView.getCamera().getParameters();
                    if(cameraDevice== null) {
                        return;
                    }
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    cameraView.getCamera().setParameters(params);
                    cameraView.getCamera().stopPreview();
                    cameraView.getCamera().startPreview();
                    globalClass.isFlashOn = true;
                }
            }
            catch (Exception e){
                Log.d(TAG, "Exception  : Turn On Flash " + e.toString());
            }
        }
    }

    private void turnOff() {
        if (globalClass.isFlashOn) {
            try{
                if(cameraView.getCamera()!=null){
                    Camera.Parameters params = cameraView.getCamera().getParameters();
                    if (params == null) {
                        return;
                    }
                    params = cameraDevice.getParameters();
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    cameraView.getCamera().setParameters(params);
                    cameraView.getCamera().stopPreview();
                    cameraView.getCamera().startPreview();
                    globalClass.isFlashOn = false;
                }

            }
            catch (Exception e){
                Log.d(TAG,"Exception : Turn Off Flash :"+e.toString());
            }
        }
    }
Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
    public void onShutter() {
        Log.d(TAG, "onShutter CallBack");
    }
};

    Camera.PictureCallback rawCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d(TAG, "onPictureTaken - raw with data = " + ((data != null) ? data.length : " NULL"));
        }
    };

    /** This function is used to store picture when take shot by click shot button**/
    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {

            try {
                new saveImage(data,camera).execute();

                Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
            }
        }
    };


    public interface onControlImageListeners{
        void enabletakePicture();
    }

    private class saveImage extends AsyncTask<String, String, String> {

        FileOutputStream outStream = null;
        byte[] data;
        Camera camera;

        public saveImage(byte[] data,Camera camera){
            this.data=data;
            this.camera=camera;
        }

        @Override
        protected void onPostExecute(String result) {
            // execution of result of Long time consuming operation
               try{
                   if(camera!=null){
                       camera.stopPreview();
                       camera.startPreview();
                   }
                   if(eventlistener!=null){
                       eventlistener.enableTakeShot();
                   }
               }
               catch (Exception e){

               }
        }


        @Override
        protected void onPreExecute() {
        }


        @Override
        protected void onProgressUpdate(String... text) {
        }

        public  Bitmap rotate(Bitmap bitmap, int degree) {
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();

            Matrix mtx = new Matrix();
            //       mtx.postRotate(degree);
            mtx.setRotate(degree);

            return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
        }

        @Override
        protected String doInBackground(String... params) {
            try{

                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

                int width = bitmap.getWidth();
                int height = bitmap.getHeight();

                Matrix matrix = new Matrix();
                switch(mCameraOrientation) {
                    case 0:
                        // data is correctly rotated
                        break;
                    case 90:
                        // rotate data by 90 degrees clockwise
                        matrix.postRotate(90);
                        break;
                    case 180:
                        // rotate data upside down
                        matrix.postRotate(180);
                        break;
                    case 270:
                        // rotate data by 90 degrees counterclockwise
                        matrix.postRotate(270);
                        break;
                }

                // recreate the new Bitmap, swap width and height and apply transform
                Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                        width, height, matrix, true);

                String imageName=globalClass.getCurrentTimeStamp()+".jpg";
                globalClass.latestimagename=imageName;
                outStream = new FileOutputStream(String.format(globalClass.STORAGE_PATH+"/"+imageName,System.currentTimeMillis()));
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                outStream.flush();
                outStream.close();

            }
            catch (Exception e){
                Log.d(TAG,"Exception ::: "+e.toString());
            }
            return null;
        }
    }





    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if(null != orientationEventListener) {
            orientationEventListener.disable();
        }
    }

    private Bitmap rotateBitmap(YuvImage yuvImage, int orientation, Rect rectangle)
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(rectangle, 100, os);

        Matrix matrix = new Matrix();
        matrix.postRotate(orientation);
        byte[] bytes = os.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        return Bitmap.createBitmap(bitmap, 0 , 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

}


