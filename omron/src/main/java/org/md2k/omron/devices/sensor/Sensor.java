package org.md2k.omron.devices.sensor;

import android.content.Context;

import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.source.platform.Platform;

import java.util.HashMap;

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
public class Sensor {
    private String dataSourceType;
    private DataSourceClient dataSourceClient;
    private Context context;

    public Sensor(Context context, String dataSourceType) {
        this.context = context;
        this.dataSourceType = dataSourceType;
    }

    public boolean equals(String dataSourceType){
        return this.dataSourceType.equals(dataSourceType);
    }

    public String getDataSourceType() {
        return dataSourceType;
    }

    public DataSourceClient getDataSourceClient() {
        return dataSourceClient;
    }

    public DataSourceBuilder createDataSourceBuilder(Platform platform){
        return new DataSourceBuilder().setType(dataSourceType)
                .setPlatform(platform);
    }

    public boolean register(Platform platform) throws DataKitException {
        dataSourceClient = DataKitAPI.getInstance(context).register(createDataSourceBuilder(platform));
        return dataSourceClient != null;
    }

    public void unregister() throws DataKitException {
        if (dataSourceClient != null)
            DataKitAPI.getInstance(context).unregister(dataSourceClient);
    }
    HashMap<String, String> createDataDescriptor(String name, int minValue, int maxValue, String unit){
        HashMap<String, String> dataDescriptor = new HashMap<>();
        dataDescriptor.put(METADATA.NAME, name);
        dataDescriptor.put(METADATA.MIN_VALUE, String.valueOf(minValue));
        dataDescriptor.put(METADATA.MAX_VALUE, String.valueOf(maxValue));
        dataDescriptor.put(METADATA.DESCRIPTION, name);
        dataDescriptor.put(METADATA.UNIT,unit);
        dataDescriptor.put(METADATA.DATA_TYPE, int.class.getName());
        return dataDescriptor;
    }

}
