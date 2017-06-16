package com.pocorusso.bearbeard;

import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import java.io.File;

public class CameraFragment extends Fragment
        implements CameraHandlerThread.CameraListener,
        CameraHandlerThread.UploadListener {

    private static String TAG = "CameraFragment";
    private static int DEFAULT_CAMERA_ID = 0;

    Preview mPreview;
    ImageButton mBtnTakePicture;
    ImageButton mBtnUpload;
    ImageButton mBtnRefresh;
    CameraHandlerThread mCameraHandlerThread;

    File mFile;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate started.");
        super.onCreate(savedInstanceState);

        Handler responseHandler = new Handler();//creating the response handler on the UI thread
        mCameraHandlerThread = new CameraHandlerThread(TAG, getActivity(), responseHandler, this);
        mCameraHandlerThread.start();
        mCameraHandlerThread.getLooper();
        Log.d(TAG, "camera handler thread started.");

    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume started.");
        super.onResume();

        //open camera on a background thread via an handler
        mCameraHandlerThread.queueOpenCamera(DEFAULT_CAMERA_ID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView started.");
        View v = inflater.inflate(R.layout.fragment_camera, container, false);
        mPreview = (Preview)v.findViewById(R.id.preview_camera);

        mBtnTakePicture = (ImageButton)v.findViewById(R.id.btn_take_picture);
        mBtnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //reset picture
                mFile = null;

                //take picture here;
                mCameraHandlerThread.takePicture();

            }
        });

        mBtnUpload = (ImageButton) v.findViewById(R.id.btn_upload);
        mBtnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //upload picture for processing
                if(mFile!=null) {
                   Uploader.getInstance(getActivity().getApplicationContext()).uploadFile(mFile);
                    //TODO start progress spinner
                }

            }
        });

        mBtnRefresh = (ImageButton) v.findViewById(R.id.btn_refresh);
        mBtnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //retake picture
                mCameraHandlerThread.cameraStartPreview();

                setButtonsState(ButtonsState.TAKE_PICTURE);
            }
        });
        return v;
    }

    /**
     * Clean up camera during onPause
     */
    @Override
    public void onPause() {
        Log.d(TAG, "onPause started.");
        super.onPause();
        mCameraHandlerThread.releaseCameraAndPreview();
        mPreview.setCamera(0,null);
    }

    /**
     * Clean up thread during onDestroy
     */
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy started.");
        super.onDestroy();
        mCameraHandlerThread.quit();
        Log.i(TAG, "Camera thread quit.");
    }


    private void refreshGallery(File file) {
        Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        getActivity().sendBroadcast(mediaScanIntent);
    }

    /**
     * Implementation of a method on CameraListener.
     * This is always run the UI thread
     */
    @Override
    public void onCameraOpened(int cameraId, Camera camera) {
        Log.d(TAG, "onCameraOpened started.");
        mPreview.setCamera(cameraId, camera);
    }

    @Override
    public void onPictureReady(File file) {
        mFile = file;
        setButtonsState(ButtonsState.UPLOAD);
    }

    @Override
    public void onUploaded(File file) {
        //TODO open file and display it.
    }

    private static enum ButtonsState {
        TAKE_PICTURE,
        UPLOAD
    }
    private void setButtonsState(ButtonsState state) {
        switch(state) {
            case TAKE_PICTURE:
                mBtnTakePicture.setClickable(true);
                mBtnTakePicture.setVisibility(View.VISIBLE);

                mBtnRefresh.setClickable(false);
                mBtnRefresh.setVisibility(View.INVISIBLE);

                mBtnUpload.setClickable(false);
                mBtnUpload.setVisibility(View.INVISIBLE);
                break;
            case UPLOAD:
                mBtnTakePicture.setClickable(false);
                mBtnTakePicture.setVisibility(View.INVISIBLE);

                mBtnRefresh.setClickable(true);
                mBtnRefresh.setVisibility(View.VISIBLE);

                mBtnUpload.setClickable(true);
                mBtnUpload.setVisibility(View.VISIBLE);
                break;
            default:
                //do nothing
        }
    }



}
