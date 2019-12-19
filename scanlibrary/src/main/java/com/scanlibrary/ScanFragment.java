package com.scanlibrary;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.davemorrissey.labs.subscaleview.ScaleImageView;

import org.opencv.core.Point;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.scanlibrary.CropActivity.EXTRA_IMAGE_URI;

public class ScanFragment extends Fragment {
    private static final String TAG = ScanFragment.class.getSimpleName();

    public static final String RESULT_IMAGE_URI = "result_image_uri";
    private static final int MODE_NONE = 0;
    private static final int MODE_BLACK_AND_WHITE = 1;
    private static final int MODE_MAGIC = 2;

    private ScaleImageView sourceImageView;
    private FrameLayout sourceFrame;
    private PolygonView polygonView;
    private ProgressDialogFragment progressDialogFragment;

    private String takenPhotoLocation;
    private Uri sourcePhotoUri;
    private Bitmap sourcePhotoBitmap;
    private Bitmap documentBitmap;
    private Bitmap documentColoredBitmap;

    private Map<Integer, PointF> points;

    private boolean isCropMode = false;
    private int currentMode = MODE_MAGIC;

    private int previousOrientation = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        sourcePhotoUri = getArguments().getParcelable(EXTRA_IMAGE_URI);
        onCropButtonClicked();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scan, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sourceImageView = (ScaleImageView) view.findViewById(R.id.sourceImageView);
        sourceFrame = (FrameLayout) view.findViewById(R.id.sourceFrame);
        polygonView = (PolygonView) view.findViewById(R.id.polygonView);
        int currentOrientation = Utils.getScreenOrientation(getActivity());
        if (previousOrientation == -1) {
            previousOrientation = currentOrientation;
        } else if (previousOrientation != currentOrientation) {
            points = null;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.scan_menu, menu);

        MenuItem cropBtn = menu.findItem(R.id.crop);
        MenuItem rotateBtn = menu.findItem(R.id.rotate);
        MenuItem modeBtn = menu.findItem(R.id.colors);
        MenuItem modeNone = menu.findItem(R.id.mode_none);
        MenuItem modeBlackAndWhite = menu.findItem(R.id.mode_black_and_white);
        MenuItem modeMagic = menu.findItem(R.id.mode_magic);

        if (currentMode == MODE_NONE) {
            modeNone.setChecked(true);
        } else if (currentMode == MODE_BLACK_AND_WHITE) {
            modeBlackAndWhite.setChecked(true);
        } else if (currentMode == MODE_MAGIC) {
            modeMagic.setChecked(true);
        }

        cropBtn.setVisible(!isCropMode);
        rotateBtn.setVisible(!isCropMode);
        modeBtn.setVisible(!isCropMode);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.crop) {
            onCropButtonClicked();
            return true;
        } else if (item.getItemId() == R.id.done) {
            onDoneButtonClicked();
            return true;
        } else if (item.getItemId() == R.id.rotate) {
            onRotateButtonClicked();
            return true;
        } else if (item.getItemId() == R.id.mode_none) {
            if (!item.isChecked()) {
                currentMode = MODE_NONE;
                item.setChecked(true);
                onNoneModeChosen();
            }
            return true;
        } else if (item.getItemId() == R.id.mode_black_and_white) {
            if (!item.isChecked()) {
                currentMode = MODE_BLACK_AND_WHITE;
                item.setChecked(true);
                onBlackAndWhiteModeChosen();
            }
            return true;
        } else if (item.getItemId() == R.id.mode_magic) {
            if (!item.isChecked()) {
                currentMode = MODE_MAGIC;
                item.setChecked(true);
                onMagicModeChosen();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onMagicModeChosen() {
        showProgressDialog();
        new ModeChangingTask(MODE_MAGIC, documentBitmap).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void onBlackAndWhiteModeChosen() {
        showProgressDialog();
        new ModeChangingTask(MODE_BLACK_AND_WHITE, documentBitmap).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void onNoneModeChosen() {
        if (documentColoredBitmap != null) {
            documentColoredBitmap.recycle();
            documentColoredBitmap = null;
        }
        updateViewsWithNewBitmap();
    }

    private void updateViewsWithNewBitmap() {
        Bitmap tmp = documentColoredBitmap != null ? documentColoredBitmap : documentBitmap;
        int width = sourceFrame.getWidth();
        int height = sourceFrame.getHeight();
        if (width <= 0 || height <= 0) {
            width = 2048;
            height = 2048;
        }
        Log.i(TAG, "ZZZ converting " + tmp.getWidth() + " x " + tmp.getHeight() + " to " + width + " x " + height);
        tmp = ImageResizer.scaleBitmap(tmp, width, height);
        Log.i(TAG, "ZZZ final size: " + tmp.getWidth() + " x " + tmp.getHeight());
        sourceImageView.setImageBitmap(tmp);
    }

    /**
     * Called From activity
     */
    public boolean onBackPressed() {
        if (isCropMode) {
            updateViewsWithNewBitmap();
            sourceImageView.setVisibility(View.VISIBLE);
            polygonView.setVisibility(View.GONE);

            getActivity().invalidateOptionsMenu();
            isCropMode = false;
            return false;
        }

        releaseAllBitmaps();
        if (takenPhotoLocation != null) {
            removeFile(takenPhotoLocation);
        }

        return true;
    }

    private void releaseAllBitmaps() {
        if (sourcePhotoBitmap != null) sourcePhotoBitmap.recycle();
        if (documentBitmap != null) documentBitmap.recycle();
    }

    private void onCropButtonClicked() {
        getActivity().invalidateOptionsMenu();
        isCropMode = true;

        if (points == null) {
            showProgressDialog(R.string.detecting);
            if (sourcePhotoBitmap != null)
                new CropTask(sourcePhotoBitmap).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            else
                new CropTask(sourcePhotoUri).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            return;
        }
        CropTaskResult result = new CropTaskResult();
        result.points = points;
        onCropTaskFinished(result);
    }

    private void onRotateButtonClicked() {
        showProgressDialog();
        new RotatingTask(sourcePhotoBitmap, documentBitmap, documentColoredBitmap).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void onDoneButtonClicked() {
        if (isCropMode) {
            isCropMode = false;
            getActivity().invalidateOptionsMenu();

            Map<Integer, PointF> points = polygonView.getPoints();
            if (isScanPointsValid(points)) {
                upScalePoints(points, sourcePhotoBitmap, sourceImageView.getWidth(), sourceImageView.getHeight());
                new DocumentFromBitmapTask(sourcePhotoBitmap, points, currentMode).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                showErrorDialog();
            }
        } else {
            File scannedDocFile = createImageFile("result");
            Bitmap tmp = documentColoredBitmap != null ? documentColoredBitmap : documentBitmap;

            try {
                tmp = ImageResizer.resizeImage(tmp, 2048, 2048); // ZZZ TODO: Why?!
            } catch (IOException e) {
                throw new RuntimeException("Not able to resize image");
            }

            saveBitmapToFile(scannedDocFile, tmp);

            sourcePhotoBitmap.recycle();
            documentBitmap.recycle();
            if (documentColoredBitmap != null) documentColoredBitmap.recycle();

            removeFile(takenPhotoLocation);
            releaseAllBitmaps();

            Intent intent = new Intent();
            Uri resultUri = Utils.provideUriForFile(getActivity(), new File(scannedDocFile.getAbsolutePath()));
            intent.putExtra(RESULT_IMAGE_URI, resultUri);
            intent.setData(resultUri);
            getActivity().setResult(Activity.RESULT_OK, intent);
            getActivity().finish();
        }
    }

    private boolean saveBitmapToFile(File file, Bitmap bitmap) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            bitmap.compress(CompressFormat.JPEG, 95, out);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    private void onPhotoTaken() {
        DocumentFromBitmapTask documentFromBitmapTask = new DocumentFromBitmapTask(sourcePhotoUri, null, currentMode);
        documentFromBitmapTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static Bitmap cropDocumentFromBitmap(Bitmap bitmap, Map<Integer, PointF> points) {
        float x1 = (points.get(0).x);
        float x2 = (points.get(1).x);
        float x3 = (points.get(2).x);
        float x4 = (points.get(3).x);
        float y1 = (points.get(0).y);
        float y2 = (points.get(1).y);
        float y3 = (points.get(2).y);
        float y4 = (points.get(3).y);
        Log.d("", "POints(" + x1 + "," + y1 + ")(" + x2 + "," + y2 + ")(" + x3 + "," + y3 + ")(" + x4 + "," + y4 + ")");
        return ScanUtils.getScannedBitmap(bitmap, x1, y1, x2, y2, x3, y3, x4, y4);
    }

    private static void upScalePoints(Map<Integer, PointF> points, Bitmap original, int scaledImgWidth, int scaledImgHeight) {
        int width = original.getWidth();
        int height = original.getHeight();
        float xRatio = (float) width / scaledImgWidth;
        float yRatio = (float) height / scaledImgHeight;

        points.get(0).x = (points.get(0).x) * xRatio;
        points.get(1).x = (points.get(1).x) * xRatio;
        points.get(2).x = (points.get(2).x) * xRatio;
        points.get(3).x = (points.get(3).x) * xRatio;
        points.get(0).y = (points.get(0).y) * yRatio;
        points.get(1).y = (points.get(1).y) * yRatio;
        points.get(2).y = (points.get(2).y) * yRatio;
        points.get(3).y = (points.get(3).y) * yRatio;
    }

    private static Map<Integer, PointF> downScalePoints(Map<Integer, PointF> points, Bitmap original, int targetImgWidth, int targetImgHeight) {
        int width = original.getWidth();
        int height = original.getHeight();
        float xRatio = (float) width / targetImgWidth;
        float yRatio = (float) height / targetImgHeight;

        Map<Integer, PointF> scaledPoints = new HashMap<>(4);
        scaledPoints.put(0, new PointF((points.get(0).x) / xRatio, (points.get(0).y) / yRatio));
        scaledPoints.put(1, new PointF((points.get(1).x) / xRatio, (points.get(1).y) / yRatio));
        scaledPoints.put(2, new PointF((points.get(2).x) / xRatio, (points.get(2).y) / yRatio));
        scaledPoints.put(3, new PointF((points.get(3).x) / xRatio, (points.get(3).y) / yRatio));
        return scaledPoints;
    }

    private File createImageFile(String fileName) {
        File dir = new File(getActivity().getFilesDir(), "images");
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

    private void removeFile(String absoluteLocation) {
        if (absoluteLocation == null) return;
        File f = new File(absoluteLocation);
        if (f.exists()) {
            f.delete();
        }
    }

    private static Map<Integer, PointF> getEdgePoints(Bitmap tempBitmap) {
        List<PointF> pointFs = getContourEdgePoints(tempBitmap);
        Map<Integer, PointF> orderedPoints = orderedValidEdgePoints(tempBitmap, pointFs);
        return orderedPoints;
    }

    private static List<PointF> getContourEdgePoints(Bitmap tempBitmap) {
        List<Point> points = ScanUtils.getPoints(tempBitmap);
        List<PointF> p = new ArrayList<>();
        p.add(new PointF((float) points.get(0).x, (float) points.get(0).y));
        p.add(new PointF((float) points.get(1).x, (float) points.get(1).y));
        p.add(new PointF((float) points.get(2).x, (float) points.get(2).y));
        p.add(new PointF((float) points.get(3).x, (float) points.get(3).y));
        return p;
    }

    private static Map<Integer, PointF> getOutlinePoints(Bitmap tempBitmap) {
        Map<Integer, PointF> outlinePoints = new HashMap<>();
        outlinePoints.put(0, new PointF(0, 0));
        outlinePoints.put(1, new PointF(tempBitmap.getWidth(), 0));
        outlinePoints.put(2, new PointF(0, tempBitmap.getHeight()));
        outlinePoints.put(3, new PointF(tempBitmap.getWidth(), tempBitmap.getHeight()));
        return outlinePoints;
    }

    private static Map<Integer, PointF> getOutlinePoints(View view) {
        Map<Integer, PointF> outlinePoints = new HashMap<>();
        outlinePoints.put(0, new PointF(0, 0));
        outlinePoints.put(1, new PointF(view.getWidth(), 0));
        outlinePoints.put(2, new PointF(0, view.getHeight()));
        outlinePoints.put(3, new PointF(view.getWidth(), view.getHeight()));
        return outlinePoints;
    }

    private static Map<Integer, PointF> orderedValidEdgePoints(Bitmap tempBitmap, List<PointF> pointFs) {
        Map<Integer, PointF> orderedPoints = PolygonView.getOrderedPoints(pointFs);
        if (!PolygonView.isValidShape(orderedPoints)) {
            orderedPoints = getOutlinePoints(tempBitmap);
        }
        return orderedPoints;
    }

    private void showErrorDialog() {
        Toast.makeText(getActivity(), R.string.cant_crop, Toast.LENGTH_LONG).show();
    }

    private boolean isScanPointsValid(Map<Integer, PointF> points) {
        return points.size() == 4;
    }

    protected void showProgressDialog() {
        showProgressDialog(getString(R.string.transforming));
    }

    protected void showProgressDialog(int stringId) {
        showProgressDialog(getString(stringId));
    }

    protected void showProgressDialog(String string) {
        Bundle args = new Bundle();
        args.putString(ProgressDialogFragment.EXTRA_MESSAGE, string);
        progressDialogFragment = new ProgressDialogFragment();
        progressDialogFragment.setArguments(args);
        progressDialogFragment.setCancelable(false);
        progressDialogFragment.show(getActivity().getSupportFragmentManager(), "progress_dialog");
    }

    protected void dismissDialog() {
        ProgressDialogFragment progressDialogFragment = (ProgressDialogFragment) getActivity().getSupportFragmentManager().findFragmentByTag("progress_dialog");
        if (progressDialogFragment != null) progressDialogFragment.dismissAllowingStateLoss();
    }

    private void onDocumentFromBitmapTaskFinished(DocumentFromBitmapTaskResult result) {
        if (documentBitmap != null) documentBitmap.recycle();
        documentBitmap = result.bitmap;
        if (documentColoredBitmap != null) documentColoredBitmap.recycle();
        documentColoredBitmap = result.coloredBitmap;
        points = result.points;

        Bitmap tmp = documentColoredBitmap != null ? documentColoredBitmap : documentBitmap;

        Bitmap scaledBitmap = ImageResizer.scaleBitmap(tmp, sourceFrame.getWidth(), sourceFrame.getHeight());
        sourceImageView.setImageBitmap(scaledBitmap);
        sourceImageView.setVisibility(View.VISIBLE);
        polygonView.setVisibility(View.GONE);
    }

    private void onRotatingTaskFinished(RotatingTaskResult rotatingTaskResult) {
        sourcePhotoBitmap = rotatingTaskResult.takenPhotoBitmap;
        documentBitmap = rotatingTaskResult.documentBitmap;
        documentColoredBitmap = rotatingTaskResult.documentColoredBitmap;

        Bitmap scaledBitmap = ImageResizer.scaleBitmap(documentColoredBitmap != null ? documentColoredBitmap : documentBitmap, sourceFrame.getWidth(), sourceFrame.getHeight());
        sourceImageView.setImageBitmap(scaledBitmap);
        sourceImageView.setVisibility(View.VISIBLE);

        points = null;
    }

    private void onCropTaskFinished(CropTaskResult cropTaskResult) {
        points = cropTaskResult.points;

        Bitmap scaledBitmap = ImageResizer.scaleBitmap(sourcePhotoBitmap, sourceFrame.getWidth(), sourceFrame.getHeight());
        sourceImageView.setImageBitmap(scaledBitmap);
        sourceImageView.setVisibility(View.VISIBLE);

        //Bitmap tempBitmap = ((BitmapDrawable) sourceImageView.getDrawable()).getBitmap();
        Bitmap tempBitmap = scaledBitmap; // ZZZ
        polygonView.setVisibility(View.VISIBLE);

        Map<Integer, PointF> pointsToUse = null;
        if (points == null) {
            pointsToUse = getEdgePoints(tempBitmap);
        } else {
            pointsToUse = downScalePoints(points, sourcePhotoBitmap, tempBitmap.getWidth(), tempBitmap.getHeight());
        }

        polygonView.setPoints(pointsToUse);
        int padding = (int) getResources().getDimension(R.dimen.polygonViewCircleWidth);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(tempBitmap.getWidth() + padding, tempBitmap.getHeight() + padding);
        layoutParams.gravity = Gravity.CENTER;
        polygonView.setLayoutParams(layoutParams);
    }

    private class DocumentFromBitmapTask extends AsyncTask<Void, Void, DocumentFromBitmapTaskResult> {

        private Bitmap bitmap;
        private Uri bitmapUri;
        private final Map<Integer, PointF> points;
        private int mode;

        public DocumentFromBitmapTask(Bitmap bitmap, Map<Integer, PointF> points, int mode) {
            this.bitmap = bitmap;
            this.points = points;
            this.mode = mode;
        }

        public DocumentFromBitmapTask(Uri bitmapUri, Map<Integer, PointF> points, int mode) {
            this.bitmapUri = bitmapUri;
            this.points = points;
            this.mode = mode;
        }

        @Override
        protected void onPreExecute() {
            showProgressDialog(R.string.detecting);
        }

        @Override
        protected DocumentFromBitmapTaskResult doInBackground(Void... params) {
            System.gc();
            if (bitmapUri != null && bitmap == null)
                bitmap = Utils.getBitmapFromUri(getActivity(), bitmapUri);
            if (sourcePhotoBitmap == null) sourcePhotoBitmap = bitmap;

            DocumentFromBitmapTaskResult result = new DocumentFromBitmapTaskResult();

            if (points != null) {
                result.points = points;
            } else {
                try {
                    Bitmap scaledBmp = ImageResizer.resizeImage(bitmap, 400, 400, false);
                    result.points = getEdgePoints(scaledBmp);
                    upScalePoints(result.points, bitmap, scaledBmp.getWidth(), scaledBmp.getHeight());
                    scaledBmp.recycle();
                } catch (IOException e) {
                    throw new RuntimeException("Not able to resize image", e);
                }
            }

            result.bitmap = cropDocumentFromBitmap(bitmap, result.points);
            try {
                result.bitmap = ImageResizer.resizeImage(result.bitmap, 2048, 2048, false); // ZZZ TODO
            } catch (IOException e) {
                throw new RuntimeException("Not able to resize image", e);
            }

            if (mode == MODE_MAGIC) {
                result.coloredBitmap = ScanUtils.getMagicColorBitmap(result.bitmap);
            } else if (mode == MODE_BLACK_AND_WHITE) {
                result.coloredBitmap = ScanUtils.getGrayBitmap(result.bitmap);
            }

            return result;
        }

        @Override
        protected void onPostExecute(DocumentFromBitmapTaskResult documentFromBitmapTaskResult) {
            onDocumentFromBitmapTaskFinished(documentFromBitmapTaskResult);
            dismissDialog();
        }
    }

    private class RotatingTask extends AsyncTask<Void, Void, RotatingTaskResult> {

        private final Bitmap takenPhotoBitmap;
        private final Bitmap documentBitmap;
        private final Bitmap documentColoredBitmap;

        public RotatingTask(Bitmap takenPhotoBitmap, Bitmap documentBitmap, Bitmap documentColoredBitmap) {
            this.takenPhotoBitmap = takenPhotoBitmap;
            this.documentBitmap = documentBitmap;
            this.documentColoredBitmap = documentColoredBitmap;
        }

        @Override
        protected RotatingTaskResult doInBackground(Void... params) {
            RotatingTaskResult result = new RotatingTaskResult();

            result.takenPhotoBitmap = Utils.rotateBitmap(takenPhotoBitmap, -90);
            takenPhotoBitmap.recycle();

            result.documentBitmap = Utils.rotateBitmap(documentBitmap, -90);
            documentBitmap.recycle();

            if (documentColoredBitmap != null) {
                result.documentColoredBitmap = Utils.rotateBitmap(documentColoredBitmap, -90);
                documentColoredBitmap.recycle();
            }

            return result;
        }

        @Override
        protected void onPostExecute(RotatingTaskResult rotatingTaskResult) {
            onRotatingTaskFinished(rotatingTaskResult);
            dismissDialog();
        }
    }

    private class ModeChangingTask extends AsyncTask<Void, Void, Bitmap> {
        private final int mode;
        private final Bitmap bitmap;

        public ModeChangingTask(int mode, Bitmap bitmap) {
            this.mode = mode;
            this.bitmap = bitmap;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            if (mode == MODE_MAGIC) {
                return ScanUtils.getMagicColorBitmap(bitmap);
            } else if (mode == MODE_BLACK_AND_WHITE) {
                return ScanUtils.getGrayBitmap(bitmap);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (documentColoredBitmap != null) documentColoredBitmap.recycle();
            documentColoredBitmap = bitmap;
            updateViewsWithNewBitmap();
            dismissDialog();
        }
    }

    private class CropTask extends AsyncTask<Void, Void, CropTaskResult> {
        private Bitmap bitmap;
        private Uri bitmapUri;

        public CropTask(Bitmap bitmap) {
            this.bitmap = bitmap;
        }

        public CropTask(Uri bitmapUri) {
            this.bitmapUri = bitmapUri;
        }

        @Override
        protected CropTaskResult doInBackground(Void... params) {
            CropTaskResult result = new CropTaskResult();

            if (bitmapUri != null && bitmap == null)
                bitmap = Utils.getBitmapFromUri(getActivity(), bitmapUri);
            if (sourcePhotoBitmap == null) sourcePhotoBitmap = bitmap;

            try {
                Bitmap scaledBmp = ImageResizer.resizeImage(bitmap, 400, 400, false);
                result.points = getEdgePoints(scaledBmp);
                upScalePoints(result.points, bitmap, scaledBmp.getWidth(), scaledBmp.getHeight());
                scaledBmp.recycle();
            } catch (IOException e) {
                throw new RuntimeException("Not able to resize image", e);
            }
            return result;
        }

        @Override
        protected void onPostExecute(CropTaskResult cropTaskResult) {
            onCropTaskFinished(cropTaskResult);
            dismissDialog();
        }
    }

    private static class CropTaskResult {
        Map<Integer, PointF> points;
    }

    private static class RotatingTaskResult {
        private Bitmap takenPhotoBitmap;
        private Bitmap documentBitmap;
        private Bitmap documentColoredBitmap;
    }

    private static class DocumentFromBitmapTaskResult {
        private Bitmap bitmap;
        private Bitmap coloredBitmap;
        private Map<Integer, PointF> points;
    }
}
