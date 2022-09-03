package example.ASPIRE.MyoHMI_Android;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.echo.holographlibrary.LineGraph;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.BLUETOOTH_SERVICE;
import static example.ASPIRE.MyoHMI_Android.R.id.conncectionProgress;

/**
 * Created by User on 2/28/2017.
 */

public class EmgFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "Tab2Fragment";

    private static final int REQUEST_ENABLE_BT = 1;
    Activity activity;
    private Handler mHandler;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt; // BLE connection
    private TextView myoConnectionText;
    private TextView connectingText;
    private MyoGattCallback mMyoCallback;
    private MyoCommandList commandList = new MyoCommandList();
    private String deviceName;
    private LineGraph graph1, graph2, graph3, graph4;
    private Plotter plotter1, plotter2, plotter3, plotter4;
    private Spinner spinner1, spinner2, spinner3, spinner4;
    private ProgressBar prog;
    private static ToggleButton emgButton;
    private ScanCallback scanCallback = new ScanCallback() {
    };
    private static boolean click = true;
    private boolean spinnerInitialized = false;
    private static final int color1 = Color.rgb(64, 64, 64);
    private static final int color2 = Color.rgb(89, 140, 175);
    private static final int color3 = Color.rgb(100, 169, 95);
    private static final int color4 = Color.rgb(169, 95, 95);
    private int numChannels = ListActivity.getNumChannels();

    // Scan and connect to the selected Myo Armband (BLE)
    private ScanCallback mScanCallback = new ScanCallback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (deviceName.equals(device.getName())) {
                Log.d(TAG, "Connecting to BLE device...");

                BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
                if (scanner != null) {
                    scanner.stopScan(mScanCallback);
                } else {
                    // Device Bluetooth is disabled; scanning should already be stopped, nothing to do here.
                }

                // Trying to connect GATT
                plotter1 = new Plotter(mHandler, graph1);
                plotter2 = new Plotter(mHandler, graph2);
                plotter3 = new Plotter(mHandler, graph3);
                plotter4 = new Plotter(mHandler, graph4);

                plotter1.setEMG(color1, 3);
                plotter2.setEMG(color2, 2);
                plotter3.setEMG(color3, 1);
                plotter4.setEMG(color4, 0);

                mMyoCallback = new MyoGattCallback(mHandler, myoConnectionText, prog, connectingText, plotter1, plotter2, plotter3, plotter4, getView());
                mBluetoothGatt = device.connectGatt(getActivity(), false, mMyoCallback);
                mMyoCallback.setBluetoothGatt(mBluetoothGatt);

                // Added so Unity can trigger vibrations
                ReceiveFromUnity.setmMyoCallback(mMyoCallback);
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final View v = inflater.inflate(R.layout.fragment_emg, container, false);
        assert v != null;

        myoConnectionText = (TextView) v.findViewById(R.id.myoConnection);
        emgButton = v.findViewById(R.id.iEMG);
        myoConnectionText.setTextColor(Color.rgb(38, 38, 38));

        List<String> channelOptions = new ArrayList<>();

        for(int i = 0; i < numChannels; i++) {
            channelOptions.add(i, String.valueOf(i+1));
        }

        spinner1 = (Spinner)v.findViewById(R.id.spinner1);
        spinner2 = (Spinner)v.findViewById(R.id.spinner2);
        spinner3 = (Spinner)v.findViewById(R.id.spinner3);
        spinner4 = (Spinner)v.findViewById(R.id.spinner4);
        ArrayAdapter<String>adapter = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_item,channelOptions);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner1.setAdapter(adapter);
        spinner1.setPrompt("Select Channel to Plot");
        spinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                Log.v("item", (String) parent.getItemAtPosition(position));
                try {
                    for (int i = 0; i < numChannels; i++) {
                        if(channelOptions.get(i).equals((String) parent.getItemAtPosition(position))) {
                            plotter1.setEMG(color1, numChannels - (i+1));
                        }
                    }

                } catch (NullPointerException ne) {

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });

        spinner2.setAdapter(adapter);
        spinner2.setPrompt("Select Channel to Plot");
        spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                Log.v("item", (String) parent.getItemAtPosition(position));
                try {
                    if(spinnerInitialized) {
                            for (int i = 0; i < numChannels; i++) {
                                if (channelOptions.get(i).equals((String) parent.getItemAtPosition(position))) {
                                    plotter2.setEMG(color2, numChannels - (i + 1));
                                }
                            }
                    } else {
                        parent.setSelection(1);
                        plotter2.setEMG(color2, 2);
                    }
                } catch (NullPointerException ne) {

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });

        spinner3.setAdapter(adapter);
        spinner3.setPrompt("Select Channel to Plot");
        spinner3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                Log.v("item", (String) parent.getItemAtPosition(position));
                try {
                    if(spinnerInitialized) {
                            for (int i = 0; i < numChannels; i++) {
                                if (channelOptions.get(i).equals((String) parent.getItemAtPosition(position))) {
                                    plotter3.setEMG(color3, numChannels - (i + 1));
                                }
                            }
                    } else {
                        parent.setSelection(2);
                        plotter3.setEMG(color3, 1);
                    }
                } catch (NullPointerException ne) {

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });

        spinner4.setAdapter(adapter);
        spinner4.setPrompt("Select Channel to Plot");
        spinner4.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                Log.v("item", (String) parent.getItemAtPosition(position));
                try {
                    if(spinnerInitialized) {
                            for (int i = 0; i < numChannels; i++) {
                                if (channelOptions.get(i).equals((String) parent.getItemAtPosition(position))) {
                                    plotter4.setEMG(color4, numChannels - (i + 1));
                                }
                            }
                    } else{
                        parent.setSelection(3);
                        plotter4.setEMG(color4, 0);
                        spinnerInitialized = true;
                    }
                } catch (NullPointerException ne) {

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });

        graph1 = v.findViewById(R.id.holo_graph_view1);
        graph2 = v.findViewById(R.id.holo_graph_view2);
        graph3 = v.findViewById(R.id.holo_graph_view3);
        graph4 = v.findViewById(R.id.holo_graph_view4);
        mHandler = new Handler();
        activity = this.getActivity();

        prog = (ProgressBar) v.findViewById(conncectionProgress);
        connectingText = (TextView) v.findViewById(R.id.connectingTextView);

        BluetoothManager mBluetoothManager = (BluetoothManager) getActivity().getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        Intent intent = getActivity().getIntent();
        deviceName = intent.getStringExtra(ListActivity.MYO);

        Log.d(TAG, "Incoming: " + deviceName);

        if (deviceName != null) {
            Log.d(TAG, "Incoming: " + deviceName);
            // Ensures Bluetooth is available on the device and it is enabled. If not,
            // displays a dialog requesting user permission to enable Bluetooth.
            if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {


                // Scanning Time out by Handler.
                // The device scanning needs high energy.

                /***********CO BY CHARLES***********/
                prog.setVisibility(View.VISIBLE);
                connectingText.setVisibility(View.VISIBLE);
                myoConnectionText.setText("");

                //mLEScanner.startScan(mScanCallback);
                BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
                if (scanner != null) {
                    scanner.startScan(mScanCallback);
                } else {
                    // Device Bluetooth is disabled; check and prompt user to enable.
                }
            }

            emgButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickedemg();
                }
            });
        }

        return v;
    }

    @Override
    public void onClick(View v) {}

    public void clickedemg() {
        click = !click;
        Log.d("Tag", String.valueOf(click));
        if (click) {
            if (mBluetoothGatt == null) {
                Log.d(TAG, "False EMG");
            } else {
                myoConnectionText.setText("");
            }
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
    }

    public static boolean emgOn() {
        return emgButton.isChecked();
    }
}
