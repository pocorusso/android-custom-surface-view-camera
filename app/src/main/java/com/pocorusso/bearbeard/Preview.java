package com.pocorusso.bearbeard;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import java.io.IOException;
import java.util.List;

class Preview extends ViewGroup implements SurfaceHolder.Callback {

    private static String TAG = "Preview";
    SurfaceView mSurfaceView;
    SurfaceHolder mHolder;
    Camera mCamera;
    Size mPreviewSize;


    Preview(Context context) {
        super(context);
        init(context);
    }

    Preview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    Preview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mSurfaceView = new SurfaceView(context);
        this.addView(mSurfaceView);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }



    public void setCamera(int cameraId, Camera camera) {
        Log.d(TAG, "setCamera called. cameraId =" + cameraId);
        if (mCamera == camera) {
            return;
        }

        //stopPreviewAndFreeCamera();

        mCamera = camera;

        if (mCamera != null) {
            setCameraDisplayOrientation(cameraId, camera);
            Camera.Parameters params = mCamera.getParameters();
            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                // set the focus mode
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }

            // set Camera parameters
            mCamera.setParameters(params);

            requestLayout();

            try {
                mCamera.setPreviewDisplay(mHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.startPreview();
        }
    }


    private void setCameraDisplayOrientation(int cameraId, android.hardware.Camera camera) {

        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = ((Activity) getContext()).getWindowManager().getDefaultDisplay()
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
        camera.setDisplayOrientation(result);
    }


    /**
     * When this function returns, mCamera will be null.
     */
    private void stopPreviewAndFreeCamera() {

        if (mCamera != null) {
            // Call stopPreview() to stop updating the preview surface.
            mCamera.stopPreview();

            // Important: Call release() to release the camera for use by other
            // applications. Applications should release the camera immediately
            // during onPause() and re-open() it during onResume()).
            mCamera.release();

            mCamera = null;
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        if (changed && getChildCount() > 0) {
            Log.d(TAG, "onLayout");
            final View child = getChildAt(0);

            final int width = r - l;
            final int height = b - t;

            int previewWidth = width;
            int previewHeight = height;
            if (mPreviewSize != null) {
                previewWidth = mPreviewSize.width;
                previewHeight = mPreviewSize.height;
            }

            Log.d(TAG, "onLayout previewWidth=" + previewWidth + ", previewHeight="+previewHeight);
            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                Log.d(TAG, "onLayout scaledChildWidth=" + scaledChildWidth);
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                Log.d(TAG, "onLayout scaledChildHeight=" + scaledChildHeight);
                child.layout(0, (height - scaledChildHeight) / 2,
                        width, (height + scaledChildHeight) / 2);
            }
        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.d(TAG, "onMeasure called.");

        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        //Requirement to set the measured dimenstion
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        if (mCamera != null) {
            List<Camera.Size> supportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            if (supportedPreviewSizes != null) {
                Log.d(TAG, "measuredSize w=" + width + ", h=" + height);
                Size newSize = getOptimalPreviewSize(supportedPreviewSizes, width, height);
                Log.d(TAG, "newSize w=" + newSize.width + ", h=" + newSize.height);
                if (newSize != mPreviewSize) {
                    mPreviewSize = newSize;
                    // stop preview before making changes
                    setPreviewSize();
                }
            }

        }

    }

    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        Log.d(TAG, "getOptimalPreviewSize");
        final double ASPECT_TOLERANCE = 0.1;

        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        double targetRatio;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            targetRatio = (double) h / w;
        } else {
            targetRatio = (double) w / h;
        }

        Log.d(TAG, "TargetRatio=" + targetRatio);

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            Log.d(TAG, "Trying ratio=" + ratio + ", w=" + size.width + ", h=" + size.height);

           //find the optimal ratio
            if(Math.abs(ratio - targetRatio) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(ratio - targetRatio);
            }
        }

        return optimalSize;
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated called.");
        if (mCamera != null) {
            requestLayout();

            try {
                mCamera.setPreviewDisplay(mHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // The Surface has been created, acquire the camera and tell it where
        // to draw.
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        Log.d(TAG, "surfaceChanged called.");
        if (mCamera != null) {

            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.
            if (mHolder.getSurface() == null || mPreviewSize == null) {
                // preview surface does not exist
                return;
            }


            // start preview with new settings
            try {
                mCamera.setPreviewDisplay(mHolder);
            } catch (Exception e) {
                Log.d(TAG, "Error starting camera preview: " + e.getMessage());
            }

            setPreviewSize();
        }
    }

    private void setPreviewSize() {
        if (mPreviewSize != null) {
            Log.d(TAG, "setPreviewSize: w=" + mPreviewSize.width + " ,h=" + mPreviewSize.height);
            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e) {
                // ignore: tried to stop a non-existent preview
            }

            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);

            mCamera.setParameters(parameters);

            // set preview size and make any resize, rotate or
            // reformatting changes here
            mCamera.startPreview();

            requestLayout();
        } else {
            Log.d(TAG, "setPreviewSize mPreviewSize is null");
        }


    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "surfaceDestroyed called.");
        // Surface will be destroyed when we return, so stop the preview.
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }
}
