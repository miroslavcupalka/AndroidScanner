package com.scanlibrary;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jhansi on 28/03/15.
 */
public class PolygonView extends FrameLayout {

    protected Context context;
    private Paint paint;
    private ImageView pointers[];
    private PolygonView polygonView;

    public PolygonView(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public PolygonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public PolygonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init();
    }

    private void init() {
        polygonView = this;
        pointers = new ImageView[8];
        // Our 8 points are in clockwise order:
        //
        // 0   1   2
        // 7       3
        // 6   5   4
        //
        // But the imported and exported corner points are in "Z" order:
        //
        // 0       1
        //
        // 2       3
        //
        for (int i = 0; i < pointers.length; i++) {
            pointers[i] = getImageView();
            addView(pointers[i]);
        }
        initPaint();
    }

    private void initPaint() {
        paint = new Paint();
        paint.setColor(getResources().getColor(R.color.blue));
        paint.setStrokeWidth(2);
        paint.setAntiAlias(true);
    }

    /** Export the four corner points in "Z" order */
    public Map<Integer, PointF> getPoints() {
        List<PointF> points = new ArrayList<PointF>();
        points.add(new PointF(pointers[0].getX(), pointers[0].getY()));
        points.add(new PointF(pointers[2].getX(), pointers[2].getY()));
        points.add(new PointF(pointers[6].getX(), pointers[6].getY()));
        points.add(new PointF(pointers[4].getX(), pointers[4].getY()));
        return getOrderedPoints(points);
    }

    public static Map<Integer, PointF> getOrderedPoints(List<PointF> points) {
        PointF centerPoint = new PointF();
        int size = points.size();
        for (PointF pointF : points) {
            centerPoint.x += pointF.x / size;
            centerPoint.y += pointF.y / size;
        }
        Map<Integer, PointF> orderedPoints = new HashMap<>();
        for (PointF pointF : points) {
            int index = -1;
            if (pointF.x < centerPoint.x && pointF.y < centerPoint.y) {
                index = 0;
            } else if (pointF.x > centerPoint.x && pointF.y < centerPoint.y) {
                index = 1;
            } else if (pointF.x < centerPoint.x && pointF.y > centerPoint.y) {
                index = 2;
            } else if (pointF.x > centerPoint.x && pointF.y > centerPoint.y) {
                index = 3;
            }
            orderedPoints.put(index, pointF);
        }
        return orderedPoints;
    }

    public void setPoints(Map<Integer, PointF> pointFMap) {
        if (pointFMap.size() == 4) {
            setPointsCoordinates(pointFMap);
        }
    }

    private void setPointsCoordinates(Map<Integer, PointF> pointFMap) {
        pointers[0].setX(pointFMap.get(0).x);
        pointers[0].setY(pointFMap.get(0).y);

        pointers[2].setX(pointFMap.get(1).x);
        pointers[2].setY(pointFMap.get(1).y);

        pointers[6].setX(pointFMap.get(2).x);
        pointers[6].setY(pointFMap.get(2).y);

        pointers[4].setX(pointFMap.get(3).x);
        pointers[4].setY(pointFMap.get(3).y);
    }

    private float midX(View v) {
        return v.getX() + v.getWidth() / 2.0f;
    }

    private float midY(View v) {
        return v.getY() + v.getHeight() / 2.0f;
    }

    private void drawLine(Canvas canvas, View from, View to) {
        canvas.drawLine(midX(from), midY(from), midX(to), midY(to), paint);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        drawLine(canvas, pointers[0], pointers[2]);
        drawLine(canvas, pointers[2], pointers[4]);
        drawLine(canvas, pointers[4], pointers[6]);
        drawLine(canvas, pointers[6], pointers[0]);
        setMidPointers();
    }

    private void setMidPointers() {
        for (int i = 1; i < 8; i += 2) {
            int before = (i + 8 - 1) % 8;
            int after = (i + 8 + 1) % 8;
            pointers[i].setX(pointers[before].getX() - ((pointers[before].getX() - pointers[after].getX()) / 2));
            pointers[i].setY(pointers[before].getY() - ((pointers[before].getY() - pointers[after].getY()) / 2));
        }
    }

    private ImageView getImageView() {
        ImageView imageView = new ImageView(context);
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        imageView.setLayoutParams(layoutParams);
        imageView.setImageResource(R.drawable.circle);
        return imageView;
    }

    private ImageView getWhichPointer(MotionEvent event) {
        float minDiff = Float.MAX_VALUE;
        ImageView best = null;
        for (ImageView v : pointers) {
            int FUZZ = v.getWidth() * 3;
            float diffX = Math.abs(event.getX() - midX(v));
            float diffY = Math.abs(event.getY() - midY(v));
            if (diffX > FUZZ) continue;
            if (diffY > FUZZ) continue;
            float diff = (float) Math.hypot(diffX, diffY);
            if (diff < minDiff) {
                minDiff = diff;
                best = v;
            }
        }
        return best;
    }

    private int getPointerNumber(ImageView view) {
        for (int i = 0; i < pointers.length; i++) {
            if (view == pointers[i]) return i;
        }
        return -1;
    }

    private PointF DownPT = new PointF(); // Record Mouse Position When Pressed Down
    private PointF start[] = new PointF[8]; // Record starting pointer positions
    private ImageView currentPointer;

    // Return true if final position was adjusted to remain inside the parent view
    private boolean movePointer(int num, float x, float y) {
        float size = pointers[num].getWidth();
        float unclippedX = start[num].x + x;
        float unclippedY = start[num].y + y;
        float newX = Math.max(0, Math.min(getWidth() - size, start[num].x + x));
        float newY = Math.max(0, Math.min(getHeight() - size, start[num].y + y));
        pointers[num].setX(newX);
        pointers[num].setY(newY);
        return newX != unclippedX || newY != unclippedY;
    }

    private void moveSidePointer(int num, float x, float y) {
        int before = num - 1;
        int after = (num + 1) % 8;
        boolean c1 = movePointer(before, x, y);
        boolean c2 = movePointer(num, x, y);
        boolean c3 = movePointer(after, x, y);
        if (c1) {
            makeLine(before, num, after, x != 0);
        } else if (c3) {
            makeLine(after, num, before, x != 0);
        }
    }

    private void makeLine(int anchor1, int anchor2, int movable, boolean changeX) {
        if (changeX)
            pointers[movable].setX(pointers[anchor2].getX() + pointers[anchor2].getX() - pointers[anchor1].getX());
        else
            pointers[movable].setY(pointers[anchor2].getY() + pointers[anchor2].getY() - pointers[anchor1].getY());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int eid = event.getAction();
        switch (eid) {
            case MotionEvent.ACTION_DOWN:
                currentPointer = getWhichPointer(event);
                if (currentPointer == null) break;
                DownPT.x = event.getX();
                DownPT.y = event.getY();
                for (int i = 0; i < pointers.length; i++) {
                    start[i] = new PointF(pointers[i].getX(), pointers[i].getY());
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (currentPointer == null) break;
                float x = Math.max(event.getX(), 0);
                x = Math.min(x, currentPointer.getWidth());
                float y = Math.max(event.getY(), 0);
                y = Math.min(y, currentPointer.getHeight());
                x = event.getX();
                y = event.getY();
                PointF mv = new PointF(x - DownPT.x, y - DownPT.y);
                int num = getPointerNumber(currentPointer);
                if (num == 1 || num == 5) {
                    moveSidePointer(num, 0, mv.y);
                } else if (num == 3 || num == 7) {
                    moveSidePointer(num, mv.x, 0);
                } else {
                    movePointer(num, mv.x, mv.y);
                    setMidPointers();
                }
                break;
            case MotionEvent.ACTION_UP:
                int color = 0;
                if (isValidShape(getPoints())) {
                    color = getResources().getColor(R.color.blue);
                } else {
                    color = getResources().getColor(R.color.orange);
                }
                paint.setColor(color);
                break;
            default:
                break;
        }
        polygonView.invalidate();
        return true;
    }

    public static boolean isValidShape(Map<Integer, PointF> pointFMap) {
        return pointFMap.size() == 4;
    }
}
