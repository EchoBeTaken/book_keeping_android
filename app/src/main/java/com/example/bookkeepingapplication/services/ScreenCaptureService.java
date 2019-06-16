package com.example.bookkeepingapplication.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.bookkeepingapplication.MainActivity;
import com.example.bookkeepingapplication.R;
import com.example.bookkeepingapplication.utils.MyApplication;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;


//另一种实现截屏方式，在原有基础上改进
public class ScreenCaptureService extends Service {

    private static final String TAG = "ScreenCaptureService";

    //储存在application中的数据
    private int mResultCode;
    private Intent mResultData;
    private MediaProjectionManager mMediaProjectionManager = null;

    //新建
    private MediaProjection mMediaProjection;
    private ImageReader mImageReader;
    private int windowWidth = 1080;
    private int windowHeight = 1920;
    private int mScreenDensity = 400;
    private VirtualDisplay mVirtualDisplay;
    private Bitmap bmp = null;
    private Bitmap croppedBitmap = null;


    private Timer timer = new Timer();
    private int Time = 1000*3;//周期时间


    public ScreenCaptureService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //用通知表示前台任务
        sendChatMsg();

        mResultCode = ((MyApplication) getApplication()).getResult();
        mResultData = ((MyApplication)getApplication()).getIntent();
        mMediaProjectionManager = ((MyApplication)getApplication()).getMediaProjectionManager();
        mMediaProjection = mMediaProjectionManager.getMediaProjection(mResultCode, mResultData);

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                System.out.println("99999998");
                Log.d(TAG, "run: aaaaaaaaaaa");
                startGetCapture();
            }
        };
        timer.schedule(timerTask,
                1000,//延迟1秒执行
                Time);//周期时间

//        startGetCapture();

    }

    private void startGetCapture() {
        mImageReader = ImageReader.newInstance(windowWidth, windowHeight, 0x1, 2); //ImageFormat.RGB_565
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-mirror",
                windowWidth, windowHeight, mScreenDensity,       DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mImageReader.getSurface(), null, null);
//                getImage();

        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader mImageReader) {

                Image image = null;
                try {
                    image = mImageReader.acquireLatestImage();
                    if (image != null) {
                        final Image.Plane[] planes = image.getPlanes();
                        if (planes.length > 0) {
                            final ByteBuffer buffer = planes[0].getBuffer();
                            int pixelStride = planes[0].getPixelStride();
                            int rowStride = planes[0].getRowStride();
                            int rowPadding = rowStride - pixelStride * windowWidth;


                            // create bitmap
                            bmp = Bitmap.createBitmap(windowWidth + rowPadding / pixelStride,
                                    windowHeight, Bitmap.Config.ARGB_8888);
                            bmp.copyPixelsFromBuffer(buffer);

                            croppedBitmap = Bitmap.createBitmap(bmp, 0, 0, windowWidth, windowHeight);

//                                    runOnUiThread(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            mIvShow.setImageBitmap(croppedBitmap);
//                                            mainLayout.setVisibility(View.VISIBLE);
//                                        }
//                                    });

//                            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
//                                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
//                                    //没有权限则申请权限
//                                    ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
//                                    Log.d(TAG, "onImageAvailable: 1111");
//                                }else {
//                                    //有权限直接执行,docode()不用做处理
//                                    Log.d(TAG, "onImageAvailable: 2222");
//                                    saveBitmap(croppedBitmap);
//
//                                }
//                            }else {
//                                //小于6.0，不用申请权限，直接执行
//                                Log.d(TAG, "onImageAvailable: 3333");
//                                saveBitmap(croppedBitmap);
//                            }
                            saveBitmap(croppedBitmap);



                            if (croppedBitmap != null) {
                                croppedBitmap.recycle();
                            }
                            if (bmp != null) {
                                bmp.recycle();
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (image != null) {
                        image.close();
                    }
                    if (mImageReader != null) {
                        mImageReader.close();
                    }
                    if (mVirtualDisplay != null) {
                        mVirtualDisplay.release();
                    }

                    mImageReader.setOnImageAvailableListener(null, null);
//                mProjection.stop();

//                onScreenshotTaskOver();
                }

            }
        }, getBackgroundHandler());

    }

    Handler backgroundHandler;

    private Handler getBackgroundHandler() {
        if (backgroundHandler == null) {
            HandlerThread backgroundThread =
                    new HandlerThread("catwindow", android.os.Process
                            .THREAD_PRIORITY_BACKGROUND);
            backgroundThread.start();
            backgroundHandler = new Handler(backgroundThread.getLooper());
        }
        return backgroundHandler;
    }

    private void saveBitmap(Bitmap bitmap) {
        String pathImage = Environment.getExternalStorageDirectory().getPath()+"/Pictures/";
        String imageName = pathImage + System.currentTimeMillis() + ".jpg";
        Log.d(TAG, "saveBitmap: " + imageName);
        try {
            File fileImage = new File(imageName);
            if(!fileImage.exists()){
                fileImage.createNewFile();
                Log.i(TAG, "image file created");
            }
            FileOutputStream out = new FileOutputStream(fileImage);
            if(out != null) {
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
    }

    //生成一个通知，表示程序当前正在运行
    public void sendChatMsg() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new NotificationCompat.Builder(this, "chat")
                    .setContentTitle("正在运行")
                    .setContentText("正在截屏...")
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_background))
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setContentIntent(pendingIntent)
                    .build();
        } else {
            notification = new NotificationCompat.Builder(this)
                    .setContentTitle("正在运行")
                    .setContentText("正在截屏...")
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_background))
                    .setAutoCancel(false)
                    .setContentIntent(pendingIntent)
                    .build();
        }

        manager.notify(1, notification);
    }

}
