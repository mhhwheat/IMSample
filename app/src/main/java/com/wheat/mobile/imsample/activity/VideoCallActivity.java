package com.wheat.mobile.imsample.activity;

import android.content.Context;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hyphenate.chat.EMCallManager;
import com.hyphenate.chat.EMCallStateChangeListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMTextMessageBody;
import com.hyphenate.exceptions.EMServiceNotReadyException;
import com.hyphenate.media.EMLocalSurfaceView;
import com.hyphenate.media.EMOppositeSurfaceView;
import com.hyphenate.util.EMLog;
import com.hyphenate.util.NetUtils;
import com.hyphenate.util.PathUtil;
import com.wheat.mobile.imsample.R;

/**
 * Created by Administrator on 2016/9/11.
 */
public class VideoCallActivity extends FragmentActivity implements View.OnClickListener{
    private final int MSG_CALL_MAKE_VIDEO = 0;
    private final int MSG_CALL_MAKE_VOICE = 1;
    private final int MSG_CALL_ANSWER = 2;
    private final int MSG_CALL_REJECT = 3;
    private final int MSG_CALL_END = 4;
    private final int MSG_CALL_RLEASE_HANDLER = 5;
    private final int MSG_CALL_SWITCH_CAMERA = 6;

    private TextView callStateTextView;
    private LinearLayout comingBtnContainer;

    //拒绝
    private Button refuseBtn;
    //接听
    private Button answerBtn;
    //挂断
    private Button hangupBtn;
    //静音
    private ImageView muteImage;
    //外放
    private ImageView handsFreeImage;

    private TextView nickTextView;
    //计时
    private Chronometer chronometer;
    private LinearLayout voiceControllerLayout;
    private RelativeLayout rootContainer;
    private LinearLayout topContainer;
    private LinearLayout bottomContainer;
    private TextView monitorTextView;
    private TextView networkStatusView;

    private Button recordBtn;
    private Button toggleVideoBtn;
    private Button switchCameraBtn;


    private boolean isInComingCall;
    private String username;

    private EMLocalSurfaceView localSurfaceView;
    private EMOppositeSurfaceView oppositeSurfaceView;

    private SoundPool soundPool;
    private int outgoing;
    private AudioManager audioManager;
    private int streamID = -1;
    private Ringtone ringtone;

    private boolean isRecording=false;
    private boolean isAnswered=false;
    private boolean isHandsfreeState=false;
    private boolean isMuteSate=false;
    private CallingState callingState= CallingState.CANCED;

    private EMCallManager.EMVideoCallHelper callHelper;

    private EMCallStateChangeListener callStateListener;

    private boolean isInCalling;

    private Handler uiHandler;
    private String callDruationText;
    private boolean endCallTriggerByMe = false;


    enum CallingState {
        CANCED, NORMAL, REFUESD, BEREFUESD, UNANSWERED, OFFLINE, NORESPONSE, BUSY, VERSION_NOT_SAME
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null){
            finish();
            return;
        }

        setContentView(R.layout.em_activity_video_call);

        audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

        //保持通话页面不会息屏
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        uiHandler = new Handler();

        callStateTextView = (TextView) findViewById(R.id.tv_call_state);
        comingBtnContainer = (LinearLayout) findViewById(R.id.ll_coming_call);
        rootContainer = (RelativeLayout) findViewById(R.id.root_layout);
        refuseBtn = (Button) findViewById(R.id.btn_refuse_call);
        answerBtn = (Button) findViewById(R.id.btn_answer_call);
        hangupBtn = (Button) findViewById(R.id.btn_hangup_call);
        muteImage = (ImageView) findViewById(R.id.iv_mute);
        handsFreeImage = (ImageView) findViewById(R.id.iv_handsfree);
        callStateTextView = (TextView) findViewById(R.id.tv_call_state);
        nickTextView = (TextView) findViewById(R.id.tv_nick);
        chronometer = (Chronometer) findViewById(R.id.chronometer);
        voiceControllerLayout = (LinearLayout) findViewById(R.id.ll_voice_control);
        RelativeLayout btnsContainer = (RelativeLayout) findViewById(R.id.ll_btns);
        topContainer = (LinearLayout) findViewById(R.id.ll_top_container);
        bottomContainer = (LinearLayout) findViewById(R.id.ll_bottom_container);
        monitorTextView = (TextView) findViewById(R.id.tv_call_monitor);
        networkStatusView = (TextView) findViewById(R.id.tv_network_status);
        recordBtn = (Button) findViewById(R.id.btn_record_video);
        switchCameraBtn = (Button) findViewById(R.id.btn_switch_camera);

        refuseBtn.setOnClickListener(this);
        answerBtn.setOnClickListener(this);
        hangupBtn.setOnClickListener(this);
        muteImage.setOnClickListener(this);
        handsFreeImage.setOnClickListener(this);
        rootContainer.setOnClickListener(this);
        recordBtn.setOnClickListener(this);
        switchCameraBtn.setOnClickListener(this);

        isInComingCall=getIntent().getBooleanExtra("isComingCall",false);
        username=getIntent().getStringExtra("username");

        nickTextView.setText(username);

        localSurfaceView=(EMLocalSurfaceView)findViewById(R.id.local_surface);
        localSurfaceView.setZOrderMediaOverlay(true);
        localSurfaceView.setZOrderOnTop(true);

        oppositeSurfaceView=(EMOppositeSurfaceView)findViewById(R.id.opposite_surface);

        addCallSateListener();
        //来电
        if(!isInComingCall){
            soundPool=new SoundPool(1, AudioManager.STREAM_RING,0);
            outgoing=soundPool.load(this,R.raw.em_outgoing,1);

            comingBtnContainer.setVisibility(View.INVISIBLE);
            hangupBtn.setVisibility(View.VISIBLE);
            String st=getResources().getString(R.string.Are_connected_to_each_other);
            callStateTextView.setText(st);
            EMClient.getInstance().callManager().setSurfaceView(localSurfaceView,oppositeSurfaceView);
            handler.sendEmptyMessage(MSG_CALL_MAKE_VIDEO);
        }else{
            voiceControllerLayout.setVisibility(View.INVISIBLE);
            localSurfaceView.setVisibility(View.INVISIBLE);
            Uri ringUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            audioManager.setMode(AudioManager.MODE_RINGTONE);
            audioManager.setSpeakerphoneOn(true);
            ringtone=RingtoneManager.getRingtone(this,ringUri);
            ringtone.play();
            EMClient.getInstance().callManager().setSurfaceView(localSurfaceView,oppositeSurfaceView);
        }

        callHelper=EMClient.getInstance().callManager().getVideoCallHelper();

        EMClient.getInstance().callManager().setCameraDataProcessor(new EMCallManager.EMCameraDataProcessor() {
            @Override
            public void onProcessData(byte[] bytes, Camera camera, int i, int i1) {
                //处理摄像头数据，调亮度，增加滤镜等操作
            }
        });
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_refuse_call:
                refuseBtn.setEnabled(false);
                handler.sendEmptyMessage(MSG_CALL_REJECT);
                break;
            case R.id.btn_answer_call:
                answerBtn.setEnabled(false);
                openSpeakerOn();
                if(ringtone!=null){
                    ringtone.stop();
                }
                callStateTextView.setText("answering");
                handler.sendEmptyMessage(MSG_CALL_ANSWER);
                handsFreeImage.setImageResource(R.mipmap.em_icon_speaker_on);
                isAnswered=true;
                isHandsfreeState=true;
                comingBtnContainer.setVisibility(View.INVISIBLE);
                hangupBtn.setVisibility(View.VISIBLE);
                voiceControllerLayout.setVisibility(View.VISIBLE);
                localSurfaceView.setVisibility(View.VISIBLE);
                break;
            case R.id.btn_hangup_call:
                hangupBtn.setEnabled(false);
                chronometer.stop();
                endCallTriggerByMe = true;
                callStateTextView.setText(getResources().getString(R.string.hanging_up));
                if(isRecording){
                    callHelper.stopVideoRecord();
                }
                handler.sendEmptyMessage(MSG_CALL_END);
                break;

            case R.id.iv_mute:
                if(isMuteSate){
                    muteImage.setImageResource(R.mipmap.em_icon_mute_normal);
                    EMClient.getInstance().callManager().resumeVoiceTransfer();
                    isMuteSate=false;
                }else{
                    muteImage.setImageResource(R.mipmap.em_icon_mute_on);
                    EMClient.getInstance().callManager().pauseVoiceTransfer();
                    isMuteSate=true;
                }
                break;
            case R.id.iv_handsfree:
                if(isHandsfreeState){
                    handsFreeImage.setImageResource(R.mipmap.em_icon_speaker_normal);
                    closeSpeakerOn();
                    isHandsfreeState=false;
                }else{
                    handsFreeImage.setImageResource(R.mipmap.em_icon_speaker_on);
                    openSpeakerOn();
                    isHandsfreeState=true;
                }
                break;
            case R.id.btn_record_video:
                if(!isRecording){
                    callHelper.startVideoRecord(PathUtil.getInstance().getVideoPath().getAbsolutePath());
                    isRecording=true;
                    recordBtn.setText(R.string.stop_record);
                }else{
                    String filepath=callHelper.stopVideoRecord();
                    isRecording=false;
                    recordBtn.setText(R.string.recording_video);
                    Toast.makeText(getApplicationContext(), String.format(getString(R.string.record_finish_toast), filepath), Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.root_layout:
                if (callingState == CallingState.NORMAL) {
                    if (bottomContainer.getVisibility() == View.VISIBLE) {
                        bottomContainer.setVisibility(View.GONE);
                        topContainer.setVisibility(View.GONE);

                    } else {
                        bottomContainer.setVisibility(View.VISIBLE);
                        topContainer.setVisibility(View.VISIBLE);

                    }
                }
                break;
            case R.id.btn_switch_camera: //switch camera
                handler.sendEmptyMessage(MSG_CALL_SWITCH_CAMERA);
            default:
                break;
        }
    }

    private void addCallSateListener(){
        callStateListener=new EMCallStateChangeListener() {
            @Override
            public void onCallStateChanged(CallState callState, CallError callError) {
                switch (callState) {

                    case CONNECTING: // is connecting
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                callStateTextView.setText(R.string.Are_connected_to_each_other);
                            }

                        });
                        break;
                    case CONNECTED: // connected
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                callStateTextView.setText(R.string.have_connected_with);
                            }

                        });
                        break;

                    case ACCEPTED: // call is accepted
                        handler.removeCallbacks(timeoutHangup);
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                try {
                                    if (soundPool != null)
                                        soundPool.stop(streamID);
                                } catch (Exception e) {
                                }
                                openSpeakerOn();
                                ((TextView)findViewById(R.id.tv_is_p2p)).setText(EMClient.getInstance().callManager().isDirectCall()
                                        ? R.string.direct_call : R.string.relay_call);
                                handsFreeImage.setImageResource(R.mipmap.em_icon_speaker_on);
                                isHandsfreeState = true;
                                isInCalling = true;
                                chronometer.setVisibility(View.VISIBLE);
                                chronometer.setBase(SystemClock.elapsedRealtime());
                                // call durations start
                                chronometer.start();
                                nickTextView.setVisibility(View.INVISIBLE);
                                callStateTextView.setText(R.string.In_the_call);
                                recordBtn.setVisibility(View.VISIBLE);
                                callingState = CallingState.NORMAL;
//                                startMonitor();
                            }

                        });
                        break;
                    case VIDEO_PAUSE:
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getApplicationContext(), "VIDEO_PAUSE", Toast.LENGTH_SHORT).show();
                            }
                        });
                        break;
                    case VIDEO_RESUME:
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getApplicationContext(), "VIDEO_RESUME", Toast.LENGTH_SHORT).show();
                            }
                        });
                        break;
                    case VOICE_PAUSE:
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getApplicationContext(), "VOICE_PAUSE", Toast.LENGTH_SHORT).show();
                            }
                        });
                        break;
                    case VOICE_RESUME:
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getApplicationContext(), "VOICE_RESUME", Toast.LENGTH_SHORT).show();
                            }
                        });
                        break;
                    case DISCONNNECTED: // call is disconnected
                        handler.removeCallbacks(timeoutHangup);
                        @SuppressWarnings("UnnecessaryLocalVariable")final CallError fError = callError;
                        runOnUiThread(new Runnable() {
                            private void postDelayedCloseMsg() {
                                uiHandler.postDelayed(new Runnable() {

                                    @Override
                                    public void run() {
//                                        saveCallRecord();
                                        Animation animation = new AlphaAnimation(1.0f, 0.0f);
                                        animation.setDuration(800);
                                        rootContainer.startAnimation(animation);
                                        finish();
                                    }

                                }, 200);
                            }

                            @Override
                            public void run() {
                                chronometer.stop();
                                callDruationText = chronometer.getText().toString();
                                String s1 = getResources().getString(R.string.The_other_party_refused_to_accept);
                                String s2 = getResources().getString(R.string.Connection_failure);
                                String s3 = getResources().getString(R.string.The_other_party_is_not_online);
                                String s4 = getResources().getString(R.string.The_other_is_on_the_phone_please);
                                String s5 = getResources().getString(R.string.The_other_party_did_not_answer);

                                String s6 = getResources().getString(R.string.hang_up);
                                String s7 = getResources().getString(R.string.The_other_is_hang_up);
                                String s8 = getResources().getString(R.string.did_not_answer);
                                String s9 = getResources().getString(R.string.Has_been_cancelled);

                                if (fError == CallError.REJECTED) {
                                    callingState = CallingState.BEREFUESD;
                                    callStateTextView.setText(s1);
                                } else if (fError == CallError.ERROR_TRANSPORT) {
                                    callStateTextView.setText(s2);
                                } else if (fError == CallError.ERROR_UNAVAILABLE) {
                                    callingState = CallingState.OFFLINE;
                                    callStateTextView.setText(s3);
                                } else if (fError == CallError.ERROR_BUSY) {
                                    callingState = CallingState.BUSY;
                                    callStateTextView.setText(s4);
                                } else if (fError == CallError.ERROR_NORESPONSE) {
                                    callingState = CallingState.NORESPONSE;
                                    callStateTextView.setText(s5);
                                }else if (fError == CallError.ERROR_LOCAL_SDK_VERSION_OUTDATED || fError == CallError.ERROR_REMOTE_SDK_VERSION_OUTDATED){
                                    callingState = CallingState.VERSION_NOT_SAME;
                                    callStateTextView.setText(R.string.call_version_inconsistent);
                                }  else {
                                    if (isAnswered) {
                                        callingState = CallingState.NORMAL;
                                        if (endCallTriggerByMe) {
//                                        callStateTextView.setText(s6);
                                        } else {
                                            callStateTextView.setText(s7);
                                        }
                                    } else {
                                        if (isInComingCall) {
                                            callingState = CallingState.UNANSWERED;
                                            callStateTextView.setText(s8);
                                        } else {
                                            if (callingState != CallingState.NORMAL) {
                                                callingState = CallingState.CANCED;
                                                callStateTextView.setText(s9);
                                            } else {
                                                callStateTextView.setText(s6);
                                            }
                                        }
                                    }
                                }
                                postDelayedCloseMsg();
                            }

                        });

                        break;

                    default:
                        break;
                }
            }
        };
        EMClient.getInstance().callManager().addCallStateChangeListener(callStateListener);
    }

    private void openSpeakerOn(){
        try {
            if (!audioManager.isSpeakerphoneOn()) {
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void closeSpeakerOn() {

        try {
            if (audioManager != null) {
                // int curVolume =
                // audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
                if (audioManager.isSpeakerphoneOn())
                    audioManager.setSpeakerphoneOn(false);
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                // audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                // curVolume, AudioManager.STREAM_VOICE_CALL);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int playMakeCallSounds() {
        try {
            audioManager.setMode(AudioManager.MODE_RINGTONE);
            audioManager.setSpeakerphoneOn(false);

            // play
            int id = soundPool.play(outgoing, // sound resource
                    0.3f, // left volume
                    0.3f, // right volume
                    1,    // priority
                    -1,   // loop，0 is no loop，-1 is loop forever
                    1);   // playback rate (1.0 = normal playback, range 0.5 to 2.0)
            return id;
        } catch (Exception e) {
            return -1;
        }
    }

    Runnable timeoutHangup = new Runnable() {

        @Override
        public void run() {
            handler.sendEmptyMessage(MSG_CALL_END);
        }
    };


    HandlerThread callHandlerThread = new HandlerThread("callHandlerThread");
    { callHandlerThread.start(); }
    private Handler handler = new Handler(callHandlerThread.getLooper()) {
        @Override
        public void handleMessage(Message msg) {
            EMLog.d("EMCallManager CallActivity", "handleMessage ---enter--- msg.what:" + msg.what);
            switch (msg.what) {
                case MSG_CALL_MAKE_VIDEO:
                case MSG_CALL_MAKE_VOICE:
                    try {
                        streamID = playMakeCallSounds();
                        if (msg.what == MSG_CALL_MAKE_VIDEO) {
                            EMClient.getInstance().callManager().makeVideoCall(username);
                        } else {
                            EMClient.getInstance().callManager().makeVoiceCall(username);
                        }

                        final int MAKE_CALL_TIMEOUT = 50 * 1000;
                        handler.removeCallbacks(timeoutHangup);
                        handler.postDelayed(timeoutHangup, MAKE_CALL_TIMEOUT);
                    } catch (EMServiceNotReadyException e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            public void run() {
                                final String st2 = getResources().getString(R.string.Is_not_yet_connected_to_the_server);
                                Toast.makeText(VideoCallActivity.this, st2, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    break;
                case MSG_CALL_ANSWER:
                    if (ringtone != null)
                        ringtone.stop();
                    if (isInComingCall) {
                        try {
                            if (NetUtils.hasDataConnection(VideoCallActivity.this)) {
                                EMClient.getInstance().callManager().answerCall();
                                isAnswered = true;
                            } else {
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        final String st2 = getResources().getString(R.string.Is_not_yet_connected_to_the_server);
                                        Toast.makeText(VideoCallActivity.this, st2, Toast.LENGTH_SHORT).show();
                                    }
                                });
                                throw new Exception();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
//                            saveCallRecord();
                            finish();
                            return;
                        }
                    }
                    break;
                case MSG_CALL_REJECT:
                    if (ringtone != null)
                        ringtone.stop();
                    try {
                        EMClient.getInstance().callManager().rejectCall();
                    } catch (Exception e1) {
                        e1.printStackTrace();
//                        saveCallRecord();
                        finish();
                    }
                    callingState = CallingState.REFUESD;
                    break;
                case MSG_CALL_END:
                    if (soundPool != null)
                        soundPool.stop(streamID);
                    try {
                        EMClient.getInstance().callManager().endCall();
                    } catch (Exception e) {
//                        saveCallRecord();
                        finish();
                    }

                    break;
                case MSG_CALL_RLEASE_HANDLER:
                    try {
                        EMClient.getInstance().callManager().endCall();
                    } catch (Exception e) {
                    }
                    handler.removeCallbacks(timeoutHangup);
                    handler.removeMessages(MSG_CALL_MAKE_VIDEO);
                    handler.removeMessages(MSG_CALL_MAKE_VOICE);
                    handler.removeMessages(MSG_CALL_ANSWER);
                    handler.removeMessages(MSG_CALL_REJECT);
                    handler.removeMessages(MSG_CALL_END);
                    callHandlerThread.quit();
                    break;
                case MSG_CALL_SWITCH_CAMERA:
                    EMClient.getInstance().callManager().switchCamera();
                    break;
                default:
                    break;
            }
            EMLog.d("EMCallManager CallActivity", "handleMessage ---exit--- msg.what:" + msg.what);
        }
    };

    @Override
    protected void onDestroy() {
        if(isRecording){
            callHelper.stopVideoRecord();
            isRecording = false;
        }
        localSurfaceView = null;
        oppositeSurfaceView = null;
        if (soundPool != null)
            soundPool.release();
        if (ringtone != null && ringtone.isPlaying())
            ringtone.stop();
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setMicrophoneMute(false);

        if(callStateListener != null)
            EMClient.getInstance().callManager().removeCallStateChangeListener(callStateListener);
        handler.sendEmptyMessage(MSG_CALL_RLEASE_HANDLER);
        super.onDestroy();
    }


}
