package com.scanner.demo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import com.scanlibrary.ScanActivity;


public class MainActivity extends ActionBarActivity implements OnClickListener {

    // ===========================================================
    // Constants
    // ===========================================================

    private static final int REQUEST_CODE_SCAN = 47;

    private static final String SAVED_SCANNED_HHOTO = "scanned_photo";

    // ===========================================================
    // Fields
    // ===========================================================

    private final ViewHolder viewHolder = new ViewHolder();

    private String scannedPhoto;

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
        setContentView(R.layout.activity_main);
        viewHolder.prepare(findViewById(android.R.id.content));

        if (savedInstanceState != null) {
            scannedPhoto = savedInstanceState.getString(SAVED_SCANNED_HHOTO);
        }

        if (scannedPhoto != null) {
            viewHolder.image.setImageBitmap(getBitmapFromLocation(scannedPhoto));
        }
    }

    @Override
    public void onClick(View v) {
        if (v.equals(viewHolder.scabBtn)) {
            onScanButtonClicked();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewHolder.scabBtn.setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        viewHolder.scabBtn.setOnClickListener(null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCAN && resultCode == Activity.RESULT_OK) {
            String imgPath = data.getStringExtra(ScanActivity.RESULT_IMAGE_PATH);
            Bitmap bitmap = getBitmapFromLocation(imgPath);
            viewHolder.image.setImageBitmap(bitmap);
//            Uri uri = data.getExtras().getParcelable(ScanConstants.SCANNED_RESULT);
//            Bitmap bitmap = null;
//            try {
//                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
//                getContentResolver().delete(uri, null, null);
//                viewHolder.image.setImageBitmap(bitmap);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVED_SCANNED_HHOTO, scannedPhoto);
    }

    // ===========================================================
    // Methods
    // ===========================================================

    private void onScanButtonClicked() {
        Intent intent = new Intent(this, ScanActivity.class);
        intent.putExtra(ScanActivity.EXTRA_BRAND_IMG_RES, R.drawable.ic_crop_white_24dp);
        intent.putExtra(ScanActivity.EXTRA_TITLE, "Crop Document");
        intent.putExtra(ScanActivity.EXTRA_ACTION_BAR_COLOR, R.color.green);
        startActivityForResult(intent, REQUEST_CODE_SCAN);
    }

    private Bitmap getBitmapFromLocation(String absLocation) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeFile(absLocation, options);
    }

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

    private static class ViewHolder {

        ImageView image;
        View scabBtn;

        void prepare(View parent) {
            image = (ImageView) parent.findViewById(R.id.image);
            scabBtn = parent.findViewById(R.id.scan);
        }
    }

}
