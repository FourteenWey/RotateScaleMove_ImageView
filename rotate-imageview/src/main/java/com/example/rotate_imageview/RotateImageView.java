package com.example.rotate_imageview;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.widget.OverScroller;

import androidx.appcompat.widget.AppCompatImageView;

import com.almeros.android.multitouch.MoveGestureDetector;
import com.almeros.android.multitouch.RotateGestureDetector;

import java.util.logging.Logger;

public class RotateImageView extends AppCompatImageView implements ViewTreeObserver.OnGlobalLayoutListener{
    private boolean mIsOneLoad = true;
    //初始化的比例,也就是最小比例
    private float mInitScale;
    //图片最大比例
    private float mMaxScale;
    //图片最小比例
    private float mMinScale ;

    private Matrix mScaleMatrix;
    //捕获用户多点触控
    private ScaleGestureDetector mScaleGestureDetector;
    //移动
    private GestureDetector gestureDetector;
    //旋转
    private RotateGestureDetector mRotateGestureDetector ;
    //滚动
    private OverScroller scroller;
    private int mCurrentX, mCurrentY;
    private ValueAnimator translationAnimation; //惯性移动动画
    //单击
    private OnClickListener onClickListener;//单击监听

    public RotateImageView(Context context) {
        this(context, null);
    }

    public RotateImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RotateImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //记住，一定要把ScaleType设置成ScaleType.MATRIX，否则无法缩放
        setScaleType(ScaleType.MATRIX);
        scroller = new OverScroller(context);
        mScaleMatrix = new Matrix();
        //手势旋转
        mRotateGestureDetector = new RotateGestureDetector(context, new RotateGestureDetector.OnRotateGestureListener() {
            @Override
            public boolean onRotate(RotateGestureDetector detector) {
                rotate(detector);
                return true;
            }

            @Override
            public boolean onRotateBegin(RotateGestureDetector detector) {
                return true;
            }

            @Override
            public void onRotateEnd(RotateGestureDetector detector) {

            }
        });
        //手势缩放
        mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                return scale(detector);
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {

            }
        });
        //滑动和双击监听
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, final float distanceX, final float distanceY) {
                //滑动监听
                onTranslationImage(-distanceX, -distanceY);
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                //滑动惯性处理
                mCurrentX = (int) e2.getX();
                mCurrentY = (int) e2.getY();
                RectF rectF = getMatrixRectF();
                if (rectF == null) {
                    return false;
                }
                //startX为当前图片左边界的x坐标
                int startX = mCurrentX;
                int startY = mCurrentY;
                int minX = 0, maxX = 0, minY = 0, maxY = 0;
                int vX = Math.round(velocityX);
                int vY = Math.round(velocityY);
                maxX = Math.round(rectF.width());
                maxY = Math.round(rectF.height());
                if (startX != maxX || startY != maxY) {
                    //调用fling方法，然后我们可以通过调用getCurX和getCurY来获得当前的x和y坐标
                    //这个坐标的计算是模拟一个惯性滑动来计算出来的，我们根据这个x和y的变化可以模拟
                    //出图片的惯性滑动
                    scroller.fling(startX, startY, vX, vY, 0, maxX, 0, maxY, maxX, maxY);
                }
                if (translationAnimation != null && translationAnimation.isStarted())
                    translationAnimation.end();
                translationAnimation = ObjectAnimator.ofFloat(0, 1);
                translationAnimation.setDuration(500);
                translationAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        if (scroller.computeScrollOffset()) {
                            //获得当前的x坐标
                            int newX = scroller.getCurrX();
                            int dx = newX - mCurrentX;
                            mCurrentX = newX;
                            //获得当前的y坐标
                            int newY = scroller.getCurrY();
                            int dy = newY - mCurrentY;
                            mCurrentY = newY;
                            //进行平移操作
                            if (dx != 0 && dy != 0)
                                onTranslationImage(dx, dy);
                        }
                    }
                });
                translationAnimation.start();
                return super.onFling(e1, e2, velocityX, velocityY);
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                //单击事件
                if(onClickListener != null){
                    onClickListener.onClick(RotateImageView.this);
                }
                return true;
            }
        });
    }
    //手势操作（旋转）
    public void rotate(RotateGestureDetector detector){
        Drawable drawable = getDrawable();
        if (drawable == null)
            return;
        //获取手势操作的值,scaleFactor>1说明放大，<1则说明缩小
        float rotateFactor = detector.getRotationDegreesDelta();
        rotateFactor = -rotateFactor;
        mScaleMatrix.postRotate(rotateFactor,currentFocusX,currentFocusY);
        setImageMatrix(mScaleMatrix);
    }
    public float currentFocusX , currentFocusY ;
    //手势操作（缩放）
    public boolean scale(ScaleGestureDetector detector) {
        Drawable drawable = getDrawable();
        if (drawable == null){
            return false;
        }
        //获取手势操作的值,scaleFactor>1说明放大，<1则说明缩小
        float scaleFactor = detector.getScaleFactor();
        float scale = getScale();
        scale = Math.abs(detector.getScaleFactor() * scale);
        //获取手势操作后的比例，当放操作后比例在[mMinScale,mMaxScale]区间时允许放大
        Log.i("缩放参数","缩放值："+scale+"| 最小缩放参数："+mMinScale +" | 最大缩放参数："+mMaxScale);
        if(scale < mMaxScale && scale > mMinScale){
            currentFocusX = detector.getFocusX();
            currentFocusY = detector.getFocusY();
            mScaleMatrix.postScale(scaleFactor, scaleFactor, currentFocusX, currentFocusY);
            setImageMatrix(mScaleMatrix);
            removeBorderAndTranslationCenter();
            return true;
        }else{
            return false;
        }
    }
    //手势操作（移动）
    private void onTranslationImage(float dx, float dy) {
        if (getDrawable() == null)
            return;
        RectF rect = getMatrixRectF();
        //图片宽度小于控件宽度时不允许左右移动
        if (rect.width() <= getWidth())
            dx = 0.0f;
        //图片高度小于控件宽度时，不允许上下移动
        if (rect.height() <= getHeight())
            dy = 0.0f;
        //移动距离等于0，那就不需要移动了
        if (dx == 0.0f && dy == 0.0f)
            return;
        mScaleMatrix.postTranslate(dx, dy);
        setImageMatrix(mScaleMatrix);
        //去除移动边界
        removeBorderAndTranslationCenter();
    }
    //消除控件边界和把图片移动到中间
    private void removeBorderAndTranslationCenter() {
        RectF rectF = getMatrixRectF();
        if (rectF == null)
            return;
        int width = getWidth();
        int height = getHeight();
        float widthF = rectF.width();
        float heightF = rectF.height();
        float left = rectF.left;
        float right = rectF.right;
        float top = rectF.top;
        float bottom = rectF.bottom;
        float translationX = 0.0f, translationY = 0.0f;
        if (left > 0) {
            //左边有边界
            if (widthF > width) {
                //图片宽度大于控件宽度，移动到左边贴边
                translationX = -left;
            } else {
                //图片宽度小于控件宽度，移动到中间
                translationX = width * 1.0f / 2f - (widthF * 1.0f / 2f + left);
            }
        } else if (right < width) {
            //右边有边界
            if (widthF > width) {
                //图片宽度大于控件宽度，移动到右边贴边
                translationX = width - right;
            } else {
                //图片宽度小于控件宽度，移动到中间
                translationX = width * 1.0f / 2f - (widthF * 1.0f / 2f + left);
            }
        }
        if (top > 0) {
            //顶部有边界
            if (heightF > height) {
                //图片高度大于控件高度，去除顶部边界
                translationY = -top;
            } else {
                //图片高度小于控件宽度，移动到中间
                translationY = height * 1.0f / 2f - (top + heightF * 1.0f / 2f);
            }
        } else if (bottom < height) {
            //底部有边界
            if (heightF > height) {
                //图片高度大于控件高度，去除顶部边界
                translationY = height - bottom;
            } else {
                //图片高度小于控件宽度，移动到中间
                translationY = height * 1.0f / 2f - (top + heightF * 1.0f / 2f);
            }
        }
        mScaleMatrix.postTranslate(translationX, translationY);
        setImageMatrix(mScaleMatrix);
    }

    //获取图片宽高以及左右上下边界
    private RectF getMatrixRectF() {
        Drawable drawable = getDrawable();
        if (drawable == null) {
            return null;
        }
        RectF rectF = new RectF(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        Matrix matrix = getImageMatrix();
        matrix.mapRect(rectF);
        return rectF;
    }

    /**
     * 获取当前图片的缩放值
     *
     * @return
     */
    private float getScale() {
        float[] values = new float[9];
        mScaleMatrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    /**
     * 解决和父控件滑动冲突 只要图片边界超过控件边界，返回true
     *
     * @param direction
     * @return true 禁止父控件滑动
     */
    @Override
    public boolean canScrollHorizontally(int direction) {
        RectF rect = getMatrixRectF();
        if (rect == null || rect.isEmpty())
            return false;
        if (direction > 0) {
            return rect.right >= getWidth() + 1;
        } else {
            return rect.left <= 0 - 1;
        }
    }
    @Override
    public boolean canScrollVertically(int direction) {
        RectF rect = getMatrixRectF();
        if (rect == null || rect.isEmpty())
            return false;
        if (direction > 0) {
            return rect.bottom >= getHeight() + 1;
        } else {
            return rect.top <= 0 - 1;
        }
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleGestureDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        mRotateGestureDetector.onTouchEvent(event);
        return true ;
    }
    @Override
    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }

    /**
     * imageView加载完成后调用，获取imageView加载完成后的图片大小
     */
    @Override
    public void onGlobalLayout() {
        if (mIsOneLoad) {
            //得到控件的宽和高
            int width = getWidth();
            int height = getHeight();
            //获取图片,如果没有图片则直接退出
            Drawable d = getDrawable();
            if (d == null)
                return;
            //获取图片的宽和高
            int dw = d.getIntrinsicWidth();
            int dh = d.getIntrinsicHeight();
            float scale = 1.0f;
            if (dw > width && dh <= height) {
                scale = width * 1.0f / dw;
            }
            if (dw <= width && dh > height) {
                scale = height * 1.0f / dh;
            }
            if ((dw <= width && dh <= height) || (dw >= width && dh >= height)) {
                scale = Math.min(width * 1.0f / dw, height * 1.0f / dh);
            }
            //图片原始比例，图片回复原始大小时使用
            mInitScale = scale;
            //手势放大时最大比例
            mMaxScale = mInitScale * 2;
            //手势缩小时的最小比例
            mMinScale = mInitScale * 0.8f ;

            //设置移动数据,把改变比例后的图片移到中心点
            float translationX = width * 1.0f / 2 - dw / 2;
            float translationY = height * 1.0f / 2 - dh / 2;
            mScaleMatrix.postTranslate(translationX, translationY);
            mScaleMatrix.postScale(mInitScale, mInitScale, width * 1.0f / 2, height * 1.0f / 2);
            setImageMatrix(mScaleMatrix);
            mIsOneLoad = false;
        }
    }
}
