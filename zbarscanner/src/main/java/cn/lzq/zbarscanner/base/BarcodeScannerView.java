package cn.lzq.zbarscanner.base;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * 基本扫码视图，包含CameraPreview（相机预览）
 * 和ViewFinderView（扫码框、阴影遮罩等）
 */
public abstract class BarcodeScannerView extends FrameLayout implements Camera.PreviewCallback {
    private static final String TAG = "BarcodeScannerView";
    private CameraWrapper cameraWrapper;
    private CameraPreview cameraPreview;
    private IViewFinder viewFinderView;
    private Rect scaledRect, rotatedRect;
    private CameraHandlerThread cameraHandlerThread;
    private boolean shouldAdjustFocusArea = false;
    private int count;
    private long lastClick;
    private float oldDist = 1f;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int pointerCount = event.getPointerCount();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_UP:
                if (pointerCount == 1) {
                    long now = System.currentTimeMillis();
                    if (now - lastClick < 500) {
                        lastClick = now;
                        count++;
                        if (count == 2) {
                            Log.e("Camera", "shuangji");
                            Toast.makeText(getContext(), "shuangji", Toast.LENGTH_SHORT).show();
                        } else if (count > 2) {
                            count = 1;
                        }
                    } else {
                        count = 1;
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (pointerCount == 2) {
                    float newDist = getFingerSpacing(event);
                    if (newDist > oldDist) {
                        Log.e("Camera", "进入放大手势");
                        handleZoom(true);
                    } else if (newDist < oldDist) {
                        Log.e("Camera", "进入缩小手势");
                        handleZoom(false);
                    }
                    oldDist = newDist;
                }

                break;
            case MotionEvent.ACTION_CANCEL:
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    private void handleZoom(boolean isZoomIn) {
        if (cameraWrapper != null) {
            Camera.Parameters params = cameraWrapper.camera.getParameters();
            if (params.isZoomSupported()) {
                int maxZoom = params.getMaxZoom();
                int zoom = params.getZoom();
                if (isZoomIn && zoom < maxZoom) {
                    Log.e("Camera", "进入放大方法zoom=" + zoom);
                    zoom++;
                } else if (zoom > 0) {
                    Log.e("Camera", "进入缩小方法zoom=" + zoom);
                    zoom--;
                }
                params.setZoom(zoom);
                cameraWrapper.camera.setParameters(params);
            } else {
                Log.i(TAG, "相机不支持zoom not supported");
            }
        }
    }

    public void cameraP() {
        if (cameraWrapper != null) {
            Camera.Parameters params = cameraWrapper.camera.getParameters();
            if (params.isZoomSupported()) {
                int zoom = params.getZoom();
                if (zoom == params.getMaxZoom()) {
                    params.setZoom(0);
                } else {
                    params.setZoom(params.getMaxZoom());
                }
            } else {
                Log.i(TAG, "相机不支持zoom not supported");
            }
        }
    }

    private static float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        Log.e("Camera", "getFingerSpacing ，计算距离 = " + (float) Math.sqrt(x * x + y * y));
        return (float) Math.sqrt(x * x + y * y);
    }

    public BarcodeScannerView(@NonNull Context context, @NonNull IViewFinder viewFinderView) {
        super(context);
        if (viewFinderView instanceof View) {
            this.viewFinderView = viewFinderView;
        } else {
            throw new IllegalArgumentException("viewFinderView必须是View对象");
        }
    }

    public BarcodeScannerView(@NonNull Context context) {
        super(context);
    }

    public BarcodeScannerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BarcodeScannerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setViewFinderView(IViewFinder viewFinderView) {
        this.viewFinderView = viewFinderView;
    }

    /**
     * 打开系统相机，并进行基本的初始化
     */
    public void startCamera() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            if (cameraHandlerThread == null) {
                cameraHandlerThread = new CameraHandlerThread(this);
            }
            cameraHandlerThread.startCamera(CameraUtils.getDefaultCameraId());
        } else {//没有相机权限
            throw new RuntimeException("没有Camera权限");
        }
    }

    /**
     * 基本初始化
     */
    public void setupCameraPreview(final CameraWrapper cameraWrapper) {
        this.cameraWrapper = cameraWrapper;
        if (this.cameraWrapper != null) {
            setupLayout(this.cameraWrapper);//创建CameraPreview对象，将CameraPreview和ViewFinderView添加到容器中
            if (!CameraUtils.isSupportedFocusModes(cameraWrapper.camera)) {
                //固定焦
                return;
            }
            Camera.Parameters parameters = cameraWrapper.camera.getParameters();
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            cameraWrapper.camera.setParameters(parameters);
            //设置对焦区域
            if (shouldAdjustFocusArea) {
                ((View) viewFinderView).getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {//确保能获取到viewFinderView的尺寸
                    @Override
                    public boolean onPreDraw() {
                        ((View) viewFinderView).getViewTreeObserver().removeOnPreDrawListener(this);
                        setupFocusAreas();
                        return true;
                    }
                });
            }
        }
    }

    /**
     * 创建CameraPreview对象，将CameraPreview和ViewFinderView添加到容器中
     */
    private void setupLayout(CameraWrapper cameraWrapper) {
        removeAllViews();
        cameraPreview = new CameraPreview(getContext(), cameraWrapper, this);
        addView(cameraPreview);
        addView(((View) viewFinderView));
    }

    /**
     * 设置对焦区域
     */
    private void setupFocusAreas() {
        /*
         * 1.根据ViewFinderView和2000*2000的尺寸之比，缩放对焦区域
         */
        Rect framingRect = viewFinderView.getFramingRect();//获得扫码框区域
        int viewFinderViewWidth = ((View) viewFinderView).getWidth();
        int viewFinderViewHeight = ((View) viewFinderView).getHeight();
        int width = 2000, height = 2000;

        Rect scaledRect = new Rect(framingRect);
        scaledRect.left = scaledRect.left * width / viewFinderViewWidth;
        scaledRect.right = scaledRect.right * width / viewFinderViewWidth;
        scaledRect.top = scaledRect.top * height / viewFinderViewHeight;
        scaledRect.bottom = scaledRect.bottom * height / viewFinderViewHeight;

        /*
         * 2.旋转对焦区域
         */
        Rect rotatedRect = new Rect(scaledRect);
        int rotationCount = getRotationCount();
        if (rotationCount == 1) {//若相机图像需要顺时针旋转90度，则将扫码框逆时针旋转90度
            rotatedRect.left = scaledRect.top;
            rotatedRect.top = 2000 - scaledRect.right;
            rotatedRect.right = scaledRect.bottom;
            rotatedRect.bottom = 2000 - scaledRect.left;
        } else if (rotationCount == 2) {//若相机图像需要顺时针旋转180度,则将扫码框逆时针旋转180度
            rotatedRect.left = 2000 - scaledRect.right;
            rotatedRect.top = 2000 - scaledRect.bottom;
            rotatedRect.right = 2000 - scaledRect.left;
            rotatedRect.bottom = 2000 - scaledRect.top;
        } else if (rotationCount == 3) {//若相机图像需要顺时针旋转270度，则将扫码框逆时针旋转270度
            rotatedRect.left = 2000 - scaledRect.bottom;
            rotatedRect.top = scaledRect.left;
            rotatedRect.right = 2000 - scaledRect.top;
            rotatedRect.bottom = scaledRect.right;
        }

        /*
         * 3.坐标系平移
         */
        Rect rect = new Rect(rotatedRect.left - 1000, rotatedRect.top - 1000, rotatedRect.right - 1000, rotatedRect.bottom - 1000);

        /*
         * 4.设置对焦区域
         */
        Camera.Parameters parameters = cameraWrapper.camera.getParameters();
        if (parameters.getMaxNumFocusAreas() > 0) {
            Camera.Area area = new Camera.Area(rect, 1000);
            ArrayList<Camera.Area> areaList = new ArrayList<>();
            areaList.add(area);
            parameters.setFocusAreas(areaList);
            cameraWrapper.camera.setParameters(parameters);
        } else {
            Log.e(TAG, "不支持设置对焦区域");
        }

        Log.e(TAG, "对焦区域：" + rect.toShortString());
    }

    /**
     * 释放相机资源等各种资源
     */
    public void stopCamera() {
        if (cameraWrapper != null) {
            cameraPreview.stopCameraPreview();//停止相机预览并置空各种回调
            cameraPreview.setCamera(null, null);
            cameraWrapper.camera.release();//释放资源
            cameraWrapper = null;
        }
        //停止线程
        if (cameraHandlerThread != null) {
            cameraHandlerThread.quit();
            cameraHandlerThread = null;
        }
    }

    /**
     * 根据ViewFinderView和preview的尺寸之比，缩放扫码区域
     * 这里决定了返回扫码实际截取区域(因为预览图横屏,但是扫码区域是竖屏展示的)
     * maxWidth 这里是询问是否按照最宽的宽度来截取,
     * 如果false 则按照扫描框时间大小截取,  ture则按照屏幕宽度截取,高度也是屏幕宽度
     */
    public Rect getScaledRect(int previewWidth, int previewHeight, boolean maxWidth) {
        if (scaledRect == null) {
            Rect framingRect = viewFinderView.getFramingRect();//获得扫码框的实际显示区域
//            int viewFinderViewWidth = ((View) viewFinderView).getWidth();
//            int viewFinderViewHeight = ((View) viewFinderView).getHeight();
            int viewFinderViewWidth = getWidth();
            int viewFinderViewHeight = getHeight();
            int width, height;
            if (DisplayUtils.getScreenOrientation(getContext()) == Configuration.ORIENTATION_PORTRAIT && previewHeight < previewWidth) {
                //竖屏使用并且预览图是横屏(竖屏横图的)
                width = previewHeight;
                height = previewWidth;
            } else if (DisplayUtils.getScreenOrientation(getContext()) == Configuration.ORIENTATION_LANDSCAPE && previewHeight > previewWidth) {
                //横屏使用并且预览图是竖屏 (  横屏竖图的)
                width = previewHeight;
                height = previewWidth;
            } else {//正常滴
                width = previewWidth;
                height = previewHeight;
            }
            scaledRect = new Rect(framingRect);
            if (maxWidth) {
                int worh = framingRect.height() > width ? framingRect.height() : width;
                int tt = (worh - framingRect.height()) / 2;
                int top = framingRect.top - tt;
                scaledRect.left = 0;
                scaledRect.top = top < 0 ? 0 : top;
                scaledRect.right = worh;
                scaledRect.bottom = scaledRect.top + (worh + tt * 2);
            } else {
                int tt = 0;
                scaledRect.left = (scaledRect.left * width / viewFinderViewWidth);
                scaledRect.right = (scaledRect.right * width / viewFinderViewWidth);
                scaledRect.top = (scaledRect.top * height / viewFinderViewHeight) - tt;
                scaledRect.bottom = (scaledRect.bottom * height / viewFinderViewHeight) - tt;
            }
        }
        return scaledRect;
    }

    /**
     * 旋转扫码框,因为预览图方向不一定,因此这里需要把扫码的位置移动
     *
     * @param previewWidth
     * @param previewHeight
     * @param rect
     * @return
     */
    public Rect getRotatedRect(int previewWidth, int previewHeight, Rect rect) {
        if (rotatedRect == null) {
            int rotationCount = getRotationCount();
            rotatedRect = new Rect(rect);
            if (rotationCount == 1) {//若相机图像需要顺时针旋转90度，则将扫码框逆时针旋转90度
                rotatedRect.left = rect.top;
                rotatedRect.top = previewHeight - rect.right;
                rotatedRect.right = rect.bottom;
                rotatedRect.bottom = previewHeight - rect.left;
            } else if (rotationCount == 2) {//若相机图像需要顺时针旋转180度,则将扫码框逆时针旋转180度
                rotatedRect.left = previewWidth - rect.right;
                rotatedRect.top = previewHeight - rect.bottom;
                rotatedRect.right = previewWidth - rect.left;
                rotatedRect.bottom = previewHeight - rect.top;
            } else if (rotationCount == 3) {//若相机图像需要顺时针旋转270度，则将扫码框逆时针旋转270度
                rotatedRect.left = previewWidth - rect.bottom;
                rotatedRect.top = rect.left;
                rotatedRect.right = previewWidth - rect.top;
                rotatedRect.bottom = rect.right;
            }
        }
        return rotatedRect;
    }

    /**
     * 旋转data(太耗时间)
     */
    public byte[] rotateData(byte[] data, Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        int width = parameters.getPreviewSize().width;
        int height = parameters.getPreviewSize().height;

        int rotationCount = getRotationCount();
        for (int i = 0; i < rotationCount; i++) {
            byte[] rotatedData = new byte[data.length];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    rotatedData[x * height + height - y - 1] = data[x + y * width];
                }
            }
            data = rotatedData;
            int tmp = width;
            width = height;
            height = tmp;
        }

        return data;
    }

    /**
     * 获取（旋转角度/90）
     */
    private int getRotationCount() {
        int displayOrientation = cameraPreview.getDisplayOrientation();
        return displayOrientation / 90;
    }

//-------------------------------------------------------------------------

    /**
     * 开启/关闭闪光灯
     */
    public void setFlash(boolean flag) {
        if (cameraWrapper != null && CameraUtils.isFlashSupported(cameraWrapper.camera)) {
            Camera.Parameters parameters = cameraWrapper.camera.getParameters();
            if (flag) {
                if (parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                    return;
                }
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            } else {
                if (parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF)) {
                    return;
                }
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }
            cameraWrapper.camera.setParameters(parameters);
        }
    }

    /**
     * 切换闪光灯的点亮状态
     */
    public void toggleFlash() {
        if (cameraWrapper != null && CameraUtils.isFlashSupported(cameraWrapper.camera)) {
            Camera.Parameters parameters = cameraWrapper.camera.getParameters();
            if (parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            } else {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            }
            cameraWrapper.camera.setParameters(parameters);
        }
    }

    /**
     * 闪光灯是否被点亮
     */
    public boolean isFlashOn() {
        if (cameraWrapper != null && CameraUtils.isFlashSupported(cameraWrapper.camera)) {
            Camera.Parameters parameters = cameraWrapper.camera.getParameters();
            if (parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    /**
     * 设置是否要根据扫码框的位置去调整对焦区域的位置<br/>
     * 默认值为false，即不调整，会使用系统默认的配置，那么对焦区域会位于预览画面的中央<br/>
     * <br/>
     * (经测试，调整对焦区域功能对极个别机型无效，即设置之后对焦区域依然位于预览画面的中央，原因不明)
     */
    public void setShouldAdjustFocusArea(boolean shouldAdjustFocusArea) {
        this.shouldAdjustFocusArea = shouldAdjustFocusArea;
    }

}