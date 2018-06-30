package cn.lzq.zbarscanner.zbar;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import com.dtr.zbar.build.ZBarDecoder;
import cn.lzq.zbarscanner.base.BarcodeScannerView;
import cn.lzq.zbarscanner.base.IViewFinder;

/**
 * zbar扫码视图预览，继承自基本扫码视图BarcodeScannerView
 * <p>
 * BarcodeScannerView内含CameraPreview（相机预览）和ViewFinderView（扫码框、阴影遮罩等）
 */
public class ZBarScannerView extends BarcodeScannerView {
    private static final String TAG = "ZBarScannerView";
    private ResultHandler resultHandler;
    private boolean maxWidth;
    private Camera camera;
    public interface ResultHandler {
        void handleResult(Result rawResult);
    }

    /**
     * 设置时候是否按照手机宽度截取二维码信息
     * @param maxWidth
     */
    public void setMaxWidth(boolean maxWidth) {
        this.maxWidth = maxWidth;
    }

    /*
     * 加载zbar动态库
     * zbar.jar中的类会用到
     */
    static {
        System.loadLibrary("ZBarDecoder");
    }

    public ZBarScannerView(@NonNull Context context, @NonNull IViewFinder viewFinderView) {
        super(context, viewFinderView);

    }

    public ZBarScannerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }
    //扫码核心
    ZBarDecoder zBarDecoder = new ZBarDecoder();
    /**
     * Called as preview frames are displayed.<br/>
     * This callback is invoked on the event thread open(int) was called from.<br/>
     * (此方法与Camera.open运行于同一线程，在本项目中，就是CameraHandlerThread线程)
     */
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        long startTime = System.currentTimeMillis();
        this.camera = camera;
        if (resultHandler == null) {
            return;
        }
        try {
            Camera.Parameters parameters = camera.getParameters();
            int previewWidth = parameters.getPreviewSize().width;
            int previewHeight = parameters.getPreviewSize().height;
            //根据ViewFinderView和preview的尺寸之比，缩放扫码区域
            //精准识别区域图片
            Rect rect = getScaledRect(previewWidth, previewHeight,maxWidth );
            String result = getResult(data, previewWidth, previewHeight, rect);
            if (!TextUtils.isEmpty(result)) {
                final Result rawResult = new Result();
                rawResult.setContents(result);
                new Handler(Looper.getMainLooper()).post(new Runnable() {//切换到主线程
                    @Override
                    public void run() {
                        if (resultHandler != null) {
                            resultHandler.handleResult(rawResult);
                        }
                    }
                });
            } else {
                getOneMoreFrame();//再获取一帧图像数据进
//                rect = getScaledRect(previewWidth, previewHeight, true);
//                result = getResult(data, previewWidth, previewHeight, rect);
//                if (TextUtils.isEmpty(result)) {
//                    getOneMoreFrame();//再获取一帧图像数据进
//                } else {
//                    final Result rawResult = new Result();
//                    rawResult.setContents(result);
//                    new Handler(Looper.getMainLooper()).post(new Runnable() {//切换到主线程
//                        @Override
//                        public void run() {
//                            if (resultHandler != null) {
//                                resultHandler.handleResult(rawResult);
//                            }
//                        }
//                    });
//                }
            }
        } catch (RuntimeException e) {
            Log.e(TAG, e.toString(), e);
        }
        Log.e(TAG, String.format("图像处理及识别耗时: %d ms", System.currentTimeMillis() - startTime));
    }

    /**
     * 去识别
     * @param data
     * @param previewWidth
     * @param previewHeight
     * @param rect
     * @return
     */
    private String getResult(byte[] data, int previewWidth, int previewHeight, Rect rect) {
        /*
         * 方案二：旋转截取区域
         */
        rect = getRotatedRect(previewWidth, previewHeight, rect);
        if (cropResult != null) {
            cropResult.result(data, previewWidth, previewHeight, rect);
        }
        return zBarDecoder.decodeCrop(data, previewWidth, previewHeight,
                rect.left, rect.top, rect.width(), rect.height());

    }


    //裁剪区域结果
    CropResult cropResult;

    public void setResult(CropResult result) {
        this.cropResult = result;
    }

    public interface CropResult {
        void result(byte[] data1, int w, int h, Rect rect);
    }
//--------------------------------------------------------------------------------------------------

    public void setResultHandler(@NonNull ResultHandler resultHandler) {
        this.resultHandler = resultHandler;
    }

    /**
     * 再获取一帧图像数据进行识别（会再次触发onPreviewFrame方法）
     */
    public void getOneMoreFrame() {
        camera.setOneShotPreviewCallback(this);
    }
}