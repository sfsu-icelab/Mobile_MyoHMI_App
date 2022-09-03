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
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.echo.holographlibrary.LineGraph;

import static android.content.Context.BLUETOOTH_SERVICE;
import static example.ASPIRE.MyoHMI_Android.R.id.conncectionProgress;

/**
 * Created by User on 2/28/2017.
 */

public class ArmbandEmgFragment extends Fragment implements View.OnClickListener {
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
    private LineGraph graph;
    private Plotter plotter;
    private final View.OnTouchListener changeColorListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            Bitmap source = v.getDrawingCache();
            Bitmap bmp = Bitmap.createBitmap(source, 0, 0, v.getWidth(), v.getHeight());
            int color = bmp.getPixel((int) event.getX(), (int) event.getY());
            if (plotter != null) {
                if (color == Color.rgb(89, 140, 175)) {
                    Log.d("Clicked on ", "blue");
                    plotter.setEMG(color, 2);
                } else if (color == Color.rgb(100, 169, 95)) {
                    Log.d("Clicked on ", "green");
                    plotter.setEMG(color, 1);
                } else if (color == Color.rgb(169, 95, 95)) {
                    Log.d("Clicked on ", "clay");
                    plotter.setEMG(color, 0);
                } else if (color == Color.rgb(189, 75, 167)) {
                    Log.d("Clicked on ", "magenta");
                    plotter.setEMG(color, 7);
                } else if (color == Color.rgb(171, 89, 43)) {
                    Log.d("Clicked on ", "brown");
                    plotter.setEMG(color, 6);
                } else if (color == Color.rgb(94, 62, 130)) {
                    Log.d("Clicked on ", "purple");
                    plotter.setEMG(color, 5);
                } else if (color == Color.rgb(171, 21, 21)) {
                    Log.d("Clicked on ", "red");
                    plotter.setEMG(color, 4);
                } else if (color == Color.rgb(64, 64, 64) || (color < -12500000 && color > -15800000)) {//gray or the logo color
                    Log.d("Clicked on ", "gray");
                    plotter.setEMG(Color.rgb(64, 64, 64), 3);
                }
                return true;
            } else {
                return false;
            }

        }
    };
    private ProgressBar prog;
    private ToggleButton emgButton;
    private ScanCallback scanCallback = new ScanCallback() {
    };
    private boolean click = true;

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
                plotter = new Plotter(mHandler, graph);
                mMyoCallback = new MyoGattCallback(mHandler, myoConnectionText, prog, connectingText, plotter, getView());
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
        final View v = inflater.inflate(R.layout.fragment_emg_armband, container, false);
        assert v != null;

//        emgDataText = (TextView)v.findViewById(R.id.emgDataTextView);
        myoConnectionText = (TextView) v.findViewById(R.id.myoConnection);
        emgButton = v.findViewById(R.id.iEMG);
        View vibrateButton = v.findViewById(R.id.iVibrate);
        myoConnectionText.setTextColor(Color.rgb(38, 38, 38));
        graph = v.findViewById(R.id.holo_graph_view);
        mHandler = new Handler();
        activity = this.getActivity();

        prog = (ProgressBar) v.findViewById(conncectionProgress);
        connectingText = (TextView) v.findViewById(R.id.connectingTextView);

        ImageView imgView = (ImageView) v.findViewById(R.id.imageView);
        imgView.setDrawingCacheEnabled(true);
        imgView.setOnTouchListener(changeColorListener);

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
                /*
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mLEScanner.stopScan(mScanCallback);
                    }
                }, SCAN_PERIOD);
                */
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

//        IntentFilter filter = new IntentFilter(mBluetoothAdapter.ACTION_STATE_CHANGED);
//        getActivity().registerReceiver(mReceiver, filter);


            /*if (MyoGattCallback.myoConnected == null) {

                Toast.makeText(getContext(), "OUTSIDE'", Toast.LENGTH_LONG).show();

                Toast.makeText(getContext(), "On the top right corner, select 'Connect'", Toast.LENGTH_LONG).show();

                AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
                alertDialog.setTitle("Myo not detected");
                alertDialog.setMessage("Myo armband should be connected before training gestures.");
                alertDialog.setIcon(R.drawable.stop_icon);

                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getContext(), "On the top right corner, select 'Connect'", Toast.LENGTH_LONG).show();
                    }
                });

                alertDialog.show();

            } else {
                Toast.makeText(getContext(), "HEY", Toast.LENGTH_LONG).show();



            }*/


            emgButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (MyoGattCallback.myoConnected == null) {
                        if (emgButton.isChecked()) {
                            emgButton.setEnabled(false);
                            emgButton.setChecked(false);
                            Toast.makeText(getContext(), "No MYO Connection", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        clickedemg();
                    }
                }
            });

            vibrateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickedvib(v);
                }
            });
        }

        return v;
    }

    @Override
    public void onClick(View v) {
        clickedemg();
    }

    public void clickedemg() {
        click = !click;
        Log.d("Tag", String.valueOf(click));
        if (click) {
            if (mBluetoothGatt == null || !mMyoCallback.setMyoControlCommand(commandList.sendImuAndEmg())) {
                Log.d(TAG, "False EMG");
            } else {
                myoConnectionText.setText("");
            }
        } else {
            if (mBluetoothGatt == null
                    || !mMyoCallback.setMyoControlCommand(commandList.sendUnsetData())
                /*|| !mMyoCallback.setMyoControlCommand(commandList.sendNormalSleep())*/) {
                Log.d(TAG, "False Data Stop");
            }
        }
    }

    public void clickedvib(View v) {
        if (mBluetoothGatt == null || !mMyoCallback.setMyoControlCommand(commandList.sendVibration3())) {
            Log.d(TAG, "False Vibrate");
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
    }
}
