package com.pocorusso.bearbeard;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.content.ContentValues.TAG;

public class PictureUtils {
    public static Bitmap getScaledBitmap(String path, int destWidth, int destHeight) {
        //Read in the dimensions of the image on disk
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        float srcWidth = options.outWidth;
        float srcHeight = options.outHeight;

        //figure out how much to scale down by
        int inSampleSize = 1;
        if (srcHeight > destHeight || srcWidth > destWidth) {
            if (srcWidth> srcHeight) {
                inSampleSize =  Math.round(srcHeight/destHeight);
            } else {
                inSampleSize = Math.round(srcWidth/destWidth);
            }
        }

        options = new BitmapFactory.Options();
        options.inSampleSize = inSampleSize;

        return BitmapFactory.decodeFile(path, options);
    }

    public static Bitmap getScaledBitmap(String path, Activity activity) {
        Point size = new Point();
        activity.getWindowManager().getDefaultDisplay().getSize(size);

        return getScaledBitmap(path, size.x, size.y);
    }


    protected static File savePictureInPrivateStorage(Context context, byte[] data) {
        File savedFile = null;
        try {
            savePicture(PictureUtils.getPrivateFileHandle(context), data);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Failed to open private file handle.");
            e.printStackTrace();
        }

        return savedFile;
    }

    protected static File savePictureInPublicStorage(Context context, byte[] data) {
        File savedFile = null;
        try {
            updateMediaScanner(context, savePicture(PictureUtils.getPublicFileHandle(), data));
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Failed to open private file handle.");
            e.printStackTrace();
        }

        return savedFile;
    }

    private static File savePicture(File file, byte[] data) {
        FileOutputStream outStream = null;

        // Write to SD Card
        try {
            outStream = new FileOutputStream(file);
            outStream.write(data);
            outStream.flush();
            outStream.close();

            //refreshGallery(outFile);
            //TODO need to notify UI thread we are done.

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
        }

        return file;
    }

    /**
     * @return
     * @throws FileNotFoundException when we get directory handle
     */
    private static File getPublicFileHandle() throws FileNotFoundException {
        File externalFilesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);

        String fileName = String.format("BearBeard" + File.separator + "IMG_%d.jpg", System.currentTimeMillis());
        if (externalFilesDir == null) {
            throw new FileNotFoundException("Failed to get directory handle to DIRECTORY_PICTUERS");
        }

        Log.d(TAG, "Creating file: " + externalFilesDir.getAbsolutePath() + fileName);
        return new File(externalFilesDir, fileName);
    }

    /**
     * @return
     * @throws FileNotFoundException when we get directory handle
     */
    private static File getPrivateFileHandle(Context context) throws FileNotFoundException {
        File externalFilesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        String fileName = String.format(File.separator+"IMG_%d.jpg", System.currentTimeMillis());
        if (externalFilesDir == null) {
            throw new FileNotFoundException("Failed to get directory handle to DIRECTORY_PICTUERS");
        }

        Log.d(TAG, "Creating file: " + externalFilesDir.getAbsolutePath() + fileName);
        return new File(externalFilesDir, fileName);
    }




    private static void updateMediaScanner(Context context, File outputFile) {
        // Tell the media scanner about the new file so that it is
        // immediately available to the user.
        MediaScannerConnection.scanFile(context,
                new String[]{outputFile.toString()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }


}
