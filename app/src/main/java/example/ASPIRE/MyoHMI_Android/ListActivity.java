package example.ASPIRE.MyoHMI_Android;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ListActivity extends AppCompatActivity {

    // Scan option and tags
    public static String TAG = "BluetoothList";
    public static final String MYO = "Myo_Armband";
    public static String myoName = null;
    public static final String HACK = "HACKberry_Arm";
    public static String hackAddress = null;

    // Device Scanning Time (ms)
    private static final long SCAN_PERIOD = 5000;

    // Intent code for requesting Bluetooth enable
    private static final int REQUEST_ENABLE_BT = 1;

    private static int numChannels;

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }
        }
    };

    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private Button connectButton;

    // Myo Armband settings
    private ArrayList<String> deviceNames = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private TextView scanningText;
    private ProgressBar prog;
    private ListView devicesList;
    private EditText numChannelsInput;

    /*Updated by Ricardo Colin 06/15/18*/
    private Button scanButton;

    // HACKberry Arm settings
    private ArrayList<String> deviceNames_hack = new ArrayList<>();
    private ArrayList<String> deviceAddresses_hack = new ArrayList<>();
    private ArrayAdapter<String> adapter_hack;
    private TextView scanningText_hack;
    private ProgressBar prog_hack;
    private ListView devicesList_hack;

    /*Updated by Justin Phan */
    private Button scanButton_hack;

    // Update ListView with nearby BLE devices
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d("callbackType", String.valueOf(callbackType));
            Log.d("result", result.toString());
            BluetoothDevice device = result.getDevice();
            ParcelUuid[] uuids = device.getUuids();
            String uuid = "";
            if (uuids != null) {
                for (ParcelUuid puuid : uuids) {
                    uuid += puuid.toString() + " ";
                }
            }

            String msg = "name=" + device.getName() + ", bondStatus="
                    + device.getBondState() + ", address="
                    + device.getAddress() + ", type" + device.getType()
                    + ", uuids=" + uuid;
            Log.d("BLEActivity", msg);

            if (device.getName() != null && !deviceNames.contains(device.getName())) {
                deviceNames.add(device.getName());
            }
            Log.d(TAG, "onScanResult: Device = " + result.getDevice().getName());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.d("BTScan", "ENTERED onBatchScanResult");
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.d("Scan Failed", "Error Code: " + errorCode);
        }
    };

    // Update ListView with nearby Bluetooth devices
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getName() != null && !deviceNames_hack.contains(device.getName())) {
                    deviceNames_hack.add(device.getName());
                    deviceAddresses_hack.add(device.getAddress());
                }
                Log.d(TAG, "Received: " + device.getName() + ": " + device.getAddress());
            }
        }
    };

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();

        unregisterReceiver(mBroadcastReceiver);
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.d("BTScan", "ENTERED onLeScan");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i("onLeScan", device.toString());
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showPhoneStatePermission();
        setContentView(R.layout.activity_list);

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mHandler = new Handler();

        // BLE components
        prog = (ProgressBar) findViewById(R.id.progressBar2);
        scanButton = (Button) findViewById(R.id.scanButton);
        scanningText = (TextView) findViewById(R.id.scanning_text);
        devicesList = (ListView) findViewById(R.id.listView1);

        adapter = new ArrayAdapter<>(this, R.layout.listview_custom_item, R.id.listView_item, deviceNames);
        devicesList.setAdapter(adapter);

        // Bluetooth components
        prog_hack = (ProgressBar) findViewById(R.id.progressBar2_hack);
        scanButton_hack = (Button) findViewById(R.id.scanButton_hack);
        scanningText_hack = (TextView) findViewById(R.id.scanning_text_hack);
        devicesList_hack = (ListView) findViewById(R.id.listView1_hack);

        adapter_hack = new ArrayAdapter<>(this, R.layout.listview_custom_item, R.id.listView_item, deviceNames_hack);
        devicesList_hack.setAdapter(adapter_hack);

        numChannelsInput = (EditText) findViewById(R.id.number_channels);

        // Connect to BLE and Bluetooth devices
        connectButton = (Button) findViewById(R.id.connectButton);

        // Check if Bluetooth is supported
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth Not Supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Turn on Bluetooth
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
                filters = new ArrayList<ScanFilter>();
            }
        }

        // Select a BLE device from ListView
        devicesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ListView listView = (ListView) parent;
                String item = (String) listView.getItemAtPosition(position);
                view.setSelected(true);

                Toast.makeText(getApplicationContext(), item + " is selected", Toast.LENGTH_SHORT).show();
//                mLEScanner.stopScan(mScanCallback);//added this for the tablet that sucks
                myoName = item;

                Log.d(TAG, "Selected: " + item + ": " + myoName);
            }
        });

        // Select a Bluetooth device from ListView
        devicesList_hack.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ListView listView = (ListView) parent;
                String item = (String) listView.getItemAtPosition(position);
                view.setSelected(true);

                Toast.makeText(getApplicationContext(), item + " is selected", Toast.LENGTH_SHORT).show();
                hackAddress = deviceAddresses_hack.get(position);

                Log.d(TAG, "Selected: " + item + ": " + hackAddress);
            }
        });

        // Button scans for BLE devices
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	scanBLE();
            }
        });

        // Button scans for Bluetooth devices
        scanButton_hack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanBL();
            }
        });

        // Button scans for Bluetooth devices
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect_BL_BLE();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            scanBLE();
        }
    }

    // Scan for BLE devices
    private void scanBLE() {
        myoName = null;
        scanButton.setVisibility(View.INVISIBLE);
        scanningText.setVisibility(View.VISIBLE);
        prog.setVisibility(View.VISIBLE);

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            deviceNames.clear();
            // Scanning Time out by Handler.
            // The device scanning needs high energy.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mLEScanner.stopScan(mScanCallback);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(getApplicationContext(), "Scan Stopped", Toast.LENGTH_SHORT).show();
                    prog.setVisibility(View.INVISIBLE);
                    scanningText.setVisibility(View.INVISIBLE);
                    scanButton.setVisibility(View.VISIBLE);

                }
            }, SCAN_PERIOD);
            mLEScanner.startScan(filters, settings, mScanCallback);
        }
    }

    // Scan for Bluetooth devices
    private void scanBL() {
        Log.d(TAG, "scanBL: Looking for unpaired devices.");
        hackAddress = null;
        scanButton_hack.setVisibility(View.INVISIBLE);
        scanningText_hack.setVisibility(View.VISIBLE);
        prog_hack.setVisibility(View.VISIBLE);

        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "scanBL: Canceling active discovery.");
        }

        //check BT permissions in manifest
        //checkBTPermissions();

        deviceNames_hack.clear();
        deviceAddresses_hack.clear();

        // Scanning time out by Handler.
        // The device scanning needs high energy.
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothAdapter.cancelDiscovery();
                adapter_hack.notifyDataSetChanged();
                Toast.makeText(getApplicationContext(), "Scan Stopped", Toast.LENGTH_SHORT).show();
                prog_hack.setVisibility(View.INVISIBLE);
                scanningText_hack.setVisibility(View.INVISIBLE);
                scanButton_hack.setVisibility(View.VISIBLE);
            }
        }, SCAN_PERIOD);

        mBluetoothAdapter.startDiscovery();
        IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mBroadcastReceiver, discoverDevicesIntent);
    }

    // Connect to your selected BLE and Bluetooth devices
    // Send name of selected BLE device and hardware address of selected Bluetooth device to MainActivity
    private void connect_BL_BLE(){
        Log.d(TAG, "Selected BLE: " + myoName);
        Log.d(TAG, "Selected BL: " + hackAddress);
        if(myoName == null){
            Toast.makeText(getApplicationContext(), "No Myo Armband was selected.", Toast.LENGTH_SHORT).show();
        }else{
            if(hackAddress == null){
                Toast.makeText(getApplicationContext(),  myoName + " is connecting...", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(getApplicationContext(),  myoName + " and HACKberry Arm are connecting...", Toast.LENGTH_SHORT).show();
            }

            numChannels = Integer.parseInt(numChannelsInput.getText().toString());

            Intent intent;
            intent = new Intent(getApplicationContext(), MainActivity.class);

            intent.putExtra(MYO, myoName);
            intent.putExtra(HACK, hackAddress);

            startActivity(intent);
        }
    }

    private void showPhoneStatePermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {

            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
            }
        } else {
            //Toast.makeText(this, "Location Permission (already) Granted!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(this, "Permission denied to access location services (coarse)", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            case 2: {

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(this, "Permission denied to access external storage", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public static int getNumChannels() {
        return numChannels;
    }

    /* // Checks Android version, need this if you are using anything higher than LOLLIPOP
    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }else{
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }
     */

}