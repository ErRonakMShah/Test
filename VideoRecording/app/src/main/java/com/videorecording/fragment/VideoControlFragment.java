package com.videorecording.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;

import com.videorecording.R;
import com.videorecording.util.GlobalClass;

import java.util.Timer;
import java.util.TimerTask;

public class VideoControlFragment extends Fragment {

    private View view;
    private OnVideoControlFragmentListener listener;
    private Button fabRecordButton, fabPauseButton;
    private boolean isRecording;
    private boolean isPause;
    public static SwitchCompat flashmode;
    GlobalClass globalClass;
    private boolean hasFlash;

    public VideoControlFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.video_control_fragment, container, false);
        initialization(view);
        return view;
    }

    public void initialization(View view){
        // Initialize recording
        isRecording = false;
        globalClass =(GlobalClass)getActivity().getApplicationContext();
        hasFlash = getActivity().getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        globalClass.isFlashOn=false;

        // Get record button
        fabRecordButton = (Button) view.findViewById(R.id.fabRecordButton);
        fabPauseButton = (Button) view.findViewById(R.id.fabPauseButton);
        flashmode =(SwitchCompat)view.findViewById(R.id.flash);
        flashmode.setChecked(false);
        // Add click functionality to the button
        fabRecordButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                isRecording = changeRecordButtonState(isRecording);
                if(isRecording) {
                    fabPauseButton.setVisibility(View.VISIBLE);
                    listener.onRecordButtonPress();
                } else {
                    fabPauseButton.setVisibility(View.GONE);
                    listener.onStopButtonPress();
                }

            }
        });

        fabPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fabPauseButton.setEnabled(false);

                isPause = changePauseButtonState(isPause);
                if(isPause) {
                    fabRecordButton.setEnabled(false);
                    listener.onPauseButtonPress();
                } else {
                    fabRecordButton.setEnabled(true);
                    listener.onResumeButtonPress();
                }
                Handler handler= new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        fabPauseButton.setEnabled(true);
                    }
                }, 1000);
            }
        });

        eventListener();
    }
	public void eventListener(){

        flashmode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!hasFlash) {

                    AlertDialog alert = new AlertDialog.Builder(getActivity()).create();
                    alert.setMessage(getResources().getString(R.string.msg_flasherror));
                    alert.setButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getActivity().finish();
                        }
                    });
                    alert.show();
                    buttonView.setChecked(false);
                    return;
                }
                if (isChecked) {
                    listener.turnOnFlash();
                } else {
                    listener.turnOffFlash();
                }
            }
        });
    }


    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        if (context instanceof OnVideoControlFragmentListener) {
            listener = (OnVideoControlFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        if(isRecording) {
            isRecording = changeRecordButtonState(isRecording);
        }
    }

    /**
     * If button is recording, change icon into "||" icon
     * else change it into the "O" icon
     * @param isRecording
     */
    private boolean changeRecordButtonState(boolean isRecording) {

        // If previously is recording (icon is pause), change to play
        if(isRecording) {
            fabRecordButton.setText("START");
        } else {
            fabRecordButton.setText("STOP");
        }

        return !isRecording;
    }

    private boolean changePauseButtonState(boolean isPause) {

        // If previously is recording (icon is pause), change to play
        if(isPause) {
            fabPauseButton.setText("PAUSE");
        } else {
            fabPauseButton.setText("RESUME");
        }

        return !isPause;
    }

    public interface OnVideoControlFragmentListener {
        // Pause the video stream
        void onRecordButtonPress();
        // Stop the video stream
        void onStopButtonPress();
        void onPauseButtonPress();
        void onResumeButtonPress();
        void turnOnFlash();
        void turnOffFlash();
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

    }

    @Override
    public void onResume() {
        super.onResume();
        fabPauseButton.setText("PAUSE");
        fabPauseButton.setVisibility(View.GONE);
        isPause = false;
        fabRecordButton.setText("START");
        isRecording = false;
    }
}
