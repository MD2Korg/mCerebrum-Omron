package org.md2k.omron.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import org.md2k.omron.IBleListener;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;
import java.util.UUID;


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

public class MyBlueTooth {
    public static final int MSG_CONNECTING = 0;
    public static final int MSG_CONNECTED = 1;
    public static final int MSG_DISCONNECTED = 2;
    public static final int MSG_WM_DATA_RECV = 3;
    public static final int MSG_ADV_CATCH_DEV = 4;
    public static final int MSG_SCAN_CANCEL = 5;
    public static final int MSG_BATTERY_DATA_RECV = 6;
    public static final int MSG_CTS_DATA_RECV = 7;
    public static final int MSG_BPM_DATA_RECV = 8;
    public static final int MSG_BPF_DATA_RECV = 9;
    public static final int MSG_LISTVIEW_CLR = 10;
    public static final int MSG_WSF_DATA_RECV = 11;
    public static final int BLE_STATE_IDLE = 0;
    public static final int BLE_STATE_SCANNING = 1;
    public static final int BLE_STATE_CONNECTING = 2;
    public static final int BLE_STATE_CONNECT = 3;
    public static final int BLE_STATE_DATA_RECV = 4;
    private static final String TAG = MyBlueTooth.class.getSimpleName();
    public int mBleState = BLE_STATE_IDLE;
    protected BleService mBleService;
    Context context;
    OnReceiveListener onReceiveListener;
    OnConnectionListener onConnectionListener;
    protected ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "[IN]onServiceConnected");
            onBleServiceConnected(service);
        }

        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "[IN]onServiceDisconnected");
            mBleService = null;
            context.unbindService(mConnection);
            onConnectionListener.onDisconnected();
        }
    };
    boolean isConnected;
    // Event handler
    protected Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            onReceiveMessage(msg);
        }
    };
    private final IBleListener.Stub mBinder = new IBleListener.Stub() {
        public void BleAdvCatch() throws RemoteException {
            Log.d(TAG, "[IN]BleAdvCatch");
            mBleState = BLE_STATE_CONNECTING;
            Message msg = new Message();
            msg.what = MSG_CONNECTING;
            mHandler.sendMessage(msg);
        }

        public void BleAdvCatchDevice(BluetoothDevice dev) throws RemoteException {
            Log.d(TAG, "[IN]BleAdvCatchDevice");
            Message msg = new Message();
            msg.what = MSG_ADV_CATCH_DEV;
            msg.obj = dev;
            mHandler.sendMessage(msg);
        }

        public void BleConnected() throws RemoteException {
            Log.d(TAG, "[IN]BleConnected");
            mBleState = BLE_STATE_CONNECT;
            Message msg = new Message();
            msg.what = MSG_CONNECTED;
            mHandler.sendMessage(msg);
        }

        public void BleDisConnected() throws RemoteException {
            Log.d(TAG, "[IN]BleDisConnected");
            mBleState = BLE_STATE_IDLE;
            Message msg = new Message();
            msg.what = MSG_DISCONNECTED;
            mHandler.sendMessage(msg);
        }

        public void BleWmDataRecv(byte[] data) throws RemoteException {
            Log.d(TAG, "[IN]BleDataRecv");
            Message msg = new Message();
            msg.what = MSG_WM_DATA_RECV;
            msg.obj = data;
            mHandler.sendMessage(msg);
        }

        public void BleBatteryDataRecv(byte[] data) throws RemoteException {
            Log.d(TAG, "[IN]BleBatteryDataRecv");
            Message msg = new Message();
            msg.what = MSG_BATTERY_DATA_RECV;
            msg.obj = data;
            mHandler.sendMessage(msg);
        }

        public void BleCtsDataRecv(byte[] data) throws RemoteException {
            Log.d(TAG, "[IN]BleCtsDataRecv");
            Message msg = new Message();
            msg.what = MSG_CTS_DATA_RECV;
            msg.obj = data;
            mHandler.sendMessage(msg);
        }

        public void BleBpmDataRecv(byte[] data) throws RemoteException {
            Log.d(TAG, "[IN]BleBpmDataRecv");
            Message msg = new Message();
            msg.what = MSG_BPM_DATA_RECV;
            msg.obj = data;
            mHandler.sendMessage(msg);
        }

        public void BleBpfDataRecv(byte[] data) throws RemoteException {
            Log.d(TAG, "[IN]BleBpfDataRecv");
            Message msg = new Message();
            msg.what = MSG_BPF_DATA_RECV;
            msg.obj = data;
            mHandler.sendMessage(msg);
        }

        public void BleWsfDataRecv(byte[] data) throws RemoteException {
            Log.d(TAG, "[IN]BleWsfDataRecv");
            Message msg = new Message();
            msg.what = MSG_WSF_DATA_RECV;
            msg.obj = data;
            mHandler.sendMessage(msg);
        }
    };

    public MyBlueTooth(Context context, OnConnectionListener onConnectionListener, OnReceiveListener onReceiveListener) {
        this.context = context;
        this.onReceiveListener = onReceiveListener;
        this.onConnectionListener = onConnectionListener;
        isConnected = false;
        if (mBleService != null) {
            mBleService.setCurrentContext(context.getApplicationContext(), mBinder);
        }
        context.bindService(new Intent(context, BleService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    public void scanOn(UUID[] uuids) {
        mBleService.BleScan(uuids);
    }

    public void scanOff() {
        mBleService.BleScanOff();
    }

    public void connect(BluetoothDevice bluetoothDevice) {
        mBleService.BleConnectDev(bluetoothDevice);
    }

    public void close() {
        context.unbindService(mConnection);
    }

    public void disconnect() {
        if (isConnected)
            mBleService.BleDisconnect();
    }

    public void enable() {
        BluetoothAdapter mBluetoothAdapter = ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        if (mBluetoothAdapter == null) return;
        if (!mBluetoothAdapter.isEnabled()) {
            Intent btIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            btIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(btIntent);
        }
    }

    public void disable() {
        BluetoothAdapter mBluetoothAdapter = ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        if (mBluetoothAdapter == null) return;
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
        }
    }

    public boolean isEnabled() {
        BluetoothAdapter mBluetoothAdapter = ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    public boolean hasSupport() {
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
            return false;
        BluetoothAdapter mBluetoothAdapter = ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        return mBluetoothAdapter != null;
    }

    protected void onReceiveMessage(Message msg) {
        Log.d(TAG, "[IN]onReceiveMessage");
        switch(msg.what){
            case MSG_CONNECTING:
                break;

            case MSG_CONNECTED:
                Log.d(TAG, "[LOG]MSG_CONNECTED");
                isConnected=true;
                onReceiveListener.onReceived(msg);
                break;

            case MSG_DISCONNECTED:
                Log.d(TAG, "[LOG]MSG_DISCONNECTED");
                isConnected=false;
                onReceiveListener.onReceived(msg);
                break;

            case MSG_ADV_CATCH_DEV:
                Log.d(TAG, "[LOG]MSG_ADV_CATCH_DEV");
                onReceiveListener.onReceived(msg);
                break;
            case MSG_BPM_DATA_RECV:
                onReceiveListener.onReceived(msg);
                break;
            case MSG_BPF_DATA_RECV:
                onReceiveListener.onReceived(msg);
                break;
            case MSG_WSF_DATA_RECV:
                onReceiveListener.onReceived(msg);
                break;
            case MSG_WM_DATA_RECV:
                onReceiveListener.onReceived(msg);
                break;

            case MSG_BATTERY_DATA_RECV:
                onReceiveListener.onReceived(msg);
                break;

            case MSG_CTS_DATA_RECV:
                byte[] ctsdata = (byte[])msg.obj;
                byte[] buf = new byte[2];
                System.arraycopy(ctsdata, 0, buf, 0, 2);
                ByteBuffer ctsyearbyteBuffer = ByteBuffer.wrap(buf);
                ctsyearbyteBuffer.order(ByteOrder.LITTLE_ENDIAN);

                int	 ctsYear	  = ctsyearbyteBuffer.getShort();
                int	 ctsMonth	  = ctsdata[2];
                int	 ctsDay		  = ctsdata[3];
                int	 ctsHour	  = ctsdata[4];
                int	 ctsMinute	  = ctsdata[5];
                int	 ctsSecond	  = ctsdata[6];
                byte AdjustReason = ctsdata[9];

                String ctsTime =
                        String.format(Locale.US, "%1$04d", ctsYear) + "-" + String.format(Locale.US, "%1$02d", ctsMonth) + "-" + String.format(Locale.US, "%1$02d", ctsDay) + " " +
                                String.format(Locale.US, "%1$02d", ctsHour) + ":" + String.format(Locale.US, "%1$02d", ctsMinute) + ":" + String.format(Locale.US, "%1$02d", ctsSecond);
                onReceiveListener.onReceived(msg);
                break;

            case MSG_LISTVIEW_CLR:
                Log.d(TAG, "[LOG]MSG_LISTVIEW_CLR");
                break;

            default:
                break;
        }
    }

    protected void onBleServiceConnected(IBinder service) {
        Log.d(TAG, "[IN]onBleReceiveMessage");
        mBleService = ((BleService.MyServiceLocalBinder)service).getService();
        mBleService.setCurrentContext(context.getApplicationContext(), mBinder);
        onConnectionListener.onConnected();
    }
}
