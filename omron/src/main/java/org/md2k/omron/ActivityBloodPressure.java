package org.md2k.omron;
/**
 * Copyright (c) 2016, The University of Memphis, MD2K Center
 * - Syed Monowar Hossain <monowar.hossain@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
import org.md2k.datakitapi.messagehandler.ResultCallback;
import org.md2k.datakitapi.source.platform.PlatformType;
import org.md2k.omron.bluetooth.MyBlueTooth;
import org.md2k.omron.configuration.Configuration;
import org.md2k.omron.devices.DeviceBloodPressure;
import org.md2k.utilities.Report.Log;
import org.md2k.utilities.UI.AlertDialogs;
import org.md2k.utilities.permission.PermissionInfo;

public class ActivityBloodPressure extends AppIntro {
    double[] heartRate;
    double[] bloodPressure;
    double[] activity;
    double[] battery;
    DeviceBloodPressure deviceBloodPressure;
    MyBlueTooth myBlueTooth;
    String deviceId;
    String deviceName;
    private DataKitAPI dataKitAPI = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PermissionInfo permissionInfo = new PermissionInfo();
        permissionInfo.getPermissions(this, new ResultCallback<Boolean>() {
            @Override
            public void onResult(Boolean result) {
                if (!result) {
                    Toast.makeText(getApplicationContext(), "!PERMISSION DENIED !!! Could not continue...", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    load();
                }
            }
        });
    }
    void load(){

        if (Configuration.getDeviceAddress(PlatformType.OMRON_BLOOD_PRESSURE) == null) {
            Toast.makeText(this, "ERROR: Blood Pressure device is not configured...", Toast.LENGTH_SHORT).show();
            finish();
        } else {
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
            setBarColor(ContextCompat.getColor(ActivityBloodPressure.this, R.color.teal_500));
            setSeparatorColor(ContextCompat.getColor(ActivityBloodPressure.this, R.color.deeporange_500));
            setSwipeLock(true);
            setDoneText("");
            setNextArrowColor(ContextCompat.getColor(ActivityBloodPressure.this, R.color.teal_500));
            addSlide(Fragment_1_Info.newInstance(PlatformType.OMRON_BLOOD_PRESSURE, "Measure Blood Pressure", "Please put on the pressure cuff and measure your blood pressure", R.drawable.omron_bp));
            addSlide(Fragment_2_Connect_BP.newInstance(PlatformType.OMRON_BLOOD_PRESSURE, "Connecting Device...", "Trying to connect Blood Pressure device...", R.drawable.omron_bp));
            addSlide(Fragment_3_Read_BP.newInstance("Blood Pressure Reading"));
            addSlide(Fragment_4_Success.newInstance(PlatformType.OMRON_BLOOD_PRESSURE, "!!! Thank you !!!", "Blood pressure data is saved successfully", R.drawable.ic_ok_teal_50dp));
        }
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        AlertDialogs.AlertDialog(ActivityBloodPressure.this, "Skip Blood Pressure Reading", "Are you sure to skip blood pressure reading?", R.drawable.ic_info_teal_48dp, "Yes", "No", null, new DialogInterface.OnClickListener() {
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

    public void startSlide() {
        pager.setCurrentItem(0);
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
        if (oldFragment != null) {
            if (oldFragment instanceof Fragment_1_Info) {
                Fragment_1_Info fragment = (Fragment_1_Info) oldFragment;
                fragment.stop();
            }
            if (oldFragment instanceof Fragment_2_Connect_BP) {
                Fragment_2_Connect_BP fragment = (Fragment_2_Connect_BP) oldFragment;
                fragment.stop();
            }
            if (oldFragment instanceof Fragment_3_Read_BP) {
                Fragment_3_Read_BP fragment = (Fragment_3_Read_BP) oldFragment;
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
            if (newFragment instanceof Fragment_2_Connect_BP) {
                Fragment_2_Connect_BP fragment = (Fragment_2_Connect_BP) newFragment;
                fragment.start();
            }
            if (newFragment instanceof Fragment_3_Read_BP) {
                Fragment_3_Read_BP fragment = (Fragment_3_Read_BP) newFragment;
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
        if (dataKitAPI != null) {
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
