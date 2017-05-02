package com.zhongche.photo.utils;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * 位图工具类，该类提供根据不同的资源获取指定尺寸要求的图片
 *
 * @author xby
 *         <p>
 *         2013-3-26上午10:00:28
 */
public class BitmapTools {
    /**
     * 将资源文件下的图片转换成String
     *
     * @param id 图片资源id
     * @return 转换后的String
     */
    public String bitmaptoString(Context context, int id) {
        Bitmap bitmap = ((BitmapDrawable) context.getResources().getDrawable(id))
                .getBitmap();
        String string = null;
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        bitmap.compress(CompressFormat.PNG, 100, bStream);
        byte[] bytes = bStream.toByteArray();
        string = Base64.encodeToString(bytes, Base64.DEFAULT);
        return string;
    }

    /**
     * 将Bitmap转换成Base64 String
     *
     * @return 转换后的String
     */
    public static String bitmaptoString(Bitmap bitmap) {
        String string = null;
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        bitmap.compress(CompressFormat.PNG, 100, bStream);
        byte[] bytes = bStream.toByteArray();
        string = Base64.encodeToString(bytes, Base64.DEFAULT);
        return string;
    }

    /**
     * 将字符串转换成Bitmap
     *
     * @param string 原字符串
     * @return Bitmap
     */
    public Bitmap stringtoBitmap(String string) {
        // 将字符串转换成Bitmap类型
        Bitmap bitmap = null;
        try {
            byte[] bitmapArray;
            bitmapArray = Base64.decode(string, Base64.DEFAULT);
            bitmap = BitmapFactory.decodeByteArray(bitmapArray, 0,
                    bitmapArray.length);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bitmap;
    }


    /**
     * 根据图片路径key,获取到对应的BitMap
     *
     * @param bitmapKey
     * @return
     */
    public static Bitmap getSoftReferenceMap(Context context, String bitmapKey,
                                             Map<String, SoftReference<Bitmap>> bitMapCaches) {
        Bitmap bitmap = null;

        // 从内存缓存中获取如果存在就返回
        if (bitMapCaches.containsKey(bitmapKey)) {
            bitmap = bitMapCaches.get(bitmapKey).get();
            // 如果缓存中的数据已经被释放，移除该路径
            if (bitmap == null) {
                bitMapCaches.remove(bitmapKey);
            } else
                return bitmap;
        }

        // 缓存中没有则从文件中读取
        File dir = context.getExternalFilesDir(
                Environment.DIRECTORY_PICTURES);
        // 创建需要的图片路径
        File file = new File(dir, bitmapKey);
        bitmap = getBitmap(file.getAbsolutePath());
        bitMapCaches.put(bitmapKey, new SoftReference<Bitmap>(bitmap));
        // 如果本地存在就从新放回到软引用并返回
        if (bitmap != null) {
            bitMapCaches.put(bitmapKey, new SoftReference<Bitmap>(bitmap));
            return bitmap;
        } else
            return null;
    }

    /**
     * 保存BitMap到软引用缓存，图片备份到sdcard
     *
     * @param bitmapKey
     * @param saveBitmap
     */
    private static void putBitmapToSoft(Context context, String bitmapKey, Bitmap saveBitmap,
                                        Map<String, SoftReference<Bitmap>> bitMapCaches) {
        bitMapCaches.put(bitmapKey, new SoftReference<Bitmap>(saveBitmap));
        // 向SD卡文件中添加缓存信息
        File dir = context.getExternalFilesDir(
                Environment.DIRECTORY_PICTURES);
        if (!dir.exists())
            dir.mkdirs();
        File file = new File(dir, bitmapKey);
        try {
            saveBitmap(file.getAbsolutePath(), saveBitmap);
        } catch (IOException e) {
        }
        saveBitmap = null;

    }

    /**
     * 根据输入流获取位图像
     *
     * @param is 图片输入流
     * @return 返回输入流对应的图片
     */
    public static Bitmap getBitmap(InputStream is) {
        return BitmapFactory.decodeStream(is);
    }

    /**
     * 根据输入流和缩放比例获得位图像
     *
     * @param is    图片输入流
     * @param scale 缩放比例
     * @return 返回获取的缩放后的图片
     */
    public static Bitmap getBitmap(InputStream is, int scale) {
        Bitmap bitmap = null;
        Options opts = new Options();
        opts.inSampleSize = scale;
        bitmap = BitmapFactory.decodeStream(is, null, opts);
        return bitmap;
    }

    /**
     * 通过字符数组获取指定宽和高的图片
     *
     * @param bytes
     * @param width
     * @param height
     * @return
     */
    public static Bitmap getBitmap(byte[] bytes, int width, int height) {
        Bitmap bitmap = null;
        Options opts = new Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
        opts.inJustDecodeBounds = false;
        int scaleX = opts.outWidth / width;
        int scaleY = opts.outHeight / height;
        int scale = scaleX > scaleY ? scaleX : scaleY;
        opts.inSampleSize = scale;
        bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
        return bitmap;
    }


    /**
     * 根据输入流和宽和高获得位图像
     *
     * @param is     获取图片的输入流
     * @param width  想要获取的图片的宽度
     * @param height 想要获取的图片的高度
     * @return 返回获取到的图片
     */
    public static Bitmap getBitmap(InputStream is, int width, int height) {
        Bitmap bitmap = null;
        Options opts = new Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, new Rect(0, 0, 0, 0), opts);
        opts.inJustDecodeBounds = false;
        int saleX = opts.outWidth / width;
        int saleY = opts.outHeight / height;
        int sale = saleX > saleY ? saleX : saleY;
        opts.inSampleSize = sale;
        bitmap = BitmapFactory.decodeStream(is, null, opts);
        return bitmap;
    }

    /**
     * 从文件中获取位图像
     *
     * @param path
     * @return
     */
    public static Bitmap getBitmap(String path) {
        BitmapFactory.Options opts=new BitmapFactory.Options();//获取缩略图显示到屏幕上
        opts.inSampleSize=2;
        return BitmapFactory.decodeFile(path,opts);
    }

    /**
     * 保存位图对象到指定位置
     *
     * @param path   文件保存路径
     * @param bitmap 所要保存的图片
     * @throws IOException
     */
    public static void saveBitmap(String path, Bitmap bitmap)
            throws IOException {
        if (path != null && bitmap != null) {
            File file = new File(path);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            OutputStream stream = new FileOutputStream(file);
            String name = file.getName();
            String end = name.substring(name.lastIndexOf('.') + 1);
            if ("png".equals(end)) {
                bitmap.compress(CompressFormat.PNG, 100, stream);
            } else {
                bitmap.compress(CompressFormat.JPEG, 100, stream);
            }
        }

    }

    /**
     * 保存位图对象到指定位置
     *
     * @param path   文件保存路径
     * @param bitmap 所要保存的图片
     * @throws IOException
     */
    public static void saveBitmap(String path, String fileName, Bitmap bitmap)
            throws IOException {
        if (path != null && bitmap != null) {
            File file = new File(path);
            if (!file.exists()) {
                file.mkdir();
            }
            File newFile = new File(file.toString() + "/" + fileName);
            OutputStream stream = new FileOutputStream(newFile);
            String name = file.getName();
            String end = name.substring(name.lastIndexOf('.') + 1);
            if ("png".equals(end)) {
                bitmap.compress(CompressFormat.PNG, 100, stream);
            } else {
                bitmap.compress(CompressFormat.JPEG, 100, stream);
            }
        }

    }

    /**
     * 保存位图对象到指定位置
     *
     * @param path   文件保存路径
     * @param bitmap 所要保存的图片
     * @throws IOException
     */
    public static void saveBitmap(String path, String fileName, Bitmap bitmap, int compressSile)
            throws IOException {
        if (path != null && bitmap != null) {
            File file = new File(path);
            if (!file.exists()) {
                file.mkdir();
            }
            File newFile = new File(file.toString() + "/" + fileName);
            OutputStream stream = new FileOutputStream(newFile);
            String name = file.getName();
            String end = name.substring(name.lastIndexOf('.') + 1);
            if ("png".equals(end)) {
                bitmap.compress(CompressFormat.PNG, compressSile, stream);
            } else {
                bitmap.compress(CompressFormat.JPEG, compressSile, stream);
            }
        }
    }

    public static int getBitmapSize(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {//API 12
            return bitmap.getByteCount();
        }
        return bitmap.getRowBytes() * bitmap.getHeight();                //earlier version
    }

    /**
     * 读取资源图片返回指定的宽高
     *
     * @param context 上下文对象
     * @param resId   资源ID
     * @param width   宽
     * @param height  高
     * @return
     */
    public static Bitmap readBitMap(Context context, int resId, int width,
                                    int height) {
        Options opts = new Options();
        // 获取资源图片
        InputStream is = context.getResources().openRawResource(resId);
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, opts);
        opts.inJustDecodeBounds = false;
        int scaleX = opts.outWidth / width;
        int scaleY = opts.outHeight / height;
        int scale = scaleX > scaleY ? scaleX : scaleY;
        opts.inSampleSize = scale;

        return BitmapFactory.decodeStream(is, null, opts);
    }

    // Read bitmap
    public static Bitmap readBitmap(Context context, Uri selectedImage) {
        Bitmap bm = null;
        Options options = new Options();
        options.inSampleSize = 5;
        AssetFileDescriptor fileDescriptor = null;
        try {
            fileDescriptor = context
                    .getContentResolver()
                    .openAssetFileDescriptor(selectedImage, "r");
        } catch (FileNotFoundException e) {
        } finally {
            try {
                bm = BitmapFactory.decodeFileDescriptor(
                        fileDescriptor.getFileDescriptor(), null, options);
                fileDescriptor.close();
            } catch (IOException e) {
            }
        }
        return bm;
    }

    // Clear bitmap
    public static void clearBitmap(Bitmap bm) {
        bm.recycle();
        System.gc();
    }

    /**
     * 把传进来的bitmap对象转换为宽度为x,长度为y的bitmap对象
     *
     * @param b 要更改的原位图
     * @param x 原位图的宽
     * @param y 原位图的高
     * @return
     */
    public static Bitmap getGivenXYBitmap(Bitmap b, float x, float y) {
        int w = b.getWidth();
        int h = b.getHeight();
        float sx = (float) x / w;// 要强制转换，不转换我的在这总是死掉。
        float sy = (float) y / h;
        Matrix matrix = new Matrix();
        matrix.postScale(sx, sy); // 长和宽放大缩小的比例
        Bitmap resizeBmp = Bitmap.createBitmap(b, 0, 0, w, h, matrix, true);
        return resizeBmp;
    }

    /**
     * 获取宽度为x,长度为y的bitmap对象
     *
     * @return
     */
    public static Bitmap getGivenXYBitmap(Context context, int resId, int width,
                                          int height) {
        Bitmap b = readBitMap(context, resId,
                width, height);
        int w = b.getWidth();
        int h = b.getHeight();
        float sx = (float) width / w;// 要强制转换，不转换我的在这总是死掉。
        float sy = (float) height / h;
        Matrix matrix = new Matrix();
        matrix.postScale(sx, sy); // 长和宽放大缩小的比例
        Bitmap resizeBmp = Bitmap.createBitmap(b, 0, 0, w, h, matrix, true);
        return resizeBmp;
    }

    /**
     * 根据文件路径和图片宽高获取图片
     *
     * @param path   文件路径
     * @param width  想要获取的图片的宽度
     * @param height 想要获取的图片的高度
     * @return 返回获取到的图片
     */

    public static Bitmap getBitmap(String path, int width, int height) {
        Bitmap bitmap = null;
        Options opts = new Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, opts);
        opts.inJustDecodeBounds = false;
        int scaleX = opts.outWidth / width;
        int scaleY = opts.outHeight / height;
        int scale = scaleX > scaleY ? scaleX : scaleY;
        opts.inSampleSize = scale;
        bitmap = BitmapFactory.decodeFile(path, opts);
        return bitmap;
    }

    /**
     * 图片圆角
     *
     * @param pBitmap
     * @param pRoundpx
     * @return
     */
    public static Bitmap RoundedCornerBitmap(Bitmap pBitmap, float pRoundpx) {

        Bitmap _NewBitmap = Bitmap.createBitmap(pBitmap.getWidth(),
                pBitmap.getHeight(), Bitmap.Config.ARGB_8888); // 创建图片画布大小
        Canvas _Canvas = new Canvas(_NewBitmap); // 创建画布
        _Canvas.drawARGB(0, 0, 0, 0); // 设置画布透明
        Paint _Paint = new Paint(); // 创建画笔
        _Paint.setAntiAlias(true); // 抗锯齿
        _Paint.setColor(0xff000000);// 画笔颜色透明

        // 画与原图片大小一致的圆角矩形
        Rect _Rect = new Rect(0, 0, pBitmap.getWidth(), pBitmap.getHeight());
        RectF _RectF = new RectF(_Rect);
        _Canvas.drawRoundRect(_RectF, pRoundpx, pRoundpx, _Paint);

        _Paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));// 设置下面张图片与上面张图片的交互模式
        _Canvas.drawBitmap(pBitmap, _Rect, _Rect, _Paint);// 画原图到画布
        return _NewBitmap;
    }

    public static Bitmap Drawable2Bitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static Drawable Bitmap2Drawable(Context context, Bitmap bitmap) {
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    /**
     * 图片压缩算法
     * */
    public static File compress(String path){
        File outputFile = new File(path);
        long fileSize = outputFile.length();
        if(fileSize==0){
            return null;
        }
        final long fileMaxSize = 200 * 1024;
        if (fileSize >= fileMaxSize) {
            Options options = new Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);
            int height = options.outHeight;
            int width = options.outWidth;

            double scale = Math.sqrt((float) fileSize / fileMaxSize);
            options.outHeight = (int) (height / scale);
            options.outWidth = (int) (width / scale);
            options.inSampleSize = (int) (scale + 0.5);
            options.inJustDecodeBounds = false;

            Bitmap bitmap = BitmapFactory.decodeFile(path, options);
            outputFile = new File(createImageFile().getPath());
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(outputFile);
                bitmap.compress(CompressFormat.JPEG, 50, fos);
                fos.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }else{
                File tempFile = outputFile;
                outputFile = new File(createImageFile().getPath());
                copyFileUsingFileChannels(tempFile, outputFile);
            }

        }
        return outputFile;

    }

    public static Uri createImageFile(){
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_ "+ timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = null;
        try {
            image = File.createTempFile(imageFileName,".jpg", storageDir);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Save a file: path for use with ACTION_VIEW intents
        return Uri.fromFile(image);
    }

    public static void copyFileUsingFileChannels(File source, File dest){
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            try {
                inputChannel = new FileInputStream(source).getChannel();
                outputChannel = new FileOutputStream(dest).getChannel();
                outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } finally {
            try {
                inputChannel.close();
                outputChannel.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /** 根据路径获取图片资源（已缩放）
     * * @param url 图片存储路径
    * @param width 缩放的宽度
    * @param height 缩放的高度
    * @return
     */
    public static Bitmap getBitmapFromUrl(String url, double width, double height) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; // 设置了此属性一定要记得将值设置为false
        Bitmap bitmap = BitmapFactory.decodeFile(url);
        // 防止OOM发生
        options.inJustDecodeBounds = false;
        int mWidth = bitmap.getWidth();
        int mHeight = bitmap.getHeight();
        Matrix matrix = new Matrix();
        float scaleWidth = 1;
        float scaleHeight = 1;
//        try {
//            ExifInterface exif = new ExifInterface(url);
//            String model = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        // 按照固定宽高进行缩放
        // 这里希望知道照片是横屏拍摄还是竖屏拍摄
        // 因为两种方式宽高不同，缩放效果就会不同
        // 这里用了比较笨的方式
        if(mWidth <= mHeight) {
            scaleWidth = (float) (width/mWidth);
            scaleHeight = (float) (height/mHeight);
        } else {
            scaleWidth = (float) (height/mWidth);
            scaleHeight = (float) (width/mHeight);
        }
//        matrix.postRotate(90); /* 翻转90度 */
        // 按照固定大小对图片进行缩放
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, mWidth, mHeight, matrix, true);
        // 用完了记得回收
        bitmap.recycle();
        return newBitmap;
    }

    /**
     * 读取图片属性：旋转的角度
     * @param path 图片绝对路径
     * @return degree旋转的角度
     */
    public static int readPictureDegree(String path) {
        int degree  = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }
    /*
     * 旋转图片
     * @param angle
     * @param bitmap
     * @return Bitmap
     */
    public static Bitmap rotaingImageView(int angle , Bitmap bitmap) {
        //旋转图片 动作
        Matrix matrix = new Matrix();;
        matrix.postRotate(angle);
        System.out.println("angle2=" + angle);
        // 创建新的图片
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return resizedBitmap;
    }
}
