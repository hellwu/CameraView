package com.otaliastudios.cameraview.demo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.otaliastudios.cameraview.BitmapCallback;
import com.otaliastudios.cameraview.CameraUtils;
import com.otaliastudios.cameraview.FileCallback;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.size.AspectRatio;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class PicturePreviewActivity extends AppCompatActivity {
    private static PictureResult picture;
    private Bitmap picBitmap;

    public static void setPictureResult(@Nullable PictureResult pictureResult) {
        picture = pictureResult;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_preview);
        getSupportActionBar().hide();
        final PictureResult result = picture;
        if (result == null) {
            finish();
            return;
        }

      final ImageView imageView = findViewById(R.id.image);
      final Button btn_save = findViewById(R.id.btn_save);
        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
                saveBitmap(picBitmap,format.format(new Date())+".JPEG");
            }
        });

        final Button btn_back = findViewById(R.id.btn_back);
        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PicturePreviewActivity.this.finish();
            }
        });

       /* final MessageView captureResolution = findViewById(R.id.nativeCaptureResolution);
        final MessageView captureLatency = findViewById(R.id.captureLatency);
        final MessageView exifRotation = findViewById(R.id.exifRotation);*/

        final long delay = getIntent().getLongExtra("delay", 0);
        AspectRatio ratio = AspectRatio.of(result.getSize());

     /*   captureLatency.setTitleAndMessage("Approx. latency", delay + " milliseconds");
        captureResolution.setTitleAndMessage("Resolution", result.getSize() + " (" + ratio + ")");
        exifRotation.setTitleAndMessage("EXIF rotation", result.getRotation() + "");*/
        try {
            result.toBitmap(result.getSize().getWidth(),result.getSize().getHeight(),new BitmapCallback() {
                @Override
                public void onBitmapReady(Bitmap bitmap) {
                    Canvas canvas = new Canvas(bitmap);  //创建画布
                    Paint paint = new Paint();  //画笔
                    paint.setStrokeWidth(1);  //设置线宽。单位为像素
                    paint.setAlpha(20);
                    paint.setStyle(Paint.Style.FILL_AND_STROKE);//设置画笔的类型是填充，还是描边，还是描边且填充
                    //paint.setAntiAlias(true); //抗锯齿
                    paint.setColor(Color.WHITE);  //画笔颜色


                    canvas.drawBitmap(bitmap,new Matrix(),paint);  //在画布上画一个和bitmap一模一样的图
                    //根据Bitmap大小，画网格线
                    //画横线
                    Bitmap bitmaps = BitmapFactory.decodeResource(getResources(), R.drawable.icon_logo);
                    canvas.drawBitmap(bitmaps,10,10,paint);
         /*           int x = result.getSize().getWidth()/(MyApplication.lineX + 1);
                    int y = result.getSize().getHeight()/(MyApplication.lineY + 1);
                    for(int i = 1; i <= MyApplication.lineX; i++){
                        if(i==MyApplication.lineX/2+1){
                            canvas.drawLine(x * i, 0, x* i, 100, paint2);//绘制直线的起始(x,y)与终止(x1,y1)与画笔。
                            canvas.drawLine(x * i, 100, x* i, result.getSize().getHeight(), paint);//绘制直线的起始(x,y)与终止(x1,y1)与画笔。
                        }else{
                            canvas.drawLine(x * i, 0, x* i, result.getSize().getHeight(), paint);//绘制直线的起始(x,y)与终止(x1,y1)与画笔。
                        }

                    }
                    for (int i = 1; i <= MyApplication.lineY; i++) {
                        canvas.drawLine(0, y * i,result.getSize().getWidth(), y * i, paint);
                    }*/
                    picBitmap=bitmap;
                    imageView.setImageBitmap(bitmap);
                }
            });
        } catch (UnsupportedOperationException e) {
            imageView.setImageDrawable(new ColorDrawable(Color.GREEN));
            Toast.makeText(this, "Can't preview this format: " + picture.getFormat(),
                    Toast.LENGTH_LONG).show();
        }

        if (result.isSnapshot()) {
            // Log the real size for debugging reason.
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(result.getData(), 0, result.getData().length, options);
            if (result.getRotation() % 180 != 0) {
                Log.e("PicturePreview", "The picture full size is " + result.getSize().getHeight() + "x" + result.getSize().getWidth());
            } else {
                Log.e("PicturePreview", "The picture full size is " + result.getSize().getWidth() + "x" + result.getSize().getHeight());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!isChangingConfigurations()) {
            setPictureResult(null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.share, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.share) {
            Toast.makeText(this, "Sharing...", Toast.LENGTH_SHORT).show();
            String extension;
            switch (picture.getFormat()) {
                case JPEG: extension = "jpg"; break;
                case DNG: extension = "dng"; break;
                default: throw new RuntimeException("Unknown format.");
            }
            File file = new File(getFilesDir(), "picture." + extension);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            picBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] data = baos.toByteArray();
            CameraUtils.writeToFile(data, file, new FileCallback() {
                @Override
                public void onFileReady(@Nullable File file) {
                    if (file != null) {
                        Context context = PicturePreviewActivity.this;
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("image/*");
                        Uri uri = FileProvider.getUriForFile(context,
                                context.getPackageName() + ".provider",
                                file);
                        intent.putExtra(Intent.EXTRA_STREAM, uri);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                    } else {
                        Toast.makeText(PicturePreviewActivity.this,
                                "Error while writing file.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveBitmap(Bitmap bitmap, String bitName){

        String fileName ;
        File file ;
        if(Build.BRAND .equals("Xiaomi") ){ // 小米手机
            fileName = Environment.getExternalStorageDirectory().getPath()+"/DCIM/Camera/"+bitName ;
        }else{ // Meizu 、Oppo
            fileName = Environment.getExternalStorageDirectory().getPath()+"/DCIM/"+bitName ;
        }
        file = new File(fileName);
        if(file.exists()){
            file.delete();
        }
        FileOutputStream out;
        try{
            out = new FileOutputStream(file);
            // 格式为 JPEG，照相机拍出的图片为JPEG格式的，PNG格式的不能显示在相册中
            if(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out))
            {
                out.flush();
                out.close();
// 插入图库
                MediaStore.Images.Media.insertImage(this.getContentResolver(), file.getAbsolutePath(), bitName, null);
            }
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        // 发送广播，通知刷新图库的显示
        this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + fileName)));
        Toast.makeText(this,"保存图片成功",Toast.LENGTH_SHORT).show();
    }
}
