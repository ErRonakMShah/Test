package com.videorecording.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.almalence.plugins.capture.video.Mp4Editor;
import com.videorecording.R;
import com.videorecording.util.CameraHelper;
import com.videorecording.util.GlobalClass;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class VideoFragment extends Fragment {

    private static final String TAG = "VideoFragment";
    private final String ERROR_IMPLEMENT_ON_FRAGMENT_INTERACTION_LISTENER = " must implement OnVideoFragmentListener";
    private final int MAX_SUPPORTED_IMAGE_WIDTH = 1024;
    private final int MAX_SUPPORTED_IMAGE_HEIGHT = 768;
    private final boolean FRONT_CAMERA = true;
    private final boolean BACK_CAMERA = false;

    private VideoControls controls;

    //Change this to stream RMTP
    private String rtmpLink;
    private OnVideoFragmentListener listener;
    private Context context;

    private FrameLayout root;
    private FrameLayout topLayout;
    GlobalClass globalClass;

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

    // Audio and video initial setting
    boolean isPreviewOn;
    int sampleAudioRateInHz = 44100;
    int imageWidth = 320;
    int imageHeight = 240;
    int frameRate = 30;
    private int actualHeight;

    private List<File> outputFiles = new ArrayList<>();
    private String startRecordingFileName = "";

    private MediaPrepareTask mediaAsyncTask;

    public VideoFragment() {
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment StreamVideoFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static VideoFragment newInstance() {
        VideoFragment fragment = new VideoFragment();
        return fragment;
    }

    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        this.context = context;
        if (context instanceof OnVideoFragmentListener) {
            listener = (OnVideoFragmentListener) context;
        } else {
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
        View view = inflater.inflate(R.layout.video_fragment, container, false);
        globalClass = (GlobalClass) getActivity().getApplicationContext();
        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        isInitialized = false;
        isPreviewOn = false;
        root = (FrameLayout) getView();
        controls = new VideoControls();
        listener.videoFragmentReady(controls);
        // Prevent the window from turning dark
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initializeLayout(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Remove the previous view and add a new one on resume
        ((ViewGroup) getView()).removeView(topLayout);
        if (mediaAsyncTask != null) {
            mediaAsyncTask.cancel(true);
        }
        stopRecorder();
        destroyCamera();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        destroyRecorder();
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
        Log.i("MY", "titleHeight = " + titleBarHeight + " statusHeight = " + statusBarHeight + " contentViewTop = " + contentViewTop);

        // By now we got the height of titleBar & statusBar
        // Now lets get the screen size
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int screenHeight = metrics.heightPixels;
        int screenWidth = metrics.widthPixels;
        Log.i("MY", "Actual Screen Height = " + screenHeight + " Width = " + screenWidth);

        // Now calculate the height that our layout can be set
        // If you know that your application doesn't have statusBar added, then don't add here also. Same applies to application bar also
        int layoutHeight = screenHeight - (titleBarHeight + statusBarHeight);
        Log.i("MY", "Layout Height = " + layoutHeight);

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

        if (!isOrientationChange) {
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

        if (!isOrientationChange) {
            //Set the camera to portrait mode
            cameraDevice = openCamera(BACK_CAMERA);
        }

        if (cameraDevice != null) {

            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                cameraDevice.setDisplayOrientation(ROTATION_90);
            } else {
                cameraDevice.setDisplayOrientation(0);
            }

            if (!isOrientationChange) {
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
                Log.d(TAG, "Camera is released! Assigning new camera!");
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
                    Log.d(TAG, "Changed to supported resolution: " + imageWidth + "x" + imageHeight);
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

            Log.v(TAG, "Setting imageWidth: " + imageWidth + " imageHeight: " + imageHeight + " frameRate: " + frameRate);

            camParams.setPreviewFrameRate(frameRate);
            Log.v(TAG, "Preview Framerate: " + camParams.getPreviewFrameRate());


            /*set parametes here...*/

            camera.setParameters(camParams);

            if (!isPreviewOn && camera != null) {
                Log.i(TAG, "Camera preview started");
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

        }

    }


    class AppendVideoFiles extends AsyncTask<List<File>, Void, Void> {

        private ContentResolver contentResolver;
        private Context mContext;
        private ProgressDialog pdia;

        AppendVideoFiles(ContentResolver contentResolver, Context mContext) {
            this.contentResolver = contentResolver;
            this.mContext = mContext;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pdia = new ProgressDialog(mContext);
            pdia.setMessage("Loading...");
            pdia.show();
        }

        @Override
        protected Void doInBackground(List<File>... files) {
            appendNew(files[0], contentResolver);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            outputFiles.clear();
            pdia.dismiss();
        }
    }


    /**
     * Appends mp4 audio/video from {@code anotherFileDescriptor} to
     * {@code mainFileDescriptor}.
     */
    public File appendNew(List<File> inputFiles, ContentResolver contentResolver) {
        try {
            int[] inputFilesFds = new int[inputFiles.size()];
            ArrayList<ParcelFileDescriptor> pfdsList = new ArrayList<ParcelFileDescriptor>();

            int i = 0;
            for (File f : inputFiles) {
                ParcelFileDescriptor pfd = contentResolver.openFileDescriptor(Uri.fromFile(new File(f.getAbsolutePath())), "rw");

                pfdsList.add(pfd);
                inputFilesFds[i] = pfd.getFd();
                i++;
            }


            File tmpTargetFile = CameraHelper.getOutputMediaFile(CameraHelper.MEDIA_TYPE_VIDEO);
            ParcelFileDescriptor targetFilePfd = contentResolver
                    .openFileDescriptor(Uri.fromFile(new File(tmpTargetFile.getAbsolutePath())), "rw");

            String result = Mp4Editor.appendFds(inputFilesFds, targetFilePfd.getFd());


            targetFilePfd.close();
            for (ParcelFileDescriptor pfd : pfdsList) {
                pfd.close();
            }

            for (File f : inputFiles) {
                f.delete();
            }

            return tmpTargetFile;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Create recorder
    private boolean initializeRecorder() {

        // BEGIN_INCLUDE (configure_media_recorder)
        mMediaRecorder = new MediaRecorder();
        // Step 1: Unlock and set camera to MediaRecorder
        cameraDevice.unlock();
        mMediaRecorder.setCamera(cameraDevice);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);


        // Use the same size for recording profile.
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        profile.videoFrameWidth = imageWidth;
        profile.videoFrameHeight = imageHeight;


        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(profile);


        // Step 4: Set output file
        mOutputFile = CameraHelper.getOutputMediaFile(CameraHelper.MEDIA_TYPE_VIDEO);

        outputFiles.add(mOutputFile);

        /*save first file name*/
        if (TextUtils.isEmpty(startRecordingFileName)) {
            startRecordingFileName = mOutputFile.getName();
        }

        if (mOutputFile == null) {
            return false;
        }

        mMediaRecorder.setOutputFile(mOutputFile.getPath());

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            mMediaRecorder.setOrientationHint(90);
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mMediaRecorder.setOrientationHint(0);
        }


        // END_INCLUDE (configure_media_recorder)

        // Step 5: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            destroyRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            destroyRecorder();
            return false;
        }

        return true;
    }

    // Start recording
    private void startRecorder() {
        if (recording) {
            stopRecorder();
        } else {
            mediaAsyncTask = new MediaPrepareTask();
            mediaAsyncTask.execute();
        }
    }

    // Stop recording
    private void stopRecorder() {
        stopMediaRecorderOnly();
        if (recording) {
            destroyRecorder(); // release the MediaRecorder object
            cameraDevice.lock(); // take camera access back from MediaRecorder
            recording = false;
            startRecordingFileName = "";
            new AppendVideoFiles(getActivity().getContentResolver(), getActivity()).execute(outputFiles);
        }
    }

    private void stopMediaRecorderOnly() {
        if (recording) {
            // stop recording and release camera
            try {
                mMediaRecorder.stop();  // stop the recording
            } catch (RuntimeException e) {
                // RuntimeException is thrown when stop() is called immediately after start().
                // In this case the output file is not properly constructed ans should be deleted.
                Log.d(TAG, "RuntimeException: stop() is called immediately after start()");
                //noinspection ResultOfMethodCallIgnored
                mOutputFile.delete();
            }
        }
    }


    private void pauseRecorder() {
        if (recording) {
            /*stop media recorder...*/
            /*store moutfile's file descriptor in array*/
            stopMediaRecorderOnly();
        }
    }


    private void resumeRecorder() {
        mediaAsyncTask = new MediaPrepareTask();
        mediaAsyncTask.execute();
    }

    private void destroyRecorderFromFragment() {
        destroyCamera();
        destroyRecorder();
    }

    private FileDescriptor getFD(String path)
            throws FileNotFoundException, IOException {
        File file = new File(path);
        FileOutputStream fos = new FileOutputStream(file);
        return fos.getFD();
    }

    private void destroyRecorder() {
        if (mMediaRecorder != null) {
            // clear recorder configuration
            mMediaRecorder.reset();
            // release the recorder object
            mMediaRecorder.release();
            mMediaRecorder = null;
            // Lock camera for later use i.e taking it back from MediaRecorder.
            // MediaRecorder doesn't need it anymore and we will release it if the activity pauses.
            cameraDevice.lock();
        }
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
     * This set of public functions control the video streamer within the fragment
     */
    public class VideoControls {

        public void startRecording() {
            startRecorder();
        }

        public void stopRecording() {
            stopRecorder();
        }

        public void destroyRecorder() {
            destroyRecorderFromFragment();
        }


        public void pauseRecording() {
            pauseRecorder();
        }

        public void resumeRecording() {
            resumeRecorder();
        }

        public void turnOnFlash() {
            turnOn();
        }

        public void turnOffFlash() {
            turnOff();
        }

    }

    public interface OnVideoFragmentListener {
        void videoFragmentReady(VideoControls control);
    }


    /**
     * Asynchronous task for preparing the {@link android.media.MediaRecorder} since it's a long blocking
     * operation.
     */
    class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            // initialize video camera
            if (initializeRecorder()) {
                // Camera is available and unlocked, MediaRecorder is prepared,
                // now you can start recording
                mMediaRecorder.start();
                recording = true;
            } else {
                // prepare didn't work, release the camera
                destroyRecorder();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
            }

        }
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        initializeLayout(true);
    }

    private void turnOn() {
        if (!globalClass.isFlashOn) {
            try {
                if (cameraView.getCamera() != null) {

                    Camera.Parameters params = cameraView.getCamera().getParameters();
                    if (cameraDevice == null) {
                        return;
                    }
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    cameraView.getCamera().setParameters(params);
                    cameraView.getCamera().stopPreview();
                    cameraView.getCamera().startPreview();
                    globalClass.isFlashOn = true;
                }
            } catch (Exception e) {
                Log.d(TAG, "Exception  : Turn On Flash " + e.toString());
            }
        }
    }

    private void turnOff() {
        if (globalClass.isFlashOn) {
            try {
                if (cameraView.getCamera() != null) {
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

            } catch (Exception e) {
                Log.d(TAG, "Exception : Turn Off Flash :" + e.toString());
            }
        }
    }

    private void reInitializeLayout() {
        ((ViewGroup) getView()).removeView(topLayout);

    }

}


