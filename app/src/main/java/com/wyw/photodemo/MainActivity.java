package com.wyw.photodemo;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.wyw.photodemo.utils.TooltipUtils;

import java.io.File;

import butterknife.ButterKnife;
import butterknife.OnClick;

import static butterknife.internal.Utils.arrayOf;

public class MainActivity extends AppCompatActivity {

    protected int REQUEST_STORAGE_READ_ACCESS_PERMISSION = 101;
    protected int REQUEST_STORAGE_WRITE_ACCESS_PERMISSION = 102;

    private int GALLERY_REQUEST_CODE = 0;   // 相册选图标记
    private int CAMERA_REQUEST_CODE = 1;    // 相机拍照标记

    int REQUEST_CROP = 69; //裁剪成功标识
    int RESULT_ERROR = 96;//裁剪失败标识

    // 拍照临时图片
    private String mTempPhotoPath;
    // 剪切后图像文件
    private Uri mDestinationUri;

    private Activity mActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mActivity = this;


        mDestinationUri = Uri.fromFile(new File(mActivity.getCacheDir(), "cropImage.jpeg"));
        mTempPhotoPath = Environment.getExternalStorageDirectory().toString() + File.separator + "photo.jpeg";

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @OnClick({R.id.btn_takephoto, R.id.btn_pick})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_takephoto: {
                takePhoto();
            }
            break;
            case R.id.btn_pick: {
                pickFromGallery();
            }
            break;
        }
    }

    /**
     * 从相册中选择
     */
    private void pickFromGallery() {

    }

    /**
     * 拍照
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void takePhoto() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN // Permission was added in API Level 16
                && ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    "拍照时需要存储权限",
                    REQUEST_STORAGE_WRITE_ACCESS_PERMISSION);
        } else {
            Intent takeIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            //下面这句指定调用相机拍照后的照片存储的路径
            takeIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(mTempPhotoPath)));
            startActivityForResult(takeIntent, CAMERA_REQUEST_CODE);
        }
    }

    /**
     * 请求权限
     *
     * @param permission  权限名称
     * @param rationale   提示语
     * @param requestCode 请求码
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestPermission(final String permission, String rationale, final int requestCode) {

        if (shouldShowRequestPermissionRationale(permission)) {

            TooltipUtils.showDialog(mActivity, "权限需求", rationale, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    requestPermissions(arrayOf(permission), requestCode);
                }
            }, null, "确定", "取消");

        } else {
            requestPermissions(arrayOf(permission), requestCode);
        }
    }
}
