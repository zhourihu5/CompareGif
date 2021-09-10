#include <jni.h>
#include <string>
#include "gif_lib.h"
#include <android/log.h>
#include <android/bitmap.h>
#include <malloc.h>

#define  LOG_TAG    "david"
#define  argb(a,r,g,b) ( ((a) & 0xff) << 24 ) | ( ((b) & 0xff) << 16 ) | ( ((g) & 0xff) << 8 ) | ((r) & 0xff)

#define  dispose(ext) (((ext)->Bytes[0] & 0x1c) >> 2)
#define  trans_index(ext) ((ext)->Bytes[3])
#define  transparency(ext) ((ext)->Bytes[0] & 1)
#define  delay(ext) (10*((ext)->Bytes[2] << 8 | (ext)->Bytes[1]))
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

typedef struct GifBean{
    int current_frame;
    int total_frame;
    int  *dealys;
} GifBean;

//绘制一张图片
void drawFrame(GifFileType *gifFileType, GifBean *gifBean, AndroidBitmapInfo info, void *pixels) {
    //播放底层代码
//        拿到当前帧
    SavedImage savedImage = gifFileType->SavedImages[gifBean->current_frame];

    GifImageDesc frameInfo = savedImage.ImageDesc;
    //整幅图片的首地址
    int* px = (int *)pixels;
//    每一行的首地址
    int *line;

//   其中一个像素的位置  不是指针  在颜色表中的索引
    int  pointPixel;
    GifByteType  gifByteType;
    GifColorType gifColorType;
    ColorMapObject* colorMapObject=frameInfo.ColorMap;
    px = (int *) ((char*)px + info.stride * frameInfo.Top);
    for (int y =frameInfo.Top; y < frameInfo.Top+frameInfo.Height; ++y) {
        line=px;
        for (int x = frameInfo.Left; x< frameInfo.Left + frameInfo.Width; ++x) {
            pointPixel = (y - frameInfo.Top) * frameInfo.Width + (x - frameInfo.Left);
            gifByteType = savedImage.RasterBits[pointPixel];
            gifColorType = colorMapObject->Colors[gifByteType];
            line[x] = argb(255,gifColorType.Red, gifColorType.Green, gifColorType.Blue);
        }
        px = (int *) ((char*)px + info.stride);
    }

}


extern "C"
JNIEXPORT jint JNICALL
Java_com_dongnao_comparegif_GifHandler_getWidth(JNIEnv *env, jclass type, jlong ndkGif) {
    GifFileType* gifFileType= (GifFileType *) ndkGif;
    return gifFileType->SWidth;

}extern "C"
JNIEXPORT jint JNICALL
Java_com_dongnao_comparegif_GifHandler_getHeight(JNIEnv *env, jclass type, jlong ndkGif) {

    GifFileType* gifFileType= (GifFileType *) ndkGif;
    return gifFileType->SHeight;

} extern "C"
JNIEXPORT jlong JNICALL
Java_com_dongnao_comparegif_GifHandler_loadGif(JNIEnv *env, jclass type, jstring path_) {
    const char *path = env->GetStringUTFChars(path_, 0);
    int err;
//用系统函数打开一个gif文件   返回一个结构体，这个结构体为句柄
    GifFileType * gifFileType=DGifOpenFileName(path, &err);

    DGifSlurp(gifFileType);
    GifBean *gifBean = (GifBean *) malloc(sizeof(GifBean));


//    清空内存地址
    memset(gifBean, 0, sizeof(GifBean));
    gifFileType->UserData=gifBean;

    gifBean->dealys = (int *) malloc(sizeof(int) * gifFileType->ImageCount);
    memset(gifBean->dealys, 0, sizeof(int) * gifFileType->ImageCount);
    gifBean->total_frame = gifFileType->ImageCount;
    ExtensionBlock* ext;
    for (int i = 0; i < gifFileType->ImageCount; ++i) {
        SavedImage frame = gifFileType->SavedImages[i];
        for (int j = 0; j < frame.ExtensionBlockCount; ++j) {
            if (frame.ExtensionBlocks[j].Function == GRAPHICS_EXT_FUNC_CODE) {
                ext = &frame.ExtensionBlocks[j];
                break;
            }
        }
        if (ext) {
            int frame_delay = 10 * (ext->Bytes[2] << 8 | ext->Bytes[1]);
            LOGE("时间  %d   ",frame_delay);
            gifBean->dealys[i] = frame_delay;

        }
    }
    LOGE("gif  长度大小    %d  ",gifFileType->ImageCount);
    env->ReleaseStringUTFChars(path_, path);
    return (jlong) gifFileType;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_dongnao_comparegif_GifHandler_updateFrame(JNIEnv *env, jclass type, jobject bitmap,
                                                   jlong ndkGif) {

    //强转代表gif图片的结构体
    GifFileType *gifFileType= (GifFileType *)ndkGif;
    GifBean * gifBean= (GifBean *) gifFileType->UserData;
    AndroidBitmapInfo info;
    //代表一幅图片的像素数组
    void *pixels;
    AndroidBitmap_getInfo(env,bitmap,&info);
    //锁定bitmap  一幅图片--》二维 数组   ===一个二维数组
    AndroidBitmap_lockPixels(env,bitmap,&pixels);
    drawFrame(gifFileType, gifBean, info, pixels );

    //播放完成之后   循环到下一帧
    gifBean->current_frame+=1;
    LOGE("当前帧  %d  ",gifBean->current_frame);
    if (gifBean->current_frame >= gifBean->total_frame-1) {
        gifBean->current_frame=0;
        LOGE("重新过来  %d  ",gifBean->current_frame);
    }
    AndroidBitmap_unlockPixels(env, bitmap);
    return gifBean->dealys[gifBean->current_frame];

}