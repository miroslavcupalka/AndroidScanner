package com.scanlibrary;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScanActivity extends Activity {

    // ===========================================================
    // Constants
    // ===========================================================


    public static final String RESULT_IMAGE_PATH = "imgPath";

    private static final int TAKE_PHOTO_REQUEST_CODE = 815;
    private static final String SAVED_ARG_TAKEN_PHOTO_LOCATION = "taken_photo_loc";


    // ===========================================================
    // Fields
    // ===========================================================

    private final ViewHolder viewHolder = new ViewHolder();
    private String takenPhotoLocation;

    private Bitmap bitmap;

    // ===========================================================
    // Constructors
    // ===========================================================

    // ===========================================================
    // Getters & Setters
    // ===========================================================

    // ===========================================================
    // Methods for/from SuperClass/Interfaces
    // ===========================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        viewHolder.preapre(findViewById(android.R.id.content));

        if (savedInstanceState != null) {
            takenPhotoLocation = savedInstanceState.getString(SAVED_ARG_TAKEN_PHOTO_LOCATION);
        }

        if (takenPhotoLocation == null) {
            takePhoto();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == TAKE_PHOTO_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                showImage();
//                Intent i = new Intent();
//                i.putExtra(RESULT_IMAGE_PATH, takenPhotoLocation);
//                setResult(Activity.RESULT_OK, i);
//                finish();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                removeFile(takenPhotoLocation);
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(SAVED_ARG_TAKEN_PHOTO_LOCATION, takenPhotoLocation);
        super.onSaveInstanceState(outState);
    }
    // ===========================================================
    // Methods
    // ===========================================================

    private void setBitmap(Bitmap original) {
        Bitmap scaledBitmap = scaledBitmap(original, sourceFrame.getWidth(), sourceFrame.getHeight());
        sourceImageView.setImageBitmap(scaledBitmap);
        Bitmap tempBitmap = ((BitmapDrawable) sourceImageView.getDrawable()).getBitmap();
        Map<Integer, PointF> pointFs = getEdgePoints(tempBitmap);
        polygonView.setPoints(pointFs);
        polygonView.setVisibility(View.VISIBLE);
        int padding = (int) getResources().getDimension(R.dimen.scanPadding);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(tempBitmap.getWidth() + 2 * padding, tempBitmap.getHeight() + 2 * padding);
        layoutParams.gravity = Gravity.CENTER;
        polygonView.setLayoutParams(layoutParams);
    }

    private void showImage() {
        bitmap = getBitmapFromLocation(takenPhotoLocation);
        List<PointF> points = getContourEdgePoints(bitmap);

        Map<Integer, PointF> pointsMap = new HashMap<>(4);
        pointsMap.put(0, points.get(0));
        pointsMap.put(1, points.get(1));
        pointsMap.put(2, points.get(2));
        pointsMap.put(3, points.get(3));

        Bitmap cropedBitmap = getScannedBitmap(bitmap, pointsMap);
        Bitmap scaledBitmap = scaleBitmap(cropedBitmap, 1000, 1000);

        viewHolder.image.setImageBitmap(scaledBitmap);
    }

    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File photoFile = createImageFile();
        takenPhotoLocation = photoFile.getAbsolutePath();

        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
        startActivityForResult(takePictureIntent, TAKE_PHOTO_REQUEST_CODE);
    }

    private File createImageFile() {
        File storageDir = getExternalFilesDir("images");
        if (storageDir == null) {
            throw new RuntimeException("Not able to get to External storage");
        }
        File image = new File(storageDir, "takenimgage.jpg");

        return image;
    }

    private void removeFile(String absoluteLocation) {
        if (absoluteLocation == null) return;

        File f = new File(absoluteLocation);
        if (f.exists()) {
            f.delete();
        }
    }

    private List<PointF> getContourEdgePoints(Bitmap tempBitmap) {
        float[] points = getPoints(tempBitmap);
        float x1 = points[0];
        float x2 = points[1];
        float x3 = points[2];
        float x4 = points[3];

        float y1 = points[4];
        float y2 = points[5];
        float y3 = points[6];
        float y4 = points[7];

        List<PointF> pointFs = new ArrayList<>();
        pointFs.add(new PointF(x1, y1));
        pointFs.add(new PointF(x2, y2));
        pointFs.add(new PointF(x3, y3));
        pointFs.add(new PointF(x4, y4));
        return pointFs;
    }

    private Bitmap getScannedBitmap(Bitmap original, Map<Integer, PointF> points) {
        int width = original.getWidth();
        int height = original.getHeight();

        float x1 = (points.get(0).x);
        float x2 = (points.get(1).x);
        float x3 = (points.get(2).x);
        float x4 = (points.get(3).x);
        float y1 = (points.get(0).y);
        float y2 = (points.get(1).y);
        float y3 = (points.get(2).y);
        float y4 = (points.get(3).y);
        Log.d("", "POints(" + x1 + "," + y1 + ")(" + x2 + "," + y2 + ")(" + x3 + "," + y3 + ")(" + x4 + "," + y4 + ")");
        Bitmap _bitmap = getScannedBitmap(original, x1, y1, x2, y2, x3, y3, x4, y4);
        return _bitmap;
    }

    private Bitmap getBitmapFromLocation(String absLocation) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeFile(absLocation, options);
    }

    private Bitmap scaleBitmap(Bitmap bitmap, int width, int height) {
        Matrix m = new Matrix();
        m.setRectToRect(new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()), new RectF(0, 0, width, height), Matrix.ScaleToFit.CENTER);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }

    public native Bitmap getScannedBitmap(Bitmap bitmap, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4);

    public native Bitmap getGrayBitmap(Bitmap bitmap);

    public native Bitmap getMagicColorBitmap(Bitmap bitmap);

    public native Bitmap getBWBitmap(Bitmap bitmap);

    public native float[] getPoints(Bitmap bitmap);

    static {
        System.loadLibrary("opencv_java");
        System.loadLibrary("Scanner");
    }

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

    private static class ViewHolder {
        ImageView image;
        PolygonView polygonView;

        void preapre(View parent) {
            image = (ImageView) parent.findViewById(R.id.image);
        }
    }


}