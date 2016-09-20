package com.wheat.mobile.imsample.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;

import com.hyphenate.chat.EMMessage;

/**
 * Created by Administrator on 2016/9/19.
 */
public abstract class MsgRow extends LinearLayout{

    protected LayoutInflater inflater;
    protected Context context;
    protected EMMessage message;
    protected int position;
    protected BaseAdapter adapter;

    public MsgRow(Context context, EMMessage  message, int position, BaseAdapter adapter) {
        super(context);
        this.context=context;
        this.message=message;
        this.position=position;
        this.adapter=adapter;
        inflater=LayoutInflater.from(context);
        initView();
    }

    private void initView(){
        onInflateView();

        onFindViewById();
    }

    public void setUpView(EMMessage message,int position){
        this.message=message;
        this.position=position;
    }

    protected abstract void onInflateView();

    protected abstract void onFindViewById();
}
