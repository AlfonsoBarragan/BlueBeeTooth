package es.esi.techlab.bluebeetoothmodule;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

import es.esi.techlab.bluebeetoothmodule.listenters.NotifyListener;

/**
 * <code>BluetoothIO</code> is the main class in order to make and receive the bluetooth communications
 * from the smartphone to the bluetooth device. This class extends from the default
 * <code>BluetoothGattCallback</code> class.
 *
 * @see BluetoothGattCallback
 * @see BluetoothGatt
 */
public class BluetoothIO extends BluetoothGattCallback {

    private static final String TAG = "BluetoothIO";

    public static final int ERROR_CONNECTION_FAILED = 1;
    public static final int ERROR_READ_RSSI_FAILED = 2;

    private BluetoothGatt bluetoothGatt;

    private final BluetoothListener bluetoothListener;
    private final HashMap<UUID, NotifyListener> notifyListeners;


    /**
     * Constructor
     *
     * @param listener Callback listener
     */
    public BluetoothIO(BluetoothListener listener) {

        bluetoothListener = listener;
        notifyListeners = new HashMap<>();
    }

    /**
     * Connects to the Bluetooth device
     *
     * @param context Context
     * @param device  Device to connect
     */
    public void connect(Context context, BluetoothDevice device) {
        device.connectGatt(context, true, this);
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);

        if (newState == BluetoothProfile.STATE_CONNECTED) {
            gatt.discoverServices();
            Log.i("CONNECTION", "Connected");

        }else {
            gatt.close();
            if (bluetoothListener != null) {
                bluetoothListener.onDisconnected();
            }
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            bluetoothGatt = gatt;
            checkAvailableServices();
            if (bluetoothListener != null) {
                bluetoothListener.onConnectionEstablished();
            }
        } else {
            notifyWithFail(ERROR_CONNECTION_FAILED, "onServicesDiscovered fail: " + String.valueOf(status));
        }
    }

    /**
     * Checks connection state.
     *
     * @throws IllegalStateException if device is not connected
     */
    public void checkConnectionState() throws IllegalStateException {
        if (bluetoothGatt == null) {
            Log.e(TAG, "Connect device first");
            throw new IllegalStateException("Device is not connected");
        }
    }

    /**
     * Writes data to the service
     *
     * @param serviceUUID      Service UUID
     * @param characteristicId Characteristic UUID
     * @param value            Value to write
     */
    public void writeCharacteristic(UUID serviceUUID, UUID characteristicId, byte[] value) {

        checkConnectionState();

        BluetoothGattService service = bluetoothGatt.getService(serviceUUID);
        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicId);
            if (characteristic != null) {
                Log.d("WRITE_CHAR", String.valueOf(characteristic.getUuid()));
                characteristic.setValue(value);
                if (!bluetoothGatt.writeCharacteristic(characteristic)) {
                    notifyWithFail(serviceUUID, characteristicId, "BluetoothGatt write operation failed");
                }
            } else {
                notifyWithFail(serviceUUID, characteristicId, "BluetoothGattCharacteristic " + characteristicId + " does not exist");
            }
        } else {
            notifyWithFail(serviceUUID, characteristicId, "BluetoothGattService " + serviceUUID + " does not exist");
        }
    }

    /**
     * Reads data from the service
     *
     * @param serviceUUID      Service UUID
     * @param characteristicId Characteristic UUID
     */
    public void readCharacteristic(UUID serviceUUID, UUID characteristicId) {

        checkConnectionState();

        BluetoothGattService service = bluetoothGatt.getService(serviceUUID);
        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicId);
            if (characteristic != null) {
                if (!bluetoothGatt.readCharacteristic(characteristic)) {
                    notifyWithFail(serviceUUID, characteristicId, "BluetoothGatt read operation failed");
                }
            } else {
                notifyWithFail(serviceUUID, characteristicId, "BluetoothGattCharacteristic " + characteristicId + " does not exist");
            }
        } else {
            notifyWithFail(serviceUUID, characteristicId, "BluetoothGattService " + serviceUUID + " does not exist");
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        Log.d("CHANGED_CHAR - Char: ", String.valueOf(characteristic.getUuid()) + " - value: " + Arrays.toString(characteristic.getValue()));
        // Notify the change...
        if (notifyListeners.containsKey(characteristic.getUuid())) {
            notifyListeners.get(characteristic.getUuid()).onNotify(characteristic.getValue());
            notifyWithResult(characteristic);
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        if (BluetoothGatt.GATT_SUCCESS == status) {
            Log.d("WRITE_CHAR", "Characteristic: " + String.valueOf(characteristic.getUuid() + " - Status: " + String.valueOf(status)));
            notifyWithResult(characteristic);
        } else {
            UUID serviceId = characteristic.getService().getUuid();
            UUID characteristicId = characteristic.getUuid();
            notifyWithFail(serviceId, characteristicId, "onCharacteristicWrite fail");
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        if (BluetoothGatt.GATT_SUCCESS == status) {
            Log.d(TAG, "READ_CHAR - Char: " + String.valueOf(characteristic.getUuid()) + " - value: " + Arrays.toString(characteristic.getValue()));
            notifyWithResult(characteristic);
        } else {
            UUID serviceId = characteristic.getService().getUuid();
            UUID characteristicId = characteristic.getUuid();
            notifyWithFail(serviceId, characteristicId, "onCharacteristicRead fail");
        }
    }

    /**
     * Notifies with success result
     *
     * @param data Result data
     */
    public void notifyWithResult(BluetoothGattCharacteristic data) {
        if (bluetoothListener != null && data != null) {
            try {
                bluetoothListener.onResult(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets remote connected device
     *
     * @return Connected device or null
     */
    public BluetoothDevice getConnectedDevice() {
        BluetoothDevice connectedDevice = null;
        if (bluetoothGatt != null) {
            connectedDevice = bluetoothGatt.getDevice();
        }
        return connectedDevice;
    }

    /**
     * Notifies with failed result
     *
     * @param serviceUUID      Service UUID
     * @param characteristicId Characteristic ID
     * @param msg              Message
     */
    public void notifyWithFail(UUID serviceUUID, UUID characteristicId, String msg) {
        if (bluetoothListener != null) {
            bluetoothListener.onFail(serviceUUID, characteristicId, msg);
        }
    }

    /**
     * Notifies with failed result
     *
     * @param errorCode Error code
     * @param msg       Message
     */
    public void notifyWithFail(int errorCode, String msg) {
        if (bluetoothListener != null) {
            bluetoothListener.onFail(errorCode, msg);
        }
    }

    /**
     * Checks available services, characteristics and descriptors
     */
    private void checkAvailableServices() {
        for (BluetoothGattService service : bluetoothGatt.getServices()) {
            Log.i(TAG, "onServicesDiscovered:" + service.getUuid());

            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                Log.i(TAG, "  char:" + characteristic.getUuid());

                for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                    Log.i(TAG, "    descriptor:" + descriptor.getUuid());
                }
            }
        }
    }
    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    public BluetoothListener getBluetoothListener() {
        return bluetoothListener;
    }

    public HashMap<UUID, NotifyListener> getNotifyListeners() {
        return notifyListeners;
    }
}
