package com.lavruk.cityguide;

import com.lavruk.cityguide.views.MultiSwitch;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

public class TestActivity extends Activity {

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout fm = new FrameLayout(this);
        setContentView(fm);
        fm.setBackgroundColor(getResources().getColor(R.color.primary_color));

        MultiSwitch multiSwitch = new MultiSwitch(this);
        fm.addView(multiSwitch,
                new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    }
}
