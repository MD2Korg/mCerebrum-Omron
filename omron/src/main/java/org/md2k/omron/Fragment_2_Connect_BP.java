package org.md2k.omron;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.github.paolorotolo.appintro.ISlidePolicy;

import org.md2k.datakitapi.source.platform.PlatformType;
import org.md2k.omron.bluetooth.MyBlueTooth;
import org.md2k.omron.bluetooth.OnReceiveListener;
import org.md2k.omron.configuration.Configuration;
import org.md2k.omron.devices.DeviceBloodPressure;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;
import java.util.UUID;

/**
 * Created by monowar on 6/26/16.
 */
public class Fragment_2_Connect_BP extends Fragment implements ISlidePolicy {
    boolean isBattery, isBp;
    ActivityBloodPressure activity;
    org.md2k.omron.bluetooth.OnConnectionListener onConnectionListener = new org.md2k.omron.bluetooth.OnConnectionListener() {
        @Override
        public void onConnected() {
            UUID uuid = Constants.SERVICE_BLOOD_PRESSURE_UUID;
            activity.myBlueTooth.scanOn(new UUID[]{uuid});
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
                    if (bluetoothDevice.getAddress().equals(activity.deviceId))
                        activity.myBlueTooth.connect((BluetoothDevice) msg.obj);
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
                    activity.bloodPressure = new double[3];
                    activity.bloodPressure[0] = systolicVal;
                    activity.bloodPressure[1] = diastolicVal;
                    activity.bloodPressure[2] = meanApVal;

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

                        dateStr = String.format(Locale.US, "%1$04d", year) + "-" + String.format(Locale.US, "%1$02d", month) + "-" + String.format(Locale.US, "%1$02d", day);
                        timeStr = String.format(Locale.US, "%1$02d", hour) + ":" + String.format(Locale.US, "%1$02d", min) + ":" + String.format(Locale.US, "%1$02d", sec);
                        timestampStr = dateStr + " " + timeStr;
                    }

                    // Parse PulseRate
                    short pulseRateVal = 0;
                    if (pulseRateFlag) {
                        System.arraycopy(data, idx, buf, 0, 2);
                        idx += 2;
                        byteBuffer = ByteBuffer.wrap(buf);
                        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                        pulseRateVal = byteBuffer.getShort();
                    }
                    activity.heartRate = new double[2];
                    activity.heartRate[0] = pulseRateVal;

                    // Parse Measurement Status
                    int measurementStatusVal = 0;
                    System.arraycopy(data, idx, buf, 0, 2);
                    idx += 2;
                    byteBuffer = ByteBuffer.wrap(buf);
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                    measurementStatusVal = byteBuffer.getShort();

                    activity.activity = new double[]{((measurementStatusVal & 0x0001) == 0 ? 0 : 1)};
                    activity.heartRate[1] = ((measurementStatusVal & 0x0004) == 0 ? 0 : 1);
                    isBp = true;
                    nextPageIf();
                    break;
                case MyBlueTooth.MSG_BATTERY_DATA_RECV:
                    byte[] batteryData = (byte[]) msg.obj;
                    activity.battery = new double[]{batteryData[0]};
                    isBattery = true;
                    nextPageIf();
                    break;
            }
        }
    };

    public static Fragment_2_Connect_BP newInstance(String platformType, String title, String message, int image) {
        Fragment_2_Connect_BP f = new Fragment_2_Connect_BP();
        Bundle b = new Bundle();
        b.putString(PlatformType.class.getSimpleName(), platformType);
        b.putString("title", title);
        b.putString("message", message);
        b.putInt("image", image);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (ActivityBloodPressure) getActivity();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_2_connect_bp, container, false);

        TextView tv = (TextView) v.findViewById(R.id.text_view_title);
        tv.setText(getArguments().getString("title"));
        tv = (TextView) v.findViewById(R.id.text_view_message);
        tv.setText(getArguments().getString("message"));
        Button button1 = (Button) v.findViewById(R.id.button_1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.startSlide();
            }
        });

        return v;
    }

    @Override
    public boolean isPolicyRespected() {
        return false;
    }

    @Override
    public void onUserIllegallyRequestedNextPage() {
//        Toast.makeText(getContext(), R.string.slide_policy_demo_error, Toast.LENGTH_SHORT).show();
    }

    public void start() {
        activity.deviceId = Configuration.getDeviceAddress(PlatformType.OMRON_BLOOD_PRESSURE);
        activity.deviceName = Configuration.getDeviceName(PlatformType.OMRON_BLOOD_PRESSURE);
        activity.deviceBloodPressure = new DeviceBloodPressure(getActivity(), activity.deviceId, activity.deviceName);
        stop();
        isBattery = false;
        isBp = false;
        activity.myBlueTooth = new MyBlueTooth(getActivity(), onConnectionListener, onReceiveListener);
    }

    public void stop() {
        if (activity.myBlueTooth != null) {
            activity.myBlueTooth.disconnect();
            activity.myBlueTooth.scanOff();
            activity.myBlueTooth.close();
            activity.myBlueTooth = null;
        }
    }

    void nextPageIf() {
        if (isBattery && isBp) {
            stop();
            isBp = false;
            isBattery = false;
            activity.nextSlide();
        }
    }
}
