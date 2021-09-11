package com.dongnao.comparegif;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "david";
    ImageView image;
    PlayGifTask mGifTask;
    GifHandler gifHandler;
    Bitmap bitmap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        image= (ImageView) findViewById(R.id.image);
        requestPermision();
    }
    //读写权限
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};
    //请求状态码
    private static int REQUEST_PERMISSION_CODE = 1;


    public void requestPermision() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_PERMISSION_CODE);
                return;
            }
            else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_PERMISSION_CODE);
                return;
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            boolean granted=true;
            for (int i = 0; i < permissions.length; i++) {
                Log.i("MainActivity", "申请的权限为：" + permissions[i] + ",申请结果：" +
                        grantResults[i]);
                if(grantResults[i]!= PackageManager.PERMISSION_GRANTED){
                    granted=false;
                    break;
                }
            }
            if(granted){
            }
        }
    }

    public void javaLoadGif(View view) {
        //对Gif图片进行解码
        InputStream fis =null;

        try {
            fis = new FileInputStream(new File(Environment.getExternalStorageDirectory(),"demo.gif"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        GifHelper gifHelper=new GifHelper();
        gifHelper.read(fis);
        mGifTask = new PlayGifTask(image, gifHelper.getFrames());
        mGifTask.startTask();
        Thread th=new Thread(mGifTask);
        th.start();
    }


    public void ndkLoadGif(View view) {
        File file=new File(Environment.getExternalStorageDirectory(),"demo.gif");
        gifHandler=GifHandler.load(file.getAbsolutePath());
        int width=gifHandler.getWidth(gifHandler.getGifPoint());
        int height=gifHandler.getHeight(gifHandler.getGifPoint());
        Log.i("david","宽   "+width+"   高  "+height);
        bitmap = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        long mNextFrameRenderTime=gifHandler.updateFrame(bitmap,gifHandler.getGifPoint());
        Log.i(TAG, "onCreate   下一帧  "+mNextFrameRenderTime);
        myHandler.sendEmptyMessageDelayed(1, mNextFrameRenderTime);
    }

    Handler myHandler=new Handler()
    {
        public void handleMessage(Message msg) {
            long mNextFrameRenderTime=gifHandler.updateFrame(bitmap,gifHandler.getGifPoint());
            myHandler.sendEmptyMessageDelayed(1,mNextFrameRenderTime);
            image.setImageBitmap(bitmap);
        };
    };
    //用来循环播放Gif每帧图片
    private class PlayGifTask implements Runnable {
        int i = 0;
        ImageView iv;
        GifHelper.GifFrame[] frames;
        int framelen,oncePlayTime=0;

        public PlayGifTask(ImageView iv, GifHelper.GifFrame[] frames) {
            this.iv = iv;
            this.frames = frames;

            int n=0;
            framelen=frames.length;
            while(n<framelen){
                oncePlayTime+=frames[n].delay;
                n++;
            }
            Log.d("msg", "playTime= "+oncePlayTime);

        }

        Handler h2=new Handler(){
            public void handleMessage(Message msg) {
                switch(msg.what){
                    case 1:
                        iv.setImageBitmap((Bitmap)msg.obj);
                        break;
                }
            };
        };
        @Override
        public void run() {
            if (!frames[i].image.isRecycled()) {
                //      iv.setImageBitmap(frames[i].image);
                Message m= Message.obtain(h2, 1, frames[i].image);
                m.sendToTarget();
            }
            iv.postDelayed(this, frames[i++].delay);
            i %= framelen;
        }

        public void startTask() {
            iv.post(this);
        }

        public void stopTask() {
            if(null != iv) iv.removeCallbacks(this);
            iv = null;
            if(null != frames) {
                for(GifHelper.GifFrame frame : frames) {
                    if(frame.image != null && !frame.image.isRecycled()) {
                        frame.image.recycle();
                        frame.image = null;
                    }
                }
                frames = null;
            }
        }
    }
}
