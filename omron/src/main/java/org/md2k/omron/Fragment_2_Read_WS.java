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
import org.md2k.omron.devices.DeviceWeightScale;
import org.md2k.utilities.Report.Log;
import org.md2k.utilities.UI.AlertDialogs;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

/**
 * Created by monowar on 6/26/16.
 */
public class Fragment_2_Read_WS extends Fragment {
    private static final String TAG = Fragment_2_Read_WS.class.getSimpleName();
    MyBlueTooth myBlueTooth;
    TextView textViewWeight;
    TextView textViewBattery;
    TextView textViewConnecting;
    Button buttonSave;
    Button buttonRetry;
    Button buttonCancel;
    boolean isRead = false;
    DeviceWeightScale deviceWeightScale;
    String deviceId;
    String deviceName;
    double[] weight;
    double[] battery;
    // Resolution table							  default  1	 2	   3	 4	   5	 6	   7
    private final static double RESOLUTION_KG[] = {0.005, 0.5, 0.2, 0.1, 0.05, 0.02, 0.01, 0.005};
    private final static double RESOLUTION_LB[] = {0.01, 1.0, 0.5, 0.2, 0.1, 0.05, 0.02, 0.01};
    private int mResolutionIdx;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_2_read_ws, container, false);
        ((TextView) v.findViewById(R.id.text_view_title)).setText(getArguments().getString("title"));

        textViewWeight = (TextView) v.findViewById(R.id.text_view_weight);
        textViewBattery = (TextView) v.findViewById(R.id.text_view_battery);
        textViewConnecting = (TextView) v.findViewById(R.id.text_view_status);
        buttonSave = (Button) v.findViewById(R.id.button_save);
        buttonRetry = (Button) v.findViewById(R.id.button_retry);
        buttonCancel = (Button) v.findViewById(R.id.button_cancel);
        isRead = false;
        updateView();
        deviceId = Configuration.getDeviceAddress(PlatformType.OMRON_WEIGHT_SCALE);
        deviceName = Configuration.getDeviceName(PlatformType.OMRON_WEIGHT_SCALE);
        deviceWeightScale = new DeviceWeightScale(getActivity(), deviceId, deviceName);
        myBlueTooth = new MyBlueTooth(getActivity(), onConnectionListener, onReceiveListener);
        return v;
    }

    private void updateView() {
        if (weight == null) {
            textViewWeight.setText("---");
        } else {
            textViewWeight.setText(String.valueOf(weight[0]));
        }
        if (battery == null) {
            textViewBattery.setText("---");
        } else {
            textViewBattery.setText(String.valueOf(battery[0]));
        }
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
                AlertDialogs.AlertDialog(getActivity(), "Cancel Recording Weight", "Do you want to cancel weight scale reading?", R.drawable.ic_info_teal_48dp, "Yes", "Cancel", null, new DialogInterface.OnClickListener() {
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
                AlertDialogs.AlertDialog(getActivity(), "Save Weight Scale Result", "Please click \"Save\" if the reading is correct", R.drawable.ic_info_teal_48dp, "Save", "Cancel", null, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            saveData();
                            Toast.makeText(getActivity(), "Weight Scale result saved...", Toast.LENGTH_SHORT).show();
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
                weight = null;
                updateView();
                myBlueTooth.disconnect();
                myBlueTooth.close();
                myBlueTooth = new MyBlueTooth(getActivity(), onConnectionListener, onReceiveListener);
            }
        });
    }

    void saveData() {
        try {
            deviceWeightScale.getSensors(DataSourceType.WEIGHT).insert(new DataTypeDoubleArray(DateTime.getDateTime(), weight));
            deviceWeightScale.getSensors(DataSourceType.BATTERY).insert(new DataTypeDoubleArray(DateTime.getDateTime(), battery));
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
            UUID uuid = Constants.SERVICE_WEIGHT_SCALE_UUID;
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
                case MyBlueTooth.MSG_WM_DATA_RECV:
                    int idx = 0;

                    data = (byte[]) msg.obj;
                    byte flags = data[idx++];

                    // Parse Flags
                    // 0: kg/cm	 1: lb/inchi
                    boolean kg = (flags & 0x01) == 0;
                    // 0: No Timestamp info 1: With Timestamp info
                    boolean timestampFlag = (flags & 0x02) > 0;
                    // 0: No UserID info 1: With UserID info
                    boolean userIDFlag = (flags & 0x04) > 0;
                    // 0: No BMI info 1: With BMI info
                    boolean bmiFlag = (flags & 0x08) > 0;

                    // Parse WeightScale
                    System.arraycopy(data, idx, buf, 0, 2);
                    idx += 2;
                    int weightMeasurementVal = (buf[0] & 0xFF) | ((buf[1] & 0xFF) << 8);

                    double kgWeight = 0;
                    double lbWeight = 0;
                    if (kg) {
                        if (mResolutionIdx >= RESOLUTION_KG.length) {
                            mResolutionIdx = 0;
                        }
                        BigDecimal bdRaw = new BigDecimal(weightMeasurementVal);
                        BigDecimal bdResolution = new BigDecimal(String.format("%.4f", RESOLUTION_KG[mResolutionIdx]));
                        kgWeight = bdRaw.multiply(bdResolution).doubleValue();
                        weight = new double[1];
                        weight[0] = kgWeight * 2.20462;

                    } else {
                        if (mResolutionIdx >= RESOLUTION_LB.length) {
                            mResolutionIdx = 0;
                        }
                        BigDecimal bdRaw = new BigDecimal(weightMeasurementVal);
                        BigDecimal bdResolution = new BigDecimal(String.format("%.4f", RESOLUTION_LB[mResolutionIdx]));
                        lbWeight = bdRaw.multiply(bdResolution).doubleValue();
                        weight = new double[1];
                        weight[0] = lbWeight;
                    }

                    // Parse Timesamp
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

                    // Parse UserID
                    int userIDVal = 0;
                    if (userIDFlag) {
                        userIDVal = data[idx++] & 0xFF;
                    }
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

    public static Fragment_2_Read_WS newInstance(String title) {
        Fragment_2_Read_WS f = new Fragment_2_Read_WS();
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
            myBlueTooth.close();
        }
        super.onDestroy();
    }
}
