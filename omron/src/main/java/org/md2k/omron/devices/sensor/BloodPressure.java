package org.md2k.omron.devices.sensor;

import android.content.Context;

import org.md2k.datakitapi.datatype.DataTypeDoubleArray;
import org.md2k.datakitapi.datatype.DataTypeInt;
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
public class BloodPressure extends Sensor {
    public BloodPressure(Context context) {
        super(context, DataSourceType.BLOOD_PRESSURE);
    }

    @Override
    public DataSourceBuilder createDataSourceBuilder(Platform platform){
        DataSourceBuilder dataSourceBuilder=super.createDataSourceBuilder(platform);
        dataSourceBuilder=dataSourceBuilder.setMetadata(METADATA.NAME, "Blood Pressure")
                .setDataDescriptors(createDataDescriptors())
                .setMetadata(METADATA.MIN_VALUE, "0")
                .setMetadata(METADATA.MAX_VALUE, "300")
                .setMetadata(METADATA.DATA_TYPE, DataTypeDoubleArray.class.getSimpleName())
                .setMetadata(METADATA.DESCRIPTION, "Blood Pressure Measurement");
        return dataSourceBuilder;
    }
    ArrayList<HashMap<String, String>> createDataDescriptors() {
        ArrayList<HashMap<String, String>> dataDescriptors = new ArrayList<>();
        dataDescriptors.add(createDataDescriptor("Systolic Blood Pressure",25, 280, "mmHg",double.class.getSimpleName()));
        dataDescriptors.add(createDataDescriptor("Diastolic Blood Pressure", 0, 255, "mmHg",double.class.getSimpleName()));
        dataDescriptors.add(createDataDescriptor("Mean Arterial Pressure", 8, 262, "mmHg",double.class.getSimpleName()));
        return dataDescriptors;
    }

}
