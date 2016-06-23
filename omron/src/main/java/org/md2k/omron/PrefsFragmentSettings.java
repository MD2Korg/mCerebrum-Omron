package org.md2k.omron;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.platform.PlatformType;
import org.md2k.omron.bluetooth.MyBlueTooth;
import org.md2k.omron.devices.Devices;
import org.md2k.utilities.UI.AlertDialogs;

import java.io.IOException;

/**
 * Copyright (c) 2015, The University of Memphis, MD2K Center
 * - Syed Monowar Hossain <monowar.hossain@gmail.com>
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p/>
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * <p/>
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p/>
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
public class PrefsFragmentSettings extends PreferenceFragment {
    private static final String TAG = PrefsFragmentSettings.class.getSimpleName();
    private static final int ADD_DEVICE = 1;
    MyBlueTooth myBlueTooth;
    Devices devices;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myBlueTooth = new MyBlueTooth(getActivity());
        devices = new Devices(getActivity());
        if (!myBlueTooth.hasSupport()) {
            Toast.makeText(getActivity(), "Bluetooth LE is not supported", Toast.LENGTH_SHORT).show();
            getActivity().finish();
        } else {
            addPreferencesFromResource(R.xml.pref_settings_general);
            setPreferenceBluetoothPair();
            setPreferenceScreenBloodPressureAdd();
            setPreferenceScreenWeightScaleAdd();
            setPreferenceScreenConfigured();
            setSaveButton();
            setCancelButton();
        }
    }

    void setPreferenceScreenConfigured() {
        for (int i = 0; i < devices.size(); i++) {
            String platformType = devices.get(i).getPlatformType();
            String name = devices.get(i).getName();
            String deviceId = devices.get(i).getDeviceId();
            addToConfiguredList(platformType, deviceId, name);
        }
    }

    @Override
    public void onResume() {
        if (!myBlueTooth.isEnabled())
            myBlueTooth.enable();
        super.onResume();
    }

    private void setPreferenceBluetoothPair() {
        Preference preference = findPreference("key_bluetooth_pair");
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_MAIN, null);
                intent.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(intent);
                return true;
            }
        });
    }

    private void setPreferenceScreenBloodPressureAdd() {
        Preference preference = findPreference("key_blood_pressure_add");
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final Intent intent = new Intent(getActivity(), ActivitySettingsPlatform.class);
                intent.putExtra(PlatformType.class.getSimpleName(), PlatformType.OMRON_BLOOD_PRESSURE);
                startActivityForResult(intent, ADD_DEVICE);
                return false;
            }
        });
    }

    private void setPreferenceScreenWeightScaleAdd() {
        Preference preference = findPreference("key_weight_scale_add");
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final Intent intent = new Intent(getActivity(), ActivitySettingsPlatform.class);
                intent.putExtra(PlatformType.class.getSimpleName(), PlatformType.OMRON_WEIGHT_SCALE);
                startActivityForResult(intent, ADD_DEVICE);
                return false;
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ADD_DEVICE) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "onActivityResult(): result ok");
                String platformType = data.getStringExtra(PlatformType.class.getSimpleName());
                String deviceId = data.getStringExtra(METADATA.DEVICE_ID);
                String name = data.getStringExtra(METADATA.NAME);
                if (devices.find(deviceId) != null)
                    Toast.makeText(getActivity(), "Error: Device is already configured...", Toast.LENGTH_SHORT).show();
                else {
                    devices.add(platformType, deviceId, name);
                    addToConfiguredList(platformType, deviceId, name);
                }
            }
        }
    }

    private void addToConfiguredList(String platformType, String deviceId, String name) {
        PreferenceCategory category = (PreferenceCategory) findPreference("key_device_configured");
        Preference preference = new Preference(getActivity());
        preference.setKey(deviceId);
        if (name == null || name.length() == 0)
            preference.setTitle(deviceId);
        else
            preference.setTitle(name + " (" + deviceId + ")");
        if (platformType.equals(PlatformType.OMRON_BLOOD_PRESSURE))
            preference.setIcon(R.drawable.ic_blood_pressure_teal_48dp);
        else if (platformType.equals(PlatformType.OMRON_WEIGHT_SCALE)) {
            preference.setIcon(R.drawable.ic_weight_scale_48dp);
        }
        preference.setOnPreferenceClickListener(preferenceListenerConfigured());
        category.addPreference(preference);
    }

    private Preference.OnPreferenceClickListener preferenceListenerConfigured() {
        return new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final String deviceId = preference.getKey();
                AlertDialogs.AlertDialog(getActivity(), "Delete Device", "Delete Device (" + preference.getTitle() + ")?", R.drawable.ic_delete_red_48dp, "Delete", "Cancel", null, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            PreferenceCategory category = (PreferenceCategory) findPreference("key_device_configured");
                            for (int i = 0; i < category.getPreferenceCount(); i++) {
                                Preference preference = category.getPreference(i);
                                if (preference.getKey().equals(deviceId)) {
                                    category.removePreference(preference);
                                    devices.delete(deviceId);
                                    return;
                                }
                            }
                        } else {
                            dialog.dismiss();
                        }
                    }
                });
                return true;
            }
        };
    }

    private void setCancelButton() {
        final Button button = (Button) getActivity().findViewById(R.id.button_2);
        button.setText("Close");
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getActivity().finish();
            }
        });
    }

    private void setSaveButton() {
        final Button button = (Button) getActivity().findViewById(R.id.button_1);
        button.setText("Save");
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    devices.writeDataSourceToFile();
                    Toast.makeText(getActivity(), "Saved...", Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                } catch (Exception e) {
                    Toast.makeText(getActivity(), "Error: Could not Save...", Toast.LENGTH_SHORT).show();
                }
            }
        });
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
}
