package org.md2k.omron.devices.sensor;

import android.content.Context;

import org.md2k.datakitapi.datatype.DataTypeIntArray;
import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.datakitapi.source.platform.Platform;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by monowar on 4/21/16.
 */
public class HeartRate extends Sensor {
    public HeartRate(Context context) {
        super(context, DataSourceType.HEART_RATE);
    }

    @Override
    public DataSourceBuilder createDataSourceBuilder(Platform platform){
        DataSourceBuilder dataSourceBuilder=super.createDataSourceBuilder(platform);
        dataSourceBuilder=dataSourceBuilder.setMetadata(METADATA.NAME, "Heart Rate")
                .setDataDescriptors(createDataDescriptors())
                .setMetadata(METADATA.MIN_VALUE, "0")
                .setMetadata(METADATA.MAX_VALUE, "255")
                .setMetadata(METADATA.DATA_TYPE, DataTypeIntArray.class.getSimpleName())
                .setMetadata(METADATA.DESCRIPTION, "Pulse Rate/Heart Rate");
        return dataSourceBuilder;
    }
    ArrayList<HashMap<String, String>> createDataDescriptors() {
        ArrayList<HashMap<String, String>> dataDescriptors = new ArrayList<>();
        dataDescriptors.add(createDataDescriptor("Heart Rate",0,255,"bpm", double.class.getSimpleName()));
        dataDescriptors.add(createDataDescriptor("Irregular Pulse",0,1,"true/false", double.class.getSimpleName()));
        return dataDescriptors;
    }
}
