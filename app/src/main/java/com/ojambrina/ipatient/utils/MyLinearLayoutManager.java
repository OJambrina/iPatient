package com.ojambrina.ipatient.utils;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;

public class MyLinearLayoutManager extends LinearLayoutManager {
    private boolean isScrollEnabled = true;

    public MyLinearLayoutManager(Context context) {
        super(context);
    }

    public void setScrollEnabled(boolean flag) {
        this.isScrollEnabled = flag;
    }

    @Override
    public boolean canScrollVertically() {
        //Similarly you can customize "canScrollHorizontally()" for managing horizontal scroll
        return isScrollEnabled && super.canScrollVertically();
    }
}