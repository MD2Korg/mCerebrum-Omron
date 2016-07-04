// BleListener.aidl.aidl
package org.md2k.omron;

// Declare any non-default types here with import statements

import android.bluetooth.BluetoothDevice;

interface IBleListener {
	void BleAdvCatch();
	void BleConnected();
	void BleDisConnected();
	void BleWmDataRecv(in byte[] data);
	void BleBatteryDataRecv(in byte[] data);
	void BleCtsDataRecv(in byte[] data);
	void BleBpmDataRecv(in byte[] data);
	void BleBpfDataRecv(in byte[] data);
	void BleAdvCatchDevice(in BluetoothDevice dev);
	void BleWsfDataRecv(in byte[] data);
}
