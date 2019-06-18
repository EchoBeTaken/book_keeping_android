package com.example.bookkeepingapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.bookkeepingapplication.services.ScreenCaptureService;
import com.example.bookkeepingapplication.utils.MyApplication;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_MEDIA_PROJECTION = 1;

    private Button mBtnStart;
    private Button mBtnStop;

    private Intent intent = null;
    private int result = 0;
    private MediaProjectionManager mMediaProjectionManager = null;

    String imagePath = "";
    private Timer timer = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnStart = findViewById(R.id.btn_start);
        mBtnStop = findViewById(R.id.btn_stop);

        mBtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: 开始服务");
//                finish();
//                Intent startIntent = new Intent(getApplicationContext(), ShotService.class);
//                startService(startIntent);
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.test);
                if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
                        //没有权限则申请权限
                        Log.d(TAG, "onImageAvailable: 1111");
                        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                    }else {
                        //有权限直接执行,docode()不用做处理
                        Log.d(TAG, "onImageAvailable: 2222");
                        imagePath = saveBitmap(bitmap);
                    }
                }else {
                    //小于6.0，不用申请权限，直接执行
                    Log.d(TAG, "onImageAvailable: 3333");
                    imagePath = saveBitmap(bitmap);
                }
//                Log.d(TAG, "onClick: " + imagePath.substring(imagePath.indexOf("\\/"), ima));

                startService();
            }
        });

        mBtnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: 结束服务");
                Intent stopIntent = new Intent(getApplicationContext(), ScreenCaptureService.class);
                stopService(stopIntent);
            }
        });

        //通知栏适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "chat";
            String channelName = "聊天消息";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            createNotificationChannel(channelId, channelName, importance);

        }

        mMediaProjectionManager = (MediaProjectionManager) getApplication().getSystemService(Context.MEDIA_PROJECTION_SERVICE);

//        startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), 1);
//        startService();
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createNotificationChannel(String channelId, String channelName, int importance) {
        NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
        NotificationManager notificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);
    }

    private void startService() {
//        ((MyApplication) getApplication()).setmMediaProjectionManager(mMediaProjectionManager);
        if (intent != null && result != 0) {
            ((MyApplication) getApplication()).setResult(result);
            ((MyApplication) getApplication()).setIntent(intent);
            Intent intent = new Intent(getApplicationContext(), ScreenCaptureService.class);
            startService(intent);
        } else {
            startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
//            ((MyApplication) getApplication()).setmMediaProjectionManager(mMediaProjectionManager);
        }
        ((MyApplication) getApplication()).setMediaProjectionManager(mMediaProjectionManager);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode != Activity.RESULT_OK) {
                return;
            } else if (data != null && resultCode != 0) {
                result = resultCode;
                intent = data;
                ((MyApplication) getApplication()).setResult(resultCode);
                ((MyApplication) getApplication()).setIntent(data);
                Intent intent = new Intent(getApplicationContext(), ScreenCaptureService.class);
                startService(intent);
                finish();
            }
        }
    }

    private String saveBitmap(Bitmap bitmap) {
        String pathImage = Environment.getExternalStorageDirectory().getPath() + "/Pictures/";
        String imageName = pathImage + System.currentTimeMillis() + ".jpg";
        Log.d(TAG, "saveBitmap: " + imageName);

        try {
            File fileImage = new File(imageName);
            if (!fileImage.exists()) {
                fileImage.createNewFile();
                Log.i(TAG, "image file created");
            }
            FileOutputStream out = new FileOutputStream(fileImage);
            if (out != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();
                Intent media = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(fileImage);
                media.setData(contentUri);
                this.sendBroadcast(media);
                Log.i(TAG, "screen image saved");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageName;
    }

}
