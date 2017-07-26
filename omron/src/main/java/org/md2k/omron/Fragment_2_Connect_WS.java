package org.md2k.omron;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.github.paolorotolo.appintro.ISlidePolicy;

import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.source.platform.PlatformType;
import org.md2k.omron.bluetooth.MyBlueTooth;
import org.md2k.omron.bluetooth.OnConnectionListener;
import org.md2k.omron.bluetooth.OnReceiveListener;
import org.md2k.omron.configuration.Configuration;
import org.md2k.omron.devices.DeviceWeightScale;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;
import java.util.UUID;

/**
 * Copyright (c) 2016, The University of Memphis, MD2K Center
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
public class Fragment_2_Connect_WS extends Fragment implements ISlidePolicy {
    int isData, isBattery;
    ActivityWeightScale activity;
    Handler handler;

    OnConnectionListener onConnectionListener = new OnConnectionListener() {
        @Override
        public void onConnected() {
            UUID uuid = Constants.SERVICE_WEIGHT_SCALE_UUID;
            activity.myBlueTooth.scanOn(new UUID[]{uuid});
        }

        @Override
        public void onDisconnected() {

        }
    };
    Runnable runnableNextPage = new Runnable() {
        @Override
        public void run() {
            if (isData == 0 || isBattery == 0) return;
            stop();
            isData = 0;
            isBattery = 0;
            activity.nextSlide();

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
                        if (activity.mResolutionIdx >= ActivityWeightScale.RESOLUTION_KG.length) {
                            activity.mResolutionIdx = 0;
                        }
                        BigDecimal bdRaw = new BigDecimal(weightMeasurementVal);
                        BigDecimal bdResolution = new BigDecimal(String.format(Locale.ENGLISH, "%.4f", ActivityWeightScale.RESOLUTION_KG[activity.mResolutionIdx]));
                        kgWeight = bdRaw.multiply(bdResolution).doubleValue();
                        activity.weight = new double[1];
                        activity.weight[0] = kgWeight * 2.20462;

                    } else {
                        if (activity.mResolutionIdx >= ActivityWeightScale.RESOLUTION_LB.length) {
                            activity.mResolutionIdx = 0;
                        }
                        BigDecimal bdRaw = new BigDecimal(weightMeasurementVal);
                        BigDecimal bdResolution = new BigDecimal(String.format(Locale.ENGLISH, "%.4f", ActivityWeightScale.RESOLUTION_LB[activity.mResolutionIdx]));
                        lbWeight = bdRaw.multiply(bdResolution).doubleValue();
                        activity.weight = new double[1];
                        activity.weight[0] = lbWeight;
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

                        dateStr = String.format(Locale.US, "%1$04d", year) + "-" + String.format(Locale.US, "%1$02d", month) + "-" + String.format(Locale.US, "%1$02d", day);
                        timeStr = String.format(Locale.US, "%1$02d", hour) + ":" + String.format(Locale.US, "%1$02d", min) + ":" + String.format(Locale.US, "%1$02d", sec);
                        timestampStr = dateStr + " " + timeStr;
                    }

                    // Parse UserID
                    int userIDVal = 0;
                    if (userIDFlag) {
                        userIDVal = data[idx++] & 0xFF;
                    }
                    isData++;
                    handler.removeCallbacks(runnableNextPage);
                    handler.postDelayed(runnableNextPage, 2000);
                    break;
                case MyBlueTooth.MSG_BATTERY_DATA_RECV:
                    byte[] batteryData = (byte[]) msg.obj;
                    activity.battery = new double[]{batteryData[0]};
                    isBattery++;
                    handler.removeCallbacks(runnableNextPage);
                    handler.postDelayed(runnableNextPage, 2000);
                    break;
            }
        }
    };
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode==1) return;
        activity.weight=data.getDoubleArrayExtra("WEIGHT");
        isData=1;
        isBattery=1;
        handler.removeCallbacks(runnableNextPage);
        handler.post(runnableNextPage);
    }

    public static Fragment_2_Connect_WS newInstance(String platformType, String title, String message, int image) {

        Fragment_2_Connect_WS f = new Fragment_2_Connect_WS();
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
        handler = new Handler();
        activity = (ActivityWeightScale) getActivity();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(runnableNextPage);
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_2_connect_ws, container, false);

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
        Button button2 = (Button) v.findViewById(R.id.button_2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(activity,ActivityWeightScaleManual.class);
                startActivityForResult(intent, 2);// Activity is started with requestCode 2
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
        activity.deviceId = Configuration.getDeviceAddress(PlatformType.OMRON_WEIGHT_SCALE);
        activity.deviceName = Configuration.getDeviceName(PlatformType.OMRON_WEIGHT_SCALE);
        activity.deviceWeightScale = new DeviceWeightScale(getActivity(), activity.deviceId, activity.deviceName);
        stop();
        try {
            activity.deviceWeightScale.register();
        } catch (DataKitException ignored) {

        }
        isBattery = 0;
        isData = 0;
        activity.myBlueTooth = new MyBlueTooth(getActivity(), onConnectionListener, onReceiveListener);
    }

    public void stop() {
        handler.removeCallbacks(runnableNextPage);
        if (activity.myBlueTooth != null) {
            activity.myBlueTooth.disconnect();
            activity.myBlueTooth.scanOff();
            activity.myBlueTooth.close();
            activity.myBlueTooth = null;
        }
    }
}