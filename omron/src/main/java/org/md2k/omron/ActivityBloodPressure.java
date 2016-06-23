/*
 * File: BlsActivity.java
 *
 * Abstract: Activity class. Display the BLS (Blood Pressure Service) data.
 *
 * Copyright (c) 2015 OMRON HEALTHCARE Co., Ltd. All rights reserved.
 */

package org.md2k.omron;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.md2k.datakitapi.source.platform.PlatformType;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

public class ActivityBloodPressure extends ActivityBase {
	private TextView mSystolicView;
	private TextView mDiastolicView;
	private TextView mMeanApView;
	private TextView mTimestampView;
	private TextView mPulseRateView;
	private TextView mUserIDView;
	private TextView mBodyMovementView;
	private TextView mIrregularPulseView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        platformType= PlatformType.OMRON_BLOOD_PRESSURE;
        super.onCreate(savedInstanceState);

		mSystolicView		= (TextView)findViewById(R.id.tvSystolicValue);
		mDiastolicView		= (TextView)findViewById(R.id.tvDiastolicValue);
		mMeanApView			= (TextView)findViewById(R.id.tvMeanAPValue);
		mTimestampView		= (TextView)findViewById(R.id.tvTimestampValue);
		mPulseRateView		= (TextView)findViewById(R.id.tvPulseRateValue);
		mUserIDView			= (TextView)findViewById(R.id.tvUserIDValue);
		mBodyMovementView	= (TextView)findViewById(R.id.tvBodyMovementValue);
		mIrregularPulseView = (TextView)findViewById(R.id.tvIrregularPulseValue);
	}
}
