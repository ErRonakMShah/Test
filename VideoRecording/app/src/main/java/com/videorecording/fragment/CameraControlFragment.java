package com.videorecording.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Toast;

import com.videorecording.R;
import com.videorecording.util.GlobalClass;

public class CameraControlFragment extends Fragment implements View.OnClickListener,CompoundButton.OnCheckedChangeListener{

    private final String TAG = CameraControlFragment.class.getSimpleName();

    private GlobalClass globalClass;
    private ImageButton btn_takePicture,btn_previewImage;
    public static SwitchCompat flashmode, burstmode;
    private boolean hasFlash,isFlashOn,isBurstMode;

    private View view;
    private onCameraListener listener;

    public CameraControlFragment() {
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
        view = inflater.inflate(R.layout.camera_control_fragment, container, false);
        initialization(view);
        eventListener(view);
        return view;
    }


    @Override
    public void onStart() {
        super.onStart();
        if(btn_takePicture!=null) {
            btn_takePicture.setEnabled(true);
        }
    }

    public void initialization(View view){
        globalClass=(GlobalClass)getActivity().getApplicationContext();
        hasFlash = getActivity().getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        globalClass.isFlashOn=false;
        isBurstMode=false;
        btn_takePicture=(ImageButton)view.findViewById(R.id.takeShot);
        btn_previewImage=(ImageButton)view.findViewById(R.id.previewImage);
        flashmode=(SwitchCompat)view.findViewById(R.id.flashmode);
        burstmode=(SwitchCompat)view.findViewById(R.id.burstmode);
        btn_takePicture.setOnClickListener(this);
        btn_previewImage.setOnClickListener(this);
        flashmode.setChecked(false);
        burstmode.setChecked (false);
    }

    public void eventListener(View view){

        burstmode.setOnCheckedChangeListener (this);
        flashmode.setOnCheckedChangeListener(this);
        btn_takePicture.setOnClickListener(this);
        btn_previewImage.setOnClickListener(this);
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (globalClass != null && isBurstMode) {
                    Toast.makeText(getActivity().getApplicationContext(), "Burst mode start", Toast.LENGTH_LONG).show();
                    globalClass.burstcount = 0;
                    globalClass.isBurstModeStart = true;
                }
                return false;
            }
        });
    }

    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        if (context instanceof onCameraListener) {
            listener = (onCameraListener) context;
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
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.burstmode:
                if (globalClass != null) {
                    if (isChecked) {
                        isBurstMode = true;
                    } else {
                        isBurstMode = false;
                    }
                    globalClass.burstcount = 0;
                }
                break;

            case R.id.flashmode:
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
                break;
            default:
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.takeShot:
                v.setEnabled(false);
                if(globalClass!=null){
                    globalClass.validateStoragePath();
                }
                Log.d(TAG,">>>>>>>>>>>>>>>>>>>>>. button false>>>>>>>>>>>>>>>>>>>>>>>>>");


                listener.onTakePictureStart();

                break;
            case R.id.previewImage:
                try{
                    if(flashmode!=null){
                        flashmode.setChecked(false);
                    }
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse("file://" + globalClass.STORAGE_PATH +"/"+ globalClass.latestimagename), "image/*");
                    startActivity(intent);
                }
                catch (Exception e){
                    Log.e(TAG,"Exception : To open image gallery"+e.toString());
                }

                break;
        }
    }


    public interface onCameraListener {
        void onBurstModeStart();
        void onTakePictureStart();
        void turnOnFlash();
        void turnOffFlash();
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){

        }
    }
public void enableTakeShot(){
//    Toast.makeText(getActivity().getApplicationContext(),"fragment called",Toast.LENGTH_LONG).show();
    btn_takePicture.setEnabled(true);
}


    public void stopBurstMode(){
        Log.d(TAG,"Stop Burst Mode Toast....");
        Toast.makeText(getActivity().getApplicationContext(), "Burst mode stop", Toast.LENGTH_LONG).show();
    }
}
