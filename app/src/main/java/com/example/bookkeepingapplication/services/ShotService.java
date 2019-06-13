package com.example.bookkeepingapplication.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import androidx.core.app.NotificationCompat;

import com.example.bookkeepingapplication.MainActivity;
import com.example.bookkeepingapplication.R;
import com.example.bookkeepingapplication.utils.MyApplication;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ShotService extends Service {

    private static final String TAG = "FloatingButtonService";

    private MediaProjectionManager mMediaProjectionManager = null;

    private MediaProjection mMediaProjection = null;
    private VirtualDisplay mVirtualDisplay = null;

    private WindowManager mWindowManager1 = null;
    public static int mResultCode = 0;
    public static Intent mResultData = null;
    public static MediaProjectionManager mMediaProjectionManager1 = null;

    private int windowWidth = 1920;
    private int windowHeight = 1080;
    private ImageReader mImageReader = null;
    private int mScreenDensity = 0;
    private DisplayMetrics metrics = null;

    public ShotService() {
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        Log.d(TAG, "onCreate: ");
        sendChatMsg();  //生成通知栏消息

        startScreenCapture();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

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

    private void startScreenCapture() {

        mWindowManager1 = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        windowWidth = mWindowManager1.getDefaultDisplay().getWidth();
        windowHeight = mWindowManager1.getDefaultDisplay().getHeight();
        metrics = new DisplayMetrics();
        mWindowManager1.getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;


        Handler handler1 = new Handler();
        handler1.postDelayed(new Runnable() {
            public void run() {
                //start virtual
                startVirtual();
            }
        }, 500);

        Handler handler2 = new Handler();
        handler2.postDelayed(new Runnable() {
            public void run() {
                //capture the screen
                startCapture();
            }
        }, 1500);

        Handler handler3 = new Handler();
        handler3.postDelayed(new Runnable() {
            public void run() {
                stopVirtual();
            }
        }, 1000);

    }

    public void startVirtual() {
        if (mMediaProjection != null) {
            Log.i(TAG, "want to display virtual");
            virtualDisplay();
        } else {
            Log.i(TAG, "start screen capture intent");
            Log.i(TAG, "want to build mediaprojection and display virtual");
            setUpMediaProjection();
            virtualDisplay();
        }
    }

    public void setUpMediaProjection() {
        mResultData = ((MyApplication) getApplication()).getIntent();
        mResultCode = ((MyApplication) getApplication()).getResult();
        mMediaProjectionManager1 = ((MyApplication) getApplication()).getmMediaProjectionManager();
        mMediaProjection = mMediaProjectionManager1.getMediaProjection(mResultCode, mResultData);
        Log.i(TAG, "mMediaProjection defined");
    }

    private void startCapture() {
        Image image = mImageReader.acquireLatestImage();
        int width = image.getWidth();
        int height = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
        image.close();
        Log.i(TAG, "image data captured");

        if (bitmap != null) {
            saveBitmap(bitmap);
        }
    }

    private void virtualDisplay() {
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-mirror",
                windowWidth, windowHeight, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mImageReader.getSurface(), null, null);
        Log.i(TAG, "virtual displayed");
    }

    private void saveBitmap(Bitmap bitmap) {
        String pathImage = Environment.getExternalStorageDirectory().getPath() + "/Pictures/";
        String imageName = pathImage + System.currentTimeMillis() + ".jpg";

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
    }

    @Override
    public void onDestroy() {
        // to remove mFloatLayout from windowManager
        super.onDestroy();
        tearDownMediaProjection();
        Log.i(TAG, "application destroy");
    }

    private void tearDownMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        Log.i(TAG, "mMediaProjection undefined");
    }

    private void stopVirtual() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        mVirtualDisplay = null;
        Log.i(TAG,"virtual display stopped");
    }
}
