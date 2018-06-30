package cn.lzq.zbarscanner.demo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;
import android.widget.RelativeLayout;

import cn.lzq.zbarscanner.base.DisplayUtils;
import cn.lzq.zbarscanner.base.IViewFinder;

/**
 * 二维码扫描
 */
public class QRView extends RelativeLayout implements IViewFinder {
    private Rect framingRect;//扫码框所占区域
    private final int maskColor = Color.parseColor("#50000000");
    Paint maskPaint;
    public QRView(Context context) {
        super(context);
        setWillNotDraw(false);//需要进行绘制
        //阴影遮罩画笔
        maskPaint = new Paint();
        maskPaint.setColor(maskColor);
    }

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        updateFramingRect();
    }

    /**
     * 设置framingRect的值（扫码框所占的区域位置）
     */
    public synchronized void updateFramingRect() {
        /** 获取布局中扫描框的位置信息 */
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int cropLeft = location[0];
        //这里减去 dp2px(getContext(),56) 主要考虑的是toolbar占位置了,导致计算位置错误
        int cropTop = location[1] - DisplayUtils.getStatusBarHeight(this.getContext()) - dp2px(getContext(),56);
        int cropWidth = view.getWidth();
        int cropHeight = view.getHeight();
        framingRect = new Rect(cropLeft, cropTop, cropLeft + cropWidth, (cropTop + cropHeight));
    }
    /**
     * convert dp to its equivalent px
     * 将dp转换为与之相等的px
     */
    public static int dp2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getFramingRect() == null) {
            return;
        }
        if (maskPaint != null) {
            drawViewFinderMask(canvas);
            return;
        }
    }

    /**
     * 绘制扫码框四周的阴影遮罩
     */
    public void drawViewFinderMask(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        Rect framingRect = getFramingRect();
        canvas.drawRect(0, 0, width, framingRect.top, maskPaint);//扫码框顶部阴影
        canvas.drawRect(0, framingRect.top, framingRect.left, framingRect.bottom, maskPaint);//扫码框左边阴影
        canvas.drawRect(framingRect.right, framingRect.top, width, framingRect.bottom, maskPaint);//扫码框右边阴影
        canvas.drawRect(0, framingRect.bottom, width, height, maskPaint);//扫码框底部阴影
    }

    @Override
    public Rect getFramingRect() {
        return framingRect;
    }

    /**
     * 设置扫码框view是哪个,会根据这个view进行截取对应的图片,以及绘制除了这个view之外的阴影遮罩
     * 必须要设置的
      */
    View view;
    public void setCropView(View view) {
        this.view = view;
    }
}