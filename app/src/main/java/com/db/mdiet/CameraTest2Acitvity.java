package com.db.mdiet;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Range;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class CameraTest2Acitvity extends AppCompatActivity implements View.OnClickListener ,View.OnTouchListener {
    CameraManager cameraManager;
    TextureView tx_camera;
    CameraDevice cameraDevice;
    SurfaceHolder surfaceHolder;
    ImageButton imbtn_camera,imbtn_close,imbtn_camera_switch;
    String imageFilePath = "";
    RelativeLayout rl_btn;
    Button zoomin;
    CaptureRequest.Builder previewbuilder;
    CameraCaptureSession captureSession;
    public boolean checkstart=false;
    static final int NONE = 0;

    static final int DRAG = 1;

    static final int ZOOM = 2;

    int mode = NONE;
    float oldDist = 1f;

    float newDist = 1f;

    private float mScaleFactor = 1.0f;
    private float zoomlevel=1f;
    Rect zoomrect;
    boolean checkfront=false;
    float maxzoom;
    OrientationEventListener orientationEventListener;
    int orientation=0;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_activity2);
        getWindow().setFormat(PixelFormat.UNKNOWN);
        init();
    }

    public void init() {
        orientationEventListener=new OrientationEventListener(this,SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int ori) {
                orientation=ori;

            }
        };
        orientationEventListener.enable();
        tx_camera = (TextureView) findViewById(R.id.tx_camera);
        imbtn_camera = (ImageButton) findViewById(R.id.imbtn_camera);
        imbtn_camera.setOnClickListener(this);
        imbtn_close=(ImageButton)findViewById(R.id.imbtn_close);
        imbtn_close.setOnClickListener(this);
        imbtn_camera_switch=(ImageButton)findViewById(R.id.imbtn_camera_switch);
        imbtn_camera_switch.setOnClickListener(this);
        rl_btn=(RelativeLayout)findViewById(R.id.rl_btn);
        rl_btn.setAlpha(230);

        inittexture();


    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    private CameraDevice.StateCallback previewcallback =new CameraDevice.StateCallback(){
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            try {
                cameraDevice=camera;
                 previewbuilder=camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                SurfaceTexture tt=tx_camera.getSurfaceTexture();
                Surface surface=new Surface(tt);
                previewbuilder.addTarget(surface);
                previewbuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                camera.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        captureSession=session;
                        updatePreview();
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                    }
                },null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }

    };
public void inittexture(){
    tx_camera.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            checkstart=true;
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    });
}
public void openCamera(){
    cameraManager=(CameraManager)getSystemService(CAMERA_SERVICE);
    try {
        String camearaId = cameraManager.getCameraIdList()[0];
        camearaId=getIdFrontalCamera(cameraManager,checkfront);
        CameraCharacteristics characteristics=cameraManager.getCameraCharacteristics(camearaId);
        int orientation=characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        zoomrect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
         maxzoom=characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        int level=characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        Range<Integer>fps[]=characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        cameraManager.openCamera(camearaId,previewcallback,null);
    }catch (Exception e){

    }
}
public void capturecameara(){
    try {
       final CaptureRequest.Builder capturebuilder=cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        SurfaceTexture csurfacetexture=tx_camera.getSurfaceTexture();
        Surface sf=new Surface(csurfacetexture);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        int ScreenWidth  = metrics.widthPixels;
        int ScreenHeight = metrics.heightPixels;
        ImageReader reader=ImageReader.newInstance(ScreenWidth,ScreenHeight, ImageFormat.JPEG,1);
        List<Surface>outputsurface=new ArrayList<>();
        outputsurface.add(reader.getSurface());
        outputsurface.add(sf);
        capturebuilder.addTarget(reader.getSurface());
        capturebuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomrect);
        reader.setOnImageAvailableListener(imagelistener,null);
        int orientation =getWindowManager().getDefaultDisplay().getRotation();
        capturebuilder.set(CaptureRequest.JPEG_ORIENTATION,orientation);
        captureSession.stopRepeating();
        cameraDevice.createCaptureSession(outputsurface, new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                try {
                    session.capture(capturebuilder.build(),captureCallback,null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {

            }
        },null);


    } catch (CameraAccessException e) {
        e.printStackTrace();
    }
}
ImageReader.OnImageAvailableListener imagelistener=new ImageReader.OnImageAvailableListener() {
    @Override
    public void onImageAvailable(ImageReader reader) {
        Image image = reader.acquireLatestImage();
        ByteBuffer bb = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[bb.capacity()];
        bb.get(bytes);
        Bitmap resultbit = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "TT_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imagefile = null;
        try {
            imagefile = File.createTempFile(
                    imageFileName,      /* prefix */
                    ".jpg",         /* suffix */
                    storageDir          /* directory */
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        imageFilePath = imagefile.getAbsolutePath();
        ExifInterface exif = null;



        int exifDegree;

        exifDegree=exifOrientationToDegrees(orientation);
        File nf = new File(imageFilePath);
        resultbit = rotate(resultbit, exifDegree);

        try {
            File ff = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures");
            if (!ff.exists()) {
                ff.mkdir();
            }
            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures/" + nf.getName();
            FileOutputStream out = new FileOutputStream(path);
            resultbit.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.close();
            File f = new File(path);
            Uri u = getImageContentUri(getApplicationContext(), f);

            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, u));
        } catch (Exception e) {

        }
    }
};
CameraCaptureSession.CaptureCallback captureCallback=new CameraCaptureSession.CaptureCallback() {
    @Override
    public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
        super.onCaptureStarted(session, request, timestamp, frameNumber);
        Log.i("start","start");
    }

    @Override
    public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
        super.onCaptureProgressed(session, request, partialResult);
        Log.i("progress","progress");
    }

    @Override
    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
        super.onCaptureCompleted(session, request, result);
        Log.i("complete","complete");
        openCamera();
    }

    @Override
    public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
        super.onCaptureFailed(session, request, failure);
        openCamera();
    }

    @Override
    public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
        super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
    }

    @Override
    public void onCaptureSequenceAborted(@NonNull CameraCaptureSession session, int sequenceId) {
        super.onCaptureSequenceAborted(session, sequenceId);
    }

    @Override
    public void onCaptureBufferLost(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull Surface target, long frameNumber) {
        super.onCaptureBufferLost(session, request, target, frameNumber);
    }
};

public void updatePreview(){
    try {
        captureSession.setRepeatingRequest(previewbuilder.build(),null,null);
    } catch (CameraAccessException e) {
        e.printStackTrace();
    }
}
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imbtn_camera:
            capturecameara();
            break;
            case R.id.imbtn_close:
                finish();
                break;
            case R.id.imbtn_camera_switch:
                if(checkfront){
                    checkfront=false;
                }else{
                    checkfront=true;
                }
                cameraDevice.close();
                openCamera();
                break;
            case R.id.zoomin:

                break;
        }
    }
    public int exifOrientationToDegrees(int exifOrientation) {

    if(exifOrientation<45){
      return 90;
    }
    else if (exifOrientation < 90) {
            return 180;
        } else if (exifOrientation < 180) {
            return 270;
        }else if(exifOrientation<225){
        return 270;
    }
    else if (exifOrientation < 270) {
            return 0;
        }else if(exifOrientation<315){
        return 0;
    }
        return 90;
    }
    public Bitmap cameramask(Activity act, Bitmap bit) {
        long now = System.currentTimeMillis();
        Date date = new Date(now);

        SimpleDateFormat sdf = new SimpleDateFormat("yy-MM-dd HH:mm");
        String getTime = sdf.format(date);
        Bitmap bmOverlay = Bitmap.createBitmap(bit.getWidth(), bit.getHeight(), bit.getConfig());


        Canvas canvas = new Canvas(bmOverlay);

        Typeface tp = Typeface.createFromAsset(getAssets(), "notosanskr_bold.otf");
        int fontsize = getResources().getDimensionPixelSize(R.dimen.picturefont);
        Paint p1 = new Paint();
        p1.setColor(Color.WHITE);
        p1.setStrokeWidth(2f);
        p1.setTextSize(fontsize);
        p1.setTypeface(tp);
        p1.setShadowLayer(10, 0, 0, Color.BLACK);

        Paint p2 = new Paint();
        p2.setColor(Color.WHITE);
        p2.setTextSize(fontsize);
        p2.setTypeface(tp);
        p2.setShadowLayer(10, 0, 0, Color.BLACK);

        Paint p3 = new Paint();
        p3.setColor(Color.WHITE);
        p3.setTextSize(fontsize);
        p3.setTypeface(tp);
        p3.setShadowLayer(10, 0, 0, Color.BLACK);
        Paint p4 = new Paint();
        p4.setColor(Color.WHITE);
        p4.setTextSize(fontsize);
        p4.setTypeface(tp);
        p4.setShadowLayer(10, 0, 0, Color.BLACK);

        canvas.drawBitmap(bit, 0, 0, null);

        DisplayMetrics metrics = new DisplayMetrics();
        act.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        int ScreenWidth = metrics.widthPixels;
        int ScreenHeight = metrics.heightPixels;
        Rect rect = new Rect();
        String tname="dd";
        p2.getTextBounds(tname, 0, tname.length(), rect);
        int textWidth = rect.width() + 50;
        int textHeight = rect.height();
        Rect rect2 = new Rect();
        p4.getTextBounds(getTime, 0, getTime.length(), rect2);
        int textWidth2 = rect2.width() + 50;
        int textHeight2 = rect2.height();

//        float width= p1.measureText("장기업무 지원팀");
        canvas.drawText("Diet", 50f, textHeight, p1);
        canvas.drawText(tname, canvas.getWidth() - textWidth, textHeight, p2);
        String text="000"+" "+"test";
        canvas.drawText(text, 50f, canvas.getHeight() - 30f, p3);
        canvas.drawText(getTime, canvas.getWidth() - textWidth2, canvas.getHeight() - 30f, p4);
        return bmOverlay;
    }
    public static Uri getImageContentUri(Context context, File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID},
                MediaStore.Images.Media.DATA + "=? ",
                new String[]{filePath}, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            cursor.close();
            return Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                return context.getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }

    public Bitmap rotate(Bitmap src, float degree) {

// Matrix 객체 생성
        Matrix matrix = new Matrix();
// 회전 각도 셋팅
        matrix.postRotate(degree);
// 이미지와 Matrix 를 셋팅해서 Bitmap 객체 생성
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(),
                src.getHeight(), matrix, true);
    }

    public boolean motion(MotionEvent event){
        int act = event.getAction();

        String strMsg = "";


        switch(act & MotionEvent.ACTION_MASK) {

            case MotionEvent.ACTION_DOWN: //첫번째 손가락 터치(드래그 용도)




                Log.d("zoom", "mode=DRAG" );

                mode = DRAG;

                break;

            case MotionEvent.ACTION_MOVE:

                if(mode == DRAG) { // 드래그 중



                } else if (mode == ZOOM) { // 핀치 중

                    newDist = spacing(event);


                    if (newDist - oldDist > 20) { // zoom in

                        oldDist = newDist;


                        strMsg = "zoom in";

                        if(zoomlevel<maxzoom) {
                            ++zoomlevel;
                            Log.d("zoomin", zoomlevel + "");
                            DisplayMetrics metrics = new DisplayMetrics();
                            getWindowManager().getDefaultDisplay().getMetrics(metrics);

                            float ratio = 1 / zoomlevel;
                            int r = zoomrect.right - (Math.round(zoomrect.right * ratio));
                            int b = zoomrect.bottom - (Math.round(zoomrect.bottom * ratio));
                            Rect rr = new Rect(r / 2, b / 2, zoomrect.right - (r / 2), zoomrect.bottom - (b / 2));
                            previewbuilder.set(CaptureRequest.SCALER_CROP_REGION, rr);
                            try {
                                captureSession.setRepeatingRequest(previewbuilder.build(), null, null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();

                            }

                        }
                    } else if(oldDist - newDist > 20) { // zoom out

                        oldDist = newDist;


                        strMsg = "zoom out";

                        if(zoomlevel>0) {
                            --zoomlevel;
                            Log.d("zoomout", zoomlevel + "");
                            DisplayMetrics metrics = new DisplayMetrics();
                            getWindowManager().getDefaultDisplay().getMetrics(metrics);

                            float ratio = 1 / zoomlevel;
                            int r = zoomrect.right - (Math.round(zoomrect.right * ratio));
                            int b = zoomrect.bottom - (Math.round(zoomrect.bottom * ratio));
                            Rect rr = new Rect(r / 2, b / 2, zoomrect.right - (r / 2), zoomrect.bottom - (b / 2));
                            previewbuilder.set(CaptureRequest.SCALER_CROP_REGION, rr);
                            try {
                                captureSession.setRepeatingRequest(previewbuilder.build(), null, null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();

                            }
                        }
                    }

                }

                break;

            case MotionEvent.ACTION_UP: // 첫번째 손가락을 떼었을 경우

            case MotionEvent.ACTION_POINTER_UP: // 두번째 손가락을 떼었을 경우

                mode = NONE;

                break;

            case MotionEvent.ACTION_POINTER_DOWN:

                //두번째 손가락 터치(손가락 2개를 인식하였기 때문에 핀치 줌으로 판별)

                mode = ZOOM;



                newDist = spacing(event);

                oldDist = spacing(event);

                // 이미지뷰 스케일에 적용


                Log.d("zoom", "newDist=" + newDist);

                Log.d("zoom", "oldDist=" + oldDist);

                Log.d("zoom", "mode=ZOOM");

                break;

            case MotionEvent.ACTION_CANCEL:

            default :

                break;

        }



        return super.onTouchEvent(event);

    }

    private float spacing(MotionEvent event) {

        float x = event.getX(0) - event.getX(1);

        float y = event.getY(0) - event.getY(1);
//        mScaleGestureDetector.onTouchEvent(event);
        return (float) Math.sqrt(x * x + y * y);



    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        motion(event);
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
    motion(event);
        return super.onTouchEvent(event);
    }

    private String getIdFrontalCamera (CameraManager manager,boolean checkfront) {
        String resultid="";
    try {

            for(String id : manager.getCameraIdList()){
                CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(id);
                //Seek frontal camera.
                if(checkfront) {
                    if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                        resultid = id;
                        return resultid;

                    }
                }else{
                    if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                        resultid = id;
                        return resultid;

                    }
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return resultid;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(cameraDevice!=null){
            cameraDevice.close();
        }
    }


}
