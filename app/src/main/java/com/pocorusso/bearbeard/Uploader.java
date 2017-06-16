package com.pocorusso.bearbeard;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;

import java.io.ByteArrayOutputStream;
import java.io.File;

public class Uploader {

    public interface UploadListener {
        void onUploaded(Bitmap bitmap);
        void onUploadError();
    }

    private static String TAG = "Uploader";
    private static String URL = "http://ec2-34-250-78-232.eu-west-1.compute.amazonaws.com/m2w";
    private static Uploader mInstance;
    private RequestQueue mQueue;

    /**
     * Singleton
     *
     * @param context Use an application context if you want the uploader to last
     *                the life time of the application
     */
    public static synchronized Uploader getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new Uploader(context);
        }
        return mInstance;
    }

    private Uploader(Context context) {
        mQueue = Volley.newRequestQueue(context);
    }

    public void cancelAll() {
        mQueue.cancelAll(TAG);
    }

    public void uploadFile(File file, final UploadListener uploadListener) {
        if (file == null) return;

        Log.d(TAG, "uploadFile file: " + file.getPath());
        //Convert bitmap to byte array
        Bitmap bitmap = PictureUtils.getScaledBitmap(file.getPath(),50, 50);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        final byte[] imageBytes = baos.toByteArray();

        ImageRequest postRequest = new ImageRequest(URL,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap response) {
                        // response
                        Log.d(TAG, "onResponse");
                        uploadListener.onUploaded(response);
                    }
                },
                100,100, ImageView.ScaleType.CENTER_INSIDE
                , Bitmap.Config.ARGB_8888
                , new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error.Response " + error.getMessage());
                        uploadListener.onUploadError();
                    }
                }
        ) {

            @Override
            public int getMethod() {
                return Method.POST;
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                return imageBytes;
            }

            @Override
            public String getBodyContentType() {
                return "image/jpeg";
            }
        };
        postRequest.setTag(TAG);
        mQueue.add(postRequest);
    }
}
