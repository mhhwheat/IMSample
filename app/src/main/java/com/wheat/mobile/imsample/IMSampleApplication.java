package com.wheat.mobile.imsample;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMOptions;
import com.wheat.mobile.imsample.activity.receiver.CallReceiver;

/**
 * Created by Administrator on 2016/9/9.
 */
public class IMSampleApplication extends Application {

    private CallReceiver callReceiver;

    @Override
    public void onCreate() {
        super.onCreate();

        EMOptions options=new EMOptions();
        EMClient.getInstance().init(getApplicationContext(), options);

//        setGlobalListener();
    }

    private void setGlobalListener(){
        IntentFilter callFilter=new IntentFilter(EMClient.getInstance().callManager().getIncomingCallBroadcastAction());
        if(callReceiver==null){
            callReceiver=new CallReceiver();
        }

        getApplicationContext().registerReceiver(callReceiver,callFilter);
    }

}
