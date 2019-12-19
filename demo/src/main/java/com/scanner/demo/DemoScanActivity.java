package com.scanner.demo;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.davemorrissey.labs.subscaleview.ScaleImageView;
import com.scanlibrary.CropActivity;
import com.scanlibrary.ScanFragment;
import com.scanlibrary.Utils;

import java.io.File;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

import static com.scanner.demo.DemoScanActivityPermissionsDispatcher.takePhotoWithPermissionCheck;

@RuntimePermissions
public class DemoScanActivity extends AppCompatActivity {
    private static final String TAG = DemoScanActivity.class.getSimpleName();
    private static final String SAVED_SCANNED_PHOTO = "scanned_photo";
    private static final int TAKE_PHOTO_REQUEST_CODE = 88;
    private static final int LOAD_PHOTO_REQUEST_CODE = 89;
    private static final int CROP_PHOTO_REQUEST_CODE = 90;

    private File photoFile;
    private Uri photoUri;
    private ScaleImageView image;
    private String scannedPhoto;
    private Button cropButton;
    private Uri resultPhotoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        image = (ScaleImageView) findViewById(R.id.image);

        cropButton = findViewById(R.id.crop);
        if (savedInstanceState != null) {
            scannedPhoto = savedInstanceState.getString(SAVED_SCANNED_PHOTO);
            if (scannedPhoto != null) {
                image.setImageBitmap(Utils.getBitmapFromLocation(scannedPhoto));
            }
        }
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                Log.e(TAG, "Uncaught exception in " + t + ":", e);
                System.exit(1);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVED_SCANNED_PHOTO, scannedPhoto);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.i(TAG, "ZZZ onRestoreInstanceState");
        super.onRestoreInstanceState(savedInstanceState);
    }

    public void onScanButtonClicked(View v) {
        takePhotoWithPermissionCheck(this);
    }

    @NeedsPermission(Manifest.permission.CAMERA)
    void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        photoFile = createImageFile("capture");
        photoUri = Utils.provideUriForFile(this, photoFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        startActivityForResult(takePictureIntent, TAKE_PHOTO_REQUEST_CODE);
    }

    public void onLoadButtonClicked(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, LOAD_PHOTO_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TAKE_PHOTO_REQUEST_CODE && resultCode == RESULT_OK) {
            Bitmap bitmap = Utils.getBitmapFromLocation(photoFile.toString());
            if (bitmap != null) {
                image.setImageBitmap(bitmap);
                cropButton.setVisibility(View.VISIBLE);
            }
        }
        if (requestCode == LOAD_PHOTO_REQUEST_CODE && resultCode == RESULT_OK) {
            photoUri = data.getData();
            Bitmap bitmap = Utils.getBitmapFromUri(this, data.getData());
            if (bitmap != null) {
                image.setImageBitmap(bitmap);
                cropButton.setVisibility(View.VISIBLE);
            }
        }
        if (requestCode == CROP_PHOTO_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getParcelableExtra(ScanFragment.RESULT_IMAGE_URI);
            resultPhotoUri = data.getData();
            Bitmap bitmap = Utils.getBitmapFromUri(this, data.getData());
            if (bitmap != null) {
                image.setImageBitmap(bitmap);
                cropButton.setVisibility(View.VISIBLE);
                MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "cropped-image", "Cropped Image");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        DemoScanActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @OnShowRationale(Manifest.permission.CAMERA)
    void showRationaleForCamera(final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setPositiveButton(R.string.button_allow, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(@NonNull DialogInterface dialog, int which) {
                        request.proceed();
                    }
                })
                .setNegativeButton(R.string.button_deny, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(@NonNull DialogInterface dialog, int which) {
                        request.cancel();
                    }
                })
                .setCancelable(false)
                .setMessage(R.string.permission_camera_rationale)
                .show();
    }

    private File createImageFile(String fileName) {
        File dir = new File(getFilesDir(), "images");
        if (!dir.exists()) dir.mkdir();
        dir.setReadable(false, false);
        dir.setReadable(true, true);
        // Remove old files
        for (File f : dir.listFiles()) {
            // Remove the file if it is more than one day old
            if (System.currentTimeMillis() - f.lastModified() > 1000 * 60 * 60 * 24) {
                f.delete();
            }
        }
        return new File(dir, fileName + ".jpg");
    }

    public void onCropButtonClicked(View view) {
        Intent intent = new Intent(this, CropActivity.class);
        intent.putExtra(CropActivity.EXTRA_TITLE, "Crop Document");
        intent.putExtra(CropActivity.EXTRA_ACTION_BAR_COLOR, R.color.green);
        intent.putExtra(CropActivity.EXTRA_LANGUAGE, "en");
        intent.putExtra(CropActivity.EXTRA_IMAGE_URI, photoUri);
        startActivityForResult(intent, CROP_PHOTO_REQUEST_CODE);
    }
}
