package com.pocorusso.bearbeard;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.File;

import static android.app.Activity.RESULT_OK;

public class CameraFragment extends Fragment
        implements CameraHandlerThread.CameraListener{

    private static String TAG = "CameraFragment";
    private static int PICK_GALLERY_IMAGE = 1;
    private static int DEFAULT_CAMERA_ID = 0;

    Preview mPreview;
    ImageButton mBtnTakePicture;
    ImageButton mBtnUpload;
    ImageButton mBtnRefresh;
    ImageButton mBtnGallery;
    ImageView mImageViewResult;

    CameraHandlerThread mCameraHandlerThread;
    Uploader mUploader;

    File mFile;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate started.");
        super.onCreate(savedInstanceState);
        mUploader = Uploader.getInstance(getActivity().getApplicationContext());

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
        obtainCamera();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView started.");
        View v = inflater.inflate(R.layout.fragment_camera, container, false);
        mPreview = (Preview) v.findViewById(R.id.preview_camera);
        mImageViewResult = (ImageView) v.findViewById(R.id.image_view_result);


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
                   Uploader.getInstance(getActivity().getApplicationContext())
                           .uploadFile(mFile, new Uploader.UploadListener() {
                               @Override
                               public void onUploaded(Bitmap bitmap) {

                                   if (bitmap!=null) {
                                       mImageViewResult.setVisibility(View.VISIBLE);
                                       mImageViewResult.setImageBitmap(bitmap);

                                       //we have a preview bitmap
                                       //we can release the camera
                                       releaseCamera();
                                   }
                               }

                               @Override
                               public void onUploadError() {
                                   //TODO handle error
                               }
                           });
                    //TODO start progress spinner
                }

            }
        });

        mBtnRefresh = (ImageButton) v.findViewById(R.id.btn_refresh);
        mBtnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //retake picture
                obtainCamera();
                setButtonsState(ButtonsState.TAKE_PICTURE);
            }
        });

        mBtnGallery = (ImageButton) v.findViewById(R.id.btn_gallery);
        mBtnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Create the Intent for Image Gallery.
                Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                // Start new activity with the LOAD_IMAGE_RESULTS to handle back the results when image is picked from the Image Gallery.
                startActivityForResult(i, PICK_GALLERY_IMAGE);
            }
        });
        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //handle return from activity to pick gallery image
        if (requestCode == PICK_GALLERY_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri pickedImage = data.getData();
            String[] filePath = { MediaStore.Images.Media.DATA };
            Cursor cursor = getActivity().getContentResolver().query(pickedImage, filePath, null, null, null);
            cursor.moveToFirst();
            String imagePath = cursor.getString(cursor.getColumnIndex(filePath[0]));

            //set the image picked into the resulting view
            mImageViewResult.setImageBitmap(BitmapFactory.decodeFile(imagePath));
            mImageViewResult.setVisibility(View.VISIBLE);
            cursor.close();
        }
    }

    /**
     * Clean up camera during onPause
     */
    @Override
    public void onPause() {
        Log.d(TAG, "onPause started.");
        super.onPause();
        releaseCamera();
    }


    private void obtainCamera(){
        //open camera on a background thread via an handler
        mCameraHandlerThread.queueOpenCamera(DEFAULT_CAMERA_ID);
    }

    private void releaseCamera() {
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
        mUploader.cancelAll();
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
        releaseCamera();
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

                mImageViewResult.setVisibility(View.GONE);
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
