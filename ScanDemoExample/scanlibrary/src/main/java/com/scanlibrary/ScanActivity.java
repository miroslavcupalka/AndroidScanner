package com.scanlibrary;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

public class ScanActivity extends Activity {


    public static final String EXTRA_BRAND_IMG_RES = "title_img_res";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_ACTION_BAR_COLOR = "ab_color";
    public static final String RESULT_IMAGE_PATH = ScanFragment.RESULT_IMAGE_PATH;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        int titleImgRes = getIntent().getExtras().getInt(EXTRA_BRAND_IMG_RES);
        int abColor = getIntent().getExtras().getInt(EXTRA_ACTION_BAR_COLOR);
        String title = getIntent().getExtras().getString(EXTRA_TITLE);

        if (title != null) setTitle(title);
        if (titleImgRes != 0) getActionBar().setLogo(titleImgRes);

        if (abColor != 0) {
            getActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(abColor)));
            getActionBar().setDisplayShowTitleEnabled(false);
            getActionBar().setDisplayShowTitleEnabled(true);
        }

        if (savedInstanceState == null) {
            Bundle args = new Bundle();
            if (getIntent().getExtras() != null) {
                args.putAll(getIntent().getExtras());
            }

            FragmentManager fragMan = getFragmentManager();
            Fragment f = new ScanFragment();
            f.setArguments(args);
            FragmentTransaction fragTransaction = fragMan.beginTransaction();
            fragTransaction.replace(R.id.contaner, f, "scan_frag").commit();
        }
    }

    public static native Bitmap getScannedBitmap(Bitmap bitmap, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4);

    public static native Bitmap getGrayBitmap(Bitmap bitmap);

    public static native Bitmap getMagicColorBitmap(Bitmap bitmap);

    public static native Bitmap getBWBitmap(Bitmap bitmap);

    public static native float[] getPoints(Bitmap bitmap);


    static {
        System.loadLibrary("opencv_java");
        System.loadLibrary("Scanner");
    }
}