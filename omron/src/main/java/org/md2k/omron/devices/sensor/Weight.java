package org.md2k.omron.devices.sensor;

import android.content.Context;

import org.md2k.datakitapi.datatype.DataTypeInt;
import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.datakitapi.source.platform.Platform;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by monowar on 4/21/16.
 */
public class Weight extends Sensor {
    public Weight(Context context) {
        super(context, DataSourceType.WEIGHT);
    }

    @Override
    public DataSourceBuilder createDataSourceBuilder(Platform platform){
        DataSourceBuilder dataSourceBuilder=super.createDataSourceBuilder(platform);
        dataSourceBuilder=dataSourceBuilder.setMetadata(METADATA.NAME, "Weight")
                .setDataDescriptors(createDataDescriptors())
                .setMetadata(METADATA.MIN_VALUE, "10")
                .setMetadata(METADATA.MAX_VALUE, "250")
                .setMetadata(METADATA.DATA_TYPE, DataTypeInt.class.getSimpleName())
                .setMetadata(METADATA.DESCRIPTION, "Weight in kg");
        return dataSourceBuilder;
    }
    ArrayList<HashMap<String, String>> createDataDescriptors() {
        ArrayList<HashMap<String, String>> dataDescriptors = new ArrayList<>();
        HashMap<String, String> dataDescriptor = new HashMap<>();
        dataDescriptor.put(METADATA.NAME, "Weight");
        dataDescriptor.put(METADATA.MIN_VALUE, "10");
        dataDescriptor.put(METADATA.MAX_VALUE, "250");
        dataDescriptor.put(METADATA.DESCRIPTION, "Weight in kg");
        dataDescriptor.put(METADATA.DATA_TYPE, int.class.getName());
        dataDescriptors.add(dataDescriptor);
        return dataDescriptors;
    }
}
