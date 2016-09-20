package com.wheat.mobile.imsample.widget;

import android.content.Context;
import android.widget.BaseAdapter;

import com.hyphenate.chat.EMMessage;

/**
 * Created by Administrator on 2016/9/19.
 */
public class MsgRowText extends MsgRow {

    public MsgRowText(Context context, EMMessage message, int position, BaseAdapter adapter){
        super(context,message,position,adapter);
    }

    @Override
    protected void onInflateView() {

    }

    @Override
    protected void onFindViewById() {

    }
}
