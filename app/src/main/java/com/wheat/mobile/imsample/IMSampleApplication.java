package com.wheat.mobile.imsample;

import android.app.Application;

import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMOptions;

/**
 * Created by Administrator on 2016/9/9.
 */
public class IMSampleApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        EMOptions options=new EMOptions();
        EMClient.getInstance().init(getApplicationContext(), options);
    }
}
