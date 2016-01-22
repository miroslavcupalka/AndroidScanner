package com.scanlibrary;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import java.io.File;

public class ScanActivity2 extends Activity implements IScanner {

    // ===========================================================
    // Constants
    // ===========================================================

    static {
        System.loadLibrary("opencv_java");
        System.loadLibrary("Scanner");
    }

    public static final String RESULT_IMAGE_PATH = "imgPath";

    private static final int TAKE_PHOTO_REQUEST_CODE = 815;
    private static final String SAVED_ARG_TAKEN_PHOTO_LOCATION = "taken_photo_loc";


    // ===========================================================
    // Fields
    // ===========================================================

    private String takenPhotoLocation;

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
        setContentView(R.layout.scan_layout);

        if (savedInstanceState != null) {
            takenPhotoLocation = savedInstanceState.getString(SAVED_ARG_TAKEN_PHOTO_LOCATION);
        }

        if (takenPhotoLocation == null) {
            takePhoto();
        }
    }

    protected int getPreferenceContent() {
        return getIntent().getIntExtra(ScanConstants.OPEN_INTENT_PREFERENCE, 0);
    }

    @Override
    public void onBitmapSelect(Uri uri) {
        ScanFragment fragment = new ScanFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ScanConstants.SELECTED_BITMAP, uri);
        fragment.setArguments(bundle);
        android.app.FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.content, fragment);
        fragmentTransaction.addToBackStack(ScanFragment.class.toString());
        fragmentTransaction.commit();
    }

    @Override
    public void onScanFinish(Uri uri) {
        ResultFragment fragment = new ResultFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ScanConstants.SCANNED_RESULT, uri);
        fragment.setArguments(bundle);
        android.app.FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.content, fragment);
        fragmentTransaction.addToBackStack(ResultFragment.class.toString());
        fragmentTransaction.commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == TAKE_PHOTO_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Intent i = new Intent();
                i.putExtra(RESULT_IMAGE_PATH, takenPhotoLocation);
                setResult(Activity.RESULT_OK, i);
                finish();
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

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================
//
//    private void init() {
//        PickImageFragment fragment = new PickImageFragment();
//        Bundle bundle = new Bundle();
//        bundle.putInt(ScanConstants.OPEN_INTENT_PREFERENCE, getPreferenceContent());
//        fragment.setArguments(bundle);
//        android.app.FragmentManager fragmentManager = getFragmentManager();
//        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
//        fragmentTransaction.add(R.id.content, fragment);
//        fragmentTransaction.commit();
//    }
//
//
//    public native Bitmap getScannedBitmap(Bitmap bitmap, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4);
//
//    public native Bitmap getGrayBitmap(Bitmap bitmap);
//
//    public native Bitmap getMagicColorBitmap(Bitmap bitmap);
//
//    public native Bitmap getBWBitmap(Bitmap bitmap);
//
//    public native float[] getPoints(Bitmap bitmap);

}