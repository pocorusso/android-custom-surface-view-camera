package com.pocorusso.bearbeard;


import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.List;

public class CameraAdapter {

    private static String TAG = "CameraAdapter";
    private static CameraAdapter mInstance = null;
    private static Context mContext;
    private static Camera mCamera;
    private static int mCameraId;
    private static Camera.Size mPreviewSize;
    private static boolean mIsPreviewRunning;


    public static synchronized CameraAdapter getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new CameraAdapter(context);
        }
        return mInstance;
    }

    private CameraAdapter(Context context) {
        mContext = context;
        mCamera = null;
        mIsPreviewRunning = false;
    }

    public boolean isValid() {
        return mCamera == null || mPreviewSize == null;
    }

    public void openCamera(int cameraId) {
        mCameraId = cameraId;
        mCamera = safeCameraOpen(cameraId);
    }

    public void releaseCamera() {
        if (mCamera != null) {
            Log.d(TAG, "release camera");
            this.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public void takePicture(Camera.ShutterCallback shutterCallback,
                            Camera.PictureCallback rawPictureCallback,
                            Camera.PictureCallback jpegPictureCallback) {
        if (mCamera != null) {
            mCamera.takePicture(shutterCallback, rawPictureCallback, jpegPictureCallback);
        }

    }

    /**
     * The calling thread should already stop and release the camera
     * before they set the camera
     */
    public void setSurfaceHolder(SurfaceHolder holder) {
        Log.d(TAG, "setSurfaceHolder");

        if (mCamera != null) {
            setCameraDisplayOrientation();
            Camera.Parameters params = mCamera.getParameters();
            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                // set the focus mode
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }

            // set Camera parameters
            mCamera.setParameters(params);

            this.setDisplayHolder(holder);

            Log.d(TAG, "setSurfaceHolder calling startPreview");
            this.startPreview();
        }
    }

    private void setDisplayHolder(SurfaceHolder holder) {
        Log.d(TAG, "setDisplayHolder called");
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            Log.e(TAG, "Failed to setPreviewDisplay.");
            e.printStackTrace();
        }
    }

    public Camera.Size getPreviewSize() {
        return mPreviewSize;
    }

    public void adjustCameraPreviewSize() {
        if (mPreviewSize != null) {
            Log.d(TAG, "adjustCameraPreviewSize: w=" + mPreviewSize.width + " ,h=" + mPreviewSize.height);
            // stop preview before making changes
            this.stopPreview();

            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);

            mCamera.setParameters(parameters);

            // set preview size and make any resize, rotate or
            // reformatting changes here
            Log.d(TAG, "adjustCameraPreviewSize startPreview");
            this.startPreview();

        } else {
            Log.d(TAG, "adjustCameraPreviewSize mPreviewSize is null");
        }
    }

    /**
     * Recalculate the optimal preview size base on given width and height
     *
     * @param width
     * @param height
     */
    public void adjustPreviewSize(int width, int height) {
        if (mCamera != null) {

            Log.d(TAG, "adjustPreviewSize");
            List<Camera.Size> supportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            if (supportedPreviewSizes != null) {
                Log.d(TAG, "measuredSize w=" + width + ", h=" + height);
                Camera.Size newSize = getOptimalPreviewSize(supportedPreviewSizes, width, height);
                Log.d(TAG, "newSize w=" + newSize.width + ", h=" + newSize.height);
                if (newSize != mPreviewSize) {
                    mPreviewSize = newSize;
                }
            }
        }
    }

    private void stopPreview() {
        if (mCamera != null && mIsPreviewRunning) {
            try {
                mCamera.stopPreview();
                Log.d(TAG, "stopped preview");
                mIsPreviewRunning = false;
            } catch (Exception e) {
                Log.d(TAG, "Tried to stop a non-existant preview. Ignoring");
            }
        }
    }

    private void startPreview() {
        if (mCamera != null && !mIsPreviewRunning) {
            Log.d(TAG, "startPreview");
            mCamera.startPreview();
            mIsPreviewRunning = true;
        }
    }

    /**
     * Compensate for the camera angle and set it to mirror image because
     * that's what people expect.
     *
     */
    private void setCameraDisplayOrientation() {
        if(mCamera == null) {
            Log.d(TAG,"Cannot set orientation. mCamera is null.");
            return;
        }

        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(mCameraId, info);
        int rotation = ((Activity) mContext).getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        Log.d(TAG, "getOptimalPreviewSize");
        final double ASPECT_TOLERANCE = 0.1;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        //The supported sizes we get from the camera doesn't change w and h when
        //the phone rotate, therefore we have to adjust our ratio when
        //the app rotate in order to find the optimal ratio.
        double targetRatio;
        if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            targetRatio = (double) h / w;
        } else {
            targetRatio = (double) w / h;
        }

        Log.d(TAG, "TargetRatio=" + targetRatio);

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            //Log.d(TAG, "Trying ratio=" + ratio + ", w=" + size.width + ", h=" + size.height);

            //find the optimal ratio
            if (Math.abs(ratio - targetRatio) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(ratio - targetRatio);
            }
        }

        return optimalSize;
    }


    private Camera safeCameraOpen(int cameraId) {
        Log.d(TAG, "safeCameraOpen");
        Camera camera = null;
        try {
            releaseCamera(); //close current camera
            camera = Camera.open(cameraId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open Camera");
            e.printStackTrace();
        }
        return camera;
    }
}
