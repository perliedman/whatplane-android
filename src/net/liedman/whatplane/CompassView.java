package net.liedman.whatplane;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

public class CompassView extends View {
    private Paint mPaint = new Paint();
    private Path mPath;
    private float mDirection = 0f;

    public CompassView(Context context) {
        super(context);
        createPath();
    }
    
    public CompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        createPath();
        //setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, 30, 30));
    }

    private void createPath() {
        mPath = new Path();
        mPath.moveTo(0, -7);
        mPath.lineTo(-4, 6);
        mPath.lineTo(0, 5);
        mPath.lineTo(4, 6);
        mPath.close();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(30, 30);
    }
    
    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
            int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        
        
    }

    public float getDirection() {
        return mDirection;
    }

    public void setDirection(float direction) {
        if (direction != mDirection) {
            mDirection = direction;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Paint paint = mPaint;

        //canvas.drawColor(Color.BLACK);

        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);

        int cx = getWidth() / 2;
        int cy = getHeight() / 2;

        canvas.translate(cx, cy);
        canvas.rotate(mDirection);
        canvas.drawPath(mPath, mPaint);
    }
}