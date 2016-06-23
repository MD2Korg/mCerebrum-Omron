/*
 * File: WssActivity.java
 *
 * Abstract: Activity class. Display the WSS (Weight Scale Service) data.
 *
 * Copyright (c) 2015 OMRON HEALTHCARE Co., Ltd. All rights reserved.
 */

package org.md2k.omron;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.md2k.datakitapi.source.platform.PlatformType;

public class ActivityBase extends AppCompatActivity {
    private final static String TAG = ActivityBase.class.getSimpleName();
    String platformType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "[IN]onCreate");
        super.onCreate(savedInstanceState);
        if (platformType.equals(PlatformType.OMRON_BLOOD_PRESSURE))
            setContentView(R.layout.activity_blood_pressure);
        else
            setContentView(R.layout.activity_weight_scale);
        reset();
    }

    void reset() {
        setSaveButton(false);
        setCloseButton();
        setResetButton();
        setInstructionLayout(true);
    }

    void setInstructionLayout(boolean isEnable) {
        TextView textView = (TextView) findViewById(R.id.textView_weight);
        Button button = (Button) findViewById(R.id.button_done);
        if (isEnable) {
            textView.setTextColor(ContextCompat.getColor(this, R.color.red_900));
            button.setBackground(ContextCompat.getDrawable(this, R.drawable.button_red));
            button.setTextColor(ContextCompat.getColor(this, R.color.white));
            button.setTypeface(Typeface.DEFAULT_BOLD);
            button.setEnabled(true);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setInstructionLayout(false);
                }
            });
        } else {
            textView.setTextColor(ContextCompat.getColor(this, R.color.teal_700));
            button.setBackground(ContextCompat.getDrawable(this, R.drawable.button_teal));
            button.setTextColor(ContextCompat.getColor(this, R.color.grey_500));
            button.setTypeface(Typeface.DEFAULT_BOLD);
            button.setEnabled(false);
        }
    }

    void setSaveButton(boolean isEnabled) {
        Button button = (Button) findViewById(R.id.button_1);
        button.setText("Save");
        button.setEnabled(isEnabled);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ActivityBase.this, "Saved...", Toast.LENGTH_SHORT).show();
                findViewById(R.id.button_1).setEnabled(false);
            }
        });
    }

    void setCloseButton() {
        Button button = (Button) findViewById(R.id.button_3);
        button.setText("Close");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    void setResetButton() {
        Button button = (Button) findViewById(R.id.button_2);
        button.setText("Reset");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reset();
            }
        });
    }

}
