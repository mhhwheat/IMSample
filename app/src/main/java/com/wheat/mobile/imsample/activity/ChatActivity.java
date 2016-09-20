package com.wheat.mobile.imsample.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;

/**
 * Created by Administrator on 2016/9/20.
 */
public class ChatActivity extends BaseActivity {

    private LayoutInflater layoutInflater;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        layoutInflater=LayoutInflater.from(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
