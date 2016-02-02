package com.scanlibrary;


import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class ScanUtils {

    // ===========================================================
    // Constants
    // ===========================================================

    private static final String LOG_TAG = ScanUtils.class.getSimpleName();

    // ===========================================================
    // Fields
    // ===========================================================

    // ===========================================================
    // Constructors
    // ===========================================================

    // ===========================================================
    // Getters & Setters
    // ===========================================================

    public static MatOfPoint2f computePoint(int p1, int p2) {

        MatOfPoint2f pt = new MatOfPoint2f();
        pt.fromArray(new Point(p1, p2));
        return pt;
    }

    //
    public static List<Point> getPoints(Bitmap bitmap) {
        Mat image = new Mat();
        Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        org.opencv.android.Utils.bitmapToMat(bmp32, image);

        Highgui.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath() + "/mat.png", image);

        double width = image.size().width;
        double height = image.size().height;
        Mat image_proc = image.clone(); // TODO check this
        List<MatOfPoint> squares = new ArrayList<>();
        // blur will enhance edge detection
        Mat blurred = image_proc.clone(); // TODO

        Highgui.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath() + "/blured.png", blurred);


        Imgproc.medianBlur(image_proc, blurred, 9);
        Mat gray0 = new Mat(blurred.size(), CvType.CV_8U);
        Mat gray = new Mat();

        Highgui.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath() + "/gray0-1.png", gray0);
        Highgui.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath() + "/blured-0.png", blurred);


        List<MatOfPoint> contours = new ArrayList<>();

        // find squares in every color plane of the image
        for (int c = 0; c < 3; c++) {
            int ch[] = {c, 0};
            List<Mat> bluredList = new ArrayList<>(1);
            bluredList.add(blurred);
            List<Mat> grayList = new ArrayList<>(1);
            grayList.add(gray0);
            Core.mixChannels(bluredList, grayList, new MatOfInt(0, 0));
            Highgui.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath() + "/gray0-2.png", gray0);
//            mixChannels(&blurred, 1, &gray0, 1, ch, 1);

            // try several threshold levels
            final int threshold_level = 2;

            for (int l = 0; l < threshold_level; l++) {
                // Use Canny instead of zero threshold level!
                // Canny helps to catch squares with gradient shading
                if (l == 0) {
                    Imgproc.Canny(gray0, gray, 10, 20, 3, false); // TODO
                    Highgui.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath() + "/gray-0.png", gray0);

//                    Canny(gray0, gray, 10, 20, 3);
                    Imgproc.dilate(gray, gray, new Mat(), new Point(-1, -1), 0); // TODO
                    Highgui.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath() + "/gray-1.png", gray0);
//                    dilate(gray, gray, Mat(), Point(-1, -1));
                } else {


//                    gray = gray0 >= (l + 1) * 255 / threshold_level; // TODO
                }

                // Find contours and store them in a list
                Imgproc.findContours(gray, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

                Highgui.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath() + "/gray-2.png", gray);
                Highgui.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath() + "/gray0-3.png", gray0);

                // Test contours
                MatOfPoint2f approx = new MatOfPoint2f();
                for (int i = 0; i < contours.size(); i++) {
                    // approximate contour with accuracy proportional
                    // to the contour perimeter
                    MatOfPoint2f curve = new MatOfPoint2f(contours.get(i).toArray());
                    double epsilon = Imgproc.arcLength(curve, true) * 0.02;
                    Imgproc.approxPolyDP(curve, approx, epsilon, true);
//                    approxPolyDP(Mat(contours[i]), approx, arcLength(Mat(contours[i]), true) * 0.02, true);

                    // Note: absolute value of an area is used because
                    // area may be positive or negative - in accordance with the
                    // contour orientation
//                    new Mat(approx.size(),CvType.CV_8UC1)

                    double val1 = Math.abs(Imgproc.contourArea(approx));
//                            fabs(contourArea(Mat(approx))) > 1000 &&
                    boolean val2 = Imgproc.isContourConvex(new MatOfPoint(approx.toArray()));
                    //                            isContourConvex(Mat(approx)))
                    if (approx.toArray().length == 4 && val1 > 1000 && val2) {
                        double maxCosine = 0;

                        for (int j = 2; j < 5; j++) {
                            Point[] approxArray = approx.toArray();
                            double cosine = Math.abs(angle(approxArray[j % 4], approxArray[j - 2], approxArray[j - 1]));
                            maxCosine = Math.max(maxCosine, cosine);
                        }

                        if (maxCosine < 0.3) {
                            squares.add(new MatOfPoint(approx.toArray()));
                        }
                    }
                }
            }

            double largest_area = -1;
            int largest_contour_index = 0;
            for (int i = 0; i < squares.size(); i++) {
                double a = Imgproc.contourArea(squares.get(i), false);
//                double a = contourArea(squares[i], false);
                if (a > largest_area) {
                    largest_area = a;
                    largest_contour_index = i;
                }
            }

            Log.d(LOG_TAG, "Scaning size() :" + squares.size());
            List<Point> points = new ArrayList<>();
            if (squares.size() > 0) {
                points = squares.get(largest_contour_index).toList();
            } else {
                points.add(new Point(0, 0));
                points.add(new Point(width, 0));
                points.add(new Point(0, height));
                points.add(new Point(width, height));
            }

            return points;
        }

        return new ArrayList<>(0);
    }

    public static double angle(Point pt1, Point pt2, Point pt0) {
        double dx1 = pt1.x - pt0.x;
        double dy1 = pt1.y - pt0.y;
        double dx2 = pt2.x - pt0.x;
        double dy2 = pt2.y - pt0.y;

        return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
    }

    public static Bitmap test(Bitmap bitmap) {
        Mat image = new Mat();
        Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        org.opencv.android.Utils.bitmapToMat(bmp32, image);

        // Consider the image for processing
//        Mat image = Highgui.imread("C:/Users/patiprad/Desktop/IwQY6.png", Imgproc.COLOR_BGR2GRAY);
        Mat imageHSV = new Mat(image.size(), Core.DEPTH_MASK_8U);
        Mat imageBlurr = new Mat(image.size(), Core.DEPTH_MASK_8U);
        Mat imageA = new Mat(image.size(), Core.DEPTH_MASK_ALL);
        Imgproc.cvtColor(image, imageHSV, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(imageHSV, imageBlurr, new Size(5, 5), 0);
        Imgproc.adaptiveThreshold(imageBlurr, imageA, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 7, 5);

        Highgui.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath() + "/test1.png", image);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(imageA, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        Imgproc.drawContours(imageBlurr, contours, 1, new Scalar(0, 0, 255));
        for (int i = 0; i < contours.size(); i++) {
//            System.out.println(Imgproc.contourArea(contours.get(i)));
            if (Imgproc.contourArea(contours.get(i)) > 50) {
                Rect rect = Imgproc.boundingRect(contours.get(i));
                System.out.println(rect.height);
                if (rect.height > 28) {
                    //System.out.println(rect.x +","+rect.y+","+rect.height+","+rect.width);
                    Core.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 255));
                }
            }
        }

        Highgui.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath() + "/test2.png", image);

        return mapToBitmap(image);

    }

    private static Bitmap mapToBitmap(Mat mat) {
        Bitmap bmp = null;

        Mat tmp = new Mat();
        try {
            //Imgproc.cvtColor(seedsImage, tmp, Imgproc.COLOR_RGB2BGRA);
            Imgproc.cvtColor(mat, tmp, Imgproc.COLOR_GRAY2RGBA, 4);
            bmp = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888);
            org.opencv.android.Utils.matToBitmap(tmp, bmp);
        } catch (CvException e) {
            Log.d("Exception", e.getMessage());
        }
        return bmp;
    }
    // ===========================================================
    // Methods for/from SuperClass/Interfaces
    // ===========================================================

    // ===========================================================
    // Methods
    // ===========================================================

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

}
