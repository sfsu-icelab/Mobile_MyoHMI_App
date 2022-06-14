package example.ASPIRE.MyoHMI_Android;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class BluetoothConnection {
    private static final String TAG = "HACKberryConnect";

    private static final UUID BLUETOOTH_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mBluetoothSocket; // Bluetooth connection
    private OutputStream os; // Output Stream for Bluetooth connection
    private String deviceHack;
    private byte[] bytes;

    public BluetoothConnection(){}

    public BluetoothConnection(String mac) {
        Log.d("SendToHACKberry", "Started");

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        deviceHack = mac;

        // Connect to Bluetooth device
        BluetoothDevice mBluetoothHack = mBluetoothAdapter.getRemoteDevice(deviceHack);
        try {
            mBluetoothSocket = mBluetoothHack.createRfcommSocketToServiceRecord(BLUETOOTH_UUID);
            mBluetoothSocket.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Create an Output Stream
        try {
            os = mBluetoothSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void sendPredictions(String gesture){
        bytes = gesture.getBytes(Charset.defaultCharset());
        try {
            os.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
