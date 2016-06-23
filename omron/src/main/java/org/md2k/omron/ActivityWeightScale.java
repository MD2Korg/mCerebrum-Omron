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
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.md2k.datakitapi.source.platform.PlatformType;

import java.awt.font.TextAttribute;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

public class ActivityWeightScale extends ActivityBase {
	private final static String TAG = ActivityWeightScale.class.getSimpleName();

	// Resolution table							  default  1	 2	   3	 4	   5	 6	   7
	private final static double RESOLUTION_KG[] = { 0.005, 0.5,	 0.2,  0.1,	 0.05, 0.02, 0.01, 0.005 };
	private final static double RESOLUTION_LB[] = { 0.01,  1.0,	 0.5,  0.2,	 0.1,  0.05, 0.02, 0.01	 };
	private int mResolutionIdx;

	private TextView mWeightScaleView;
	private TextView mTimestampView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "[IN]onCreate");
        platformType= PlatformType.OMRON_WEIGHT_SCALE;
        super.onCreate(savedInstanceState);
        mWeightScaleView = (TextView)findViewById(R.id.tvWeightValue);
        mTimestampView	 = (TextView)findViewById(R.id.tvTimestampValue);
        reset();
	}
}
