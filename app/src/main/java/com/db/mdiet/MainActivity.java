package com.db.mdiet;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    Button btn_camera,btn_camera2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }
    public void init(){
        btn_camera=(Button)findViewById(R.id.btn_camera);
        btn_camera.setOnClickListener(this);
        btn_camera2=(Button)findViewById(R.id.btn_camera2);
        btn_camera2.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_camera:
                startActivity(new Intent(getApplicationContext(),CameraTestAcitvity.class));
                break;
            case R.id.btn_camera2:
                startActivity(new Intent(getApplicationContext(),CameraTest2Acitvity.class));
                break;
        }

    }
}
