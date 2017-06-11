package com.pocorusso.bearbeard;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class CameraFragment extends Fragment implements CameraHandlerThread.CameraListener {
    private static String TAG = "CameraFragment";
    Preview mPreview;
    Button mBtnTakePicture;
    CameraHandlerThread mCameraHandlerThread;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Handler responseHandler = new Handler();//creating the response handler on the UI thread
        mCameraHandlerThread = new CameraHandlerThread(TAG, responseHandler, this);
        mCameraHandlerThread.start();
        mCameraHandlerThread.getLooper();
        Log.i(TAG, "CameraHandlerThread started.");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_camera, container, false);
        mPreview = new Preview(getActivity(), (SurfaceView)v.findViewById(R.id.surface_view_preview));

        mBtnTakePicture = (Button)v.findViewById(R.id.btn_take_picture);
        mBtnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //take picture here;
            }
        });

        mCameraHandlerThread.openCamera();
        return v;
    }

    @Override
    public void onCameraOpened() {
        mPreview.setCamera(mCameraHandlerThread.getCamera());
    }

    @Override
    public void onPause() {
        super.onPause();
        mPreview.setCamera(null);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCameraHandlerThread.quit();
        Log.i(TAG, "Camera thread quit.");
    }


}
