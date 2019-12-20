package com.scanlibrary;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.PointF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.media.ExifInterface;
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

    /** The source photo URI (probably a JPEG). */
    private Uri sourcePhotoUri;
    /** The source bitmap.  Unmodified except for rotation. */
    private Bitmap sourcePhotoBitmap;
    /** The cropped and rotated bitmap.  No color changes are applied. */
    private Bitmap croppedBitmap;

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
        croppedBitmap = sourcePhotoBitmap = Utils.getBitmapFromUri(getActivity(), sourcePhotoUri);
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
        sourceImageView.setImageBitmap(sourcePhotoBitmap);
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
        MenuItem autoCropBtn = menu.findItem(R.id.auto_crop);
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
        autoCropBtn.setVisible(isCropMode);
        rotateBtn.setVisible(!isCropMode);
        modeBtn.setVisible(!isCropMode);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.crop) {
            onCropButtonClicked();
            return true;
        } else if (item.getItemId() == R.id.auto_crop) {
            new CropTask(sourcePhotoBitmap).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
                new ModeChangingTask(MODE_NONE).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            return true;
        } else if (item.getItemId() == R.id.mode_black_and_white) {
            if (!item.isChecked()) {
                currentMode = MODE_BLACK_AND_WHITE;
                item.setChecked(true);
                new ModeChangingTask(MODE_BLACK_AND_WHITE).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            return true;
        } else if (item.getItemId() == R.id.mode_magic) {
            if (!item.isChecked()) {
                currentMode = MODE_MAGIC;
                item.setChecked(true);
                new ModeChangingTask(MODE_MAGIC).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called From activity
     */
    public boolean onBackPressed() {
        if (isCropMode) {
            sourceImageView.setImageBitmap(croppedBitmap);
            sourceImageView.setVisibility(View.VISIBLE);
            polygonView.setVisibility(View.GONE);

            getActivity().invalidateOptionsMenu();
            isCropMode = false;
            return false;
        }

        releaseAllBitmaps();

        return true;
    }

    private void releaseAllBitmaps() {
        if (sourcePhotoBitmap != null) sourcePhotoBitmap.recycle();
        sourcePhotoBitmap = null;
        if (croppedBitmap != null) croppedBitmap.recycle();
        croppedBitmap = null;
    }

    private void onCropButtonClicked() {
        getActivity().invalidateOptionsMenu();
        isCropMode = true;

        if (points == null) {
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
        new RotatingTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void onDoneButtonClicked() {
        if (isCropMode) {
            isCropMode = false;
            getActivity().invalidateOptionsMenu();

            Map<Integer, PointF> points = polygonView.getPoints();
            if (isScanPointsValid(points)) {
                upScalePoints(points, sourcePhotoBitmap, sourceImageView.getSWidth(), sourceImageView.getSHeight());
                new FinishCropModeTask(sourcePhotoBitmap, points, currentMode).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                showErrorDialog();
            }
        } else {
            File scannedDocFile = createImageFile("result");
            saveBitmapToFile(scannedDocFile, changeBitmapColorMode(currentMode, croppedBitmap));
            try {
                ExifInterface srcExif = new ExifInterface(getActivity().getContentResolver().openInputStream(sourcePhotoUri));
                ExifInterface destExif = new ExifInterface(scannedDocFile.toString());
                ExifInterfaceUtils.copyExifInterface(srcExif, destExif, null);
                destExif.resetOrientation();
                destExif.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, Integer.toString(croppedBitmap.getWidth()));
                destExif.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, Integer.toString(croppedBitmap.getHeight()));
                destExif.setAttribute(ExifInterface.TAG_LIGHT_SOURCE, null);
                destExif.saveAttributes();
            } catch (IOException e) {
                Log.e(TAG, "Could not copy EXIF", e);
            }

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

    /** Detect the four points of the document. */
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
        dismissDialog();
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

    private void onCropTaskFinished(CropTaskResult cropTaskResult) {
        points = cropTaskResult.points;

        // Scale the bitmap to fit within the source frame.  We then use the bitmap size
        // to scale the points because sourceFrame probably doesn't have the right aspect ratio
        // (ScaleImageView doesn't support "adjustViewBounds" or "wrap_content").
        Bitmap scaledBitmap = ImageResizer.scaleBitmap(sourcePhotoBitmap, sourceFrame.getWidth(), sourceFrame.getHeight());
        sourceImageView.setImageBitmap(scaledBitmap);
        Bitmap tempBitmap = scaledBitmap;
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

    private class FinishCropModeTask extends AsyncTask<Void, Void, DocumentFromBitmapTaskResult> {

        private Bitmap bitmap;
        private Uri bitmapUri;
        private final Map<Integer, PointF> points;
        private int mode;

        public FinishCropModeTask(Bitmap bitmap, Map<Integer, PointF> points, int mode) {
            this.bitmap = bitmap;
            this.points = points;
            this.mode = mode;
        }

        public FinishCropModeTask(Uri bitmapUri, Map<Integer, PointF> points, int mode) {
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
                } catch (IOException e) {
                    throw new RuntimeException("Not able to resize image", e);
                }
            }
            croppedBitmap = result.bitmap = cropDocumentFromBitmap(bitmap, result.points);
            result.bitmap = changeBitmapColorMode(mode, result.bitmap);
            return result;
        }

        @Override
        protected void onPostExecute(DocumentFromBitmapTaskResult documentFromBitmapTaskResult) {
            ScanFragment.this.points = documentFromBitmapTaskResult.points;
            Bitmap tmp = documentFromBitmapTaskResult.bitmap;
            sourceImageView.setImageBitmap(tmp);
            polygonView.setVisibility(View.GONE);
            dismissDialog();
        }
    }

    private class RotatingTask extends AsyncTask<Void, Void, Bitmap> {

        @Override
        protected void onPreExecute() {
            showProgressDialog();
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            sourcePhotoBitmap = Utils.rotateBitmap(sourcePhotoBitmap, -90);
            croppedBitmap = Utils.rotateBitmap(croppedBitmap, -90);
            return changeBitmapColorMode(currentMode, croppedBitmap);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            sourceImageView.setImageBitmap(bitmap);
            points = null; // TODO: Should we rotate the points?
            dismissDialog();
        }
    }

    Bitmap changeBitmapColorMode(int mode, Bitmap bitmap) {
        if (mode == MODE_MAGIC) {
            return ScanUtils.getMagicColorBitmap(bitmap);
        } else if (mode == MODE_BLACK_AND_WHITE) {
            return ScanUtils.getGrayBitmap(bitmap);
        } else {
            return bitmap;
        }
    }

    private class ModeChangingTask extends AsyncTask<Void, Void, Bitmap> {
        private final int mode;

        @Override
        protected void onPreExecute() {
            showProgressDialog();
        }

        public ModeChangingTask(int mode) {
            this.mode = mode;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            return changeBitmapColorMode(mode, croppedBitmap);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            sourceImageView.setImageBitmap(bitmap);
            dismissDialog();
        }
    }

    private class CropTask extends AsyncTask<Void, Void, CropTaskResult> {
        private Bitmap bitmap;
        private Uri bitmapUri;

        @Override
        protected void onPreExecute() {
            showProgressDialog(R.string.detecting);
        }

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
        private Bitmap sourcePhotoBitmap;
        private Bitmap documentBitmap;
        private Bitmap documentColoredBitmap;
    }

    private static class DocumentFromBitmapTaskResult {
        private Bitmap bitmap;
        private Map<Integer, PointF> points;
    }
}
