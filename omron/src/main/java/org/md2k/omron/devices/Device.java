package org.md2k.omron.devices;

import android.content.Context;

import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.platform.Platform;
import org.md2k.datakitapi.source.platform.PlatformBuilder;
import org.md2k.omron.bluetooth.MyBlueTooth;
import org.md2k.omron.devices.sensor.Battery;
import org.md2k.omron.devices.sensor.Sensor;

import java.util.ArrayList;

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
public class Device {
    protected String platformType;
    protected String deviceId;
    protected Context context;
    protected String name;
    protected ArrayList<Sensor> sensors;
//    MyBlueTooth myBlueTooth;

    public Device(Context context, String platformType, String deviceId, String name) {
        this.context = context;
        this.platformType = platformType;
        this.deviceId = deviceId;
        this.name=name;
        sensors =new ArrayList<>();
//        myBlueTooth=new MyBlueTooth(context, null);
    }
    public void register() throws DataKitException {
        for (int i = 0; i < sensors.size(); i++) {
            sensors.get(i).register(createPlatform());
        }
    }
    public void unregister() throws DataKitException {
        for (int i = 0; i < sensors.size(); i++) {
            sensors.get(i).unregister();
        }
    }
    public Platform createPlatform(){
        return new PlatformBuilder().setType(platformType).setMetadata(METADATA.DEVICE_ID, deviceId).setMetadata(METADATA.NAME, name).build();
    }

    public String getPlatformType() {
        return platformType;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getName() {
        return name;
    }

    public ArrayList<Sensor> getSensors() {
        return sensors;
    }
    public Sensor getSensors(String dataSourceType) {
        for(int i=0;i<sensors.size();i++)
            if(sensors.get(i).equals(dataSourceType))
        return sensors.get(i);
        return null;
    }
}
