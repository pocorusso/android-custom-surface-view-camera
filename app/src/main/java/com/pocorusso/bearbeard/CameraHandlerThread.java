package com.pocorusso.bearbeard;

import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

public class CameraHandlerThread extends HandlerThread {

    private static int CAMERA_OPEN = 0;
    private Handler mRequestHandler;

    public interface CameraListener {
        void onCameraOpened();
    }

    private static String TAG = "CameraHandlerThread";
    private boolean mHasQuit = false;
    private Camera mCamera;
    private CameraListener mCameraListener;
    private Handler mResponseHandler; // this is created on the UI thread

    public CameraHandlerThread(String name, Handler responseHandler, CameraListener cameraListener) {
        super(name);
        mResponseHandler = responseHandler;
        mCameraListener = cameraListener;
    }


    @Override
    protected void onLooperPrepared() {
        //running the request on the CameraHandler thread
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == CAMERA_OPEN) {
                    Log.i(TAG, "Camera open requested.");
                    safeCameraOpen(0);
                }
            }
        };
    }

    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }

    public void openCamera() {
        mRequestHandler.obtainMessage(CAMERA_OPEN).sendToTarget();
    }

    public Camera getCamera() {
        return mCamera;
    }

    public void releaseCameraAndPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();

            mCamera = null;
        }
    }

    private boolean safeCameraOpen(int id) {
        boolean qOpened = false;

        try {
            releaseCameraAndPreview();
            mCamera = Camera.open(id);
            qOpened = (mCamera != null);

            notifyCameraOpened();
        } catch (Exception e) {
            Log.e(TAG, "failed to open Camera");
            e.printStackTrace();
        }

        return qOpened;
    }

    private void notifyCameraOpened(){
        mResponseHandler.post(new Runnable() {
            public void run() {
                if(!mHasQuit) {
                    mCameraListener.onCameraOpened();
                }
            }
        });
    }


}