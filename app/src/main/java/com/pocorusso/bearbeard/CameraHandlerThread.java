package com.pocorusso.bearbeard;

import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

public class CameraHandlerThread extends HandlerThread {

    public interface CameraListener {
        void onCameraOpened(int cameraId, Camera camera);
    }

    private static String TAG = "CameraHandlerThread";
    private static final int CAMERA_OPEN = 0;
    private static final int SAVE_PICTURE = 1;
    private Context mContext;
    private Handler mRequestHandler;  //handler for worker thread
    private Handler mResponseHandler; //handler for UI thread

    private boolean mHasQuit = false;
    private Camera mCamera;
    private int mCameraId;
    private CameraListener mCameraListener; //listner to run on UI thread via response handler

    /**
     * Constructor
     *
     * @param name            name of the worker thread
     * @param context         activity context
     * @param responseHandler Handler to be created on the UI thread
     * @param cameraListener  listener for work be run on the UI thread
     */
    public CameraHandlerThread(String name, Context context, Handler responseHandler, CameraListener cameraListener) {
        super(name);
        mContext = context;
        mResponseHandler = responseHandler;
        mCameraListener = cameraListener;
    }


    /**
     * Set up the request handler to run on the worker thread
     */
    @Override
    protected void onLooperPrepared() {
        //running the request on the worker thread
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                switch (msg.what) {
                    case CAMERA_OPEN:
                        Log.d(TAG, "onLooperPrepared calling safeCameraOpen");
                        int cameraId = msg.arg1;
                        safeCameraOpen(cameraId);
                        break;
                    case SAVE_PICTURE: {
                        Log.d(TAG, "onLooperPrepared calling savePictureInPrivateStorage");
                        PictureUtils.savePictureInPrivateStorage(mContext, (byte[]) msg.obj);
                        break;
                    }
                    default:
                        //do nothing
                }
            }
        };
    }

    @Override
    public boolean quit() {
        Log.d(TAG, "Quitting");

        mHasQuit = true;
        return super.quit();
    }

    /**
     * Called from the UI thread to queue up work to
     * open the camera
     *
     * @param cameraId
     */
    public void queueOpenCamera(int cameraId) {
        Log.d(TAG, "Queue request to open camera");
        mRequestHandler.obtainMessage(CAMERA_OPEN, cameraId).sendToTarget();
    }

    /**
     * Releasing camera and preview at onPause
     */
    public void releaseCameraAndPreview() {
        if (mCamera != null) {
            Log.d(TAG, "releaseCameraAndPreview");
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private void safeCameraOpen(int cameraId) {
        Log.d(TAG, "safeCameraOpen");
        boolean qOpened = false;

        try {
            releaseCameraAndPreview();
            mCamera = Camera.open(cameraId);
            if (mCamera != null) {
                mCameraId = cameraId;
                notifyCameraOpened();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to open Camera");
            e.printStackTrace();
        }
    }

    private void notifyCameraOpened() {
        mResponseHandler.post(new Runnable() {
            public void run() {
                Log.d(TAG, "Camera opened. Calling listener.");
                if (!mHasQuit) {
                    mCameraListener.onCameraOpened(mCameraId, mCamera);
                }
            }
        });
    }

    public void takePicture() {
        if (mCamera != null) {
            mCamera.takePicture(mShutterCallback, mRawCallback, mJpegCallback);
        }
    }


    Camera.ShutterCallback mShutterCallback = new Camera.ShutterCallback() {
        public void onShutter() {
            //			 Log.d(TAG, "onShutter'd");
        }
    };

    Camera.PictureCallback mRawCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] bytes, Camera camera) {

        }
    };

    Camera.PictureCallback mJpegCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d(TAG, "onPictureTaken - jpeg");
            mRequestHandler.obtainMessage(SAVE_PICTURE, data).sendToTarget();
        }
    };




}