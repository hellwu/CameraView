package com.otaliastudios.cameraview.demo;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;

public class GridSurfaceView extends SurfaceView {
  private int lineX=27;
  private int lineY=45;
  private Paint mPaint=null;
  private Paint mPaint2=null;
  private int width;
  private int height;
  private  int mRatioWidth=0;
  private int mRatioHeight=0;
  private int specifiedWeight;
  private int specifiedHeight;
  Handler mHandle;

  public GridSurfaceView(Context context) {
    this(context,null);
  }

  public GridSurfaceView(Context context, AttributeSet attrs) {
    this(context, attrs,0);
  }

  public GridSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    TypedArray a=context.obtainStyledAttributes(attrs, R.styleable.GridAutoTextureView);
    lineX=a.getInteger(R.styleable.GridAutoTextureView_linesX,lineX);
    lineY=a.getInteger(R.styleable.GridAutoTextureView_linesY,lineY);
    MyApplication.lineX=lineX;
    MyApplication.lineY=lineY;
    a.recycle();
    init();
    setWillNotDraw(false);//这个方法是保证回调ondraw方法用的，可以考虑用监听Surface状态的时候使用
    //new TimeThread().start(); //启动新的线程



  }

  /**
   * 设置长宽比
   */
  public void setAspectRatio(int width,int heigth){
    if(width<0||heigth<0){
      throw new IllegalArgumentException("长宽参数不能为负");
    }
    mRatioHeight=heigth;
    mRatioWidth=width;
    requestLayout();//宽高比之后重新绘制
  }
  private void init() {//关于paint类
    mPaint=new Paint();
    mPaint.setColor(Color.WHITE);
   // mPaint.setAlpha(40);
    mPaint.setStyle(Paint.Style.FILL_AND_STROKE);//设置画笔的类型是填充，还是描边，还是描边且填充
    mPaint.setStrokeWidth(2);//设置笔刷的粗细

    mPaint2=new Paint();
    mPaint2.setColor(Color.WHITE);
    // mPaint.setAlpha(40);
    mPaint2.setStyle(Paint.Style.FILL_AND_STROKE);//设置画笔的类型是填充，还是描边，还是描边且填充
    mPaint2.setStrokeWidth(3);//设置笔刷的粗细

  }
  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    width= MeasureSpec.getSize(widthMeasureSpec);
    height= MeasureSpec.getSize(heightMeasureSpec);
    if(0==mRatioWidth||0==mRatioHeight){//初次绘制的情况
      setMeasuredDimension(width,height);
      specifiedWeight=width;//将当下绘制的SurfaceView的长宽比用于赋值，以便计算格线的位置
      specifiedHeight=height;
    }else{
      if(width<height*mRatioWidth/mRatioHeight)//哪边占比小就用它为绘制参考便，实际上是在选择同比例最大绘制范围
      {
        setMeasuredDimension(width,width*mRatioHeight/mRatioWidth);//设置SurfaceView的大小适应于预览流的大小
        specifiedWeight=width;//将当下绘制的SurfaceView的长宽比用于赋值，以便计算格线的位置
        specifiedHeight=width*mRatioHeight/mRatioWidth;
      }else{
        setMeasuredDimension(height*mRatioWidth/mRatioHeight,height);
        specifiedWeight=height*mRatioWidth/mRatioHeight;
        specifiedHeight=height;
      }
    }
  }
  @Override
  protected void onDraw(final Canvas canvas) {
    super.onDraw(canvas);
    Log.d("xxx","onDraw!!!!!!!!!!!!!!!!!!!");

  }
/*  class TimeThread extends Thread {
    @Override
    public void run() {
      do {
        try {
          Thread.sleep(1000);
          Message msg = new Message();
          msg.what = 1;  //消息(一个整型值)
          mHandle.sendMessage(msg);// 每隔1秒发送一个msg给mHandler
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      } while (true);
    }
  }*/


}
