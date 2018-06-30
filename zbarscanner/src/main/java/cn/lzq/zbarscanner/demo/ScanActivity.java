package cn.lzq.zbarscanner.demo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;

import cn.lzq.zbarscanner.R;
import cn.lzq.zbarscanner.zbar.Result;
import cn.lzq.zbarscanner.zbar.ZBarScannerView;

public class ScanActivity extends AppCompatActivity {
    /**
     * 是否开启显示裁剪结果:
     */
    public static boolean isDebug;
    private RelativeLayout scanCropView;
    private ImageView scanLine;
    private ImageView cropView;
    ZBarScannerView zBarScannerView;
    private boolean dialogDismiss = true;
    private TranslateAnimation animation;
    private final int CAMERA = 886;
    private static final int REQUEST_CODE_PICK_IMAGE = 3;
    // 显示给用户的解释
    AlertDialog.Builder builder;
    AlertDialog.Builder setting;
    private ImageView flashlight;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_scan);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        if (animation == null) {
            animation = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.1f, Animation.RELATIVE_TO_PARENT, 0.80f);
            animation.setDuration(2500);
            animation.setRepeatCount(-1);
            animation.setRepeatMode(Animation.REVERSE);
        }
        findViewById();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    zBarScannerView.startCamera();
                } else {
                    requsetPermisstion(true);
                }
                return;
            }
        }
    }

    /**
     * 请求权限
     */
    private void requsetPermisstion(boolean fins) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            if (dialogDismiss) {
                builder.show();
                dialogDismiss = false;
            }
        } else {
            if (fins) {
                if (dialogDismiss) {
                    setting.show();
                    dialogDismiss = false;
                }
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA);
            }
        }
    }

    @Override
    protected void onStart() {
        // 首先是判断
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requsetPermisstion(false);
        } else {
            zBarScannerView.startCamera();
        }
        super.onStart();
    }

    /**
     * @param data   预览图源数据
     * @param width  相机宽度
     * @param height 相机高度
     * @return
     */
    public static Bitmap decodeYUV2Bitmap(byte[] data, int width, int height) {
        ByteArrayOutputStream baos;
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        newOpts.inJustDecodeBounds = true;
        YuvImage yuvimage = new YuvImage(
                data,
                ImageFormat.NV21,
                width,
                height,
                null);
        baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new Rect(0, 0, width, height), 100, baos);// 80--JPG图片的质量[0-100],100最高
        byte[] rawImage = baos.toByteArray();
        //将rawImage转换成bitmap
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length, options);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        menu.add("相册").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem menuItem) {
//                Toast.makeText(getBaseContext(), "相册", Toast.LENGTH_LONG).show();
//                return false;
//            }
//        }).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    private void choosePhoto() {
        /**
         * 打开选择图片的界面
         */
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");//相片类型
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);

    }

    private void findViewById() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        flashlight = (ImageView) findViewById(R.id.img_flashlight);
        flashlight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (zBarScannerView != null) {
                    if (zBarScannerView.isFlashOn()) {
                        zBarScannerView.setFlash(false);
                        flashlight.setImageDrawable(getResources().getDrawable(R.drawable.flashlight_off));
                    } else {
                        zBarScannerView.setFlash(true);
                        flashlight.setImageDrawable(getResources().getDrawable(R.drawable.flashlight_on));
                    }
                }
            }
        });
        scanCropView = (RelativeLayout) findViewById(R.id.capture_crop_view);
        cropView = (ImageView) findViewById(R.id.capture_mask_bottom);
        scanLine = (ImageView) findViewById(R.id.capture_scan_line);
        scanLine.setAnimation(animation);
        zBarScannerView = (ZBarScannerView) findViewById(R.id.zsanView);
        zBarScannerView.setResultHandler(new ZBarScannerView.ResultHandler() {
            @Override
            public void handleResult(Result rawResult) {
                //      Toast.makeText(ScanActivity.this, "Contents = " + rawResult.getContents( ), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent();
                intent.putExtra("result", rawResult.getContents());
                setResult(RESULT_OK, intent);
                finish();
//                //2秒后再次识别
//                new Handler().postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        zBarScannerView.getOneMoreFrame();//再获取一帧图像数据进行识别
//                    }
//                }, 2000);
            }
        });
        QRView qrView = new QRView(this);
        //设置扫码区域
        qrView.setCropView(scanCropView);
        zBarScannerView.setViewFinderView(qrView);
        cropView.setVisibility(View.GONE);
        zBarScannerView.setResult(new ZBarScannerView.CropResult() {
            @Override
            public void result(byte[] data, int w, int h, Rect rect) {
                if (isDebug) {
                    cropView.setVisibility(View.VISIBLE);
                    final Bitmap bitmap = decodeYUV2Bitmap(data, w, h);
                    final Bitmap bitmap1 = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            cropView.setImageBitmap(bitmap1);
                        }
                    });
                 }
            }
        });
        zBarScannerView.setShouldAdjustFocusArea(true);
        builder = new AlertDialog.Builder(this).
                setTitle("相机权限申请").setMessage("扫码需要相机权限,请授予!").setCancelable(false)
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(getBaseContext(), "取消了", Toast.LENGTH_LONG).show();
                    }
                }).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(ScanActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA);
                    }
                });
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                dialogDismiss = true;
            }
        });
        setting = new AlertDialog.Builder(this).
                setTitle("相机权限申请").setMessage("扫码需要相机权限,请授予!").setCancelable(false)
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(getBaseContext(), "取消了", Toast.LENGTH_LONG).show();
                    }
                }).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        gotoSetting();
                    }
                });
        setting.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                dialogDismiss = true;
            }
        });
    }

    @Override
    protected void onPause() {
        flashlight.setImageDrawable(getResources().getDrawable(R.drawable.flashlight_off));
        zBarScannerView.stopCamera();//释放相机资源等各种资源
        super.onPause();
    }

    /**
     * 去设置
     */
    public void gotoSetting() {
        Intent localIntent = new Intent();
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 9) {
            localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            localIntent.setData(Uri.fromParts("package", getPackageName(), null));
        } else if (Build.VERSION.SDK_INT <= 8) {
            localIntent.setAction(Intent.ACTION_VIEW);
            localIntent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
            localIntent.putExtra("com.android.settings.ApplicationPkgName", getPackageName());
        }
        startActivity(localIntent);
    }
}
