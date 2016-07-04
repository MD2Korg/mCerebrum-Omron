package org.md2k.omron;

import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.md2k.datakitapi.datatype.DataTypeDoubleArray;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.datakitapi.source.platform.PlatformType;
import org.md2k.datakitapi.time.DateTime;
import org.md2k.omron.bluetooth.MyBlueTooth;
import org.md2k.omron.bluetooth.OnConnectionListener;
import org.md2k.omron.bluetooth.OnReceiveListener;
import org.md2k.omron.configuration.Configuration;
import org.md2k.omron.devices.DeviceBloodPressure;
import org.md2k.utilities.Report.Log;
import org.md2k.utilities.UI.AlertDialogs;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

/**
 * Created by monowar on 6/26/16.
 */
public class Fragment_2_Read_BP extends Fragment {
    private static final String TAG = Fragment_2_Read_BP.class.getSimpleName();
    MyBlueTooth myBlueTooth;
    TextView textViewSystolic;
    TextView textViewDiastolic;
    TextView textViewPulseRate;
    TextView textViewPulse;
    TextView textViewIrregularPulse;
    TextView textViewBattery;
    TextView textViewMovement;
    TextView textViewConnecting;
    Button buttonSave;
    Button buttonRetry;
    Button buttonCancel;
    boolean isRead = false;
    DeviceBloodPressure deviceBloodPressure;
    String deviceId;
    String deviceName;
    double[] heartRate;
    double[] bloodPressure;
    double[] activity;
    double[] battery;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_2_read_bp, container, false);
        ((TextView) v.findViewById(R.id.text_view_title)).setText(getArguments().getString("title"));

        textViewSystolic = (TextView) v.findViewById(R.id.text_view_systolic);
        textViewDiastolic = (TextView) v.findViewById(R.id.text_view_diastolic);
        textViewPulseRate = (TextView) v.findViewById(R.id.text_view_pulse_rate);
        textViewPulse = (TextView) v.findViewById(R.id.text_view_pulse);
        textViewIrregularPulse = (TextView) v.findViewById(R.id.text_view_irregular_pulse);
        textViewBattery = (TextView) v.findViewById(R.id.text_view_battery);
        textViewMovement = (TextView) v.findViewById(R.id.text_view_movement);
        textViewConnecting = (TextView) v.findViewById(R.id.text_view_status);
        buttonSave = (Button) v.findViewById(R.id.button_save);
        buttonRetry = (Button) v.findViewById(R.id.button_retry);
        buttonCancel = (Button) v.findViewById(R.id.button_cancel);
        isRead = false;
        updateView();
        deviceId = Configuration.getDeviceAddress(PlatformType.OMRON_BLOOD_PRESSURE);
        deviceName = Configuration.getDeviceName(PlatformType.OMRON_BLOOD_PRESSURE);
        deviceBloodPressure = new DeviceBloodPressure(getActivity(), deviceId, deviceName);
        myBlueTooth = new MyBlueTooth(getActivity(), onConnectionListener, onReceiveListener);
        return v;
    }

    private void updateView() {
        if (bloodPressure == null) {
            textViewSystolic.setText("---");
            textViewDiastolic.setText("---");
            textViewPulse.setText("---");
        } else {
            textViewSystolic.setText(String.valueOf(bloodPressure[0]));
            textViewDiastolic.setText(String.valueOf(bloodPressure[1]));
            textViewPulse.setText(String.valueOf(bloodPressure[2]));
        }
        if (heartRate == null) {
            textViewPulseRate.setText("---");
            textViewIrregularPulse.setText("---");
        } else {
            textViewPulseRate.setText(String.valueOf(heartRate[0]));
            textViewIrregularPulse.setText(String.valueOf(heartRate[1]));
        }
        if (battery == null) {
            textViewBattery.setText("---");
        } else {
            textViewBattery.setText(String.valueOf(battery[0]));
        }
        if (activity == null)
            textViewMovement.setText("---");
        else textViewMovement.setText(String.valueOf(activity[0]));
        if (isRead) {
            textViewConnecting.setText("Status: Connected");
            textViewConnecting.setTextColor(ContextCompat.getColor(getActivity(), R.color.teal_500));
        } else {
            textViewConnecting.setText("Status: Connecting.........");
            textViewConnecting.setTextColor(ContextCompat.getColor(getActivity(), R.color.red_500));
        }
        setupButton();
    }

    void setupButton() {
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialogs.AlertDialog(getActivity(), "Cancel Recording BP", "Do you want to cancel blood pressure reading?", R.drawable.ic_info_teal_48dp, "Yes", "Cancel", null, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE)
                            getActivity().finish();
                    }
                });
            }
        });
        if (isRead) {
            buttonSave.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.button_red));
            buttonSave.setTextColor(ContextCompat.getColor(getActivity(), R.color.white));
            buttonSave.setEnabled(true);
            buttonRetry.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.button_teal));
            buttonRetry.setTextColor(ContextCompat.getColor(getActivity(), R.color.black));
            buttonRetry.setEnabled(true);
        } else {
            buttonSave.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.button_teal));
            buttonSave.setTextColor(ContextCompat.getColor(getActivity(), R.color.grey_500));
            buttonSave.setEnabled(false);
            buttonRetry.setBackground(ContextCompat.getDrawable(getActivity(), R.drawable.button_red));
            buttonRetry.setTextColor(ContextCompat.getColor(getActivity(), R.color.white));
            buttonRetry.setEnabled(true);
        }
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialogs.AlertDialog(getActivity(), "Save Blood Pressure Result", "Please click \"Save\" if the reading is correct", R.drawable.ic_info_teal_48dp, "Save", "Cancel", null, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            saveData();
                            Toast.makeText(getActivity(), "Blood Pressure result saved...", Toast.LENGTH_SHORT).show();
                            getActivity().finish();
                        }
                    }
                });
            }
        });
        buttonRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRead = false;
                battery = null;
                heartRate = null;
                bloodPressure = null;
                activity = null;
                updateView();
                myBlueTooth.disconnect();
                myBlueTooth.close();
                myBlueTooth = new MyBlueTooth(getActivity(), onConnectionListener, onReceiveListener);
            }
        });
    }

    void saveData() {
        try {
            deviceBloodPressure.getSensors(DataSourceType.BLOOD_PRESSURE).insert(new DataTypeDoubleArray(DateTime.getDateTime(), bloodPressure));
            deviceBloodPressure.getSensors(DataSourceType.HEART_RATE).insert(new DataTypeDoubleArray(DateTime.getDateTime(), heartRate));
            deviceBloodPressure.getSensors(DataSourceType.ACTIVITY).insert(new DataTypeDoubleArray(DateTime.getDateTime(), activity));
            deviceBloodPressure.getSensors(DataSourceType.BATTERY).insert(new DataTypeDoubleArray(DateTime.getDateTime(), battery));
        } catch (DataKitException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    OnConnectionListener onConnectionListener = new OnConnectionListener() {
        @Override
        public void onConnected() {
            UUID uuid = Constants.SERVICE_BLOOD_PRESSURE_UUID;
            myBlueTooth.scanOn(new UUID[]{uuid});
        }

        @Override
        public void onDisconnected() {

        }
    };
    OnReceiveListener onReceiveListener = new OnReceiveListener() {
        @Override
        public void onReceived(Message msg) {
            byte[] data;
            byte[] buf = new byte[2];
            ByteBuffer byteBuffer;
            switch (msg.what) {
                case MyBlueTooth.MSG_ADV_CATCH_DEV:
                    BluetoothDevice bluetoothDevice = (BluetoothDevice) msg.obj;
                    if (bluetoothDevice.getAddress().equals(deviceId))
                        myBlueTooth.connect((BluetoothDevice) msg.obj);
                    break;
                case MyBlueTooth.MSG_CONNECTED:
                    break;
                case MyBlueTooth.MSG_BPM_DATA_RECV:
                    int idx = 0;
                    data = (byte[]) msg.obj;

                    byte flags = data[idx++];

                    // 0: mmHg	1: kPa
                    boolean kPa = (flags & 0x01) > 0;
                    // 0: No Timestamp info 1: With Timestamp info
                    boolean timestampFlag = (flags & 0x02) > 0;
                    // 0: No PlseRate info 1: With PulseRate info
                    boolean pulseRateFlag = (flags & 0x04) > 0;
                    // 0: No UserID info 1: With UserID info
                    boolean userIdFlag = (flags & 0x08) > 0;
                    // 0: No MeasurementStatus info 1: With MeasurementStatus info
                    boolean measurementStatusFlag = (flags & 0x10) > 0;

                    // Set BloodPressureMeasurement unit
                    String unit;
                    if (kPa) {
                        unit = "kPa";
                    } else {
                        unit = "mmHg";
                    }

                    // Parse Blood Pressure Measurement
                    short systolicVal = 0;
                    short diastolicVal = 0;
                    short meanApVal = 0;

                    System.arraycopy(data, idx, buf, 0, 2);
                    idx += 2;
                    byteBuffer = ByteBuffer.wrap(buf);
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                    systolicVal = byteBuffer.getShort();

                    System.arraycopy(data, idx, buf, 0, 2);
                    idx += 2;
                    byteBuffer = ByteBuffer.wrap(buf);
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                    diastolicVal = byteBuffer.getShort();

                    System.arraycopy(data, idx, buf, 0, 2);
                    idx += 2;
                    byteBuffer = ByteBuffer.wrap(buf);
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                    meanApVal = byteBuffer.getShort();
                    bloodPressure = new double[3];
                    bloodPressure[0] = systolicVal;
                    bloodPressure[1] = diastolicVal;
                    bloodPressure[2] = meanApVal;

                    textViewSystolic.setText(String.valueOf(systolicVal));
                    textViewDiastolic.setText(String.valueOf(diastolicVal));
                    textViewPulse.setText(String.valueOf(meanApVal));

                    // Parse Timestamp
                    String timestampStr = "----";
                    String dateStr = "--";
                    String timeStr = "--";
                    if (timestampFlag) {
                        System.arraycopy(data, idx, buf, 0, 2);
                        idx += 2;
                        byteBuffer = ByteBuffer.wrap(buf);
                        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

                        int year = byteBuffer.getShort();
                        int month = data[idx++];
                        int day = data[idx++];
                        int hour = data[idx++];
                        int min = data[idx++];
                        int sec = data[idx++];

                        dateStr = String.format("%1$04d", year) + "-" + String.format("%1$02d", month) + "-" + String.format("%1$02d", day);
                        timeStr = String.format("%1$02d", hour) + ":" + String.format("%1$02d", min) + ":" + String.format("%1$02d", sec);
                        timestampStr = dateStr + " " + timeStr;
                    }

                    // Parse PulseRate
                    short pulseRateVal = 0;
                    String pulseRateStr = "----";
                    if (pulseRateFlag) {
                        System.arraycopy(data, idx, buf, 0, 2);
                        idx += 2;
                        byteBuffer = ByteBuffer.wrap(buf);
                        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                        pulseRateVal = byteBuffer.getShort();
                        pulseRateStr = Short.toString(pulseRateVal);
                    }
                    heartRate = new double[2];
                    heartRate[0] = pulseRateVal;
                    textViewPulseRate.setText(pulseRateStr);

                    // Parse UserID
                    int userIDVal = 0;
                    String userIDStr = "----";
                    if (userIdFlag) {
                        userIDVal = data[idx++];
                        userIDStr = String.valueOf(userIDVal);
                    }

                    // Parse Measurement Status
                    int measurementStatusVal = 0;
                    System.arraycopy(data, idx, buf, 0, 2);
                    idx += 2;
                    byteBuffer = ByteBuffer.wrap(buf);
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                    measurementStatusVal = byteBuffer.getShort();

                    activity = new double[]{((measurementStatusVal & 0x0001) == 0 ? 0 : 1)};
                    heartRate[1] = ((measurementStatusVal & 0x0004) == 0 ? 0 : 1);
                    isRead = true;
                    break;
                case MyBlueTooth.MSG_BATTERY_DATA_RECV:
                    isRead = true;
                    byte[] batteryData = (byte[]) msg.obj;
                    battery = new double[]{batteryData[0]};
                    break;
            }
            updateView();
        }
    };

    public static Fragment_2_Read_BP newInstance(String title) {
        Fragment_2_Read_BP f = new Fragment_2_Read_BP();
        Bundle b = new Bundle();
        b.putString("title", title);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()...");
        if (myBlueTooth != null) {
            myBlueTooth.disconnect();
            myBlueTooth.scanOff();
            myBlueTooth.close();
        }
        super.onDestroy();
    }
}
