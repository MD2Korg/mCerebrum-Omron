/*
 * File: WssActivity.java
 *
 * Abstract: Activity class. Display the WSS (Weight Scale Service) data.
 *
 * Copyright (c) 2015 OMRON HEALTHCARE Co., Ltd. All rights reserved.
 */

package org.md2k.omron;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.paolorotolo.appintro.AppIntro;

import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.messagehandler.OnConnectionListener;
import org.md2k.datakitapi.source.platform.PlatformType;
import org.md2k.omron.configuration.Configuration;

import java.awt.font.TextAttribute;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

public class ActivityWeightScale extends AppIntro {
	private DataKitAPI dataKitAPI = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        if(Configuration.getDeviceAddress(PlatformType.OMRON_WEIGHT_SCALE)==null){
            Toast.makeText(this, "ERROR: Weight Scale device is not configured...", Toast.LENGTH_SHORT).show();
            finish();
        }else {

            dataKitAPI = DataKitAPI.getInstance(getApplicationContext());
            try {
                dataKitAPI.connect(new OnConnectionListener() {
                    @Override
                    public void onConnected() {
                    }
                });
            } catch (DataKitException e) {
                Toast.makeText(this, "Datakit Connection Error", Toast.LENGTH_SHORT).show();
                finish();
            }
            addSlide(Fragment_1_Measure.newInstance(PlatformType.OMRON_WEIGHT_SCALE, "Measure Weight", "Please put on the pressure cuff and initiate a measurement", R.drawable.omron_weight_scale));
            addSlide(Fragment_2_Read_WS.newInstance("Weight Scale Reading"));
            setBarColor(ContextCompat.getColor(ActivityWeightScale.this, R.color.teal_500));
            setSeparatorColor(ContextCompat.getColor(ActivityWeightScale.this, R.color.deeporange_500));
            showDoneButton(false);
            setSwipeLock(true);
            setNextPageSwipeLock(true);
            setProgressButtonEnabled(false);
        }
	}

	@Override
	public void onSkipPressed(Fragment currentFragment) {
		super.onSkipPressed(currentFragment);
		// Do something when users tap on Skip button.
	}
    public void nextSlide() {
        pager.setCurrentItem(pager.getCurrentItem() + 1);
    }

    public void prevSlide() {
        pager.setCurrentItem(pager.getCurrentItem() - 1);
    }

	@Override
	public void onDonePressed(Fragment currentFragment) {
		super.onDonePressed(currentFragment);
		// Do something when users tap on Done button.
	}

	@Override
	public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
		super.onSlideChanged(oldFragment, newFragment);
		// Do something when the slide changes.
	}
    @Override
    public void onDestroy() {
        if(dataKitAPI!=null) {
            dataKitAPI.disconnect();
        }
        super.onDestroy();
    }
}
