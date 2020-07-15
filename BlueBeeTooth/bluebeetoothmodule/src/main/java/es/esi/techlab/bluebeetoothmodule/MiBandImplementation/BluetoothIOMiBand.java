package es.esi.techlab.bluebeetoothmodule.MiBandImplementation;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.util.Arrays;
import java.util.UUID;

import es.esi.techlab.bluebeetoothmodule.BluetoothListener;
import es.esi.techlab.bluebeetoothmodule.listenters.NotifyListener;
import es.esi.techlab.bluebeetoothmodule.MiBandImplementation.model.Profile;
import es.esi.techlab.bluebeetoothmodule.BluetoothIO;

/**
 * <code>BluetoothIO</code> is the main class in order to make and receive the bluetooth communications
 * from the smartphone to the bluetooth device. This class extends from the default
 * <code>BluetoothGattCallback</code> class.
 *
 * @see BluetoothGattCallback
 * @see BluetoothGatt
 */
public final class BluetoothIOMiBand extends BluetoothIO {

    private static final String TAG = "BluetoothIOMiBand";


    /**
     * Constructor
     *
     * @param listener Callback listener
     */
    public BluetoothIOMiBand(BluetoothListener listener) {
        super(listener);
    }

    /**
     * Sets notification listener for specific service and specific characteristic
     *
     * @param serviceUUID      Service UUID
     * @param characteristicId Characteristic UUID
     * @param listener         New listener
     */
    public void setNotifyListener(UUID serviceUUID, UUID characteristicId, NotifyListener listener)  {

        checkConnectionState();

        BluetoothGattService service = super.getBluetoothGatt().getService(serviceUUID);

        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicId);
            if (characteristic != null) {
                super.getBluetoothGatt().setCharacteristicNotification(characteristic, true);
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(Profile.UUID_DESCRIPTOR_UPDATE_NOTIFICATION);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

                if (!super.getBluetoothGatt().writeDescriptor(descriptor)) {
                    notifyWithFail(serviceUUID, characteristicId, "BluetoothGattCharacteristic " + characteristicId + " does not exist");
                } else {
                    super.getNotifyListeners().put(characteristicId, listener);
                }
            } else {
                notifyWithFail(serviceUUID, characteristicId, "BluetoothGattCharacteristic " + characteristicId + " does not exist");
            }
        } else {
            notifyWithFail(serviceUUID, characteristicId, "BluetoothGattService " + serviceUUID + " does not exist");
        }
    }

    /**
     * Removes notification listener for the service and characteristic
     *
     * @param serviceUUID      Service UUID
     * @param characteristicId Characteristic UUID
     */
    public void removeNotifyListener(UUID serviceUUID, UUID characteristicId) {

        checkConnectionState();

        BluetoothGattService service = super.getBluetoothGatt().getService(serviceUUID);
        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicId);
            if (characteristic != null) {
                super.getBluetoothGatt().setCharacteristicNotification(characteristic, false);
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(Profile.UUID_DESCRIPTOR_UPDATE_NOTIFICATION);
                descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                super.getBluetoothGatt().writeDescriptor(descriptor);
                super.getNotifyListeners().remove(characteristicId);
            } else {
                notifyWithFail(serviceUUID, characteristicId, "BluetoothGattCharacteristic " + characteristicId + " does not exist");
            }
        } else {
            notifyWithFail(serviceUUID, characteristicId, "BluetoothGattService " + serviceUUID + " does not exist");
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);    // Ready for pairing/auth
        if (descriptor.getCharacteristic().getUuid().equals(Profile.UUID_CHAR_PAIR)) {
            notifyWithResult(descriptor.getCharacteristic());
        }
        if (descriptor.getCharacteristic().getUuid().equals(Profile.UUID_CHAR_ACTIVITY_DATA)) {
            notifyWithResult(descriptor.getCharacteristic());
        }
        if (descriptor.getCharacteristic().getUuid().equals(Profile.UUID_CHAR_FETCH)) {
            notifyWithResult(descriptor.getCharacteristic());
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        Log.d("CHANGED_CHAR - Char: ", String.valueOf(characteristic.getUuid()) + " - value: " + Arrays.toString(characteristic.getValue()));
        // Notify the change...
        if (super.getNotifyListeners().containsKey(characteristic.getUuid())) {
            super.getNotifyListeners().get(characteristic.getUuid()).onNotify(characteristic.getValue());
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
            super.notifyWithResult(characteristic);
        } else {
            UUID serviceId = characteristic.getService().getUuid();
            UUID characteristicId = characteristic.getUuid();
            super.notifyWithFail(serviceId, characteristicId, "onCharacteristicRead fail");
        }
    }


}
