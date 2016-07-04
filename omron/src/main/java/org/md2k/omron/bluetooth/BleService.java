/*
 * File: BleService.java
 *
 * Abstract: BLE service class.
 *
 * Copyright (c) 2015 OMRON HEALTHCARE Co., Ltd. All rights reserved.
 */

package org.md2k.omron.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import org.md2k.omron.IBleListener;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


public class BleService extends Service {
	private final static String TAG = "BleService";
	private final static String LOG_TAG = "BLE_LOG";

	// CCCD
	public static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

	// Service UUID
	public static final UUID Blood_Pressure_SERVICE_UUID = UUID.fromString("00001810-0000-1000-8000-00805f9b34fb");
	public static final UUID Weight_Scale_SERVICE_UUID	 = UUID.fromString("0000181D-0000-1000-8000-00805f9b34fb");
	public static final UUID Battery_SERVICE_UUID		 = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");
	public static final UUID Current_Time_SERVICE_UUID	 = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb");

	// Characteristic UUID
	public static final UUID Blood_Pressure_Measurement_CHAR_UUID = UUID.fromString("00002A35-0000-1000-8000-00805f9b34fb");
	public static final UUID Blood_Pressure_Feature_CHAR_UUID	  = UUID.fromString("00002A49-0000-1000-8000-00805f9b34fb");
	public static final UUID Weight_Measurement_CHAR_UUID		  = UUID.fromString("00002A9D-0000-1000-8000-00805f9b34fb");
	public static final UUID Weight_Scale_Feature_CHAR_UUID		  = UUID.fromString("00002A9E-0000-1000-8000-00805f9b34fb");
	public static final UUID Battery_Serv_CHAR_UUID				  = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb");
	public static final UUID Current_Time_Serv_CHAR_UUID		  = UUID.fromString("00002A2B-0000-1000-8000-00805f9b34fb");

	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothGatt mBluetoothGatt;
	private BluetoothDevice mBluetoothDevice = null;
	IBleListener mAppListener=null;

	//Create Binder
	private final IBinder mBinder = new MyServiceLocalBinder();

	private boolean mIsDiscoveredService = false;
	private boolean mIsACLConnected = false;
	private boolean mIsConnected = false;
	private boolean mIsBonded = false;
	private boolean mWriteCtsIsEnable = false;
	private boolean mCtsNeedToWrite = false;

	private static final int MSG_CONNECT = 1;
	private static final int MSG_DISCONNECT = 2;
	private static final int MSG_REQ_TIMEOUT = 3;
	private static final int MSG_SCAN_START = 4;
	private static final int MSG_SCAN_STOP = 5;
	private static final int MSG_NOTIFY_ACL_CONNECTED = 10;
	private static final int MSG_NOTIFY_ACL_DISCONNECTED = 11;
	private static final int MSG_NOTIFY_BOND_NONE = 12;
	private static final int MSG_NOTIFY_BOND_BONDED = 13;

	class BleRequest {
		public static final int TYPE_NONE		= 0;
		public static final int TYPE_DESC_WRITE = 1;
		public static final int TYPE_DESC_READ	= 2;
		public static final int TYPE_CHAR_WRITE = 3;
		public static final int TYPE_CHAR_READ	= 4;
		public static final int TYPE_MAX		= 5;
		public int type;
		public Object o;
		public Date date;
	}

	private static final int BLE_REQ_RETRY_MAX  = 20;
	private static final int BLE_REQ_TIMEOUT_MS = 1500;

	private Queue<BleRequest> mBleReqQueue = new LinkedList<BleRequest>();
	private Queue<BleRequest> mBleReqQueueAfterAuth = new LinkedList<BleRequest>();
	private int mBleReqRetryCount = 0;
	private Timer mBleReqTimer = null;
	private boolean mBleReqExecuting = false;


	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "[IN]onCreate");
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "[IN]onDestroy");
		super.onDestroy();
		mBluetoothGatt = null;
		mBluetoothAdapter = null;
		mBluetoothDevice = null;
		bleReq_QueueClear();
		mBleReqQueueAfterAuth.clear();
		if (mBleReqTimer != null){
			mBleReqTimer.cancel();
		}
		mBleReqTimer = null;
	}

	public boolean setWriteCts(boolean enabled) {
		Log.d(TAG, "[IN]setWriteCts:" + enabled);
		mWriteCtsIsEnable = enabled;
		return true;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		Log.d(TAG, "[IN]onBind");

		mBluetoothManager = (BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
		mBluetoothAdapter = mBluetoothManager.getAdapter();

		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(mReceiver, filter);

		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.d(TAG, "[IN]onUnbind");
		//Nothing to do
		//When onUnbind is overridden with return true, onRebind will be called at next bind
        unregisterReceiver(mReceiver);
		return false;
	}

	public void setCurrentContext(Context c, IBleListener listener) {
		Log.d(TAG, "[IN]setCurrentContext");
		mAppListener = listener;
	}

	public void BleScan(UUID[] uuids) {
		Log.d(TAG, "[IN]BleScan");
		Message msg = new Message();
		msg.what = MSG_SCAN_START;
		msg.obj  = uuids;
		mHandler.sendMessage(msg);
	}

	public void BleScanOff() {
		Log.d(TAG, "[IN]BleScanOff");
		Message msg = new Message();
		msg.what = MSG_SCAN_STOP;
		mHandler.sendMessage(msg);
	}

	public boolean BleIsConnected() {
		return mIsACLConnected && mIsBonded;
	}

	public void BleConnectDev(Object object) {
		Log.d(TAG, "[IN]BleConnectDev");
		Message msg = new Message();
		msg.what = MSG_CONNECT;
		msg.obj  = object;
		mHandler.sendMessage(msg);
	}

	public String BleGetLocalName() {
		String localName = new String();
		if (mBluetoothDevice != null){
			localName = mBluetoothDevice.getName();
		}
		return localName;
	}

	public String BleGetAddress() {
		String addr = new String();
		if (mBluetoothDevice != null){
			addr = mBluetoothDevice.getAddress();
		}
		return addr;
	}

	public boolean BleSendmsg(byte[] data) {
		Log.d(TAG, "[IN]BleSendmsg");
		return true;
	}

	public void BleDisconnect() {
		Log.d(TAG, "[IN]BleDisconnect");
		if (mBluetoothGatt != null){
			Message msg = new Message();
			msg.what=MSG_DISCONNECT;
			mHandler.sendMessage(msg);
		}
	}


	private BluetoothAdapter.LeScanCallback mLeScanCallback =
		new BluetoothAdapter.LeScanCallback() {
			@Override
			public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
				try{
					Log.d(TAG, "[IN]onLeScan");
					Log.d(TAG, "[ADDR]"+device.getAddress());
					Log.d(TAG, "[DEV NAME]"+device.getName());

					mAppListener.BleAdvCatchDevice(device);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			BluetoothDevice device;

			switch(msg.what){
			case MSG_NOTIFY_ACL_DISCONNECTED:
				device = (BluetoothDevice)msg.obj;
				if (mBluetoothDevice != null && mBluetoothDevice.getAddress().equals(device.getAddress())){
					Log.i(TAG, "[LOG]ACL_DISCONNECTED");
					mIsACLConnected = false;
					releaseConnection();
				}
				break;

			case MSG_NOTIFY_ACL_CONNECTED:
				device = (BluetoothDevice)msg.obj;
				if (mBluetoothDevice != null && mBluetoothDevice.getAddress().equals(device.getAddress())){
					mIsACLConnected = true;
					Log.i(TAG, "[LOG]ACL_CONNECTED");
					Log.d(TAG, "[LOG]Bond state = " + String.format("%d", device.getBondState()));
				}
				break;

			case MSG_NOTIFY_BOND_NONE:
				device = (BluetoothDevice)msg.obj;
				if (mBluetoothDevice != null && mBluetoothDevice.getAddress().equals(device.getAddress())){
					Log.i(TAG, "[LOG]Bond state = NONE");
					mIsBonded = false;
				}
				break;

			case MSG_NOTIFY_BOND_BONDED:
				device = (BluetoothDevice)msg.obj;
				if (mBluetoothDevice != null && mBluetoothDevice.getAddress().equals(device.getAddress())){
					Log.i(TAG, "[LOG]Bond state = BONDED");
					mIsBonded = true;

					// Notify connection state to Activity
					if (mIsACLConnected){
						Log.i(LOG_TAG, "[LOG_OUT]CONNECT");
						try {
							mAppListener.BleConnected();
						} catch (RemoteException e) {
							e.printStackTrace();
						}
					}

					// Add to Request Queue
					while (true){
						BleRequest req = mBleReqQueueAfterAuth.poll();
						if ( req == null ){
							break;
						}
						bleRequest(req.type, req.o);
					}
				}
				break;

			case MSG_CONNECT:
				if (mBluetoothGatt != null){
					mBluetoothGatt.disconnect();
					mBluetoothGatt.close();
				}
				mBluetoothDevice = (BluetoothDevice)msg.obj;
				mBluetoothGatt = mBluetoothDevice.connectGatt(BleService.this, false, mGattCallback);
				break;

			case MSG_DISCONNECT:
				Log.d(TAG, "mBluetoothGatt.disconnect()");
				mBluetoothGatt.disconnect();

				if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()){
					Log.i(TAG, "Bluetooth is disable now.");
					releaseConnection();
				}
				break;

			case MSG_SCAN_START:
				UUID[] uuids = (UUID[])msg.obj;
				mBluetoothGatt = null;
				mBluetoothDevice = null;
				bleReq_QueueClear();
				mBleReqQueueAfterAuth.clear();
				if (mBleReqTimer != null){
					mBleReqTimer.cancel();
				}
				mBleReqTimer = null;

				if (mBluetoothAdapter != null){
					mBluetoothAdapter.stopLeScan(mLeScanCallback);

					Log.d(TAG,"[IN]mBluetoothAdapter:" + mBluetoothAdapter);
					Log.d(TAG,"[CALL]startLeScan(mLeScanCallback)");
					if (uuids == null){
						mBluetoothAdapter.startLeScan(mLeScanCallback);
					} else {
						mBluetoothAdapter.startLeScan(uuids, mLeScanCallback);
					}
				} else {
					Log.d(TAG, "[LOG]mBluetoothAdapter = null");
				}
				break;

			case MSG_SCAN_STOP:
				mBluetoothAdapter.stopLeScan(mLeScanCallback);
				break;

			case MSG_REQ_TIMEOUT:
				mBleReqTimer.cancel();
				mBleReqTimer = null;
				if (mBleReqRetryCount < BLE_REQ_RETRY_MAX){
					Log.d(TAG, "bleReq retry.");
					BleRequest req = mBleReqQueue.peek();
					bleReq_QueueExec(req);
					mBleReqRetryCount++;
				} else {
					Log.d(TAG, "bleReq retry ... NG.");
					bleReq_QueueDelRequest();

					// execute next one
					bleReq_QueueExec();
				}
			}
		}
	};

	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			Log.d(TAG, "[CALLBACK]onConnectionStateChange(): " + String.format("status=0x%02X, newState=0x%02X", status, newState));

			if (newState == BluetoothProfile.STATE_CONNECTED) {
				Log.d(TAG, "Connected to GATT.");

				// Service discovering
				// Attempts to discover services after successful connection.
				mIsDiscoveredService = mBluetoothGatt.discoverServices();
				Log.d(TAG, "Attempting to start service discovery:" + mIsDiscoveredService);
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				Log.d(TAG, "Disconnected from GATT.");
				if (mBluetoothGatt != null){
					 mBluetoothGatt.close();
				}

				if (!mBluetoothAdapter.isEnabled()){
					Log.d(TAG, "Bluetooth is disable now.");
					releaseConnection();
				}
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			Log.d(TAG, "[IN]onServicesDiscovered: " + String.format("Status=0x%02X", status));
			boolean ret;

			if (status == BluetoothGatt.GATT_SUCCESS) {
				BluetoothGattService serv;
				List<BluetoothGattService> services = gatt.getServices();

				int i,j;
				for (i = 0; i < services.size(); i++){
					serv = services.get(i);

					// Blood Pressure Service is discovered
					if (serv.getUuid().equals(Blood_Pressure_SERVICE_UUID)) {
						Log.i(TAG, "[LOG]Blood Pressure Service is discovered");

						List<BluetoothGattCharacteristic> chars = serv.getCharacteristics();
						for (j = 0; j < chars.size(); j++){
							BluetoothGattCharacteristic characteristic = chars.get(j);
							if (characteristic == null){
								continue;
							}

							if (characteristic.getUuid().equals(Blood_Pressure_Feature_CHAR_UUID)) {
								if (mIsBonded){
									ret = bleRequest(BleRequest.TYPE_CHAR_READ, characteristic);
								} else {
									ret = bleRequestAfterAuth(BleRequest.TYPE_CHAR_READ, characteristic);
								}
								if (!ret){
									Log.e(TAG, "[LOG]mBluetoothGatt.readCharacteristic ret="+ret);
								}
							} else if (characteristic.getUuid().equals(Blood_Pressure_Measurement_CHAR_UUID)) {
								ret = gatt.setCharacteristicNotification(characteristic, true);
								if (!ret){
									continue;
								}

								Log.i(TAG, "[LOG]Blood_Pressure_Measurement:characteristic.getDescriptor");

								// Set enable Indication value to the descriptor
								BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CCCD);
								descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
								if (mIsBonded){
									ret = bleRequest(BleRequest.TYPE_DESC_WRITE, descriptor);
								} else {
									ret = bleRequestAfterAuth(BleRequest.TYPE_DESC_WRITE, descriptor);
								}
								if (!ret){
									Log.e(TAG, "[LOG]writeDescriptor="+ret);
								}
							}
						}
					}

					// When Weight Scale Service is discovered
					if (serv.getUuid().equals(Weight_Scale_SERVICE_UUID)) {
						Log.i(TAG, "[LOG]Weight Scale Service is discovered");

						List<BluetoothGattCharacteristic> chars = serv.getCharacteristics();
						for (j = 0; j < chars.size(); j++){
							BluetoothGattCharacteristic characteristic = chars.get(j);

							if (characteristic == null){
								continue;
							}

							if (characteristic.getUuid().equals(Weight_Scale_Feature_CHAR_UUID)) {
								if (mIsBonded){
									ret = bleRequest(BleRequest.TYPE_CHAR_READ, characteristic);
								} else {
									ret = bleRequestAfterAuth(BleRequest.TYPE_CHAR_READ, characteristic);
								}
								if (!ret){
									Log.e(TAG, "[LOG]mBluetoothGatt.readCharacteristic ret="+ret);
								}
							} else if (characteristic.getUuid().equals(Weight_Measurement_CHAR_UUID)) {
								ret = gatt.setCharacteristicNotification(characteristic, true);
								if (!ret){
									continue;
								}

								Log.i(TAG, "[LOG]Weight_Measurement:characteristic.getDescriptor");

								// Set enable Indication value to the descriptor
								BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CCCD);
								descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
								if (mIsBonded){
									ret = bleRequest(BleRequest.TYPE_DESC_WRITE, descriptor);
								} else {
									ret = bleRequestAfterAuth(BleRequest.TYPE_DESC_WRITE, descriptor);
								}
								if (!ret){
									Log.i(TAG, "[LOG]writeDescriptor="+ret);
								}
							}
						}
					}

					// When Battery Service is discovered
					if (serv.getUuid().equals(Battery_SERVICE_UUID)) {
						Log.i(TAG, "[LOG]Battery Service is discovered");

						BluetoothGattCharacteristic characteristic = serv.getCharacteristic(Battery_Serv_CHAR_UUID);
						if (characteristic == null){
							continue;
						}

						ret = gatt.setCharacteristicNotification(characteristic, true);
						if (!ret){
							continue;
						}

						Log.i(TAG, "[LOG]Battery Service:characteristic.getDescriptor");

						// Set enable Indication value to the descriptor
						BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CCCD);
						descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
						if (mIsBonded){
							ret = bleRequest(BleRequest.TYPE_DESC_WRITE, descriptor);
						} else {
							ret = bleRequestAfterAuth(BleRequest.TYPE_DESC_WRITE, descriptor);
						}
						if (!ret){
							Log.i(TAG, "[LOG]writeDescriptor="+ret);
						}
					}

					// Current Time Service is discovered
					if (serv.getUuid().equals(Current_Time_SERVICE_UUID)) {
						Log.i(TAG, "[LOG]Current Time Service is discovered");

						BluetoothGattCharacteristic characteristic = serv.getCharacteristic(Current_Time_Serv_CHAR_UUID);
						if (characteristic == null){
							continue;
						}

						ret = gatt.setCharacteristicNotification(characteristic, true);
						if (!ret){
							continue;
						}
						Log.i(TAG, "[LOG]Current Time Service:characteristic.getDescriptor");

						// Set enable Indication value to the descriptor
						BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CCCD);
						descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
						if (mIsBonded){
							ret = bleRequest(BleRequest.TYPE_DESC_WRITE, descriptor);
						} else {
							ret = bleRequestAfterAuth(BleRequest.TYPE_DESC_WRITE, descriptor);
						}
						if (!ret){
							Log.i(TAG, "[LOG]writeDescriptor="+ret);
						}
					}
				}
			}
		}

		@Override
		public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			Log.d(TAG, "[IN]onDescriptorRead: " + String.format("Status=0x%02X", status));

			if (status == BluetoothGatt.GATT_SUCCESS){
				if (bleReq_QueueConfirmRsp(BleRequest.TYPE_DESC_READ, descriptor)){
					bleReq_QueueExec();
				}
			} else {
				Log.e(TAG, "[LOG]onDescriptorRead: " + String.format("Status=0x%02X", status));
			}
		}

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			Log.d(TAG, "[IN]onDescriptorWrite: " + String.format("Status=0x%02X", status));

			if (status == BluetoothGatt.GATT_SUCCESS){

				if (bleReq_QueueConfirmRsp(BleRequest.TYPE_DESC_WRITE, descriptor)){
					bleReq_QueueExec();
				}

				Log.i(TAG, "[LOG]BluetoothGatt.GATT_SUCCESS");
				if (descriptor.getUuid().equals(CCCD)){
					Log.i(TAG, "[LOG]characteristic="+descriptor.getCharacteristic().getUuid());
					if (descriptor.getCharacteristic().getUuid().equals(Blood_Pressure_Measurement_CHAR_UUID)){
						Log.i(TAG, "[LOG]Blood Pressure Service connected");
						mIsConnected = true;
						mCtsNeedToWrite = true;
					} else if (descriptor.getCharacteristic().getUuid().equals(Weight_Measurement_CHAR_UUID)){
						Log.i(TAG, "[LOG]WSS connected");
						mIsConnected = true;
						mCtsNeedToWrite = true;
					} else if (descriptor.getCharacteristic().getUuid().equals(Battery_Serv_CHAR_UUID)){
						Log.i(TAG, "[LOG]Battery Service connected");
					} else if (descriptor.getCharacteristic().getUuid().equals(Current_Time_Serv_CHAR_UUID)){
						Log.i(TAG, "[LOG]CTS connected");
					}
				} else {
					Log.i(TAG, "[LOG]Not CCCD");
				}
			} else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION){
				Log.e(TAG, "[LOG]onDescriptorWrite: Status=GATT_INSUFFICIENT_AUTHENTICATION");
				mBleReqQueueAfterAuth.offer(mBleReqQueue.peek());
				bleReq_QueueDelRequest();
				bleReq_QueueExec();
			} else {
				Log.e(TAG, "[LOG]onDescriptorWrite: " + String.format("Status=0x%02X", status));
			}
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			Log.d(TAG, "[IN]onCharacteristicWrite: " + String.format("Status=0x%02X", status));

			if (status == BluetoothGatt.GATT_SUCCESS){
				if (bleReq_QueueConfirmRsp(BleRequest.TYPE_CHAR_WRITE, characteristic)){
					bleReq_QueueExec();
				}
			} else if (status == 0x80){ // 0x80: Write Request Rejected
				Log.e(TAG, "[LOG]onCharacteristicWrite: " + String.format("Status=0x%02X", status));

				// If the slave sends error response in CTS,
				// you don't retry and should send next request.
				if (characteristic.getUuid().equals(Current_Time_Serv_CHAR_UUID)){
					if (bleReq_QueueConfirmRsp(BleRequest.TYPE_CHAR_WRITE, characteristic)){
						bleReq_QueueExec();
					}
				}
			} else {
				Log.e(TAG, "[LOG]onCharacteristicWrite: " + String.format("Status=0x%02X", status));
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			Log.d(TAG, "[IN]onCharacteristicRead: " + String.format("Status=0x%02X", status));

			if (status == BluetoothGatt.GATT_SUCCESS){
				if (bleReq_QueueConfirmRsp(BleRequest.TYPE_CHAR_READ, characteristic)){
					bleReq_QueueExec();
				}
			}
			else {
				Log.e(TAG, "[LOG]onCharacteristicRead: UUID=" + characteristic.getUuid().toString() + " Status=" + status);
				return;
			}

			if (characteristic.getUuid().equals(Blood_Pressure_Feature_CHAR_UUID)) {
				try {
					byte[] data = characteristic.getValue();
					String str = "";
					mAppListener.BleBpfDataRecv(data);
					for (byte cnt = 0; cnt < data.length; cnt++){
						str += String.format("%02x"+",", data[data.length - 1 - cnt]);
					}
					Log.i(LOG_TAG, "[LOG_OUT]BPF Recv Data:"+str);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			} else if (characteristic.getUuid().equals(Weight_Scale_Feature_CHAR_UUID)) {
				try {
					byte[] data = characteristic.getValue();
					String str = "";
					mAppListener.BleWsfDataRecv(data);
					for (byte cnt = 0; cnt < data.length; cnt++){
						str += String.format("%02x"+",", data[data.length - 1 - cnt]);
					}
					Log.i(LOG_TAG, "[LOG_OUT]WSF Recv Data:"+str);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			} else {
				byte[] data = characteristic.getValue();
				String str = "";
				for (byte cnt = 0; cnt < data.length; cnt++){
					str += String.format("%02x"+",", data[data.length - 1 - cnt]);
				}
				Log.i(LOG_TAG, "[LOG_OUT]Recv Data:"+str);
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			Log.d(TAG, "[IN]onCharacteristicChanged "+characteristic.getUuid());

			try {
				String str = "";
				if (characteristic.getUuid().equals(Blood_Pressure_Measurement_CHAR_UUID)) {
					byte[] data = characteristic.getValue();
					mAppListener.BleBpmDataRecv(data);
					for (byte cnt = 0; cnt < data.length; cnt++){
						str += String.format("%02x"+",", data[cnt]);
					}
					Log.i(LOG_TAG, "[LOG_OUT]BPM Recv Data:"+str);

				} else if (characteristic.getUuid().equals(Weight_Measurement_CHAR_UUID)) {
					Log.d(TAG, "[IN]WSS characteristic received");
					byte[] data = characteristic.getValue();
					mAppListener.BleWmDataRecv(data);

					for (byte cnt = 0; cnt < data.length; cnt++){
						str += String.format("%02x"+",", data[cnt]);
					}
					Log.i(LOG_TAG, "[LOG_OUT]WSS Recv Data:"+str);

				} else if (characteristic.getUuid().equals(Battery_Serv_CHAR_UUID)) {
					Log.d(TAG, "[IN]Battery Service characteristic received");
					byte[] batterydata = characteristic.getValue();
					mAppListener.BleBatteryDataRecv(batterydata);

					for (byte cnt = 0; cnt < batterydata.length; cnt++){
						str += String.format("%02x"+",", batterydata[cnt]);
					}
					Log.i(LOG_TAG, "[LOG_OUT]Battery Service Recv Data:"+str);

				} else if (characteristic.getUuid().equals(Current_Time_Serv_CHAR_UUID)){
					Log.d(TAG, "[IN]CTS characteristic received");
					byte[] ctsdata = characteristic.getValue();
					mAppListener.BleCtsDataRecv(ctsdata);

					for (byte cnt = 0; cnt < ctsdata.length; cnt++){
						str += String.format("%02x"+",", ctsdata[cnt]);
					}
					Log.i(LOG_TAG, "[LOG_OUT]CTS Recv Data:"+str);

					if (mCtsNeedToWrite) {
						// Write time
						Log.d(TAG, "Write CTS");

						byte[] data = new byte[10];
						Calendar cal = Calendar.getInstance();
						int year = cal.get(Calendar.YEAR);
						data[0] = (byte)year;
						data[1] = (byte)((year >> 8) & 0xFF);
						data[2] = (byte)(cal.get(Calendar.MONTH) + 1);
						data[3] = (byte)cal.get(Calendar.DAY_OF_MONTH);
						data[4] = (byte)cal.get(Calendar.HOUR_OF_DAY);
						data[5] = (byte)cal.get(Calendar.MINUTE);
						data[6] = (byte)cal.get(Calendar.SECOND);
						data[7] = (byte)((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1); // Rotate
						data[8] = (byte)(cal.get(Calendar.MILLISECOND)*256/1000); // Fractions256
						data[9] = 0x01; // Adjust Reason: Manual time update

						BluetoothGattCharacteristic characteristicW = characteristic;
						boolean ret = characteristicW.setValue(data);

						if (ret){
							String date = year
									+ "/" + data[2]
									+ "/" + data[3]
									+ " " + String.format("%1$02d", data[4])
									+ ":" + String.format("%1$02d", data[5])
									+ ":" + String.format("%1$02d", data[6])
									+ " (WeekOfDay:" + data[7]
									+ " Fractions256:" + data[8]
									+ " AdjustReason:" + data[9] + ")";

							str = "";
							for (byte cnt = 0; cnt < characteristicW.getValue().length; cnt++){
								str += String.format("%02x"+",", characteristicW.getValue()[cnt]);
							}

							if (mWriteCtsIsEnable) {
								boolean retval = bleRequest(BleRequest.TYPE_CHAR_WRITE, characteristicW);
								Log.i(LOG_TAG, "[LOG_OUT]CTS Tx Time(ret="+retval+"):"+date);
								Log.i(LOG_TAG, "[LOG_OUT]CTS Tx Data(ret="+retval+"):"+str);
							} else {
								Log.i(LOG_TAG, "[LOG_OUT]CTS Tx Time(No write):"+date);
								Log.i(LOG_TAG, "[LOG_OUT]CTS Tx Data(No write):"+str);
							}
						} else {
							Log.e(TAG, "[LOG]CTS Data set Fail:");
						}
						mCtsNeedToWrite = false;
					}
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
			Log.d(TAG, "[IN]onReliableWriteCompleted: " + String.format("Status=0x%02X", status));
		}
	};

	public class MyServiceLocalBinder extends Binder {
		BleService getService(){
			return BleService.this;
		}
	}

	private boolean sendMessage(int what, Object obj) {
		Message message = new Message();
		message.what = what;
		message.obj  = obj;
		boolean ret = mHandler.sendMessage(message);
		if (!ret){
			Log.e(TAG, "[LOG]Handler.sendMessage() error (" + String.format("%d", message.what) + ")");
		}
		return ret;
	}

	private void releaseConnection() {
		Log.i(LOG_TAG, "[LOG_OUT]DISCONNECT");
		mIsACLConnected = false;
		mIsConnected = false;
		mIsBonded = false;
		mIsDiscoveredService = false;
		mCtsNeedToWrite = false;

		try {
			Log.d(TAG, "[LOG]mAppListener.BleDisConnected()");
			mAppListener.BleDisConnected();

			if (mBluetoothGatt != null){
				 mBluetoothGatt.close();
			}

			mBluetoothGatt = null;
			mBluetoothDevice = null;
			bleReq_QueueClear();
			mBleReqQueueAfterAuth.clear();
			if (mBleReqTimer != null){
				mBleReqTimer.cancel();
			}
			mBleReqTimer = null;

			if (mBluetoothAdapter != null){
				mBluetoothAdapter.stopLeScan(mLeScanCallback);
			} else {
				Log.d(TAG, "[LOG]mBluetoothAdapter = null");
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "[IN]BondReceiver onReceive: " + intent.getAction());

			if (intent.getAction().equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
				int prev_bond_state = intent.getExtras().getInt(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE);
				int bond_state = intent.getExtras().getInt(BluetoothDevice.EXTRA_BOND_STATE);
				Log.i(TAG, "[LOG]ACTION_BOND_STATE_CHANGED: " + String.format("bond_state prev=%d, now=%d", prev_bond_state, bond_state));
				if ( (prev_bond_state==BluetoothDevice.BOND_BONDING) && (bond_state==BluetoothDevice.BOND_BONDED)){
					Log.d(TAG, "[LOG](prev_bond_state==BluetoothDevice.BOND_BONDING)&&(bond_state==BluetoothDevice.BOND_BONDED)");
					Log.d(TAG, "[LOG]not Pairing Device!!!");
					BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					sendMessage(MSG_NOTIFY_BOND_BONDED, device);
				}

			} else if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)){
				Log.d(TAG, "[LOG]ACTION_ACL_DISCONNECTED");
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				sendMessage(MSG_NOTIFY_ACL_DISCONNECTED, device);

			} else if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_CONNECTED)){
				Log.d(TAG, "[LOG]ACTION_ACL_CONNECTED");
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				sendMessage(MSG_NOTIFY_ACL_CONNECTED, device);
				if (device.getBondState() == BluetoothDevice.BOND_BONDED){
					sendMessage(MSG_NOTIFY_BOND_BONDED, device);
				} else {
					sendMessage(MSG_NOTIFY_BOND_NONE, device);
				}

			} else if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
				Log.d(TAG, "[LOG]ACTION_STATE_CHANGED");
				int state = intent.getExtras().getInt(BluetoothAdapter.EXTRA_STATE);
				if ((state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF)
						&& (mIsConnected || mIsACLConnected)){
					sendMessage(MSG_NOTIFY_ACL_DISCONNECTED, null);
				}
			}
		}
	};

	private boolean bleRequest(int reqType, Object o) {
		Log.d(TAG, "[IN]bleRequest");

		BleRequest req = new BleRequest();
		req.type = reqType;
		req.o = o;
		req.date = new Date();
		bleReq_QueueAdd(req);

		bleReq_QueueExec(); // If any request isn't queued, immediately this request is executed.
		return true;
	}

	private boolean bleRequestAfterAuth(int reqType, Object o) {
		Log.d(TAG, "[IN]bleRequestAfterAuth");

		BleRequest req = new BleRequest();
		req.type = reqType;
		req.o = o;
		req.date = new Date();
		mBleReqQueueAfterAuth.offer(req);
		return true;
	}

	private boolean bleReq_QueueAdd(BleRequest req) {
		mBleReqQueue.offer(req); // add
		Log.d(TAG, "[LOG]add queue - type:" + req.type + " num:" + mBleReqQueue.size());
		return true;
	}

	private boolean bleReq_QueueDelRequest() {
		BleRequest req = mBleReqQueue.remove(); // del
		mBleReqExecuting = false;
		mBleReqRetryCount = 0;
		Log.d(TAG, "[LOG]del queue - type:" + req.type + " num:" + mBleReqQueue.size());
		return true;
	}

	private boolean bleReq_QueueExec() {
		Log.d(TAG, "[IN]bleReq_QueueExec");

		if (mBleReqQueue.isEmpty()){
			Log.d(TAG, "[LOG]bleReq_Queue is empty.");
			return false;
		}

		if (mBleReqExecuting){
			Log.d(TAG, "[LOG]Other request is executed.");
			return false;
		}

		if (mBluetoothGatt == null){
			Log.d(TAG, "[LOG]mBluetoothGatt == null.");
			return false;
		}

		BleRequest req = mBleReqQueue.peek(); // get
		if (req.type <= BleRequest.TYPE_NONE
				|| req.type >= BleRequest.TYPE_MAX){
			Log.d(TAG, "[LOG]Unknown reqType.");
			return false;
		}

		bleReq_QueueExec(req);
		mBleReqRetryCount = 0;
		return true;
	}

	private boolean bleReq_QueueExec(BleRequest req) {
		Log.d(TAG, "[IN]bleReq_QueueExec(BleRequest)");

		if (mBluetoothGatt == null){
			Log.d(TAG, "[LOG]mBluetoothGatt == null.");
			return false;
		}

		switch (req.type){
		case BleRequest.TYPE_DESC_WRITE:
			Log.d(TAG, "[LOG]exec queue: DESC_WRITE");
			BluetoothGattDescriptor desc_w = (BluetoothGattDescriptor) req.o;
			mBluetoothGatt.writeDescriptor(desc_w);
			break;
		case BleRequest.TYPE_DESC_READ:
			Log.d(TAG, "[LOG]exec queue: DESC_READ");
			BluetoothGattDescriptor desc_r = (BluetoothGattDescriptor) req.o;
			mBluetoothGatt.readDescriptor(desc_r);
			break;
		case BleRequest.TYPE_CHAR_WRITE:
			Log.d(TAG, "[LOG]exec queue: CHAR_WRITE");
			BluetoothGattCharacteristic char_w = (BluetoothGattCharacteristic) req.o;
			mBluetoothGatt.writeCharacteristic(char_w);
			break;
		case BleRequest.TYPE_CHAR_READ:
			Log.d(TAG, "[LOG]exec queue: CHAR_READ");
			BluetoothGattCharacteristic char_r = (BluetoothGattCharacteristic) req.o;
			mBluetoothGatt.readCharacteristic(char_r);
			break;
		default:
			break;
		}

		mBleReqExecuting = true;

		// request timeout timer
		mBleReqTimer = new Timer();
		mBleReqTimer.schedule(new TimerTask(){
			@Override
			public void run() {
				Log.d(TAG, "[IN]Timer.run");
				if (mBleReqExecuting){
					Message message = new Message();
					message.what = MSG_REQ_TIMEOUT;
					mHandler.sendMessage(message);
					if (mBleReqTimer != null){
						 mBleReqTimer.cancel();
					}
				}
			}
		}, BLE_REQ_TIMEOUT_MS); // oneshot

		return true;
	}

	private boolean bleReq_QueueConfirmRsp(int rspType, Object rsp) {
		Log.d(TAG, "[IN]bleReq_QueueConfirmRsp");

		boolean ret = false;

		if (mBleReqQueue.isEmpty()){
			Log.d(TAG, "[LOG]bleReq_Queue is empty.");
			return false;
		}

		if (!mBleReqExecuting){
			Log.d(TAG, "[LOG]not request.");
			return false;
		}

		BleRequest req = mBleReqQueue.peek(); // get
		if (req.type != rspType){
			Log.d(TAG, "[LOG]reqType don't match.");
			return false;
		}

		switch (req.type){
		case BleRequest.TYPE_DESC_WRITE:
		case BleRequest.TYPE_DESC_READ:
			Log.d(TAG, "[LOG]confirm rsp: DESC_READ/WRITE");
			BluetoothGattDescriptor d_req = (BluetoothGattDescriptor) req.o;
			BluetoothGattDescriptor d_rsp = (BluetoothGattDescriptor) rsp;
			if (d_req.getUuid().equals(d_rsp.getUuid()) &&
				d_req.getCharacteristic().getUuid().equals(d_rsp.getCharacteristic().getUuid())){
				ret = true;
			}
			break;
		case BleRequest.TYPE_CHAR_WRITE:
		case BleRequest.TYPE_CHAR_READ:
			Log.d(TAG, "[LOG]confirm rsp: CHAR_READ/WRITE");
			BluetoothGattCharacteristic c_req = (BluetoothGattCharacteristic) req.o;
			BluetoothGattCharacteristic c_rsp = (BluetoothGattCharacteristic) rsp;
			if (c_req.getUuid().equals(c_rsp.getUuid())){
				ret = true;
			}
			break;
		case BleRequest.TYPE_NONE:
		default:
			break;
		}

		if (ret == true){
			if (mBleReqTimer != null){
				mBleReqTimer.cancel();
			}
			mBleReqTimer = null;
			bleReq_QueueDelRequest();
		}

		return ret;
	}

	private void bleReq_QueueClear() {
		Log.d(TAG, "[IN]bleReq_QueueClear");
		mBleReqQueue.clear();
		mBleReqExecuting = false;
		mBleReqRetryCount = 0;
	}
}
