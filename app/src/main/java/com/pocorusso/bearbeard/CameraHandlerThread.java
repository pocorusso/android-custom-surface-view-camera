package com.pocorusso.bearbeard;

import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;

public class CameraHandlerThread extends HandlerThread {

    public interface CameraListener {
        void onCameraOpened();
        void onPictureReady(File file);
    }

    private static String TAG = "CameraHandlerThread";
    private static final int CAMERA_OPEN = 0;
    private static final int SAVE_PICTURE = 1;
    private Context mContext;
    private Handler mRequestHandler;  //handler for worker thread
    private Handler mResponseHandler; //handler for UI thread

    private boolean mHasQuit = false;
    private CameraAdapter mCameraAdapter;

    //private Camera mCamera;
    //private int mCameraId;
    private CameraListener mCameraListener; //listener to run on UI thread via response handler

    private LinkedBlockingQueue<Integer> mRequestOpenCameraQueue = new LinkedBlockingQueue<Integer>();

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
        mCameraAdapter = CameraAdapter.getInstance(context);
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
                        Log.d(TAG, "onLooperPrepared opening camera");
                        int cameraId = msg.arg1;
                        mCameraAdapter.openCamera(cameraId);
                        if (mCameraAdapter.isValid()) {
                            notifyCameraOpened();
                        }
                        break;
                    case SAVE_PICTURE: {
                        Log.d(TAG, "onLooperPrepared calling savePictureInPrivateStorage");
                        File file = PictureUtils.savePictureInPrivateStorage(mContext, (byte[]) msg.obj);
                        if(file!=null) {
                            notifiedPictureReady(file);
                        }
                        break;
                    }
                    default:
                        //do nothing
                }
            }
        };

        //Just in case the UI tried to make a request before looper is ready.
        //the request is stashed in the queue
        if(mRequestOpenCameraQueue.size()>0) {
            try {
                Log.d(TAG, "mRequestOpenCameraQueue take");
                int cameraId = mRequestOpenCameraQueue.take();
                mRequestHandler.obtainMessage(CAMERA_OPEN, cameraId).sendToTarget();
            }catch(InterruptedException e) {
                Log.d(TAG, "failed to take from mRequestOpenCameraQueue");
            }
        }
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
     * @param cameraId the camera id to be open.
     */
    public void queueOpenCamera(int cameraId) {
        Log.d(TAG, "Queue request to open camera");

        //a bit of complication here. It is possible that
        //we are queuing up the request to open the camera
        //before the looper is ready, in which case
        //onLooperPrepared has not been called and mRequestHandler
        //have not been initialized. So we stash the request somewhere
        //and then read it out again when looper is ready
        if(mRequestHandler == null) {
            mRequestOpenCameraQueue.add(cameraId);
        } else {
            mRequestHandler.obtainMessage(CAMERA_OPEN, cameraId).sendToTarget();
        }
    }




    private void notifyCameraOpened() {
        mResponseHandler.post(new Runnable() {
            public void run() {
                Log.d(TAG, "Camera opened. Calling listener.");
                if (!mHasQuit) {
                    mCameraListener.onCameraOpened();
                }
            }
        });
    }

    private void notifiedPictureReady(final File file) {
        mResponseHandler.post(new Runnable() {
            public void run() {
                Log.d(TAG, "Picture ready. Calling listener.");
                if (!mHasQuit) {
                    mCameraListener.onPictureReady(file);
                }
            }
        });
    }

    public void takePicture() {
        if (mCameraAdapter.isValid()) {
            mCameraAdapter.takePicture(
                    new Camera.ShutterCallback() { //shutter on click listening
                        @Override
                        public void onShutter() {
                            //do nothing
                        }
                    }, new Camera.PictureCallback() { //raw picture hanlding
                        @Override
                        public void onPictureTaken(byte[] bytes, Camera camera) {
                           //do nothing
                        }
                    }, new Camera.PictureCallback() { //jpeg picture handling
                        @Override
                        public void onPictureTaken(byte[] bytes, Camera camera) {
                            //call back for handling jpeg picture
                            Log.d(TAG, "onPictureTaken - jpeg");
                            mRequestHandler.obtainMessage(SAVE_PICTURE, bytes).sendToTarget();

                            //For some reason the camera does not stop the preview after
                            //take picture after the first time so we have to
                            //manually stop the preview? Something to look into
                            mCameraAdapter.releaseCamera();
                        }
                    });
        }
    }
}