package com.pocorusso.bearbeard;

import android.content.Context;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

class Preview extends ViewGroup implements SurfaceHolder.Callback {

    private static String TAG = "Preview";
    SurfaceView mSurfaceView;
    SurfaceHolder mHolder;
    CameraAdapter mCameraAdapter;


    public Preview(Context context) {
        super(context);
        init(context);
    }

    public Preview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public Preview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mSurfaceView = new SurfaceView(context);
        this.addView(mSurfaceView);

        mCameraAdapter = CameraAdapter.getInstance(getContext());

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void initCameraSurfaceHolder() {
        mCameraAdapter.setSurfaceHolder(mHolder);
        requestLayout();
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
            Size previewSize = mCameraAdapter.getPreviewSize();
            if (previewSize != null) {
                previewWidth = previewSize.width;
                previewHeight = previewSize.height;
            }

            Log.d(TAG, "onLayout previewWidth=" + previewWidth + ", previewHeight=" + previewHeight);
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

        //Requirement to set the measured dimenstion
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        mCameraAdapter.adjustPreviewSize(width, height);
    }



    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated called.");
        if (mCameraAdapter.isValid()) {
            requestLayout();
            //mCameraAdapter.setDisplayHolder(mHolder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        Log.d(TAG, "surfaceChanged called.");
        if (mCameraAdapter.isValid()) {

            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.
            if (mHolder.getSurface() == null) {
                // preview surface does not exist
                return;
            }

            // start preview with new settings
            //mCameraAdapter.setDisplayHolder(mHolder);
            mCameraAdapter.adjustCameraPreviewSize();
            requestLayout();
        }
    }



    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "surfaceDestroyed called.");
        // Surface will be destroyed when we return, so stop the preview.
        mCameraAdapter.releaseCamera();
    }
}
