package cn.lzq.zbarscanner.demo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;

import cn.lzq.zbarscanner.R;
import cn.lzq.zbarscanner.zbar.Result;
import cn.lzq.zbarscanner.zbar.ZBarScannerView;

/**
 * 最简单的使用示例
 */
public class SimpleScannerActivity extends AppCompatActivity implements ZBarScannerView.ResultHandler {
    private static final String TAG = "SimpleScannerActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 0;
    private ZBarScannerView zBarScannerView;
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_simple_scanner);
        initView();
    }

    private void initView() {
        ViewGroup container = findViewById(R.id.container);
        //ViewFinderView是根据需求自定义的视图，会被覆盖在相机预览画面之上，通常包含扫码框、扫描线、扫码框周围的阴影遮罩等
        zBarScannerView = new ZBarScannerView(this, new ViewFinderView(this));
        zBarScannerView.setShouldAdjustFocusArea(true);//设置是否要根据扫码框的位置去调整对焦区域的位置,默认不调整
        container.addView(zBarScannerView);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            zBarScannerView.setResultHandler(this);
            zBarScannerView.startCamera();//打开系统相机，并进行基本的初始化
        } else {//没有相机权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }
    @Override
    public void onPause() {
        super.onPause();
        zBarScannerView.stopCamera();//释放相机资源等各种资源
    }

    @Override
    public void handleResult(Result rawResult) {
       // Toast.makeText(this, "Contents = " + rawResult.getContents()  , Toast.LENGTH_SHORT).show();
        Intent intent = new Intent();
        intent.putExtra("result",rawResult.getContents( ));
        setResult(RESULT_OK,intent);
        finish();
//        //2秒后再次识别
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                zBarScannerView.getOneMoreFrame();//再获取一帧图像数据进行识别
//            }
//        }, 2000);
    }
}