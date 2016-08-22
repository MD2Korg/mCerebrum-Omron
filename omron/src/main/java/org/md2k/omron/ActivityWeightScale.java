/*
 * File: WssActivity.java
 *
 * Abstract: Activity class. Display the WSS (Weight Scale Service) data.
 *
 * Copyright (c) 2015 OMRON HEALTHCARE Co., Ltd. All rights reserved.
 */

package org.md2k.omron;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.github.paolorotolo.appintro.AppIntro;

import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.messagehandler.OnConnectionListener;
import org.md2k.datakitapi.source.platform.PlatformType;
import org.md2k.omron.bluetooth.MyBlueTooth;
import org.md2k.omron.configuration.Configuration;
import org.md2k.omron.devices.DeviceWeightScale;
import org.md2k.utilities.Report.Log;
import org.md2k.utilities.UI.AlertDialogs;

public class ActivityWeightScale extends AppIntro {
    // Resolution table							  default  1	 2	   3	 4	   5	 6	   7
    public final static double[] RESOLUTION_KG = {0.005, 0.5, 0.2, 0.1, 0.05, 0.02, 0.01, 0.005};
    public final static double[] RESOLUTION_LB = {0.01, 1.0, 0.5, 0.2, 0.1, 0.05, 0.02, 0.01};
    public int mResolutionIdx;
    boolean isRead = false;
    DeviceWeightScale deviceWeightScale;
    MyBlueTooth myBlueTooth;
    String deviceId;
    String deviceName;
    double[] weight;
    double[] battery;
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
            setBarColor(ContextCompat.getColor(ActivityWeightScale.this, R.color.teal_500));
            setSeparatorColor(ContextCompat.getColor(ActivityWeightScale.this, R.color.deeporange_500));
            setBarColor(ContextCompat.getColor(ActivityWeightScale.this, R.color.teal_500));
            setSeparatorColor(ContextCompat.getColor(ActivityWeightScale.this, R.color.deeporange_500));
            setSwipeLock(true);
            setDoneText("");
            setNextArrowColor(ContextCompat.getColor(ActivityWeightScale.this, R.color.teal_500));
            addSlide(Fragment_1_Info.newInstance(PlatformType.OMRON_WEIGHT_SCALE, "Measure Weight", "Please stand on the weight scale and measure your weight", R.drawable.omron_weight_scale));
            addSlide(Fragment_2_Connect_WS.newInstance(PlatformType.OMRON_WEIGHT_SCALE, "Connecting Device...", "Trying to connect weight scale device...", R.drawable.omron_weight_scale));
            addSlide(Fragment_3_Read_WS.newInstance("Weight Scale Reading"));
            addSlide(Fragment_4_Success.newInstance(PlatformType.OMRON_WEIGHT_SCALE, "!!! Thank you !!!", "Weight data is saved successfully", R.drawable.ic_ok_teal_50dp));
        }
    }

	@Override
	public void onSkipPressed(Fragment currentFragment) {
		super.onSkipPressed(currentFragment);
        AlertDialogs.AlertDialog(ActivityWeightScale.this, "Skip Weight Scale Reading", "Are you sure to skip Weight Scale reading?", R.drawable.ic_info_teal_48dp, "Yes", "No", null, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    finish();
                }
            }
        });
    }
    public void nextSlide() {
        pager.setCurrentItem(pager.getCurrentItem() + 1);
    }

    public void prevSlide() {
        pager.setCurrentItem(pager.getCurrentItem() - 1);
    }

    public void startSlide() {
        pager.setCurrentItem(0);
    }

	@Override
	public void onDonePressed(Fragment currentFragment) {
		super.onDonePressed(currentFragment);
		// Do something when users tap on Done button.
	}

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
        if (oldFragment != null) {
            if (oldFragment instanceof Fragment_1_Info) {
                Fragment_1_Info fragment = (Fragment_1_Info) oldFragment;
                fragment.stop();
            }
            if (oldFragment instanceof Fragment_2_Connect_WS) {
                Fragment_2_Connect_WS fragment = (Fragment_2_Connect_WS) oldFragment;
                fragment.stop();
            }
            if (oldFragment instanceof Fragment_3_Read_WS) {
                Fragment_3_Read_WS fragment = (Fragment_3_Read_WS) oldFragment;
                fragment.stop();
            }
            if (oldFragment instanceof Fragment_4_Success) {
                Fragment_4_Success fragment = (Fragment_4_Success) oldFragment;
                fragment.stop();
            }
        }
        if (newFragment != null) {
            if (newFragment instanceof Fragment_1_Info) {
                Fragment_1_Info fragment = (Fragment_1_Info) newFragment;
                fragment.start();
            }
            if (newFragment instanceof Fragment_2_Connect_WS) {
                Fragment_2_Connect_WS fragment = (Fragment_2_Connect_WS) newFragment;
                fragment.start();
            }
            if (newFragment instanceof Fragment_3_Read_WS) {
                Fragment_3_Read_WS fragment = (Fragment_3_Read_WS) newFragment;
                fragment.start();
            }
            if (newFragment instanceof Fragment_4_Success) {
                Fragment_4_Success fragment = (Fragment_4_Success) newFragment;
                fragment.start();
            }
        }
        Log.d("abc", "old=" + oldFragment + " new=" + newFragment);
    }

    @Override
    public void onDestroy() {
        if(dataKitAPI!=null) {
            dataKitAPI.disconnect();
        }
        if (myBlueTooth != null) {
            myBlueTooth.disconnect();
            myBlueTooth.scanOff();
            myBlueTooth.close();
            myBlueTooth = null;
        }
        super.onDestroy();
    }
}
