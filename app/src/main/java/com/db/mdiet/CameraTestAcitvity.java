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
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.media.ExifInterface;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;



import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CameraTestAcitvity extends AppCompatActivity implements SurfaceHolder.Callback, View.OnClickListener {
    Camera camera;
    SurfaceView sv_camera;
    SurfaceHolder surfaceHolder;
    ImageButton imbtn_camera,imbtn_close,imbtn_camera_switch;
    String imageFilePath = "";
    RelativeLayout rl_btn;
    Button zoomin;
    OrientationEventListener orientationEventListener;
    int orientation=0;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_activity);
        getWindow().setFormat(PixelFormat.UNKNOWN);
        init();
    }

    public void init() {
        orientationEventListener=new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int ori) {
                orientation=ori;

            }
        };
        orientationEventListener.enable();
        sv_camera = (SurfaceView) findViewById(R.id.sv_camera);
        surfaceHolder = sv_camera.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        imbtn_camera = (ImageButton) findViewById(R.id.imbtn_camera);
        imbtn_camera.setOnClickListener(this);
        imbtn_close=(ImageButton)findViewById(R.id.imbtn_close);
        imbtn_close.setOnClickListener(this);
        imbtn_camera_switch=(ImageButton)findViewById(R.id.imbtn_camera_switch);
        imbtn_camera_switch.setOnClickListener(this);
        rl_btn=(RelativeLayout)findViewById(R.id.rl_btn);
        rl_btn.setAlpha((float)1.0);
        zoomin=(Button)findViewById(R.id.zoomin);
        zoomin.setOnClickListener(this);
    }

    Camera.PictureCallback takepicturecallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "TT_" + timeStamp + "_";
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File image = null;
            try {
                image = File.createTempFile(
                        imageFileName,      /* prefix */
                        ".jpg",         /* suffix */
                        storageDir          /* directory */
                );
            } catch (IOException e) {
                e.printStackTrace();
            }
            imageFilePath = image.getAbsolutePath();
            ExifInterface exif = null;

            try {
                exif = new ExifInterface(imageFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }

            int exifOrientation;
            int exifDegree;

            if (exif != null) {
                exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                Log.i("exif", exifOrientation + "");
                exifDegree = exifOrientationToDegrees(exifOrientation);
            } else {
                exifDegree = 0;
            }
            if (getResources().getConfiguration().orientation!= Configuration.ORIENTATION_LANDSCAPE) {
              exifDegree=90;
            }else{
             exifDegree=0;
            }
            File nf = new File(imageFilePath);
            Bitmap resultbit = rotate(bitmap, exifDegree);

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

                camera.startPreview();

            } catch (Exception e) {
                Log.e("dd", e.getMessage().toString());

            }
        }

    };
    @Override
    public void surfaceCreated(SurfaceHolder holder) {

            camera = Camera.open();
        try {

            camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Camera.Parameters parameters = camera.getParameters();
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

        for(Camera.Size size : parameters.getSupportedPreviewSizes()) {
            parameters.setPreviewSize(size.width,size.height);
        }
        if (getResources().getConfiguration().orientation!= Configuration.ORIENTATION_LANDSCAPE) {
            parameters.set("orientation","portrait");
            camera.setDisplayOrientation(90);
            parameters.setRotation(90);
        }else{
            parameters.set("orientation","landscape");
            camera.setDisplayOrientation(0);
            parameters.setRotation(0);
        }

        camera.startPreview();
        camera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {

            }
        });
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (surfaceHolder != null) {
            return;
        }
        try {
            camera.stopPreview();
        } catch (Exception e) {

        }
        Camera.Parameters parameters = camera.getParameters();
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        if (rotation == android.view.Surface.ROTATION_0) {
            camera .setDisplayOrientation(90);
            parameters.setRotation(90);
        }else if(rotation ==android.view.Surface.ROTATION_90){
            camera .setDisplayOrientation(0);
            parameters.setRotation(0);
        }else if(rotation == android.view.Surface.ROTATION_180){
            camera .setDisplayOrientation(270);
            parameters.setRotation(270);
        }else{
            camera .setDisplayOrientation(180);
            parameters.setRotation(180);
        }



            parameters.setPreviewSize(width,height);

        camera.setParameters(parameters);

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
            if(camera!=null){
                camera.stopPreview();
                camera.release();
            }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        Log.d("dd","move");
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imbtn_camera:
            if (camera != null) {
                camera.takePicture(null, null, takepicturecallback);
            }
            break;
            case R.id.imbtn_close:
                finish();
                break;
            case R.id.imbtn_camera_switch:
                break;
            case R.id.zoomin:
                Camera.Parameters parameters=camera.getParameters();
               int zoom= parameters.getZoom();
               parameters.setZoom(++zoom);
               camera.setParameters(parameters);
              //camera.startSmoothZoom(++zoom);
                break;
        }
    }
    public int exifOrientationToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
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
}
