package org.md2k.omron;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.github.paolorotolo.appintro.ISlidePolicy;

import org.md2k.datakitapi.datatype.DataTypeDoubleArray;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.datakitapi.time.DateTime;
import org.md2k.utilities.UI.AlertDialogs;

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
public class Fragment_3_Read_BP extends Fragment implements ISlidePolicy {
    private static final String TAG = Fragment_3_Read_BP.class.getSimpleName();
    TextView textViewSystolic;
    TextView textViewDiastolic;
    TextView textViewPulseRate;
    TextView textViewPulse;
    TextView textViewIrregularPulse;
    TextView textViewBattery;
    TextView textViewMovement;
    Button button_1;
    Button button_2;
    ActivityBloodPressure activity;

    public static Fragment_3_Read_BP newInstance(String title) {
        Fragment_3_Read_BP f = new Fragment_3_Read_BP();
        Bundle b = new Bundle();
        b.putString("title", title);
        f.setArguments(b);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_3_read_bp, container, false);
        activity = (ActivityBloodPressure) getActivity();
        ((TextView) v.findViewById(R.id.text_view_title)).setText(getArguments().getString("title"));

        textViewSystolic = (TextView) v.findViewById(R.id.text_view_systolic);
        textViewDiastolic = (TextView) v.findViewById(R.id.text_view_diastolic);
        textViewPulseRate = (TextView) v.findViewById(R.id.text_view_pulse_rate);
        textViewPulse = (TextView) v.findViewById(R.id.text_view_pulse);
        textViewIrregularPulse = (TextView) v.findViewById(R.id.text_view_irregular_pulse);
        textViewBattery = (TextView) v.findViewById(R.id.text_view_battery);
        textViewMovement = (TextView) v.findViewById(R.id.text_view_movement);
        button_1 = (Button) v.findViewById(R.id.button_1);
        button_2 = (Button) v.findViewById(R.id.button_2);
        setupButton();
        updateView();
        return v;
    }

    public void start() {
        updateView();
    }

    public void stop() {

    }

    @Override
    public boolean isPolicyRespected() {
        return false;
    }

    @Override
    public void onUserIllegallyRequestedNextPage() {
//        Toast.makeText(getContext(), R.string.slide_policy_demo_error, Toast.LENGTH_SHORT).show();
    }

    private void updateView() {
        if (activity.bloodPressure == null) {
            textViewSystolic.setText("---");
            textViewDiastolic.setText("---");
            textViewPulse.setText("---");
        } else {
            if(activity.bloodPressure[0]!=-1)
            textViewSystolic.setText(String.valueOf(activity.bloodPressure[0]));
            if(activity.bloodPressure[1]!=-1)
            textViewDiastolic.setText(String.valueOf(activity.bloodPressure[1]));
            if(activity.bloodPressure[2]!=-1)
            textViewPulse.setText(String.valueOf(activity.bloodPressure[2]));
        }
        if (activity.heartRate == null) {
            textViewPulseRate.setText("---");
            textViewIrregularPulse.setText("---");
        } else {
            if(activity.heartRate[0]!=-1)
            textViewPulseRate.setText(String.valueOf(activity.heartRate[0]));
            if(activity.heartRate[1]!=-1)
            textViewIrregularPulse.setText(String.valueOf(activity.heartRate[1]));
        }
        if (activity.battery == null) {
            textViewBattery.setText("---");
        } else {
            textViewBattery.setText(String.valueOf(activity.battery[0]));
        }
        if (activity.activity == null)
            textViewMovement.setText("---");
        else textViewMovement.setText(String.valueOf(activity.activity[0]));
        setupButton();
    }

    void setupButton() {
        button_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.startSlide();
            }
        });
        button_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialogs.AlertDialog(getActivity(), "Correct Reading?", "Please click \"Save\" if the reading is correct", R.drawable.ic_info_teal_48dp, "Save", "Cancel", null, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            ActivityBloodPressure activity = (ActivityBloodPressure) getActivity();
                            saveData();
                            activity.nextSlide();
                        }
                    }
                });
            }
        });
    }

    void saveData() {
        try {
            activity.deviceBloodPressure.getSensors(DataSourceType.BLOOD_PRESSURE).insert(new DataTypeDoubleArray(DateTime.getDateTime(), activity.bloodPressure));
            activity.deviceBloodPressure.getSensors(DataSourceType.HEART_RATE).insert(new DataTypeDoubleArray(DateTime.getDateTime(), activity.heartRate));
            activity.deviceBloodPressure.getSensors(DataSourceType.ACTIVITY).insert(new DataTypeDoubleArray(DateTime.getDateTime(), activity.activity));
            activity.deviceBloodPressure.getSensors(DataSourceType.BATTERY).insert(new DataTypeDoubleArray(DateTime.getDateTime(), activity.battery));
        } catch (DataKitException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
