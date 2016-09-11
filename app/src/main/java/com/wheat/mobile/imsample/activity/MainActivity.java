package com.wheat.mobile.imsample.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.wheat.mobile.imsample.R;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity {

    private EditText etUserName;
    private Button btCallVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etUserName=(EditText)findViewById(R.id.call_user_name);
        btCallVideo=(Button)findViewById(R.id.call_video);

        btCallVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userName=etUserName.getText().toString().trim();
                if(TextUtils.isEmpty(userName)){
                    Toast.makeText(MainActivity.this, "用户名不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent=new Intent(MainActivity.this,VideoCallActivity.class);
                intent.putExtra("username",userName);
                intent.putExtra("isComingCall",false);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
    }
}
