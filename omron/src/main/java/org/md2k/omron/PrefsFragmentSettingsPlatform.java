package org.md2k.omron;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.platform.PlatformType;
import org.md2k.omron.bluetooth.MyBlueTooth;

import java.util.ArrayList;
import java.util.UUID;

/*
 * Copyright (c) 2015, The University of Memphis, MD2K Center
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

public class PrefsFragmentSettingsPlatform extends PreferenceFragment {
    public static final String TAG = PrefsFragmentSettingsPlatform.class.getSimpleName();
    private static final long SCAN_PERIOD = 10000;
    String deviceId = "", platformType;
    private ArrayAdapter<String> adapterDevices;
    private ArrayList<String> devices = new ArrayList<>();
    private MyBlueTooth myBlueTooth;
    Handler handler;
    boolean isScanning;
    UUID uuid;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        platformType = getActivity().getIntent().getStringExtra(PlatformType.class.getSimpleName());
        if(platformType.equals(PlatformType.OMRON_BLOOD_PRESSURE)){
            uuid=Constants.BLOOD_PRESSURE_SERVICE_UUID;
        }else if(platformType.equals(PlatformType.OMRON_WEIGHT_SCALE)){
            uuid=Constants.WEIGHT_SCALE_SERVICE_UUID;
        }
        myBlueTooth = new MyBlueTooth(getActivity());
        handler = new Handler();
        addPreferencesFromResource(R.xml.pref_settings_platform);
        setupListViewDevices();
        setupPreferenceDeviceId();
        setAddButton();
        setCancelButton();
        setScanButton();
        scanLeDevice();
    }

    private void scanLeDevice() {
        // Stops scanning after a pre-defined scan period.
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                isScanning = false;
                myBlueTooth.scanStop(mLeScanCallback);
                setScanButton();
            }
        }, SCAN_PERIOD);

        isScanning = true;
        myBlueTooth.scanStart(mLeScanCallback);
        setScanButton();
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("abc", "devicename=" + device.getName() + " deviceaddress=" + device.getAddress() + " " + " scanrecord=" + scanRecord.length + " device=" + device.getUuids());
                            String name;
                            if (!myBlueTooth.hasUUID(scanRecord, uuid))
                                return;
                            if (device.getName() == null || device.getName().length() == 0)
                                name = device.getAddress();
                            else
                                name = device.getName() + " (" + device.getAddress() + ")";
                            for (int i = 0; i < devices.size(); i++)
                                if (devices.get(i).equals(name))
                                    return;
                            devices.add(name);
                            adapterDevices.notifyDataSetChanged();
                        }
                    });
                }
            };

    void setupListViewDevices() {
        adapterDevices = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_single_choice,
                android.R.id.text1, devices);
        ListView listViewDevices = (ListView) getActivity().findViewById(R.id.listView_devices);
        listViewDevices.setAdapter(adapterDevices);
        listViewDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = ((TextView) view).getText().toString().trim();
                Preference preference = findPreference("deviceId");
                deviceId = item;
                preference.setSummary(item);
            }
        });
    }


    private void setupPreferenceDeviceId() {
        Preference preference = findPreference("deviceId");
        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Log.d(TAG, preference.getKey() + " " + newValue.toString());
                deviceId = newValue.toString().trim();
                preference.setSummary(newValue.toString().trim());
                return false;
            }
        });

    }

    private void setAddButton() {
        final Button button = (Button) getActivity().findViewById(R.id.button_1);
        button.setText("Save");

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (deviceId == null || deviceId.equals(""))
                    Toast.makeText(getActivity(), "!!! Device ID is missing !!!", Toast.LENGTH_LONG).show();
                else {
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra(PlatformType.class.getSimpleName(), platformType);
                    returnIntent.putExtra(METADATA.DEVICE_ID, getDeviceId(deviceId));
                    returnIntent.putExtra(METADATA.NAME, getName((deviceId)));
                    getActivity().setResult(getActivity().RESULT_OK, returnIntent);
                    getActivity().finish();
                }
            }
        });
    }
    private String getName(String str){
        if(str.endsWith(")")){
            String[] arr = str.split(" ");
            return arr[0];
        }else
            return null;
    }
    private String getDeviceId(String str){
        if(str.endsWith(")")){
            String[] arr = deviceId.split(" ");
            return arr[1].substring(1,arr[1].length()-1);
        }else
            return str;
    }

    private void setCancelButton() {
        final Button button = (Button) getActivity().findViewById(R.id.button_3);
        button.setText("Close");

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent returnIntent = new Intent();
                getActivity().setResult(getActivity().RESULT_CANCELED, returnIntent);
                getActivity().finish();
            }
        });
    }

    private void setScanButton() {
        try {
            final Button button = (Button) getActivity().findViewById(R.id.button_2);
            if (isScanning) {
                button.setText("Scanning...");
                button.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        isScanning = false;
                        myBlueTooth.scanStop(mLeScanCallback);
                    }
                });
            } else {
                button.setText("Scan");
                button.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        scanLeDevice();
                    }
                });
            }
        } catch (Exception ignored) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        assert v != null;
        ListView lv = (ListView) v.findViewById(android.R.id.list);
        lv.setPadding(0, 0, 0, 0);
        return v;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                getActivity().finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        if (isScanning) {
            isScanning = false;
            myBlueTooth.scanStop(mLeScanCallback);
        }
        super.onDestroy();
    }

}
