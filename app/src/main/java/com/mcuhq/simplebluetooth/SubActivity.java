package com.mcuhq.simplebluetooth;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class SubActivity extends AppCompatActivity {

    private Button mLedOnBtn;
    private Button mLedOffBtn;
    private ImageView mMotorStartBtn;
    private ImageView mMotorReverseBtn;
    private ImageView mMotorStopBtn;
    private ImageView mMotorUpBtn;
    private ImageView mMotorDownBtn;
    private ConnectedThread mConnectedThread;
    Intent secondIntent = getIntent();
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.subactivity_main);

        mLedOnBtn = (Button) findViewById(R.id.btn_led_on);
        mLedOffBtn = (Button) findViewById(R.id.btn_led_off);
        mMotorStartBtn = (ImageView) findViewById(R.id.btn_motor_start);
        mMotorReverseBtn = (ImageView) findViewById(R.id.btn_motor_revserse);
        mMotorStopBtn = (ImageView) findViewById(R.id.btn_motor_stop);
        mMotorUpBtn = (ImageView) findViewById(R.id.btn_motor_up);
        mMotorDownBtn = (ImageView) findViewById(R.id.btn_motor_down);


        mLedOnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickButtonSend(view);
            }
        });



    }

    public void onClickButtonSend(View view){
        mConnectedThread= (ConnectedThread) secondIntent.getCharSequenceExtra("BtConnected");
        if(mConnectedThread!=null){

            byte[] buff = new byte[1024];


            int test[] = {0xff,0xA2,0x5D,0xff};

            for(int i=0; i<=3; i++) {
                mConnectedThread.write1(test[i]);
            }

        }
    }


}
