package cn.lzq.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import cn.lzq.zbarscanner.demo.ScanActivity;
import cn.lzq.zbarscanner.demo.SimpleScannerActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void zxing(View view) {//startActivityFromChild
        startActivityForResult(new Intent(this, ScanActivity.class),66);
    }

    public void drawanimaiton(View view) {
        startActivityForResult(new Intent(this, SimpleScannerActivity.class),66);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (data != null) {
                String result = data.getStringExtra("result");
                Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
