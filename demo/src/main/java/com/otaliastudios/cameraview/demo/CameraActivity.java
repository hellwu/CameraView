package com.otaliastudios.cameraview.demo;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.YuvImage;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.otaliastudios.cameraview.BitmapCallback;
import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Mode;
import com.otaliastudios.cameraview.controls.Preview;
import com.otaliastudios.cameraview.demo.listener.GPSLocationListener;
import com.otaliastudios.cameraview.demo.listener.GPSLocationManager;
import com.otaliastudios.cameraview.demo.listener.GPSProviderStatus;
import com.otaliastudios.cameraview.filter.Filters;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;
import com.otaliastudios.cameraview.size.AspectRatio;
import com.otaliastudios.cameraview.size.SizeSelector;
import com.otaliastudios.cameraview.size.SizeSelectors;

import net.tsz.afinal.FinalHttp;
import net.tsz.afinal.http.AjaxCallBack;
import net.tsz.afinal.http.AjaxParams;

import org.apache.commons.lang.RandomStringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;


public class CameraActivity extends AppCompatActivity implements View.OnClickListener, OptionView.Callback {

    private final static CameraLogger LOG = CameraLogger.create("DemoApp");
    private final static boolean USE_FRAME_PROCESSOR = false;
    private final static boolean DECODE_BITMAP = true;

    private CameraView camera;
    //private ViewGroup controlPanel;
    private long mCaptureTime;

    private int mCurrentFilter = 0;
    private final Filters[] mAllFilters = Filters.values();
    private CheckBox cb_sp;
  // private  View gridview;
    SharedPreferences photo;

    TextView tv_time;
    TextView tv_jd;
    TextView tv_wd;
    TextView tv_place;
    TextView   tv_name;
    TextView   tv_id;
    TextView tv_length, tv_weight;
    Button bt_weight;
    Button bt_ori;
    String timeStr;

    String placeStr="该位置信息暂无";

    String wdStr="0.0";

    String jdStr="0.0";
    private Handler mHandler;
    String weight;
    String length;
    private GPSLocationManager gpsLocationManager;
    AlertDialog.Builder builder;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        getSupportActionBar().hide();
        CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE);
        camera = findViewById(R.id.camera);
        bt_ori = findViewById(R.id.bt_ori);

        cb_sp = findViewById(R.id.cb_sp);
        tv_name = findViewById(R.id.textView11);
        tv_id = findViewById(R.id.textView10);
        bt_weight = findViewById(R.id.bt_weight);
        tv_length = findViewById(R.id.tv_length);
        tv_weight = findViewById(R.id.tv_weight);

        camera.setLifecycleOwner(this);
        camera.addCameraListener(new Listener());

        Configuration mConfiguration = this.getResources().getConfiguration(); //获取设置的配置信息
        int ori = mConfiguration.orientation; //获取屏幕方向

         photo = getSharedPreferences("photo", MODE_PRIVATE);

        String name = photo.getString("name", "");
         weight = photo.getString("weight", "");
         length = photo.getString("length", "");
        if(!name.equals("")){
                tv_name.setText(name);
                tv_id.setText(photo.getString("ID", ""));
        }

        boolean isHaveWeight = photo.getBoolean("isHaveWeight", false);
        if(isHaveWeight) {
            tv_length.setVisibility(View.VISIBLE);
            tv_weight.setVisibility(View.VISIBLE);
            tv_length.setText("体长:"+length+"厘米");
            tv_weight.setText("重量:"+weight+"公斤");
        }
        else {
            tv_length.setVisibility(View.GONE);
            tv_weight.setVisibility(View.GONE);
        }

        if(!isHaveWeight){
            if (ori == mConfiguration.ORIENTATION_LANDSCAPE) {//横屏
                SizeSelector width = SizeSelectors.maxWidth(2000);
                SizeSelector height = SizeSelectors.maxHeight(2000);
                SizeSelector minWidth = SizeSelectors.minWidth(1000);
                SizeSelector minHeight = SizeSelectors.minHeight(1000);
                SizeSelector dimensions = SizeSelectors.and(width, height,minWidth,minHeight); // Matches sizes bigger than 1000x2000.
                SizeSelector ratio = SizeSelectors.aspectRatio(AspectRatio.of(4, 3),0); // Matches 1:1 sizes.

                SizeSelector result = SizeSelectors.or(
                        SizeSelectors.and(ratio, dimensions), // Try to match both constraints
                        ratio, // If none is found, at least try to match the aspect ratio
                        SizeSelectors.biggest() // If none is found, take the biggest
                );
                camera.setPictureSize(result);
            } else if (ori == mConfiguration.ORIENTATION_PORTRAIT) {
                SizeSelector width = SizeSelectors.maxWidth(2000);
                SizeSelector height = SizeSelectors.maxHeight(2000);
                SizeSelector minWidth = SizeSelectors.minWidth(1000);
                SizeSelector minHeight = SizeSelectors.minHeight(1000);
                SizeSelector dimensions = SizeSelectors.and(width, height,minWidth,minHeight); // Matches sizes bigger than 1000x2000.
                SizeSelector ratio = SizeSelectors.aspectRatio(AspectRatio.of(3, 4),0); // Matches 1:1 sizes.

                SizeSelector result = SizeSelectors.or(
                        SizeSelectors.and(ratio, dimensions), // Try to match both constraints
                        ratio, // If none is found, at least try to match the aspect ratio
                        SizeSelectors.biggest() // If none is found, take the biggest
                );
                camera.setPictureSize(result);
            }
        }else{
            if (ori == mConfiguration.ORIENTATION_LANDSCAPE) {
                SizeSelector width = SizeSelectors.maxWidth(2500);
                SizeSelector height = SizeSelectors.maxHeight(1500);
                SizeSelector minWidth = SizeSelectors.minWidth(2000);
                SizeSelector minHeight = SizeSelectors.minHeight(1000);
                SizeSelector dimensions = SizeSelectors.and(width, height,minWidth,minHeight); // Matches sizes bigger than 1000x2000.
                SizeSelector ratio = SizeSelectors.aspectRatio(AspectRatio.of(16, 9),0); // Matches 1:1 sizes.

                SizeSelector result = SizeSelectors.or(
                        SizeSelectors.and(ratio, dimensions), // Try to match both constraints
                        ratio, // If none is found, at least try to match the aspect ratio
                        SizeSelectors.biggest() // If none is found, take the biggest
                );
                camera.setPictureSize(result);
            } else if (ori == mConfiguration.ORIENTATION_PORTRAIT) {
                SizeSelector width = SizeSelectors.maxWidth(1500);
                SizeSelector height = SizeSelectors.maxHeight(2500);
                SizeSelector minWidth = SizeSelectors.minWidth(1000);
                SizeSelector minHeight = SizeSelectors.minHeight(2000);
                SizeSelector dimensions = SizeSelectors.and(width, height,minWidth,minHeight); // Matches sizes bigger than 1000x2000.
                SizeSelector ratio = SizeSelectors.aspectRatio(AspectRatio.of(9, 16),0); // Matches 1:1 sizes.

                SizeSelector result = SizeSelectors.or(
                        SizeSelectors.and(ratio, dimensions), // Try to match both constraints
                        ratio, // If none is found, at least try to match the aspect ratio
                        SizeSelectors.biggest() // If none is found, take the biggest
                );
                camera.setPictureSize(result);
            }
        }


        if (USE_FRAME_PROCESSOR) {
            camera.addFrameProcessor(new FrameProcessor() {
                private long lastTime = System.currentTimeMillis();

                @Override
                public void process(@NonNull Frame frame) {
                    long newTime = frame.getTime();
                    long delay = newTime - lastTime;
                    lastTime = newTime;
                    LOG.e("Frame delayMillis:", delay, "FPS:", 1000 / delay);
                    if (DECODE_BITMAP) {
                        YuvImage yuvImage = new YuvImage(frame.getData(), ImageFormat.NV21,
                                frame.getSize().getWidth(),
                                frame.getSize().getHeight(),
                                null);
                        ByteArrayOutputStream jpegStream = new ByteArrayOutputStream();
                        yuvImage.compressToJpeg(new Rect(0, 0,
                                frame.getSize().getWidth(),
                                frame.getSize().getHeight()), 100, jpegStream);
                        byte[] jpegByteArray = jpegStream.toByteArray();
                        Bitmap bitmap = BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.length);
                        //noinspection ResultOfMethodCallIgnored
                        bitmap.toString();
                    }
                }
            });
        }

        bt_weight.setOnClickListener(this);
        bt_ori.setOnClickListener(this);
        findViewById(R.id.edit).setOnClickListener(this);
        findViewById(R.id.person).setOnClickListener(this);
        findViewById(R.id.capturePicture).setOnClickListener(this);
        findViewById(R.id.capturePictureSnapshot).setOnClickListener(this);
        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/FZHTXH_GB.TTF");
         tv_time = findViewById(R.id.textView2);
        tv_time.setTypeface(typeface);
         tv_jd = findViewById(R.id.textView4);
        tv_jd.setTypeface(typeface);
         tv_wd = findViewById(R.id.textView3);
        tv_wd.setTypeface(typeface);
        tv_place = findViewById(R.id.textView5);
        tv_place.setTypeface(typeface);
        tv_name.setTypeface(typeface);
        tv_id.setTypeface(typeface);

        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 1:
                        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                         timeStr = simpleDateFormat.format(new Date());
                        tv_time.setText(timeStr);
                        break;
                    default:
                        break;

                }
            }
        };
        new TimeThread().start();
        initData();

      cb_sp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    camera.setFlash(Flash.AUTO);
                    Toast.makeText(CameraActivity.this,"闪光灯自动",Toast.LENGTH_SHORT).show();
                }else{
                    camera.setFlash(Flash.OFF);
                    Toast.makeText(CameraActivity.this,"闪光灯关闭",Toast.LENGTH_SHORT).show();
                }

            }
        });



        camera.setFlash(Flash.AUTO);
        ValueAnimator animator = ValueAnimator.ofFloat(1F, 0.8F);
        animator.setDuration(300);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
            }
        });
        animator.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void message(@NonNull String content, boolean important) {
        if (important) {
            LOG.w(content);
           // Toast.makeText(this, content, Toast.LENGTH_LONG).show();
        } else {
            LOG.i(content);

        }
    }


    class TimeThread extends Thread {
        @Override
        public void run() {
            do {
                try {
                    Thread.sleep(1000);
                    Message msg = new Message();
                    msg.what = 1;  //消息(一个整型值)
                    mHandler.sendMessage(msg);// 每隔1秒发送一个msg给mHandler
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (true);
        }
    }


    private class Listener extends CameraListener {

        @Override
        public void onCameraOpened(@NonNull CameraOptions options) {

        }

        @Override
        public void onCameraError(@NonNull CameraException exception) {
            super.onCameraError(exception);
            message("Got CameraException #" + exception.getReason(), true);
        }

        @Override
        public void onPictureTaken(@NonNull PictureResult result) {
            super.onPictureTaken(result);
            if (camera.isTakingVideo()) {
                message("Captured while taking video. Size=" + result.getSize(), false);
                return;
            }

            // This can happen if picture was taken with a gesture.
            long callbackTime = System.currentTimeMillis();
            if (mCaptureTime == 0) mCaptureTime = callbackTime - 300;
            LOG.w("onPictureTaken called! Launching activity. Delay:", callbackTime - mCaptureTime);

            savePicture(result);
            mCaptureTime = 0;
            LOG.w("onPictureTaken called! Launched activity.");
        }

        @Override
        public void onVideoTaken(@NonNull VideoResult result) {
            super.onVideoTaken(result);
            LOG.w("onVideoTaken called! Launching activity.");
            VideoPreviewActivity.setVideoResult(result);
            Intent intent = new Intent(CameraActivity.this, VideoPreviewActivity.class);
            startActivity(intent);
            LOG.w("onVideoTaken called! Launched activity.");
        }

        @Override
        public void onVideoRecordingStart() {
            super.onVideoRecordingStart();
            LOG.w("onVideoRecordingStart!");
        }

        @Override
        public void onVideoRecordingEnd() {
            super.onVideoRecordingEnd();
            message("Video taken. Processing...", false);
            LOG.w("onVideoRecordingEnd!");
        }

        @Override
        public void onExposureCorrectionChanged(float newValue, @NonNull float[] bounds, @Nullable PointF[] fingers) {
            super.onExposureCorrectionChanged(newValue, bounds, fingers);
            message("Exposure correction:" + newValue, false);
        }

        @Override
        public void onZoomChanged(float newValue, @NonNull float[] bounds, @Nullable PointF[] fingers) {
            super.onZoomChanged(newValue, bounds, fingers);
            message("Zoom:" + newValue, false);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.edit: edit(); break;
            case R.id.capturePicture: capturePicture(); break;
            case R.id.capturePictureSnapshot: capturePictureSnapshot(); break;
            case R.id.person: toggleDialog(); break;
            case R.id.bt_weight:
                toggleWeightDialog();
                break;
            case R.id.bt_ori:
                changeOri();
                break;
        }
    }

    private  void resetApp() {
        Intent intent = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        //与正常页面跳转一样可传递序列化数据,在Launch页面内获得
        intent.putExtra("REBOOT","reboot");
        startActivity(intent);
    }

    private void changeOri() {
        //判断当前屏幕方向
        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            //切换竖屏
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            //切换横屏
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    private void toggleDialog(){
        builder = new AlertDialog.Builder(CameraActivity.this);
        builder.setIcon(R.mipmap.logo);
        builder.setTitle("个人信息");
        //    通过LayoutInflater来加载一个xml的布局文件作为一个View对象
        View view = LayoutInflater.from(CameraActivity.this).inflate(R.layout.dialog, null);
        //    设置我们自己定义的布局文件作为弹出框的Content
        builder.setView(view);

        final EditText password = (EditText)view.findViewById(R.id.editText);
        final EditText et_id = (EditText)view.findViewById(R.id.editText3);

        builder.setPositiveButton("确定", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                String s = password.getText().toString();
                String id = et_id.getText().toString();
                if(s.equals("")){
                    Toast.makeText(CameraActivity.this,"名字不能为空",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(id.equals("")){
                    Toast.makeText(CameraActivity.this,"ID不能为空",Toast.LENGTH_SHORT).show();
                    return;
                }
                String s6 = id.substring(0, 6);
                String e4 = id.substring(14, 18);
                id=s6+"********"+e4;
                SharedPreferences.Editor edit = photo.edit();
                edit.putString("name",s);
                edit.putString("ID","ID:"+id);
                edit.commit();
                tv_name.setText(s);
                tv_id.setText("ID:"+id);

            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
            }
        });
        builder.setCancelable(false);
       builder.show();
    }

    private void toggleWeightDialog(){
        builder = new AlertDialog.Builder(CameraActivity.this);
        builder.setIcon(R.mipmap.logo);
        builder.setTitle("体重身长");
        //    通过LayoutInflater来加载一个xml的布局文件作为一个View对象
        View view = LayoutInflater.from(CameraActivity.this).inflate(R.layout.weight_length, null);
        //    设置我们自己定义的布局文件作为弹出框的Content
        builder.setView(view);

        final EditText et_length = (EditText)view.findViewById(R.id.editText);
        final Button bt_clear = view.findViewById(R.id.bt_clear);
        bt_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor edit = photo.edit();
                edit.putString("weight","");
                edit.putString("length","");
                edit.putBoolean("isHaveWeight", false);
                edit.commit();
                resetApp();
            }
        });
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                String length = et_length.getText().toString();
                if(length.equals("")){
                    Toast.makeText(CameraActivity.this,"体长不能为空",Toast.LENGTH_SHORT).show();
                    return;
                }
                String fLength = String.format("%.2f", Double.valueOf(length));
                Toast.makeText(CameraActivity.this, "fLength = "+fLength, Toast.LENGTH_LONG).show();
                double ww = Double.valueOf(fLength);
                Random random = new Random();
                float randomValue = (random.nextInt(60) + 1) * 0.1f;
                double result = ww * ww / (150.0 + randomValue);
                String fWeight = String.format("%.2f", Double.valueOf(result));
                weight = fWeight;
                length = fLength;
                SharedPreferences.Editor edit = photo.edit();
                edit.putString("weight",weight);
                edit.putString("length",length);
                edit.putBoolean("isHaveWeight", true);
                edit.commit();
                resetApp();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void edit() {
        Facing facing = camera.getFacing();
        if(facing==Facing.BACK){
            camera.setFacing(Facing.FRONT);
        }else{
            camera.setFacing(Facing.BACK);
        }
    }

    private void capturePicture() {
        if (camera.getMode() == Mode.VIDEO) {
            message("Can't take HQ pictures while in VIDEO mode.", false);
            return;
        }
        if (camera.isTakingPicture()) return;
        mCaptureTime = System.currentTimeMillis();
        message("Capturing picture...", false);
        camera.takePicture();
    }

    private void capturePictureSnapshot() {
        if (camera.isTakingPicture()) return;
        if (camera.getPreview() != Preview.GL_SURFACE) {
            message("Picture snapshots are only allowed with the GL_SURFACE preview.", true);
            return;
        }
        mCaptureTime = System.currentTimeMillis();
        message("Capturing picture snapshot...", false);
        camera.takePictureSnapshot();
    }

    private void captureVideo() {
        if (camera.getMode() == Mode.PICTURE) {
            message("Can't record HQ videos while in PICTURE mode.", false);
            return;
        }
        if (camera.isTakingPicture() || camera.isTakingVideo()) return;
        message("Recording for 5 seconds...", true);
        camera.takeVideo(new File(getFilesDir(), "video.mp4"), 5000);
    }

    private void captureVideoSnapshot() {
        if (camera.isTakingVideo()) {
            message("Already taking video.", false);
            return;
        }
        if (camera.getPreview() != Preview.GL_SURFACE) {
            message("Video snapshots are only allowed with the GL_SURFACE preview.", true);
            return;
        }
        message("Recording snapshot for 5 seconds...", true);
        camera.takeVideoSnapshot(new File(getFilesDir(), "video.mp4"), 5000);
    }

    private void toggleCamera() {
        if (camera.isTakingPicture() || camera.isTakingVideo()) return;
        switch (camera.toggleFacing()) {
            case BACK:
                message("Switched to back camera!", false);
                break;

            case FRONT:
                message("Switched to front camera!", false);
                break;
        }
    }

    private int readPictureDegree(String path) {
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
    private Bitmap convertBmp(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(-1, 1);
        Bitmap convertBmp = Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);
        return convertBmp;
    }

    private void drawTextStroke(Paint paint, String text, int x, int y, int outStrokeWidth, Canvas canvas) {
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(outStrokeWidth);
        paint.setColor(Color.BLACK);  //画笔颜色

//        paint.setAlpha(191);
        paint.setAlpha(128);
//        paint.setColor(Color.BLACK);  //画笔颜色
        canvas.drawText(text, x, y ,paint);

        paint.setColor(Color.WHITE);  //画笔颜色
        paint.setAlpha(230);
        paint.setStrokeWidth(0);
        canvas.drawText(text, x, y ,paint);
    }

    private void  savePicture(final PictureResult result){
        Toast.makeText(CameraActivity.this, "翻转角度: " + result.getRotation(), Toast.LENGTH_LONG).show();
        try {
            result.toBitmap(result.getSize().getWidth(),result.getSize().getHeight(),new BitmapCallback() {
                @Override
                public void onBitmapReady(Bitmap bitmap) {
                    Log.d("xxx",bitmap.getWidth()+"--"+bitmap.getHeight() +", Rotation = "+result.getRotation());
                    Matrix matrix=new Matrix();
                    boolean isHaveWeight = photo.getBoolean("isHaveWeight", false);
                    Configuration mConfiguration = CameraActivity.this.getResources().getConfiguration(); //获取设置的配置信息
                    int ori = mConfiguration.orientation; //获取屏幕方向
                    if(isHaveWeight&&ori==mConfiguration.ORIENTATION_LANDSCAPE){
                        matrix.postScale((float) 960/bitmap.getWidth(),(float)540/bitmap.getHeight());
                    }else if(isHaveWeight&&ori==mConfiguration.ORIENTATION_PORTRAIT){
                        matrix.postScale((float) 720/bitmap.getWidth(),(float)1280/bitmap.getHeight());
                    }else if(!isHaveWeight&&ori==mConfiguration.ORIENTATION_LANDSCAPE){
                        matrix.postScale((float) 1707/bitmap.getWidth(),(float)1280/bitmap.getHeight());
                    }else{
                        matrix.postScale((float) 1280/bitmap.getWidth(),(float)1707/bitmap.getHeight());
                    }

                    Paint paint = new Paint();  //画笔
                    paint.setStrokeWidth(1);  //设置线宽。单位为像素
                    paint.setAlpha(20);
                    paint.setStyle(Paint.Style.FILL_AND_STROKE);//设置画笔的类型是填充，还是描边，还是描边且填充
                    //paint.setAntiAlias(true); //抗锯齿
                    paint.setColor(Color.WHITE);  //画笔颜色
                    Canvas canvas;
                    Facing facing = camera.getFacing();
                    Bitmap tepBitmap=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
                    Bitmap newBitmap;
                    if(facing != Facing.BACK) {
//                        newBitmap = convertBmp(tepBitmap);
                        newBitmap = tepBitmap;
                    }
                    else {
                        newBitmap = tepBitmap;
                    }
                    canvas = new Canvas(newBitmap);  //创建画布
                    canvas.drawBitmap(newBitmap,new Matrix(),paint);  //在画布上画一个和bitmap一模一样的图

//                    if(facing==Facing.BACK){//后置摄像头，绘制分辨率
                        if(isHaveWeight){  //16:9
                            if(ori==mConfiguration.ORIENTATION_LANDSCAPE) {//横屏
                                //960*540
                                Bitmap bitmaps = BitmapFactory.decodeResource(getResources(), R.drawable.icon_logo);
                                bitmaps= Bitmap.createScaledBitmap(bitmaps, 79, 79, true);
                                canvas.drawBitmap(bitmaps,26,13,paint);

                                Paint paint2 = new Paint(Paint.ANTI_ALIAS_FLAG);  //画笔
                                paint2.setStrokeWidth(3);  //设置线宽。单位为像素
                                paint2.setAntiAlias(true); //抗锯齿
                                paint2.setTextAlign(Paint.Align.RIGHT);
                                paint2.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/FZHTXH_GB.TTF"));
                                paint2.setTextSize(30);

                                Rect rect = new Rect();
                                paint2.getTextBounds(timeStr, 0, timeStr.length(), rect);
                                drawTextStroke(paint2, timeStr, 960 - 24 + 2, 38 - 2 + rect.height(), 2, canvas);

                                //绘制wifi图片
                                Bitmap bitmapwifi = BitmapFactory.decodeResource(getResources(), R.drawable.icon_service);
                                bitmapwifi= Bitmap.createScaledBitmap(bitmapwifi, 39, 39, true);
                                canvas.drawBitmap(bitmapwifi,960 - rect.width() - 24 - 14 - 39 + 1 , 32, paint);

                                //绘制经纬度
                                Paint paint3 = new Paint(Paint.ANTI_ALIAS_FLAG);  //画笔
                                paint3.setStrokeWidth(2);  //设置线宽。单位为像素
                                paint3.setAntiAlias(true);
                                paint3.setStyle(Paint.Style.FILL_AND_STROKE);
                                paint3.setTextAlign(Paint.Align.LEFT);
                                paint3.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/FZHTXH_GB.TTF"));
                                paint3.setTextSize(30);

                                Paint paint4 = new Paint(Paint.ANTI_ALIAS_FLAG);  //画笔
                                paint4.setStrokeWidth(2);  //设置线宽。单位为像素
                                paint4.setAntiAlias(true);
                                paint3.setStyle(Paint.Style.FILL_AND_STROKE);
                                paint4.setTextAlign(Paint.Align.LEFT);
                                paint4.setColor(Color.WHITE);  //画笔颜色
                                paint4.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/FZHTXH_GB.TTF"));
                                paint4.setTextSize(29);
//
                                //1.
                                Rect rWdlable = new Rect();
                                String wdLableStr = "纬度";
                                paint3.getTextBounds(wdLableStr, 0, wdLableStr.length(), rWdlable);
                                drawTextStroke(paint3, wdLableStr, 26, 540 - 27 - 3, 2, canvas);

                                Rect rWd0 = new Rect();
                                paint4.getTextBounds(":", 0, ":".length(), rWd0);
                                drawTextStroke(paint4, ":", 26 + rWdlable.width() + 4, 540 - 29 - 1, 2, canvas);

                                Rect rWd = new Rect();
                                paint4.getTextBounds(wdStr, 0, wdStr.length(), rWd);
                                drawTextStroke(paint4, wdStr, 26 + rWdlable.width() + 4 + rWd0.width() + 6, 540 - 29 - 1, 2, canvas);
                                //2.
                                Rect rJdlable = new Rect();
                                String jdLableStr = "经度";
                                paint3.getTextBounds(jdLableStr, 0, jdLableStr.length(), rJdlable);
                                drawTextStroke(paint3, jdLableStr, 26, 540 - 27 - 3 - (rWdlable.height() +17) + 1, 2, canvas);

                                Rect rJd0 = new Rect();
                                paint3.getTextBounds(":", 0, ":".length(), rJd0);
                                drawTextStroke(paint3, ":", 26 + rJdlable.width() + 4, 540 - 27 - 3 - (rWdlable.height() +17) + 1, 2, canvas);

                                Rect rJd = new Rect();
                                paint4.getTextBounds(jdStr, 0, jdStr.length(), rJd);
                                drawTextStroke(paint4, jdStr, 26 + rJdlable.width() + 4 + rJd0.width() + 4, 540 - 29 - 1 - (rWd.height() +20) + 1 - 1, 2, canvas);

                                //3.绘制位置
                                //绘制位置
                                Rect rPlace = new Rect();
                                paint3.getTextBounds(placeStr, 0, placeStr.length(), rPlace);
                                drawTextStroke(paint3, placeStr, 26, 540 - 27 - 3 - (rWdlable.height() + 17) + 1 - (rJdlable.height() + 22) + 1, 2, canvas);


                                //6.绘制重量
                                String weightUnit ="重量";
                                Rect rWeightUnit = new Rect();
                                paint3.getTextBounds(weightUnit, 0, weightUnit.length(), rWeightUnit);
                                drawTextStroke(paint3, weightUnit, 26, 540 - 29 - 1 - (rWd.height() + 20) + 1 - (rJd.height() + 22) + 1 - (rPlace.height() + 22) - 3, 2, canvas);

                                Rect rWeight0 = new Rect();
                                paint3.getTextBounds(":", 0, ":".length(), rWeight0);
                                drawTextStroke(paint3, ":", 26 + rWeightUnit.width() + 7 , 540 - 29 - 1 - (rWd.height() + 20) + 1 - (rJd.height() + 22) + 1 - (rPlace.height() + 22) - 5, 2, canvas);

                                Rect rWeight = new Rect();
                                paint3.getTextBounds(weight+"公斤", 0, (weight+"公斤").length(), rWeight);
                                drawTextStroke(paint3, weight+"公斤", 26 + rWeightUnit.width() + 7 + rWeight0.width() + 24, 540 - 29 - 1 - (rWd.height() + 20) + 1 - (rJd.height() + 22) + 1 - (rPlace.height() + 22) - 3, 2, canvas);
//

                                //7.绘制体长
                                String lengthUnit = "体长";
                                Rect rLengthUnit = new Rect();
                                paint3.getTextBounds(lengthUnit, 0, lengthUnit.length(), rLengthUnit);
                                drawTextStroke(paint3, lengthUnit, 26, 540 - 29 - 1 - (rWd.height() + 20) + 1 - (rJd.height() + 22) + 1 - (rPlace.height() + 22) - 5 - (rLengthUnit.height() + 9) + 3, 2, canvas);

                                Rect rLength0 = new Rect();
                                paint3.getTextBounds(":", 0, ":".length(), rLength0);
                                drawTextStroke(paint3, ":", 26 + rLengthUnit.width() + 6, 540 - 29 - 1 - (rWd.height() + 20) + 1 - (rJd.height() + 22) + 1 - (rPlace.height() + 22) - 5 - (rLengthUnit.height() + 9) + 1, 2, canvas);

                                Rect rLength = new Rect();
                                paint3.getTextBounds(length+"厘米", 0, (length+"厘米").length(), rLength);
                                drawTextStroke(paint3, length+"厘米", 26 + rLengthUnit.width() + 6 + rLength0.width() + 23, 540 - 29 - 1 - (rWd.height() + 20) + 1 - (rJd.height() + 22) + 1 - (rPlace.height() + 22) - 5 - (rLengthUnit.height() + 9) + 3, 2, canvas);

                                //4.绘制ID
                                String idStr = tv_id.getText().toString();

                                Rect rID = new Rect();
                                paint3.setTextAlign(Paint.Align.RIGHT);
                                paint3.getTextBounds(idStr, 0, idStr.length(), rID);
                                drawTextStroke(paint3, idStr, 960 - 24 + 2, 540 - 51 - 1, 2, canvas);

                                //5.绘制名字
                                String nameStr = tv_name.getText().toString();
                                Rect rName = new Rect();
                                paint3.setTextAlign(Paint.Align.RIGHT);
                                paint3.getTextBounds(nameStr, 0, nameStr.length(), rName);
                                drawTextStroke(paint3, nameStr, 960 - 24 + 1, 540 - 51 - 1 - (rID.height() + 46), 2, canvas);
                            }else{
                                //720*1280
                                Bitmap bitmaps = BitmapFactory.decodeResource(getResources(), R.drawable.icon_logo);
                                bitmaps= Bitmap.createScaledBitmap(bitmaps, 72, 72, true);
                                canvas.drawBitmap(bitmaps,24,24,paint);

                                Paint paint2 = new Paint(Paint.ANTI_ALIAS_FLAG);  //画笔
                                paint2.setStrokeWidth(2);  //设置线宽。单位为像素
//                                paint2.setStyle(Paint.Style.FILL_AND_STROKE);//设置画笔的类型是填充，还是描边，还是描边且填充
                                paint2.setAntiAlias(true); //抗锯齿
                                paint2.setStyle(Paint.Style.FILL_AND_STROKE);
                                paint2.setTextAlign(Paint.Align.RIGHT);
                                paint2.setColor(Color.BLACK);  //画笔颜色

                                paint2.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/FZHTXH_GB.TTF"));
                                paint2.setTextSize(28);

                                Rect rect = new Rect();
                                paint2.getTextBounds(timeStr, 0, timeStr.length(), rect);
                                drawTextStroke(paint2, timeStr, 720 - 42 + 2, 48 + rect.height() - 2, 2, canvas);


                                //绘制wifi图片
                                Bitmap bitmapwifi = BitmapFactory.decodeResource(getResources(), R.drawable.icon_service);
                                bitmapwifi= Bitmap.createScaledBitmap(bitmapwifi, 36, 36, true);
                                canvas.drawBitmap(bitmapwifi,720 - rect.width() - 42 - 13 - 36 + 2, 42, paint);

                                //绘制经纬度
                                Paint paint3 = new Paint(Paint.ANTI_ALIAS_FLAG);  //画笔
                                paint3.setStrokeWidth(2);  //设置线宽。单位为像素
                                paint3.setAntiAlias(true);
                                paint3.setTextAlign(Paint.Align.LEFT);
                                paint3.setColor(Color.WHITE);  //画笔颜色
                                paint3.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/FZHTXH_GB.TTF"));
                                paint3.setTextSize(28);

                                Paint paint4 = new Paint(Paint.ANTI_ALIAS_FLAG);  //画笔
                                paint4.setStrokeWidth(2);  //设置线宽。单位为像素
//
//                                paint4.setTextAlign(Paint.Align.LEFT);
//                                paint4.setColor(Color.WHITE);  //画笔颜色
//                                paint4.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/FZHTXH_GB.TTF"));
//                                paint4.setTextSize(29);
//
                                //1.
                                Rect rWdlable = new Rect();
                                String wdLableStr = "纬度";
                                paint3.getTextBounds(wdLableStr, 0, wdLableStr.length(), rWdlable);
                                drawTextStroke(paint3, wdLableStr, 24, 1280 - 24 - 4, 2, canvas);

                                Rect rWd0 = new Rect();
                                paint3.getTextBounds(":", 0, ":".length(), rWd0);
                                drawTextStroke(paint3, ":", 24 + rWdlable.width() + 3, 1280 - 24 - 4, 2, canvas);

                                Rect rWd = new Rect();
                                paint3.getTextBounds(wdStr, 0, wdStr.length(), rWd);
                                drawTextStroke(paint3, wdStr, 24 + rWdlable.width() + 3 + rWd0.width() + 6, 1280 - 27 - 1, 2, canvas);

                                //2.
                                Rect rJdlable = new Rect();
                                String jdLableStr = "经度";
                                paint3.getTextBounds(jdLableStr, 0, jdLableStr.length(), rJdlable);
                                drawTextStroke(paint3, jdLableStr, 24, 1280 - 24 - 3 - (rWdlable.height() +15), 2, canvas);

                                Rect rJd0 = new Rect();
                                paint3.getTextBounds(":", 0, ":".length(), rJd0);
                                drawTextStroke(paint3, ":", 24 + rJdlable.width() + 3, 1280 - 24 - 3 - (rWdlable.height() +15), 2, canvas);

                                Rect rJd = new Rect();
                                paint3.getTextBounds(jdStr, 0, jdStr.length(), rJd);
                                drawTextStroke(paint3, jdStr, 24 + rJdlable.width() + 3 + rJd0.width() + 4, 1280 - 27 - 1 - (rWd.height() + 18), 2, canvas);

                                //3.绘制位置
                                //绘制位置
                                Rect rPlace = new Rect();
                                paint3.getTextBounds(placeStr, 0, placeStr.length(), rPlace);
                                drawTextStroke(paint3, placeStr, 24, 1280 - 24 - 3 - (rWd.height() + 15) - (rJd.height() + 18) - 5, 2, canvas);

                                //4.绘制ID
                                String idStr = tv_id.getText().toString();

                                Rect rID = new Rect();
                                paint3.getTextBounds(idStr, 0, idStr.length(), rID);
                                drawTextStroke(paint3, idStr, 24, 1280 - 24 - 3 - (rWd.height() + 15) - 1 - (rJd.height() + 18) + 1 - (rPlace.height() + 17) - 2 , 2, canvas);

                                //5.绘制名字
                                String nameStr = tv_name.getText().toString();
                                Rect rName = new Rect();
                                paint3.getTextBounds(nameStr, 0, nameStr.length(), rName);
                                drawTextStroke(paint3, nameStr, 24, 1280 - 24 - 3 - (rWd.height() + 15) - 1 - (rJd.height() + 18) + 1 - (rPlace.height() + 17)  - (rID.height() + 18) - 3, 2, canvas);

                                //6.绘制重量
                                String weightUnit ="重量";
                                Rect rWeightUnit = new Rect();
                                paint3.getTextBounds(weightUnit, 0, weightUnit.length(), rWeightUnit);
                                drawTextStroke(paint3, weightUnit, 24, 1280 - 24 - 3 - (rWd.height() + 15) - 1 - (rJd.height() + 18) + 1 - (rPlace.height() + 17) - (rID.height() + 18) - 1 - (rName.height() + 16), 2, canvas);

                                Rect rWeigh0 = new Rect();
                                paint3.getTextBounds(":", 0, ":".length(), rWeigh0);
                                drawTextStroke(paint3, ":", 24 + rWeightUnit.width() + 4, 1280 - 24 - 3 - (rWd.height() + 15) - 1 - (rJd.height() + 18) + 1 - (rPlace.height() + 17) - (rID.height() + 18) - 1 - (rName.height() + 16) - 2, 2, canvas);


                                Rect rWeight = new Rect();
                                paint3.getTextBounds(weight+"公斤", 0, (weight+"公斤").length(), rWeight);
                                drawTextStroke(paint3, weight+"公斤", 24 + rWeightUnit.width() + 6 + rWeigh0.width() + 21, 1280 - 24 - 3 - (rWd.height() + 15) - 1 - (rJd.height() + 18) + 1 - (rPlace.height() + 17) - (rID.height() + 18) - 1 - (rName.height() + 16), 2, canvas);


                                //7.绘制体长
                                String lengthUnit = "体长";
                                Rect rLengthUnit = new Rect();
                                paint3.getTextBounds(lengthUnit, 0, lengthUnit.length(), rLengthUnit);
                                drawTextStroke(paint3, lengthUnit, 24, 1280 - 24 - 3 - (rWd.height() + 15) - 1 - (rJd.height() + 18) + 1 - (rPlace.height() + 17) - (rID.height() + 18) - 1 - (rName.height() + 16) + 1 - (rLengthUnit.height() + 8) - 1, 2, canvas);

                                Rect rLength0 = new Rect();
                                paint3.getTextBounds(":", 0, ":".length(), rLength0);
                                drawTextStroke(paint3, ":", 24 + rLengthUnit.width() + 4, 1280 - 24 - 3 - (rWd.height() + 15) - 1 - (rJd.height() + 18) + 1 - (rPlace.height() + 17) - (rID.height() + 18) - 1 - (rName.height() + 16) + 1 - (rLengthUnit.height() + 8) - 3, 2, canvas);

                                Rect rLength = new Rect();
                                paint3.getTextBounds(length+"厘米", 0, (length+"厘米").length(), rLength);
                                drawTextStroke(paint3, length+"厘米", 24 + rLengthUnit.width() + 6 + rLength0.width() + 20 - 1, 1280 - 24 - 3 - (rWd.height() + 15) - 1 - (rJd.height() + 18) + 1 - (rPlace.height() + 17) - (rID.height() + 18) - 1 - (rName.height() + 16) + 1 - (rLengthUnit.height() + 8) - 1, 2, canvas);
                            }
                        }else{   //4:3
                            if(ori==mConfiguration.ORIENTATION_LANDSCAPE) {//4:3横屏
                                //画logo
                                Bitmap bitmaps = BitmapFactory.decodeResource(getResources(), R.drawable.icon_logo);
                                bitmaps= Bitmap.createScaledBitmap(bitmaps, 138, 138, true);
                                canvas.drawBitmap(bitmaps,46,23,paint);

                                Paint paint2 = new Paint(Paint.ANTI_ALIAS_FLAG);  //画笔
                                paint2.setStrokeWidth(3);  //设置线宽。单位为像素
//                                paint2.setStyle(Paint.Style.FILL_AND_STROKE);//设置画笔的类型是填充，还是描边，还是描边且填充
                                paint2.setAntiAlias(true); //抗锯齿
                                paint2.setStyle(Paint.Style.FILL_AND_STROKE);
                                paint2.setTextAlign(Paint.Align.RIGHT);
                                paint2.setColor(Color.BLACK);  //画笔颜色

                                paint2.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/FZHTXH_GB.TTF"));
                                paint2.setTextSize(53);

                                Rect rect = new Rect();
                                paint2.getTextBounds(timeStr, 0, timeStr.length(), rect);
                                drawTextStroke(paint2, timeStr, 1707 - 40 + 3, 68 + rect.height() - 1, 4, canvas);


                                //绘制wifi图片
                                Bitmap bitmapwifi = BitmapFactory.decodeResource(getResources(), R.drawable.icon_service);
                                bitmapwifi= Bitmap.createScaledBitmap(bitmapwifi, 70, 70, true);
                                canvas.drawBitmap(bitmapwifi,1707 - rect.width() - 40 - 24 - 70, 56, paint);

                                //绘制经纬度
                                Paint paint3 = new Paint(Paint.ANTI_ALIAS_FLAG);  //画笔
                                paint3.setStrokeWidth(4);  //设置线宽。单位为像素
                                paint3.setAntiAlias(true);
                                paint3.setTextAlign(Paint.Align.LEFT);
                                paint3.setColor(Color.WHITE);  //画笔颜色
                                paint3.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/FZHTXH_GB.TTF"));
                                paint3.setTextSize(53);

                                Paint paint4 = new Paint(Paint.ANTI_ALIAS_FLAG);  //画笔
                                paint4.setStrokeWidth(4);  //设置线宽。单位为像素
                                paint4.setAntiAlias(true);
                                paint4.setTextAlign(Paint.Align.LEFT);
                                paint4.setColor(Color.WHITE);  //画笔颜色
                                paint4.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/FZHTXH_GB.TTF"));
                                paint4.setTextSize(53);
//
                                //1.
                                Rect rWdlable = new Rect();
                                String wdLableStr = "纬度";
                                paint3.getTextBounds(wdLableStr, 0, wdLableStr.length(), rWdlable);
                                drawTextStroke(paint3, wdLableStr, 44 + 2, 1280 - 48 - 4, 4, canvas);

                                Rect rWd0 = new Rect();
                                paint3.getTextBounds(":", 0, ":".length(), rWd0);
                                drawTextStroke(paint3, ":", 44 + 2 + rWdlable.width() + 5 , 1280 - 48 - 4, 4, canvas);

                                Rect rWd = new Rect();
                                paint4.getTextBounds(wdStr, 0, wdStr.length(), rWd);
                                drawTextStroke(paint4, wdStr, 44 + 2 + rWd0.width() + 5 + rWdlable.width() + 11, 1280 - 51 - 2, 4, canvas);

                                //2.
                                Rect rJdlable = new Rect();
                                String jdLableStr = "经度";
                                paint3.getTextBounds(jdLableStr, 0, jdLableStr.length(), rJdlable);
                                drawTextStroke(paint3, jdLableStr, 44 + 2, 1280 - 48 - 4 - (rWdlable.height() +28) - 1, 4, canvas);

                                Rect rJd0 = new Rect();
                                paint3.getTextBounds(":", 0, ":".length(), rJd0);
                                drawTextStroke(paint3, ":", 44 + 2 + rJdlable.width() + 5, 1280 - 48 - 5 - (rWdlable.height() +28), 4, canvas);

                                Rect rJd = new Rect();
                                paint4.getTextBounds(jdStr, 0, jdStr.length(), rJd);
                                drawTextStroke(paint4, jdStr, 44 + 2 + rJd0.width() + 5 + rJdlable.width() + 6, 1280 - 51 - 2 - (rWd.height() +36), 4, canvas);

                                //3.绘制位置
                                //绘制位置
                                Rect rPlace = new Rect();
                                paint3.getTextBounds(placeStr, 0, placeStr.length(), rPlace);
                                drawTextStroke(paint3, placeStr, 44 + 2, 1280 - 48 - 4 - (rWdlable.height() +28) - (rJdlable.height() + 38) - 3, 4, canvas);

                                //4.绘制ID
                                String idStr = tv_id.getText().toString();

                                Rect rID = new Rect();
                                paint4.setTextAlign(Paint.Align.RIGHT);
                                paint4.getTextBounds(idStr, 0, idStr.length(), rID);
                                drawTextStroke(paint4, idStr, 1707 - 40 + 3, 1280 - 90 - 3, 4, canvas);

                                //5.绘制名字
                                String nameStr = tv_name.getText().toString();
                                Rect rName = new Rect();
                                paint3.setTextAlign(Paint.Align.RIGHT);
                                paint3.getTextBounds(nameStr, 0, nameStr.length(), rName);
                                drawTextStroke(paint3, nameStr, 1707 - 40 + 2, 1280 - 90 - (rID.height() + 78)  - 8, 4, canvas);

                            }else{//4:3竖屏
                                //根据Bitmap大小，画网格线
                                //画logo
                                Bitmap bitmaps = BitmapFactory.decodeResource(getResources(), R.drawable.icon_logo);
                                bitmaps= Bitmap.createScaledBitmap(bitmaps, 128, 128, true);
                                canvas.drawBitmap(bitmaps,42,42,paint);

                                Paint paint2 = new Paint(Paint.ANTI_ALIAS_FLAG);  //画笔
                                paint2.setStrokeWidth(4);  //设置线宽。单位为像素
//                                paint2.setStyle(Paint.Style.FILL_AND_STROKE);//设置画笔的类型是填充，还是描边，还是描边且填充
                                paint2.setAntiAlias(true); //抗锯齿
                                paint2.setStyle(Paint.Style.FILL_AND_STROKE);
                                paint2.setTextAlign(Paint.Align.RIGHT);
                                paint2.setColor(Color.BLACK);  //画笔颜色

                                paint2.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/FZHTXH_GB.TTF"));
                                paint2.setTextSize(50);

                                Rect rect = new Rect();
                                paint2.getTextBounds(timeStr, 0, timeStr.length(), rect);
                                drawTextStroke(paint2, timeStr, 1280 - 72, 83 + rect.height() , 4, canvas);

                                //绘制wifi图片
                                Bitmap bitmapwifi = BitmapFactory.decodeResource(getResources(), R.drawable.icon_service);
                                bitmapwifi= Bitmap.createScaledBitmap(bitmapwifi, 65, 65, true);
                                canvas.drawBitmap(bitmapwifi,1280 - rect.width() - 72 - 22 - 65 - 3, 74, paint);

                                //绘制经纬度
                                Paint paint3 = new Paint(Paint.ANTI_ALIAS_FLAG);  //画笔
                                paint3.setStrokeWidth(3);  //设置线宽。单位为像素
                                paint3.setAntiAlias(true);
                                paint3.setTextAlign(Paint.Align.LEFT);
                                paint3.setColor(Color.WHITE);  //画笔颜色
                                paint3.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/FZHTXH_GB.TTF"));
                                paint3.setTextSize(50);

                                Paint paint4 = new Paint(Paint.ANTI_ALIAS_FLAG);  //画笔
                                paint4.setStrokeWidth(4);  //设置线宽。单位为像素
                                paint4.setAntiAlias(true);
                                paint4.setTextAlign(Paint.Align.LEFT);
                                paint4.setColor(Color.WHITE);  //画笔颜色
                                paint4.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/FZHTXH_GB.TTF"));
                                paint4.setTextSize(50);

                                //1.
                                Rect rWdlable = new Rect();
                                String wdLableStr = "纬度";
                                paint3.getTextBounds(wdLableStr, 0, wdLableStr.length(), rWdlable);
                                drawTextStroke(paint3, wdLableStr, 40 + 2, 1707 - 44 - 5, 4, canvas);

                                Rect rWd0 = new Rect();
                                paint3.getTextBounds(":", 0, ":".length(), rWd0);
                                drawTextStroke(paint3, ":", 40 + 2 + rWdlable.width() + 5, 1707 - 44 - 5, 4, canvas);

                                Rect rWd = new Rect();
                                paint4.getTextBounds(wdStr, 0, wdStr.length(), rWd);
                                drawTextStroke(paint4, wdStr, 40 + 2 + rWdlable.width() + 6 + rWd0.width() + 10, 1707 - 44 - 6, 4, canvas);

                                //2.
                                Rect rJdlable = new Rect();
                                String jdLableStr = "经度";
                                paint3.getTextBounds(jdLableStr, 0, jdLableStr.length(), rJdlable);
                                drawTextStroke(paint3, jdLableStr, 40 + 2, 1707 - 44 - 5 - (rWdlable.height() +26), 4, canvas);

                                Rect rJd0 = new Rect();
                                paint3.getTextBounds(":", 0, ":".length(), rJd0);
                                drawTextStroke(paint3, ":", 40 + 2 + rJdlable.width() + 5, 1707 - 44 - 5 - (rWdlable.height() +26), 4, canvas);

                                Rect rJd = new Rect();
                                paint4.getTextBounds(jdStr, 0, jdStr.length(), rJd);
                                drawTextStroke(paint4, jdStr, 40 + 2 + rJdlable.width() + 6 + rJd0.width() + 5, 1707 - 44 - 5 - (rWd.height() + 26 + 8), 4, canvas);

                                //3.绘制位置
                                //绘制位置
                                Rect rPlace = new Rect();
                                paint3.getTextBounds(placeStr, 0, placeStr.length(), rPlace);
                                drawTextStroke(paint3, placeStr, 40 + 2, 1707 - 44 - 5 - (rWd.height() + 26 + 4) - (rJd.height() + 30 + 4) - 4, 4, canvas);

                                //4.绘制ID
                                String idStr = tv_id.getText().toString();

                                Rect rID = new Rect();
                                paint4.getTextBounds(idStr, 0, idStr.length(), rID);
                                drawTextStroke(paint4, idStr, 40 + 2, 1707 - 44 - 5  - (rWd.height() + 26 + 4) - (rJd.height() + 30 + 4 ) - (rPlace.height() + 27) - 5, 4, canvas);

                                //5.绘制名字
                                String nameStr = tv_name.getText().toString();
                                Rect rName = new Rect();
                                paint3.getTextBounds(nameStr, 0, nameStr.length(), rName);
                                drawTextStroke(paint3, nameStr, 40 + 2, 1707 - 44 - 5 - (rWd.height() + 26 + 4) - (rJd.height() + 30 + 4) - (rPlace.height() + 27) - (rID.height() + 32) - 8, 4, canvas);
                            }
                        }


                    DateFormat format = new SimpleDateFormat("yyyy年MM月dd日-HHmmss-");
                    Date date = new Date();
                    saveBitmap(newBitmap,format.format(date)+date.getTime()+".jpg", isHaveWeight);
                }
            });
        } catch (UnsupportedOperationException e) {
            e.printStackTrace();
           // imageView.setImageDrawable(new ColorDrawable(Color.GREEN));
            Toast.makeText(this, "Can't preview this format: " + result.getFormat(),
                Toast.LENGTH_LONG).show();
        }
    }

    private void initData() {
        gpsLocationManager = GPSLocationManager.getInstances(CameraActivity.this);
        gpsLocationManager.start(new MyListener());

    }

    class MyListener implements GPSLocationListener {

        @Override
        public void UpdateLocation(Location location) {
            if (location != null) {
               // text_gps_3.setText("经度：" + location.getLongitude() + "\n纬度：" + location.getLatitude());
                Log.d("xxx","经度" + location.getLongitude() + "纬度：" + location.getLatitude());

                wdStr="" + location.getLatitude();
                jdStr="" + location.getLongitude();
                tv_jd.setText(jdStr);
                tv_wd.setText(wdStr);


                final Gson gs = new Gson();
                FinalHttp fh = new FinalHttp();
                String url = "http://api.map.baidu.com/reverse_geocoding/v3";
                AjaxParams params=new AjaxParams();
                params.put("ak","Bsl8nFTp3bzzPTVvafZdz43ueucnfVRL");
                params.put("output","json");
                params.put("coordtype","wgs84ll");
                params.put("location",location.getLatitude()+","+location.getLongitude());

                fh.post(url, params, new AjaxCallBack<Object>() {
                    @Override
                    public void onSuccess(Object o) {
                        Log.d("LoginActivity", o.toString());
                        LocationInfoJson result = gs.fromJson(o.toString(), LocationInfoJson.class);
                        tv_place.setText(result.getResult().getFormatted_address());
                        placeStr=result.getResult().getFormatted_address();
                    }
                    @Override
                    public void onFailure(Throwable t, int errorNo, String strMsg) {
                        Toast.makeText(CameraActivity.this,"网络错误",Toast.LENGTH_SHORT).show();
                    }
                });

            }
        }

        @Override
        public void UpdateStatus(String provider, int status, Bundle extras) {
            if ("gps" == provider) {
                Toast.makeText(CameraActivity.this, "定位类型：" + provider, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void UpdateGPSProviderStatus(int gpsStatus) {
            switch (gpsStatus) {
                case GPSProviderStatus.GPS_ENABLED:
                    Toast.makeText(CameraActivity.this, "GPS开启", Toast.LENGTH_SHORT).show();
                    break;
                case GPSProviderStatus.GPS_DISABLED:
                    Toast.makeText(CameraActivity.this, "GPS关闭", Toast.LENGTH_SHORT).show();
                    break;
                case GPSProviderStatus.GPS_OUT_OF_SERVICE:
                    Toast.makeText(CameraActivity.this, "GPS不可用", Toast.LENGTH_SHORT).show();
                    break;
                case GPSProviderStatus.GPS_TEMPORARILY_UNAVAILABLE:
                    Toast.makeText(CameraActivity.this, "GPS暂时不可用", Toast.LENGTH_SHORT).show();
                    break;
                case GPSProviderStatus.GPS_AVAILABLE:
                    Toast.makeText(CameraActivity.this, "GPS可用啦", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    private void saveBitmap(Bitmap bitmap, String bitName, boolean isHaveWeight){

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
            if(bitmap.compress(Bitmap.CompressFormat.JPEG, isHaveWeight ? 90 : 85, out))
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


    private void changeCurrentFilter() {
        if (camera.getPreview() != Preview.GL_SURFACE) {
            message("Filters are supported only when preview is Preview.GL_SURFACE.", true);
            return;
        }
        if (mCurrentFilter < mAllFilters.length - 1) {
            mCurrentFilter++;
        } else {
            mCurrentFilter = 0;
        }
        Filters filter = mAllFilters[mCurrentFilter];
        message(filter.toString(), false);

        // Normal behavior:
        camera.setFilter(filter.newInstance());

        // To test MultiFilter:
        // DuotoneFilter duotone = new DuotoneFilter();
        // duotone.setFirstColor(Color.RED);
        // duotone.setSecondColor(Color.GREEN);
        // camera.setFilter(new MultiFilter(duotone, filter.newInstance()));
    }

    @Override
    public <T> boolean onValueChanged(@NonNull Option<T> option, @NonNull T value, @NonNull String name) {
        if ((option instanceof Option.Width || option instanceof Option.Height)) {
            Preview preview = camera.getPreview();
            boolean wrapContent = (Integer) value == ViewGroup.LayoutParams.WRAP_CONTENT;
            if (preview == Preview.SURFACE && !wrapContent) {
                message("The SurfaceView preview does not support width or height changes. " +
                        "The view will act as WRAP_CONTENT by default.", true);
                return false;
            }
        }
        option.set(camera, value);
    /*    BottomSheetBehavior b = BottomSheetBehavior.from(controlPanel);
        b.setState(BottomSheetBehavior.STATE_HIDDEN);*/
        message("Changed " + option.getName() + " to " + name, false);
        return true;
    }

    //region Permissions

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean valid = true;
        for (int grantResult : grantResults) {
            valid = valid && grantResult == PackageManager.PERMISSION_GRANTED;
        }
        if (valid && !camera.isOpened()) {
            camera.open();
        }
        String[] PERMISSIONS = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.ACCESS_FINE_LOCATION",
           "android.permission.ACCESS_COARSE_LOCATION"};
//检测是否有写的权限
        int permission = ContextCompat.checkSelfPermission(this,
            "android.permission.WRITE_EXTERNAL_STORAGE");
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // 没有写的权限，去申请写的权限，会弹出对话框
            ActivityCompat.requestPermissions(this, PERMISSIONS,1);
        }
    }

    //endregion
}
