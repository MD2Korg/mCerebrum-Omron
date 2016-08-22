package org.md2k.omron.devices;

import android.content.Context;
import android.widget.Toast;

import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.platform.Platform;
import org.md2k.datakitapi.source.platform.PlatformType;
import org.md2k.omron.configuration.Configuration;

import java.io.FileNotFoundException;
import java.util.ArrayList;

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
public class Devices {
    private Context context;
    private ArrayList<Device> devices;

    public Devices(Context context) {
        this.context = context;
        devices = new ArrayList<>();
        readDataSourceFromFile();
    }

    public int size() {
        return devices.size();
    }

    public Device get(int i) {
        return devices.get(i);
    }

    private void readDataSourceFromFile() {
        try {
            ArrayList<DataSource> dataSources = Configuration.getDataSources();
            if (dataSources == null) throw new FileNotFoundException();
            for (int i = 0; i < dataSources.size(); i++) {
                String deviceId = dataSources.get(i).getPlatform().getMetadata().get(METADATA.DEVICE_ID);
                String name = dataSources.get(i).getPlatform().getMetadata().get(METADATA.NAME);
                Device device = find(deviceId);
                if (device == null) {
                    if (dataSources.get(i).getPlatform().getType().equals(PlatformType.OMRON_BLOOD_PRESSURE))
                        device = new DeviceBloodPressure(context, deviceId, name);
                    else
                        device = new DeviceWeightScale(context, deviceId, name);
                    this.devices.add(device);
                }
            }
        } catch (FileNotFoundException e) {
            Toast.makeText(context, "Omron device is not configured", Toast.LENGTH_LONG).show();
        }
    }

    public Device find(String deviceId) {
        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).getDeviceId().equals(deviceId))
                return devices.get(i);
        }
        return null;
    }

    public ArrayList<Device> findType(String type) {
        ArrayList<Device> tempOmronPlatforms = new ArrayList<>();
        for (int i = 0; i < devices.size(); i++)
            if (devices.get(i).getPlatformType().equals(type))
                tempOmronPlatforms.add(devices.get(i));
        return tempOmronPlatforms;
    }

    public void delete(String deviceId) {
        Device device = find(deviceId);
        if (device == null) return;
        devices.remove(device);
    }

    public void add(String platformType, String deviceId, String name) {
        Device device = null;
        if(find(deviceId)!=null) return;
        if (platformType.equals(PlatformType.OMRON_BLOOD_PRESSURE)) {
            device = new DeviceBloodPressure(context, deviceId, name);
        } else if (platformType.equals(PlatformType.OMRON_WEIGHT_SCALE)) {
            device = new DeviceWeightScale(context, deviceId, name);
        }
        devices.add(device);
    }

    public ArrayList<Device> find() {
        return devices;
    }

    public void writeDataSourceToFile() throws Exception {
        ArrayList<DataSource> dataSources = new ArrayList<>();
        if (devices == null) throw new NullPointerException();
        for (int i = 0; i < devices.size(); i++) {
            Platform platform = devices.get(i).createPlatform();
            for (int j = 0; j < devices.get(i).getSensors().size(); j++) {
                DataSourceBuilder dataSourceBuilder = devices.get(i).getSensors().get(j).createDataSourceBuilder(platform);
                if (dataSourceBuilder == null) continue;
                dataSourceBuilder = dataSourceBuilder.setPlatform(platform);
                DataSource dataSource = dataSourceBuilder.build();
                dataSources.add(dataSource);
            }
        }
        if (dataSources.size() == 0)
            Toast.makeText(context, "Error: No device is configured...", Toast.LENGTH_SHORT).show();
        Configuration.write(dataSources);
    }

    public void register() throws DataKitException {
        for (int i = 0; i < devices.size(); i++) {
            devices.get(i).register();
        }
    }

    public void unregister() throws DataKitException {
        for (int i = 0; i < devices.size(); i++) {
            devices.get(i).unregister();
        }
    }
}
