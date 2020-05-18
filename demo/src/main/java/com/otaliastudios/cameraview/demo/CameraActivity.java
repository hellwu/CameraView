package com.otaliastudios.cameraview.demo;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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


public class CameraActivity extends AppCompatActivity implements View.OnClickListener, OptionView.Callback {

    private final static CameraLogger LOG = CameraLogger.create("DemoApp");
    private final static boolean USE_FRAME_PROCESSOR = false;
    private final static boolean DECODE_BITMAP = true;

    private CameraView camera;
    //private ViewGroup controlPanel;
    private long mCaptureTime;

    private int mCurrentFilter = 0;
    private final Filters[] mAllFilters = Filters.values();

    private SensorManager sensorManager;

    private MySensorEventListenner mySensorEventListener;
   // private Handler mHandler;

   private CheckBox cb_wg;
  private CheckBox cb_sp;
  // private  View gridview;
    SharedPreferences photo;

    TextView tv_time;
    TextView tv_jd;
    TextView tv_wd;
    TextView tv_place;
    TextView   tv_name;
    TextView   tv_id;

    String timeStr;

    String placeStr="";

    String wdStr="";

    String jdStr="";
    LinearLayout top_ll;
    private Handler mHandler;

    private GPSLocationManager gpsLocationManager;
    AlertDialog.Builder builder;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        getSupportActionBar().hide();
        CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mySensorEventListener=new MySensorEventListenner();
        camera = findViewById(R.id.camera);
        cb_wg = findViewById(R.id.cb_wg);
        top_ll = findViewById(R.id.top_ll);
        cb_sp = findViewById(R.id.cb_sp);
        tv_name = findViewById(R.id.textView11);
        tv_id = findViewById(R.id.textView10);



        camera.setLifecycleOwner(this);
        camera.addCameraListener(new Listener());

        Configuration mConfiguration = this.getResources().getConfiguration(); //获取设置的配置信息
        int ori = mConfiguration.orientation; //获取屏幕方向

         photo = getSharedPreferences("photo", MODE_PRIVATE);

        String name = photo.getString("name", "");
        if(!name.equals("")){
                tv_name.setText(name);
                tv_id.setText(photo.getString("ID", ""));
        }

        boolean is43 = photo.getBoolean("is43", false);
        if(is43){


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
                top_ll.setLayoutParams(new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,2));
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
                top_ll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,2));
            }
            cb_wg.setChecked(true);
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
                top_ll.setLayoutParams(new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,0));
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
                top_ll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,0));
            }
            cb_wg.setChecked(false);
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


        cb_wg.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                   // top_ll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,2));
               /*     SizeSelector width = SizeSelectors.minWidth(3000);
                    SizeSelector height = SizeSelectors.minHeight(4000);
                    SizeSelector dimensions = SizeSelectors.and(width, height); // Matches sizes bigger than 1000x2000.
                    SizeSelector ratio = SizeSelectors.aspectRatio(AspectRatio.of(3, 4), 0); // Matches 1:1 sizes.
                    SizeSelector result = SizeSelectors.or(
                            SizeSelectors.and(ratio, dimensions), // Try to match both constraints
                            ratio, // If none is found, at least try to match the aspect ratio
                            SizeSelectors.biggest() // If none is found, take the biggest
                    );
                    camera.setPictureSize(result);*/
                    SharedPreferences.Editor edit = photo.edit();
                     edit.putBoolean("is43",true);
                    edit.commit();
                    startActivity(new Intent(CameraActivity.this,CameraActivity.class));
                    finish();
                }else{

                    //top_ll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,0));
                 /*   SizeSelector width = SizeSelectors.minWidth(2000);
                    SizeSelector height = SizeSelectors.minHeight(4000);
                    SizeSelector dimensions = SizeSelectors.and(width, height); // Matches sizes bigger than 1000x2000.
                    SizeSelector ratio = SizeSelectors.aspectRatio(AspectRatio.of(1, 2), 0); // Matches 1:1 sizes.

                    SizeSelector result = SizeSelectors.or(
                            SizeSelectors.and(ratio, dimensions), // Try to match both constraints
                            ratio, // If none is found, at least try to match the aspect ratio
                            SizeSelectors.biggest() // If none is found, take the biggest
                    );
                    camera.setPictureSize(result);*/
                    SharedPreferences.Editor edit = photo.edit();
                    edit.putBoolean("is43",false);
                    edit.commit();
                    startActivity(new Intent(CameraActivity.this,CameraActivity.class));
                    finish();
                }

            }
        });

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
        Sensor sensor_orientation=sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sensorManager.registerListener(mySensorEventListener,sensor_orientation, SensorManager.SENSOR_DELAY_UI);
        super.onResume();
    }

    private void message(@NonNull String content, boolean important) {
        if (important) {
            LOG.w(content);
           // Toast.makeText(this, content, Toast.LENGTH_LONG).show();
        } else {
            LOG.i(content);
            //Toast.makeText(this, content, Toast.LENGTH_SHORT).show();
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

    private class MySensorEventListenner implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if(event.sensor.getType()==Sensor.TYPE_ORIENTATION){
//x表示手机指向的方位，0表示北,90表示东，180表示南，270表示西
                float x = event.values[SensorManager.DATA_X];
                float y = event.values[SensorManager.DATA_Y];
                float z = event.values[SensorManager.DATA_Z];
              //  Log.d("xxx","Orientation:"+x+","+y+","+z);
                Message msg=new Message();
                msg.obj=z;
                //mHandler.sendMessage(msg);
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

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
           /* PicturePreviewActivity.setPictureResult(result);
            Intent intent = new Intent(CameraActivity.this, PicturePreviewActivity.class);
            intent.putExtra("delay", callbackTime - mCaptureTime);
            startActivity(intent);*/


    /*        Collection<Size> supportedPictureSizes = camera.getCameraOptions().getSupportedPictureSizes();
            for(Size s:supportedPictureSizes){
                Log.d("xxx",s.getWidth()+"--"+s.getHeight());
            }
*/
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
            //case R.id.captureVideo: captureVideo(); break;
            //case R.id.toggleCamera: toggleCamera(); break;
           // case R.id.changeFilter: changeCurrentFilter(); break;
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

    @Override
    public void onBackPressed() {
     /*   BottomSheetBehavior b = BottomSheetBehavior.from(controlPanel);
        if (b.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            b.setState(BottomSheetBehavior.STATE_HIDDEN);
            return;
        }*/
        super.onBackPressed();
    }

    private void edit() {
        Facing facing = camera.getFacing();
        if(facing==Facing.BACK){
            camera.setFacing(Facing.FRONT);
        }else{
            camera.setFacing(Facing.BACK);
        }
       /* BottomSheetBehavior b = BottomSheetBehavior.from(controlPanel);
        b.setState(BottomSheetBehavior.STATE_COLLAPSED);*/
    }

    private void capturePicture() {
        //******************************
        int time = photo.getInt("time", 0);
        if(time==100000000){
            Toast.makeText(CameraActivity.this,"已经超过测试拍照次数，请联系开发人员",Toast.LENGTH_SHORT).show();
            return;
        }
        SharedPreferences.Editor edit = photo.edit();
        edit.putInt("time",time+1);
        edit.commit();
        //******************************
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

    private void  savePicture(final PictureResult result){
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

                    Paint paint2 = new Paint();  //画笔
                    paint2.setStrokeWidth(1);  //设置线宽。单位为像素

                    paint2.setStyle(Paint.Style.FILL_AND_STROKE);//设置画笔的类型是填充，还是描边，还是描边且填充
                    //paint.setAntiAlias(true); //抗锯齿
                    paint2.setColor(Color.WHITE);  //画笔颜色

                    canvas.drawBitmap(bitmap,new Matrix(),paint);  //在画布上画一个和bitmap一模一样的图

                    Facing facing = camera.getFacing();

                    Configuration mConfiguration = CameraActivity.this.getResources().getConfiguration(); //获取设置的配置信息
                    int ori = mConfiguration.orientation; //获取屏幕方向
                    if(facing==Facing.BACK){//后置摄像头，绘制分辨率

                        boolean is43 = photo.getBoolean("is43", false);
                        if(!is43){  //16:9



                            if(ori==mConfiguration.ORIENTATION_LANDSCAPE) {//横屏
                                //画logo
                                Bitmap bitmaps = BitmapFactory.decodeResource(getResources(), R.drawable.icon_logo);
                                bitmaps= Bitmap.createScaledBitmap(bitmaps, 167, 167, true);
                                canvas.drawBitmap(bitmaps,55,29,paint);

                                //绘制当前时间
                                Bitmap timeBitmap = GLFont.getImageOnRl(getAssets(),700, 80, timeStr, 66,Color.WHITE, Typeface.create("宋体",Typeface.BOLD));
                                canvas.drawBitmap(timeBitmap,result.getSize().getWidth()-timeBitmap.getWidth()-50,68,paint);
                                Log.d("xxx1",result.getSize().getWidth()+"");
                                Log.d("xxx1",result.getSize().getWidth()-timeBitmap.getWidth()+"");

                                //绘制wifi图片
                                Bitmap bitmapwifi = BitmapFactory.decodeResource(getResources(), R.drawable.icon_service);
                                bitmapwifi= Bitmap.createScaledBitmap(bitmapwifi, 85, 85, true);
                                canvas.drawBitmap(bitmapwifi,result.getSize().getWidth()-timeBitmap.getWidth()-bitmapwifi.getWidth()-85,68,paint);

                                //绘制经纬度
                                Bitmap jdBitmap = GLFont.getImage(getAssets(),1500, 80,wdStr , 66);
                                canvas.drawBitmap(jdBitmap,55,result.getSize().getHeight()-jdBitmap.getHeight()-50,paint);

                                Bitmap wdBitmap = GLFont.getImage(getAssets(),1500, 80,jdStr , 66);
                                canvas.drawBitmap(wdBitmap,55,result.getSize().getHeight()-jdBitmap.getHeight()-wdBitmap.getHeight()-70,paint);

                                //绘制位置
                                Bitmap placeBitmap = GLFont.getImage(getAssets(),2000, 80, placeStr, 66);
                                canvas.drawBitmap(placeBitmap,55,result.getSize().getHeight()-jdBitmap.getHeight()-wdBitmap.getHeight()-placeBitmap.getHeight()-90,paint);
                                //绘制ID
                                Bitmap idBitmap = GLFont.getImageOnRl(getAssets(),1200, 80, tv_id.getText().toString(), 66, Color.WHITE, Typeface.create("宋体",Typeface.BOLD));
                                canvas.drawBitmap(idBitmap,result.getSize().getWidth()-idBitmap.getWidth()-50,result.getSize().getHeight()-idBitmap.getHeight()*2-15,paint);
                                //绘制名字
                                Bitmap nameBitmap = GLFont.getImageOnRl(getAssets(),1200, 80, tv_name.getText().toString(), 66,Color.WHITE, Typeface.create("宋体",Typeface.BOLD));
                                canvas.drawBitmap(nameBitmap,result.getSize().getWidth()-nameBitmap.getWidth()-50,result.getSize().getHeight()-jdBitmap.getHeight()-wdBitmap.getHeight()-placeBitmap.getHeight()-80,paint);
                            }else{
                                //画logo
                                Bitmap bitmaps = BitmapFactory.decodeResource(getResources(), R.drawable.icon_logo);
                                bitmaps= Bitmap.createScaledBitmap(bitmaps, 180, 180, true);
                                canvas.drawBitmap(bitmaps,58,50,paint);

                                //绘制当前时间
                                Bitmap timeBitmap = GLFont.getImageOnRl(getAssets(),820, 80, timeStr, 69,Color.WHITE, Typeface.create("宋体",Typeface.BOLD));
                                canvas.drawBitmap(timeBitmap,result.getSize().getWidth()-timeBitmap.getWidth()-85,100,paint);

                                //绘制wifi图片
                                Bitmap bitmapwifi = BitmapFactory.decodeResource(getResources(), R.drawable.icon_service);
                                bitmapwifi= Bitmap.createScaledBitmap(bitmapwifi, 85, 85, true);
                                canvas.drawBitmap(bitmapwifi,result.getSize().getWidth()-timeBitmap.getWidth()-bitmapwifi.getWidth()-50,95,paint);

                                //绘制经纬度
                                Bitmap jdBitmap = GLFont.getImage(getAssets(),1500, 80,wdStr , 69);
                                canvas.drawBitmap(jdBitmap,55,result.getSize().getHeight()-jdBitmap.getHeight()-45,paint);

                                Bitmap wdBitmap = GLFont.getImage(getAssets(),1500, 80,jdStr , 69);
                                canvas.drawBitmap(wdBitmap,55,result.getSize().getHeight()-jdBitmap.getHeight()-wdBitmap.getHeight()-65,paint);

                                //绘制位置
                                Bitmap placeBitmap = GLFont.getImage(getAssets(),2000, 80, placeStr, 69);
                                canvas.drawBitmap(placeBitmap,55,result.getSize().getHeight()-jdBitmap.getHeight()-wdBitmap.getHeight()-placeBitmap.getHeight()-85,paint);
                                //绘制ID
                                Bitmap idBitmap = GLFont.getImage(getAssets(),1200, 80, tv_id.getText().toString(), 69);
                                canvas.drawBitmap(idBitmap,55,result.getSize().getHeight()-jdBitmap.getHeight()-wdBitmap.getHeight()-placeBitmap.getHeight()-idBitmap.getHeight()-105,paint);
                                //绘制名字
                                Bitmap nameBitmap = GLFont.getImage(getAssets(),1200, 80, tv_name.getText().toString(), 69);
                                canvas.drawBitmap(nameBitmap,55,result.getSize().getHeight()-jdBitmap.getHeight()-wdBitmap.getHeight()-placeBitmap.getHeight()-idBitmap.getHeight()-nameBitmap.getHeight()-125,paint);
                            }
                        }else{   //4:3



                            if(ori==mConfiguration.ORIENTATION_LANDSCAPE) {//4:3横屏

                                //根据Bitmap大小，画网格线
                                //画logo
                                Bitmap bitmaps = BitmapFactory.decodeResource(getResources(), R.drawable.icon_logo);
                                bitmaps= Bitmap.createScaledBitmap(bitmaps, 152, 152, true);
                                canvas.drawBitmap(bitmaps,48,25,paint);

                                //绘制当前时间
                                Bitmap timeBitmap = GLFont.getImageOnRl(getAssets(),700, 80, timeStr, 60,Color.WHITE, Typeface.create("宋体",Typeface.BOLD));
                                canvas.drawBitmap(timeBitmap,result.getSize().getWidth()-timeBitmap.getWidth()-50,68,paint);

                                //绘制wifi图片
                                Bitmap bitmapwifi = BitmapFactory.decodeResource(getResources(), R.drawable.icon_service);
                                bitmapwifi= Bitmap.createScaledBitmap(bitmapwifi, 75, 75, true);
                                canvas.drawBitmap(bitmapwifi,result.getSize().getWidth()-timeBitmap.getWidth()-bitmapwifi.getWidth()-30,68,paint);

                                //绘制经纬度
                                Bitmap jdBitmap = GLFont.getImage(getAssets(),1500, 80,wdStr , 60);
                                canvas.drawBitmap(jdBitmap,55,result.getSize().getHeight()-jdBitmap.getHeight()-40,paint);

                                Bitmap wdBitmap = GLFont.getImage(getAssets(),1500, 100,jdStr , 60);
                                canvas.drawBitmap(wdBitmap,55,result.getSize().getHeight()-jdBitmap.getHeight()-wdBitmap.getHeight()-40,paint);

                                //绘制位置
                                Bitmap placeBitmap = GLFont.getImage(getAssets(),2000, 100, placeStr, 60);
                                canvas.drawBitmap(placeBitmap,55,result.getSize().getHeight()-jdBitmap.getHeight()-wdBitmap.getHeight()-placeBitmap.getHeight()-40,paint);

                                //绘制ID
                                Bitmap idBitmap = GLFont.getImageOnRl(getAssets(),1200, 80, tv_id.getText().toString(), 60, Color.WHITE, Typeface.create("宋体",Typeface.BOLD));
                                canvas.drawBitmap(idBitmap,result.getSize().getWidth()-idBitmap.getWidth()-50,result.getSize().getHeight()-idBitmap.getHeight()*2,paint);
                                //绘制名字
                                Bitmap nameBitmap = GLFont.getImageOnRl(getAssets(),1200, 100, tv_name.getText().toString(), 60,Color.WHITE, Typeface.create("宋体",Typeface.BOLD));
                                canvas.drawBitmap(nameBitmap,result.getSize().getWidth()-nameBitmap.getWidth()-50,result.getSize().getHeight()-jdBitmap.getHeight()-wdBitmap.getHeight()-placeBitmap.getHeight()-40,paint);
                            }else{//4:3竖屏

                                //根据Bitmap大小，画网格线
                                //画logo
                                Bitmap bitmaps = BitmapFactory.decodeResource(getResources(), R.drawable.icon_logo);
                                bitmaps= Bitmap.createScaledBitmap(bitmaps, 150, 150, true);
                                canvas.drawBitmap(bitmaps,45,43,paint);

                                //绘制当前时间
                                Bitmap timeBitmap = GLFont.getImageOnRl(getAssets(),700, 80, timeStr, 58,Color.WHITE, Typeface.create("宋体",Typeface.BOLD));
                                canvas.drawBitmap(timeBitmap,result.getSize().getWidth()-timeBitmap.getWidth()-75,82,paint);

                                //绘制wifi图片
                                Bitmap bitmapwifi = BitmapFactory.decodeResource(getResources(), R.drawable.icon_service);
                                bitmapwifi= Bitmap.createScaledBitmap(bitmapwifi, 75, 75, true);
                                canvas.drawBitmap(bitmapwifi,result.getSize().getWidth()-timeBitmap.getWidth()-bitmapwifi.getWidth()-10,80,paint);

                                //绘制经纬度
                                Bitmap jdBitmap = GLFont.getImage(getAssets(),1500, 84,wdStr , 58);
                                canvas.drawBitmap(jdBitmap,55,result.getSize().getHeight()-jdBitmap.getHeight()-35,paint);

                                Bitmap wdBitmap = GLFont.getImage(getAssets(),1500, 84,jdStr , 58);
                                canvas.drawBitmap(wdBitmap,55,result.getSize().getHeight()-jdBitmap.getHeight()-wdBitmap.getHeight()-35,paint);

                                //绘制位置
                                Bitmap placeBitmap = GLFont.getImage(getAssets(),2000, 84, placeStr, 58);
                                canvas.drawBitmap(placeBitmap,55,result.getSize().getHeight()-jdBitmap.getHeight()-wdBitmap.getHeight()-placeBitmap.getHeight()-35,paint);
                                //绘制ID
                                Bitmap idBitmap = GLFont.getImage(getAssets(),1200, 84, tv_id.getText().toString(), 58);
                                canvas.drawBitmap(idBitmap,55,result.getSize().getHeight()-jdBitmap.getHeight()-wdBitmap.getHeight()-placeBitmap.getHeight()-idBitmap.getHeight()-35,paint);
                                //绘制名字
                                Bitmap nameBitmap = GLFont.getImage(getAssets(),1200, 84, tv_name.getText().toString(), 58);
                                canvas.drawBitmap(nameBitmap,55,result.getSize().getHeight()-jdBitmap.getHeight()-wdBitmap.getHeight()-placeBitmap.getHeight()-idBitmap.getHeight()-nameBitmap.getHeight()-35,paint);
                            }
                        }


                    }else{
                        //根据Bitmap大小，画网格线
                        //画logo
                        Bitmap bitmaps = BitmapFactory.decodeResource(getResources(), R.drawable.icon_logo);
                        bitmaps= Bitmap.createScaledBitmap(bitmaps, 25, 25, true);
                        canvas.drawBitmap(bitmaps,5,5,paint);

                        //绘制当前时间
                        Bitmap timeBitmap = GLFont.getImage(getAssets(),120, 20, timeStr, 10);
                        canvas.drawBitmap(timeBitmap,result.getSize().getWidth()-timeBitmap.getWidth(),0,paint);
                        //绘制wifi图片
                        Bitmap bitmapwifi = BitmapFactory.decodeResource(getResources(), R.drawable.icon_service);
                        bitmapwifi= Bitmap.createScaledBitmap(bitmapwifi, 15, 15, true);
                        canvas.drawBitmap(bitmapwifi,result.getSize().getWidth()-timeBitmap.getWidth()-bitmapwifi.getWidth()-10,5,paint);

                        //绘制经纬度
                        Bitmap jdBitmap = GLFont.getImage(getAssets(),375, 20,wdStr , 10);
                        canvas.drawBitmap(jdBitmap,20,result.getSize().getHeight()-jdBitmap.getHeight(),paint);

                        Bitmap wdBitmap = GLFont.getImage(getAssets(),375, 20,jdStr , 10);
                        canvas.drawBitmap(wdBitmap,20,result.getSize().getHeight()-jdBitmap.getHeight()-wdBitmap.getHeight(),paint);

                        //绘制位置
                        Bitmap placeBitmap = GLFont.getImage(getAssets(),500, 20, placeStr, 10);
                        canvas.drawBitmap(placeBitmap,20,result.getSize().getHeight()-jdBitmap.getHeight()-wdBitmap.getHeight()-placeBitmap.getHeight(),paint);

                        if(ori==mConfiguration.ORIENTATION_LANDSCAPE) {//横屏
                            //绘制ID
                            Bitmap idBitmap = GLFont.getImageOnRl(getAssets(),300, 20, tv_id.getText().toString(), 10, Color.WHITE, Typeface.create("宋体",Typeface.BOLD));
                            canvas.drawBitmap(idBitmap,result.getSize().getWidth()-idBitmap.getWidth()-20,result.getSize().getHeight()-idBitmap.getHeight(),paint);
                            //绘制名字
                            Bitmap nameBitmap = GLFont.getImageOnRl(getAssets(),300, 20, tv_name.getText().toString(), 10, Color.WHITE, Typeface.create("宋体",Typeface.BOLD));
                            canvas.drawBitmap(nameBitmap,result.getSize().getWidth()-nameBitmap.getWidth()-20,result.getSize().getHeight()-nameBitmap.getHeight()-idBitmap.getHeight(),paint);
                        }else{
                            //绘制ID
                            Bitmap idBitmap = GLFont.getImage(getAssets(),300, 20, tv_id.getText().toString(), 10);
                            canvas.drawBitmap(idBitmap,20,result.getSize().getHeight()-jdBitmap.getHeight()-wdBitmap.getHeight()-placeBitmap.getHeight()-idBitmap.getHeight(),paint);
                            //绘制名字
                            Bitmap nameBitmap = GLFont.getImage(getAssets(),100, 20, tv_name.getText().toString(), 10);
                            canvas.drawBitmap(nameBitmap,20,result.getSize().getHeight()-jdBitmap.getHeight()-wdBitmap.getHeight()-placeBitmap.getHeight()-idBitmap.getHeight()-nameBitmap.getHeight(),paint);
                        }
                    }
                    Log.d("xxx",bitmap.getWidth()+"--"+bitmap.getHeight());
                   Matrix matrix=new Matrix();
                    boolean is43 = photo.getBoolean("is43", false);
                   if(!is43&&ori==mConfiguration.ORIENTATION_LANDSCAPE){
                       matrix.postScale((float) 2048/bitmap.getWidth(),(float)1152/bitmap.getHeight());
                   }else if(!is43&&ori==mConfiguration.ORIENTATION_PORTRAIT){
                       matrix.postScale((float) 1152/bitmap.getWidth(),(float)2048/bitmap.getHeight());
                   }else if(is43&&ori==mConfiguration.ORIENTATION_LANDSCAPE){
                       matrix.postScale((float) 1707/bitmap.getWidth(),(float)1280/bitmap.getHeight());
                   }else{
                       matrix.postScale((float) 1280/bitmap.getWidth(),(float)1707/bitmap.getHeight());
                   }

                   Bitmap newBitmap=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);

                    DateFormat format = new SimpleDateFormat("yyyy年MM月dd日-HHmmss-");
                    Date date = new Date();
                    saveBitmap(newBitmap,format.format(date)+date.getTime()+".JPEG");
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

                wdStr="纬度:" + location.getLatitude();
                jdStr="经度:" + location.getLongitude();
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
