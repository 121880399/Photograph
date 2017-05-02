package com.zhongche.photo;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.zhongche.photo.utils.BitmapTools;
import com.zhongche.photo.utils.DialogManager;
import com.zhongche.photo.utils.ImageUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int DOCAMERA = 1;
    private static final String DIRECTORY = "/photoAPP";
    private Button mTakePhotoBtn;
    private ImageView mImageIv;
    private TextView mPathTv;
    private StringBuilder mImagePath;
    private StringBuilder mOriginImagePath;
    //需要存储卡和相机权限
    private String[] mPermissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initData();
        initView();
    }

    private void initData() {
        mImagePath = new StringBuilder();
        mOriginImagePath = new StringBuilder();
        mImagePath.append(Environment.getExternalStorageDirectory().toString());
        mImagePath.append(DIRECTORY);
        mOriginImagePath.append(Environment.getExternalStorageDirectory().toString());
        mOriginImagePath.append(DIRECTORY);
        mOriginImagePath.append("/originImage.png");
    }

    private void initView() {
        mTakePhotoBtn = (Button) findViewById(R.id.btn_takePhoto);
        mImageIv = (ImageView) findViewById(R.id.iv_image);
        mPathTv = (TextView) findViewById(R.id.tv_path);
        mTakePhotoBtn.setOnClickListener(this);
    }


    /**
     * 此方法用于判断是否有存储SD卡权限和SD是否可以存储
     */
    private boolean isAbleStore() {
        requestAuthority();
        if (android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            Toast.makeText(this, "SD卡不存在", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * 安卓6.0以上需要请求权限
     */
    private void requestAuthority() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission : mPermissions) {
                //判断是否授权
                int result = checkSelfPermission(permission);
                if (result == PackageManager.PERMISSION_GRANTED) {
                    continue;
                } else {
                    //判断是否需要向用户说明权限申请原因
                    if (shouldShowRequestPermissionRationale(permission)) {
                        //以对话框的形式呈现，提醒用户设置权限
                        DialogManager.getInstance().showMessageDialogWithDoubleButton(this, "权限申请",
                                "为了不影响使用请开启以下权限\n1、读写手机存储\n2、相机\n", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        switch (v.getId()) {
                                            case R.id.btn_cancle_on_dialog:
                                                break;
                                            case R.id.btn_confirm_on_dialog:
                                                Intent localIntent = new Intent();
                                                localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                if (Build.VERSION.SDK_INT > 8) {
                                                    localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                                                    localIntent.setData(Uri.fromParts("package", getPackageName(), null));
                                                } else {
                                                    localIntent.setAction(Intent.ACTION_VIEW);
                                                    localIntent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
                                                    localIntent.putExtra("com.android.settings.ApplicationPkgName", getPackageName());
                                                }
                                                startActivity(localIntent);
                                                break;
                                        }
                                    }
                                }, "前往开启", "取消");
                    } else {
                        //申请权限
                        requestPermissions(mPermissions, 1);
                    }
                }
                return;
            }
        }
    }

    /**
     * 调用系统相机拍照
     */
    public void doCarmer() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra("autofocus", true); // 自动对焦
        //必须使用ContentValue传递uri，否则会报错
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(MediaStore.Images.Media.DATA,mOriginImagePath.toString());
        Uri uri = this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contentValues);
        //设置将原图保存在给定地址
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        startActivityForResult(intent, DOCAMERA);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_takePhoto:
                if (isAbleStore()) {
                    mImagePath.append("/");
                    mImagePath.append(System.currentTimeMillis());
                    mImagePath.append(".png");
                    doCarmer();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case DOCAMERA:
                    try {
                        //得到当前时间
                        Date date = new Date();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String currentTime = sdf.format(date);
                        //从存储卡中获取原始图片
                        Bitmap originBitmap = BitmapTools.getBitmap(mOriginImagePath.toString());
                        // 获取图片的旋转角度，有些系统把拍照的图片旋转了，有的没有旋转
                        int degree = BitmapTools.readPictureDegree(mOriginImagePath.toString());
                        //对旋转过后的图片进行还原
                        Bitmap newBitmap=BitmapTools.rotaingImageView(degree,originBitmap);
                        //在图片上面添加水印
                        Bitmap outBitmap = ImageUtil.drawTextToRightBottom(this, newBitmap, currentTime, 20, Color.WHITE, 2, 2);
                        //将添加水印的图片保存到存储卡
                        BitmapTools.saveBitmap(mImagePath.toString(), outBitmap);
                        //在UI上面显示
                        mImageIv.setImageBitmap(outBitmap);
                        mPathTv.setText(mImagePath.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }


}
