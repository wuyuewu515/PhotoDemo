package com.wyw.photodemo;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.kevin.crop.UCrop;
import com.wyw.photodemo.utils.TooltipUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static butterknife.internal.Utils.arrayOf;

public class MainActivity extends AppCompatActivity {

    protected int REQUEST_STORAGE_READ_ACCESS_PERMISSION = 101;
    protected int REQUEST_STORAGE_WRITE_ACCESS_PERMISSION = 102;

    public final int GALLERY_REQUEST_CODE = 0;   // 相册选图标记
    public final int CAMERA_REQUEST_CODE = 1;    // 相机拍照标记

    public final int REQUEST_CROP = 69; //裁剪成功标识
    public final int RESULT_ERROR = 96;//裁剪失败标识
    @BindView(R.id.iv_result)
    ImageView ivResult;

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
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void pickFromGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN // Permission was added in API Level 16
                && ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE,
                    "选择图片时需要读取权限",
                    REQUEST_STORAGE_READ_ACCESS_PERMISSION);
        } else {
            Intent pickIntent = new Intent(Intent.ACTION_PICK, null);
            // 如果限制上传到服务器的图片类型时可以直接写如："image/jpeg 、 image/png等的类型"
            pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
            startActivityForResult(pickIntent, GALLERY_REQUEST_CODE);
        }
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
     * 请求权限   需要在配置清单中添加声明
     * <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
     * <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case CAMERA_REQUEST_CODE: {  // 调用相机拍照

                    File temp = new File(mTempPhotoPath);
                //    TooltipUtils.showToastL(mActivity, mTempPhotoPath);
                    startCropActivity(Uri.fromFile(temp));
                }
                break;
                case GALLERY_REQUEST_CODE: {  // 直接从相册获取

                    startCropActivity(data.getData());
                }
                break;
                case REQUEST_CROP: {   // 裁剪图片结果
                    handleCropResult(data);
                }
                break;
                case RESULT_ERROR:   // 裁剪图片错误
                {
                    handleCropError(data);
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 裁剪失败执行操作
     *
     * @param data
     */
    private void handleCropError(Intent data) {

        deleteTempPhotoFile();
        Throwable cropError = UCrop.getError(data);
        if (cropError != null) {
            Toast.makeText(mActivity, cropError.getMessage(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(mActivity, "无法剪切选择图片", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 裁剪成功操作
     *
     * @param data
     */
    private void handleCropResult(Intent data) {

        deleteTempPhotoFile();
        Uri resultUri = UCrop.getOutput(data);
        if (null != resultUri) {
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(mActivity.getContentResolver(), resultUri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            onPictureSelected(resultUri, bitmap);
        } else {
            Toast.makeText(mActivity, "无法剪切选择图片", Toast.LENGTH_SHORT).show();
        }
    }


    private void onPictureSelected(Uri fileUri, Bitmap bitmap) {
        String filePath = fileUri.getEncodedPath();
        String imagePath = Uri.decode(filePath);

        Glide.with(mActivity).load(filePath).placeholder(R.mipmap.ic_launcher).into(ivResult);
        Toast.makeText(mActivity, "图片地址是：" + filePath, Toast.LENGTH_SHORT).show();


//        var toBase64 = BitmapUtils.imgToBase64WithRoteteAndScale(imagePath);
//
//        if (!TextUtils.isEmpty(toBase64)) {
//            var params: HashMap<String, String> = HashMap()
//            params.put("headBase64", toBase64)
//            HttpUtil.getInstance(mActivity)
//                    .doPostByJson(HttpUtil.getServiceUrl(InterfaceConfig.mine.HEADICON), params as Map<String, Any>, headListener)
        //      }
    }

    /**
     * 删除拍照临时文件
     */
    private void deleteTempPhotoFile() {
        File tempFile = new File(mTempPhotoPath);
        if (tempFile.exists() && tempFile.isFile()) {
            tempFile.delete();
        }
    }

    /**
     * 跳转裁剪界面
     *
     * @param uri
     */
    private void startCropActivity(Uri uri) {
        UCrop.of(uri, mDestinationUri)
                .withAspectRatio(1f, 1f)
                .withMaxResultSize(512, 512)
                .withTargetActivity(CropActivity.class)
                .start(mActivity, REQUEST_CROP);
    }


}
