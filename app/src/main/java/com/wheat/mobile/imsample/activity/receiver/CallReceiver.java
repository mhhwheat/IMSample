package com.wheat.mobile.imsample.activity.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.wheat.mobile.imsample.activity.VideoCallActivity;

/**
 * Created by Administrator on 2016/9/11.
 */
public class CallReceiver extends BroadcastReceiver{
    private static final String TAG= CallReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        //username
        String from = intent.getStringExtra("from");
        //call type
        String type = intent.getStringExtra("type");

        Log.i(TAG,"receive from"+from);
        if("video".equals(type)){ //video call
            context.startActivity(new Intent(context, VideoCallActivity.class).
                    putExtra("username", from).putExtra("isComingCall", true).
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

}
