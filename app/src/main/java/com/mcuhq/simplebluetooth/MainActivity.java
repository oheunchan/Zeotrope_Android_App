package com.mcuhq.simplebluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getSimpleName();

    private static final UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

    // #defines for identifying shared types between calling functions
    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    public final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status

    // GUI Components
    private TextView mBluetoothStatus; // 상태표시창 Text
    private TextView mReadBuffer;  // 읽어온 버퍼값 Text
    private Button mScanBtn; // 블루투스 on 버튼
    private Button mOffBtn; // 블루투스 off 버튼
    private Button mListPairedDevicesBtn; // 페어링된 디바이스 불러오기 버튼
    private Button mDiscoverBtn; // 연결가능한 디바이스 목록찾기 버튼
    private ListView mDevicesListView; // 블투 연결 할 디바이스 목록 찾기 뷰리스트



    private Button mAutoModeOnbtn;
    private Button mMoterOnBtn;
    private Button mMoterOffBtn;
    private Button mResetBtn;
    private Button mEtcBtn1;
    private Button mEtcBtn2;
    private Button mEtcBtn3;

    private Button m1Minute;
    private Button m2Minute;
    private Button m3Minute;


    private Button mTestBtn;
    private Button mBtnPlay;
    private Button mBtnAPlay;
    private Button mBtnCPlay;


    private BluetoothAdapter mBTAdapter; // 블루투스 관련 설정 객체
    private Set<BluetoothDevice> mPairedDevices;
    private ArrayAdapter<String> mBTArrayAdapter;

    private Handler mHandler; // Our main handler that will receive callback notifications
    private ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path

    private Timer timerCall;
    private Timer timerOffCall; // 2분뒤 끄는 용도용
    private boolean menuMode;   //  메뉴얼모드 키 (true : 오토모드 / false : 메뉴얼모드)

    private TextView timerTextView;

    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;

    private int cntOn;
    private int cntOff;
    private int timeSet = 2;

    // 노래 재생 배열
    private int currentSongIndex = 0;

    private int playbackPositionA = 0; // A곡 재생 위치 저장 변수
    private int playbackPositionC = 0; // C곡 재생 위치 저장 변수
    // 음악 리스트
    //private int[] songResources = {R.raw.test1, R.raw.test2};
    //private MediaPlayer mMediaPlayer;
    // 평상시 리스트
    private MediaPlayer mMediaPlayerA;
    private int[] songResourcesA = {R.raw.test1, R.raw.test2};
    // 타이머 동작시 바뀔 음악 리스트
    private MediaPlayer mMediaPlayerC;
    private int[] songResourcesC = {R.raw.change};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 처음 보여줄 화면
        setContentView(R.layout.activity_main);

        // View 단의 속성을 부여
        mBluetoothStatus = (TextView)findViewById(R.id.bluetooth_status);
        mDiscoverBtn = (Button)findViewById(R.id.discover);
        mListPairedDevicesBtn = (Button)findViewById(R.id.paired_btn);

        // 임시버튼

        mAutoModeOnbtn = (Button)findViewById(R.id.btn_autoModeOn);
        mMoterOnBtn = (Button)findViewById(R.id.btn_moterOn);
        mMoterOffBtn = (Button)findViewById(R.id.btn_moterOff);

        mResetBtn = (Button)findViewById(R.id.btn_reset);
        mEtcBtn1 = (Button)findViewById(R.id.btn_etc1);
        mEtcBtn2 = (Button)findViewById(R.id.btn_etc2);
        mEtcBtn3 = (Button)findViewById(R.id.btn_etc3);
        //mTestBtn = (Button)findViewById(R.id.btn_test);
        //mBtnPlay = (Button)findViewById(R.id.btn_play);
        //mBtnAPlay = (Button)findViewById(R.id.btn_a_play);
        //mBtnCPlay = (Button)findViewById(R.id.btn_c_play);
        timerTextView = (TextView) findViewById(R.id.timerTextView);
        m1Minute = (Button)findViewById(R.id.btn_1minute);
        m2Minute = (Button)findViewById(R.id.btn_2minute);
        m3Minute = (Button)findViewById(R.id.btn_3minute);

        mBTArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1); // 리스트로 보여줄 어댑터 (텍스트뷰 하나로 구성된 레이아웃)
        mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // 블루투스 연결이 가능한지 여부 파악 (연결이 안 됐으면 null 값 반환)

        mDevicesListView = (ListView)findViewById(R.id.devices_list_view); // listView View단과 바인딩
        mDevicesListView.setAdapter(mBTArrayAdapter); // 어댑터를 이용해서 데이터를 View로 만들어서 뿌려줌
        mDevicesListView.setOnItemClickListener(mDeviceClickListener); // 각 View들은 누를시 이벤트를 가짐 (블투연결이벤트)



        // 위치 권한 요청
        // 권한 보유 유무 체크 ContextCompat.checkSelfPermission 메서드 반환타입은 PERMISSION_GRANTED or PERMISSION_DENIED
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            // 권한 없을시 아래 메서드를 통해 권한을 다시 요청함
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

        mHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg){
                if(msg.what == MESSAGE_READ){
                    String readMessage = null;
                    readMessage = new String((byte[]) msg.obj, StandardCharsets.UTF_8);
                   // mReadBuffer.setText(readMessage);
                }

                if(msg.what == CONNECTING_STATUS){
                    char[] sConnected;
                    if(msg.arg1 == 1)
                        mBluetoothStatus.setText(getString(R.string.BTConnected) + msg.obj);
                    else
                        mBluetoothStatus.setText(getString(R.string.BTconnFail));
                }
            }
        };

        if (mBTArrayAdapter == null) {
            // Device does not support Bluetooth
            mBluetoothStatus.setText(getString(R.string.sBTstaNF));
            Toast.makeText(getApplicationContext(),getString(R.string.sBTdevNF),Toast.LENGTH_SHORT).show();
        }
        else {

            /*mScanBtn.setOnClickListener(new View.OnClickListener() { // 블루투스 on 버튼 이벤트 부여
                @Override
                public void onClick(View v) {
                    bluetoothOn();
                } // 블루투스 on 메서드
            });

            mOffBtn.setOnClickListener(new View.OnClickListener(){ // 블루투스 off 버튼 이벤트 부여
                @Override
                public void onClick(View v){
                    bluetoothOff();
                } //블루투스 off 메서드
            });*/

            mListPairedDevicesBtn.setOnClickListener(new View.OnClickListener() { //블루투스 페어링된 디바이스 리스트 버튼 이벤트 부여
                @Override
                public void onClick(View v){
                    listPairedDevices();
                } // 페어링된 디바이스 메서드
            });

            mDiscoverBtn.setOnClickListener(new View.OnClickListener(){  // 블루투스 연결가능한 디바이스 찾기 버튼 이벤트 부여
                @Override
                public void onClick(View v){
                    discover();
                } // 연결가능한 디바이스 찾기 메서드
            });

            mAutoModeOnbtn.setOnClickListener(new View.OnClickListener(){  // 오토모드 버튼 누를시 이벤트
                @Override
                public void onClick(View v){
                    onClickButtonAutoModeOn();
                }
            });

            m1Minute.setOnClickListener(new View.OnClickListener(){  // 1분 버튼 누를시 이벤트
                @Override
                public void onClick(View v){
                    onClickButton1minute();
                }
            });

            m2Minute.setOnClickListener(new View.OnClickListener(){  // 2분 버튼 누를시 이벤트
                @Override
                public void onClick(View v){
                    onClickButton2minute();
                }
            });

            m3Minute.setOnClickListener(new View.OnClickListener(){  // 3분 버튼 누를시 이벤트
                @Override
                public void onClick(View v){
                    onClickButton3minute();
                }
            });

            mMoterOnBtn.setOnClickListener(new View.OnClickListener(){  // 모터On 버튼 이벤트
                @Override
                public void onClick(View v){
                    onClickButtonMoterOn();
                }
            });

            mMoterOffBtn.setOnClickListener(new View.OnClickListener(){  // 모터OFF 버튼 이벤트
                @Override
                public void onClick(View v){
                    onClickButtonMoterOff();
                }
            });

            mResetBtn.setOnClickListener(new View.OnClickListener(){  // 리셋 버튼 이벤트
                @Override
                public void onClick(View v){
                    onClickButtonReset();
                }
            });

            mEtcBtn1.setOnClickListener(new View.OnClickListener(){  // etc1 버튼 이벤트
                @Override
                public void onClick(View v){
                    onClickButtonEtc1();
                }
            });

            mEtcBtn2.setOnClickListener(new View.OnClickListener(){  // etc2 버튼 이벤트
                @Override
                public void onClick(View v){
                    onClickButtonEtc2();
                }
            });

            mEtcBtn3.setOnClickListener(new View.OnClickListener(){  // etc3 버튼 이벤트
                @Override
                public void onClick(View v){
                    onClickButtonEtc3();
                }
            });

            /*mTestBtn.setOnClickListener(new View.OnClickListener(){  // 테스트모드 버튼 누를시 이벤트
                @Override
                public void onClick(View v){
                    onClickTest();
                }
            });

            mBtnAPlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onClickA(view);
                }
            });
            mBtnCPlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onClickC(view);
                }
            });*/

        }
    }

    private void bluetoothOn(){
        if (!mBTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            mBluetoothStatus.setText(getString(R.string.BTEnable));
            Toast.makeText(getApplicationContext(),getString(R.string.sBTturON),Toast.LENGTH_SHORT).show();

        }
        else{
            Toast.makeText(getApplicationContext(),getString(R.string.BTisON), Toast.LENGTH_SHORT).show();
        }
    }

    // Enter here after user selects "yes" or "no" to enabling radio
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data){
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                mBluetoothStatus.setText(getString(R.string.sEnabled));
            }
            else
                mBluetoothStatus.setText(getString(R.string.sDisabled));
        }
    }

    private void bluetoothOff(){
        mBTAdapter.disable(); // turn off
        mBluetoothStatus.setText(getString(R.string.sBTdisabl));
        Toast.makeText(getApplicationContext(),"Bluetooth turned Off", Toast.LENGTH_SHORT).show();
    }

    private void discover(){
        // Check if the device is already discovering
        if(mBTAdapter.isDiscovering()){
            mBTAdapter.cancelDiscovery();
            Toast.makeText(getApplicationContext(),getString(R.string.DisStop),Toast.LENGTH_SHORT).show();
        }
        else{
            if(mBTAdapter.isEnabled()) {
                mBTArrayAdapter.clear(); // clear items
                mBTAdapter.startDiscovery();
                Toast.makeText(getApplicationContext(), getString(R.string.DisStart), Toast.LENGTH_SHORT).show();
                registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            }
            else{
                Toast.makeText(getApplicationContext(), getString(R.string.BTnotOn), Toast.LENGTH_SHORT).show();
            }
        }
    }

    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name to the list
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mBTArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    private void listPairedDevices(){ // 페어링된 디바이스 보기 함수
        mBTArrayAdapter.clear(); // 기존에 있던 값들을 지워줌
        mPairedDevices = mBTAdapter.getBondedDevices(); // 기존 페어링된 목록을 가져옴
        if(mBTAdapter.isEnabled()) { // 블루투스어댑터 사용여부 가능한지
            // put it's one to the adapter
            for (BluetoothDevice device : mPairedDevices)
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress()); // 어댑터배열에 이름 및 주소 부여

            Toast.makeText(getApplicationContext(), getString(R.string.show_paired_devices), Toast.LENGTH_SHORT).show(); // 문구 짧게 출력
        }
        else
            Toast.makeText(getApplicationContext(), getString(R.string.BTnotOn), Toast.LENGTH_SHORT).show(); // 문구 짧게 출력
    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            if(!mBTAdapter.isEnabled()) {
                Toast.makeText(getBaseContext(), getString(R.string.BTnotOn), Toast.LENGTH_SHORT).show();
                return;
            }

            mBluetoothStatus.setText(getString(R.string.cConnet));
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) view).getText().toString();
            final String address = info.substring(info.length() - 17);
            final String name = info.substring(0,info.length() - 17);

            // Spawn a new thread to avoid blocking the GUI one
            new Thread()
            {
                @Override
                public void run() {
                    boolean fail = false;

                    BluetoothDevice device = mBTAdapter.getRemoteDevice(address);

                    try {
                        mBTSocket = createBluetoothSocket(device);
                    } catch (IOException e) {
                        fail = true;
                        Toast.makeText(getBaseContext(), getString(R.string.ErrSockCrea), Toast.LENGTH_SHORT).show();
                    }
                    // Establish the Bluetooth socket connection.
                    try {
                        mBTSocket.connect();
                    } catch (IOException e) {
                        try {
                            fail = true;
                            mBTSocket.close();
                            mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                    .sendToTarget();
                        } catch (IOException e2) {
                            //insert code to deal with this
                            Toast.makeText(getBaseContext(), getString(R.string.ErrSockCrea), Toast.LENGTH_SHORT).show();
                        }
                    }
                    if(!fail) {
                        mConnectedThread = new ConnectedThread(mBTSocket, mHandler);
                        mConnectedThread.start();

                        mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                                .sendToTarget();
                    }
                }
            }.start();
        }
    };

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, BT_MODULE_UUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection",e);
        }
        return  device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
    }

    private  void btConnectedPage(View view){
        Intent intent = new Intent(getApplicationContext(), SubActivity.class);
        intent.putExtra("BtConnected", mConnectedThread);
        startActivity(intent);
    }

    //타이머
    private void startTimer() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                countDownTimer = new CountDownTimer(timeSet * 60 * 1000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        long seconds = millisUntilFinished / 1000;
                        long minutes = seconds / 60;
                        seconds = seconds % 60;

                        String timeFormatted = String.format("%02d:%02d", minutes, seconds);
                        timerTextView.setText(timeFormatted);
                    }

                    @Override
                    public void onFinish() {
                        timerTextView.setText("Motor Off");
                        isTimerRunning = false;
                    }
                }.start();

                isTimerRunning = true;
            }
        });
    }

    private void stopTimer() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                    timerTextView.setText("Motor OfftimeSet");
                    isTimerRunning = false;
                }
            }
        });
    }

    // 이전에 예약된 타이머 작업 취소
    private void cancelTimers() {
        if (timerCall != null) {
            timerCall.cancel();
            timerCall.purge();
            timerCall = null;
        }
        if (timerOffCall != null) {
            timerOffCall.cancel();
            timerOffCall.purge();
            timerOffCall = null;
        }
    }



    // 오토모드 버튼 on

    public void onClickButtonAutoModeOn() {
        System.out.println("오토모드 버튼");

        // 현재 시간을 기준으로 다음 0분, 20분, 40분의 시간을 계산하여 예약
        Calendar currentTime = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"));
        int currentMinute = currentTime.get(Calendar.MINUTE);
        int currentSecond = currentTime.get(Calendar.SECOND);
        System.out.println("현재 시간: " + currentMinute + "분 " + currentSecond + "초");

        if (mConnectedThread != null) {
            if (menuMode) {
                System.out.println("menumode true");
                mMoterOnBtn.setEnabled(true);
                mMoterOffBtn.setEnabled(true);
                cancelTimers(); // 이전에 예약된 작업 취소
                menuMode = false;
            } else {
                System.out.println("menumode false");
                mMoterOnBtn.setEnabled(false);
                mMoterOffBtn.setEnabled(false);

                TimerTask timerTask = new TimerTask() {

                    @Override
                    public void run() {
                        Date date = new Date();
                        SimpleDateFormat simpl = new SimpleDateFormat("yyyy년 MM월 dd일 aa hh시 mm분 ss초");
                        String s = simpl.format(date);
                        System.out.println("모터시작  " + s);
                        onClickButtonMoterOn();

                        TimerTask newTimerOffTask = new TimerTask() {
                            @Override
                            public void run() {
                                Date date = new Date();
                                SimpleDateFormat simpl = new SimpleDateFormat("yyyy년 MM월 dd일 aa hh시 mm분 ss초");
                                String s = simpl.format(date);
                                System.out.println("2분뒤 실행 " + s);
                                onClickButtonMoterOff();
                                cancel();
                                System.out.println("타이머 종료");
                            }

                            @Override
                            public boolean cancel() {
                                return super.cancel();
                            }
                        };
                        timerOffCall.schedule(newTimerOffTask, timeSet * 60 * 1000);
                    }
                };

                cancelTimers(); // 이전에 예약된 작업 취소

                timerCall = new Timer();
                timerOffCall = new Timer();
                int delay = ((20 - (currentMinute % 20)) * 60 - currentSecond) * 1000;
                System.out.println("delay = " + delay);
                menuMode = true;
                timerCall.schedule(timerTask, delay, 20 * 60 * 1000); // 20분마다 작업 반복
            }
        }
    }

    // 1 분
    public void onClickButton1minute(){
        timeSet = 1;
        colorSet();
        // 진한 회색 색상 부여
        int colorStrongGrey = ContextCompat.getColor(this, R.color.colorStrongGrey);
        m1Minute.setBackgroundTintList(ColorStateList.valueOf(colorStrongGrey));
    }

    // 2 분
    public void onClickButton2minute(){
        colorSet();
        timeSet = 2;
        int colorStrongGrey = ContextCompat.getColor(this, R.color.colorStrongGrey);
        m2Minute.setBackgroundTintList(ColorStateList.valueOf(colorStrongGrey));
    }

    // 3 분
    public void onClickButton3minute(){
        colorSet();
        timeSet = 3;
        int colorStrongGrey = ContextCompat.getColor(this, R.color.colorStrongGrey);
        m3Minute.setBackgroundTintList(ColorStateList.valueOf(colorStrongGrey));
    }
    // 색상 초기화
    public void colorSet(){
        int colorGrey = ContextCompat.getColor(this, R.color.colorButtonNormal);
        m1Minute.setBackgroundTintList(ColorStateList.valueOf(colorGrey));
        m2Minute.setBackgroundTintList(ColorStateList.valueOf(colorGrey));
        m3Minute.setBackgroundTintList(ColorStateList.valueOf(colorGrey));
    }
    // 모터 on
    public void onClickButtonMoterOn(){
        System.out.println("MainActivity.onClickButtonMoterOn");
        cntOn++;
        if(mConnectedThread!=null){
            if(menuMode) {
                startTimer();
            }

            byte[] buff = new byte[1024];
            int array[] = {0xFF,0x02,0xFD,0xFF};

            for(int i=0; i< array.length; i++) {
                mConnectedThread.write1(array[i]);
            }
            System.out.println("카운트 on 갯수 " + cntOn);
        }
    }

    // 모터 off
    public void onClickButtonMoterOff(){
        cntOff++;
        System.out.println("MainActivity.onClickButtonMoterOff");
        if(mConnectedThread!=null){
            //if(menuMode) {
                stopTimer();
            //}
            System.out.println("hi");
            byte[] buff = new byte[1024];
            int array[] = {0xFF,0xC2,0x3D,0xFF};

            for(int i=0; i< array.length; i++) {
                mConnectedThread.write1(array[i]);
            }
            System.out.println("카운트 off 갯수 " + cntOff);
        }
    }

    // 리셋
    public void onClickButtonReset(){
        if(mConnectedThread!=null){

            byte[] buff = new byte[1024];
            int array[] = {0xFF,0x68,0x97,0xFF};

            for(int i=0; i< array.length; i++) {
                mConnectedThread.write1(array[i]);
            }
        }
    }

    // ETC 1
    public void onClickButtonEtc1(){
        if(mConnectedThread!=null){

            byte[] buff = new byte[1024];
            int array[] = {0xFF,0x90,0x6F,0xFF};

            for(int i=0; i< array.length; i++) {
                mConnectedThread.write1(array[i]);
            }
        }
    }

    // ETC 2
    public void onClickButtonEtc2(){
        if(mConnectedThread!=null){

            byte[] buff = new byte[1024];
            int array[] = {0xFF,0xA2,0x5D,0xFF};

            for(int i=0; i< array.length; i++) {
                mConnectedThread.write1(array[i]);
            }
        }
    }

    // ETC 3
    public void onClickButtonEtc3(){
        if(mConnectedThread!=null){

            byte[] buff = new byte[1024];
            int array[] = {0xFF,0x62,0x9D,0xFF};

            for(int i=0; i< array.length; i++) {
                mConnectedThread.write1(array[i]);
            }
        }
    }

    public void onClickTest() {
        System.out.println("오토모드 버튼");

        // 현재 시간을 기준으로 다음 5분, 10분, 15분, ... 의 시간을 계산하여 예약
        Calendar currentTime = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"));
        int currentMinute = currentTime.get(Calendar.MINUTE);
        int currentSecond = currentTime.get(Calendar.SECOND);
        System.out.println("현재 시간: " + currentMinute + "분 " + currentSecond + "초");


        if (menuMode) {
            System.out.println("menumode true");
            mMoterOnBtn.setEnabled(true);
            mMoterOffBtn.setEnabled(true);
            cancelTimers(); // 이전에 예약된 작업 취소
            menuMode = false;
        } else {
            System.out.println("menumode false");
            mMoterOnBtn.setEnabled(false);
            mMoterOffBtn.setEnabled(false);

            TimerTask timerTask = new TimerTask() {
                //private boolean isCanceled = false;

                @Override
                public void run() {
                    Date date = new Date();
                    SimpleDateFormat simpl = new SimpleDateFormat("yyyy년 MM월 dd일 aa hh시 mm분 ss초");
                    simpl.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
                    String s = simpl.format(date);
                    System.out.println("모터시작  " + s);
                    onClickButtonMoterOn();

                    // 2분 후에 모터를 끄는 작업 예약
                    TimerTask newTimerOffTask = new TimerTask() {
                        @Override
                        public void run() {
                            System.out.println("2분뒤 실행 " + currentMinute);
                            System.out.println("!isCanceled");
                            onClickButtonMoterOff();
                            cancel();
                            System.out.println("타이머 종료");
                        }

                        @Override
                        public boolean cancel() {
                            return super.cancel();
                        }
                    };
                    timerOffCall.schedule(newTimerOffTask, timeSet * 60 * 1000);
                }
            };

            cancelTimers(); // 이전에 예약된 작업 취소

            timerCall = new Timer();
            timerOffCall = new Timer();
            int delay = ((3 - (currentMinute % 3)) * 60 - currentSecond) * 1000;
            System.out.println("delay = " + delay);
            timerCall.schedule(timerTask, delay, 3 * 60 * 1000); // 3분마다 작업 반복
            menuMode = true;
        }
    }

    /*
    // PLAY 버튼 이벤트
    public void onClick(View view) {
        // MediaPlayer 없으면 초기화 진행
        if (mMediaPlayer == null) {
            int songResource = songResources[currentSongIndex];
            mMediaPlayer = MediaPlayer.create(MainActivity.this, songResource);	// 출력할 음악 파일 세팅
            // 음악 파일 재생이 완료됐을 때 호출될 콜백 세팅
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    // null 처리 및 버튼 텍스트 PLAY로 변경
                    mMediaPlayer = null;
                    mBtnPlay.setText("PLAY");
                    playNextSong(view); // 재생이 완료되면 다음 노래를 재생
                }
            });
        }

        isPlaying();
    }

    // 다음노래재생
    private void playNextSong(View view) {
        currentSongIndex++;
        if (currentSongIndex >= songResources.length) {
            currentSongIndex = 0; // 모든 노래를 재생한 경우, 처음 노래부터 다시 재생
        }
        onClick(view); // 다음 노래를 재생하기 위해 onClickPlay() 호출
    }

    // 플레이 체크
    private void isPlaying() {
        if (mMediaPlayer.isPlaying()) {    // 음악 파일 플레이중인 상태
            // 음악 파일 멈추고 버튼 텍스트 PLAY로 변경
            mMediaPlayer.pause();
            mBtnPlay.setText("PLAY");
        } else {    // 음악 파일 재생중이 아닌 상태
            // 음악 파일 재생하고 버튼 텍스트 STOP으로 변경
            mMediaPlayer.start();
            mBtnPlay.setText("STOP");
        }
    }*/



    public void onClickA(View view) {
        isPlaying();
        if (mMediaPlayerA == null) {
            int songResource = songResourcesA[currentSongIndex];
            mMediaPlayerA = MediaPlayer.create(MainActivity.this, songResource);
            mMediaPlayerA.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    mMediaPlayerA.release();
                    mMediaPlayerA = null;
                    // Handle completion of A song here if needed
                }
            });
            mMediaPlayerA.seekTo(playbackPositionA); // Seek to the saved playback position
            mMediaPlayerA.start();
            mBtnAPlay.setText("STOP A");
        } else {
            mMediaPlayerA.pause();
            playbackPositionA = mMediaPlayerA.getCurrentPosition(); // Save the current playback position
            mMediaPlayerA.release();
            mMediaPlayerA = null;
            mBtnAPlay.setText("PLAY A");
        }
    }

    public void onClickC(View view) {
        isPlaying();
        if (mMediaPlayerC == null) {
            int songResource = songResourcesC[currentSongIndex];
            mMediaPlayerC = MediaPlayer.create(MainActivity.this, songResource);
            mMediaPlayerC.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    mMediaPlayerC.release();
                    mMediaPlayerC = null;
                    // Handle completion of C song here if needed
                }
            });
            mMediaPlayerC.seekTo(playbackPositionC); // Seek to the saved playback position
            mMediaPlayerC.start();
            mBtnCPlay.setText("STOP C");
        } else {
            mMediaPlayerC.pause();
            playbackPositionC = mMediaPlayerC.getCurrentPosition(); // Save the current playback position
            mMediaPlayerC.release();
            mMediaPlayerC = null;
            mBtnCPlay.setText("PLAY C");
        }
    }

    private void isPlaying() {
        if (mMediaPlayerA != null && mMediaPlayerA.isPlaying()) {
            mMediaPlayerA.pause();
            playbackPositionA = mMediaPlayerA.getCurrentPosition(); // Save the current playback position
            mBtnAPlay.setText("PLAY A");
        }
        if (mMediaPlayerC != null && mMediaPlayerC.isPlaying()) {
            mMediaPlayerC.pause();
            playbackPositionC = mMediaPlayerC.getCurrentPosition(); // Save the current playback position
            mBtnCPlay.setText("PLAY C");
        }
    }




}
