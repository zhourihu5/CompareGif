package com.dongnao.comparegif;

import android.graphics.Bitmap;

/**
 * Created by Administrator on 2017/4/25 0025.
 */

public class GifHandler {
    private long gifPoint;
    static {
        System.loadLibrary("native-lib");
    }
    public static native int getWidth(long gifPoint);
    public static native int getHeight(long gifPoint);
    public static native long loadGif(String path);
    public static native int updateFrame(Bitmap bitmap, long gifPoint);
    public GifHandler(long gifPoint) {
        this.gifPoint = gifPoint;
    }

    public static GifHandler load(String path) {
        long gifHander=loadGif(path);
        GifHandler gifHandler=new GifHandler(gifHander);
        return gifHandler;
    }

    public long getGifPoint() {
        return gifPoint;
    }
}
