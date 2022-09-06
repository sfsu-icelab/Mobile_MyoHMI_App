package example.ASPIRE.MyoHMI_Android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.content.Context.INPUT_METHOD_SERVICE;


/**
 * Created by User on 2/28/2017.
 */

public class ClassificationFragment extends Fragment {

    //create an ArrayList object to store selected items
    public static ArrayList<String> selectedItems = new ArrayList<>();
    public static ArrayList<String> selectedCommands = new ArrayList<>();
    private static SaveData saver;
    //    Runnable r1;
    EditText GetValue;
    EditText GetCommand;
    ImageButton addButton;
    ImageButton deleteButton;
    ImageButton clearButton;
    ImageButton uploadButton;
    ImageButton trainButton;
    ImageButton loadButton;
    ImageButton resetButton;
    ListView listview_Classifier;
    ListView listview;
    ProgressBar progressBar;
    LayoutInflater inflater;
    ViewGroup container;
    Classifier classifier = new Classifier();
    ServerCommunicationThread comm = new ServerCommunicationThread();
    String[] ListElements = new String[]{
            "Rest",
            "Fist",
            "Point",
            "Open Hand",
            "Wave-In",
            "Wave-Out",
            "Supination",
            "Pronation"
    };
    List<String> ListCommands = new ArrayList<>(
            Arrays.asList("P90 T100 O140",  // Rest
                          "P30 T30 O30",    // Fist
                          "P130 T30 O30",   // Point
                          "P140 T140 O150", // Open Hand
                          "P60 T30 O60",    // Wave-In
                          "P100 T30 O100",  // Wave-Out
                          "P130 T140 O30",  // Supination
                          "P30 T30 O150"    // Pronation
            )
    );
    String[] classifier_options = new String[]{
            "LDA",
            "SVM",
            "Logistic Regression",
            "Decision Tree",
            "Neural Net",
            "KNN",
            "Adaboost",
            "CNN"
    };
    private FeatureCalculator fcalc;
    private List<String> ListElementsArrayList;
    private List<String> ClassifierArrayList;
    private ArrayList<DataVector> trainData = new ArrayList<>();
    private int count = 4;
    //    private Handler mHandler = new Handler();
    private Handler mHandler;
    private HandlerThread readThread;
    private int gestureCounter = 0;
    private TextView liveView, status;
    private CloudUpload cloudUpload;
    private ArrayList<Runnable> taskList = new ArrayList<Runnable>();
    private Context context;
    private boolean listsDone = false;

    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    boolean getListsDone() {
        return listsDone;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View v = inflater.inflate(R.layout.fragment_classification, container, false);

        assert v != null;
        context = this.getContext();

        this.inflater = inflater;
        this.container = container;

        fcalc = new FeatureCalculator(v, getActivity());
        classifier = new Classifier(getActivity());
        //saver = new SaveData(this.getContext());
        //saver.checkReadExternalStoragePermission();

        liveView = (TextView) v.findViewById(R.id.gesture_detected);
        GetValue = (EditText) v.findViewById(R.id.add_gesture_text);
        GetCommand = (EditText) v.findViewById(R.id.add_command_text);
        trainButton = (ImageButton) v.findViewById(R.id.bt_train);
        loadButton = (ImageButton) v.findViewById(R.id.bt_load);
        addButton = (ImageButton) v.findViewById(R.id.im_add);
        deleteButton = (ImageButton) v.findViewById(R.id.im_delete);
        uploadButton = (ImageButton) v.findViewById(R.id.im_upload);
        resetButton = (ImageButton) v.findViewById(R.id.im_reset);
        listview = (ListView) v.findViewById(R.id.listView);
        listview_Classifier = (ListView) v.findViewById(R.id.listView1);
        listview.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listview_Classifier.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        ListElementsArrayList = new ArrayList<String>(Arrays.asList(ListElements));
        ClassifierArrayList = new ArrayList<String>(Arrays.asList(classifier_options));

        //cloudUpload = new CloudUpload(getActivity());

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), R.layout.mytextview, ListElementsArrayList);
        ArrayAdapter<String> adapter_classifier = new ArrayAdapter<String>(getActivity(), R.layout.myradioview, ClassifierArrayList);

        listview.setAdapter(adapter);
        listview_Classifier.setAdapter(adapter_classifier);

        listview_Classifier.setItemChecked(0, true);

        // Add default selected gestures to an arraylist
        selectedItems = new ArrayList<>();
        selectedCommands = new ArrayList<>();
        for (int i = 0; i < ListElements.length; i++) {
            listview.setItemChecked(i, true);
            selectedItems.add(i, adapter.getItem(i));
            selectedCommands.add(i, ListCommands.get(i));
        }

        // Add/Remove selected gestures in the arraylist
        listview.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItem = ((TextView) view).getText().toString();
            if (selectedItems.contains(selectedItem)) {
                Log.d("Removing", selectedItem);
                selectedCommands.remove(selectedItems.indexOf(selectedItem));
                selectedItems.remove(selectedItem);
                return;
            }
            int sCount = 0;
            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItem(i) == selectedItem) {
                    selectedItems.add(sCount, selectedItem);
                    selectedCommands.add(sCount, ListCommands.get(i));
                    Log.d("Selected Gestures", String.valueOf(selectedItems));
                    return;
                }
                if (selectedItems.contains(adapter.getItem(i))) {
                    sCount++;
                }
            }
        });

        listview_Classifier.setOnItemClickListener((parent, view, position, id) -> {
            classifier.setChoice(position);
            String Classifier_selectedItem = ((TextView) view).getText().toString();
            Toast.makeText(getActivity(), "selected: " + Classifier_selectedItem, Toast.LENGTH_SHORT).show();
        });

        // Start Bluetooth connection if a BT device was selected
        Intent intent = getActivity().getIntent();
        String deviceHack = intent.getStringExtra(ListActivity.HACK);

        Log.d("ClassificationFragment", "Incoming: " + deviceHack);
        if(deviceHack != null){
            fcalc.startBTConnection(deviceHack);
        }

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedItems.size() > 0) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                    builder.setTitle("Delete?");
                    builder.setMessage("Are you sure you want to delete " + selectedItems + "?");

                    builder.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                        }
                    });
                    builder.setNegativeButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            String selItems = "";
                            while (selectedItems.size() > 0) {
                                for (int i = 0; i < selectedItems.size(); ++i) {
                                    String item = selectedItems.get(i);
                                    for (int x = 0; x <= item.length(); ++x) {
                                        ListCommands.remove(selectedItems.indexOf(item)); //
                                        selectedCommands.remove(selectedItems.indexOf(item));
                                        selectedItems.remove(item); //remove deselected item from the list of selected items
                                        listview.setItemChecked(x, false);
                                        adapter.remove(item);
                                    }
                                    selItems += "," + item;
                                }
                            }
                            Toast.makeText(getActivity(), "Deleting: " + selItems, Toast.LENGTH_SHORT).show();
                            adapter.notifyDataSetChanged();
                        }
                    });
                    builder.show();

                } else if (ListElementsArrayList.size() > 0) {
                    Toast.makeText(getActivity(), "Please select the desired gestures to be deleted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "There is nothing to delete!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        addButton.setOnClickListener(v12 -> {
            try {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
            } catch (Exception e) {
                e.printStackTrace();
            }

            String newGesture = GetValue.getText().toString();
            String newCommand = GetCommand.getText().toString();
            if(newCommand.isEmpty()){
                newCommand = "P90 T100 O140"; // Rest
            }

            if (newGesture.matches("")) {
                Toast.makeText(getActivity(), "Please Enter A Gesture", Toast.LENGTH_SHORT).show();
            } else {
                ListElementsArrayList.add(GetValue.getText().toString());
                ListCommands.add(newCommand);
                GetValue.setText("");
                GetCommand.setText("");
                adapter.notifyDataSetChanged();
            }
        });

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fileUpload();
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gestureCounter = 0;
                liveView.setText("");
                fcalc.reset();
                classifier.reset();
                trainButton.setVisibility(View.VISIBLE);
                liveView.setText("");
            }
        });

        trainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                countdown();
            }
        });

        loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fileLoad();
            }
        });

        return v;
    }

    private void countdown() {
        readThread = new HandlerThread("");
        readThread.start();
        mHandler = new Handler(readThread.getLooper());
        count = 4;
        gestureCounter = 0;
        fcalc.sendClasses(selectedItems);
        fcalc.sendCommands(selectedCommands);
        Runnable countdown = new Runnable() {
            @Override
            public void run() {
                if (selectedItems.size() > 1) {
                    trainButton.setVisibility(View.GONE);

                    if ((--count != -1) && (gestureCounter != selectedItems.size())) {
                        mHandler.postDelayed(this, 1000);

                        updateLiveViewText("Do " + selectedItems.get(gestureCounter) + " in " + String.valueOf(count));
//                      progressBar.setVisibility(View.VISIBLE);

                        if (count == 0) {
//                          progressBar.setVisibility(View.INVISIBLE);
                            updateLiveViewText("Hold " + selectedItems.get(gestureCounter));
                        }
                    } else if (gestureCounter != selectedItems.size()) {
                        count = 4;//3 seconds + 1
                        mHandler.post(this);
                        fcalc.setTrain(true);
                        while (fcalc.getTrain()) {//wait till training is done

                            /* For some reason we must print something here or else it gets stuck */
                            System.out.print("");
                        }
                        gestureCounter++;
                        Log.d("Gesture Counter", String.valueOf(gestureCounter));
                    } else {
                        updateLiveViewText("");
                        fcalc.Train();
                        fcalc.setClassify(true);
                        readThread.quit();
                    }
                } else if (selectedItems.size() == 1) {
                    Toast.makeText(getActivity(), "at least 2 gestures must be selected!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "No gestures selected!", Toast.LENGTH_SHORT).show();
                }
            }
        };
        countdown.run();
    }


    private void updateLiveViewText(String text) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                liveView.setText(text);
            }
        });
//        new Thread() {
//            public void run() {
//                    try {
//                        getActivity().runOnUiThread(new Runnable() {
//
//                            @Override
//                            public void run() {
//                                liveView.setText(text);
//                            }
//                        });
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//            }
//        }.start();
    }

    private void fileLoad() {
        if (MyoGattCallback.myoConnected == null) {
            AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
            alertDialog.setTitle("Myo not detected");
            alertDialog.setMessage("Myo armband should be connected before importing data.");
            alertDialog.setIcon(R.drawable.stop_icon);

            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(getContext(), "On the top right corner, select 'Connect'", Toast.LENGTH_LONG).show();
                }
            });

            alertDialog.show();

        } else {
            AlertDialog.Builder loadDialog = new AlertDialog.Builder(getContext());
            loadDialog.setTitle("Load From:");
            loadDialog.setMessage("Where would you like to load the Trained Gestures from?");
            loadDialog.setIcon(R.drawable.add_icon_extra);
            loadDialog.setCancelable(true);

            loadDialog.setPositiveButton(
                    "SD CARD",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            openFolder();
                        }
                    });

//            loadDialog.setNegativeButton(
//                    "Cloud",
//                    new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int id) {
//                        }
//                    });

            loadDialog.setNeutralButton(
                    "Cancel",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });

            AlertDialog loadOptions = loadDialog.create();
            loadOptions.show();

            // Get the alert dialog buttons reference
            Button positiveButton = loadOptions.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = loadOptions.getButton(AlertDialog.BUTTON_NEGATIVE);
            Button neutralButton = loadOptions.getButton(AlertDialog.BUTTON_NEUTRAL);

            // Change the alert dialog buttons text and background color
            positiveButton.setTextColor(Color.parseColor("#FFFFFF"));
            positiveButton.setBackgroundColor(Color.parseColor("#000000"));

            negativeButton.setTextColor(Color.parseColor("#FFFFFF"));
            negativeButton.setBackgroundColor(Color.parseColor("#030000"));

            neutralButton.setTextColor(Color.parseColor("#FFFFFF"));
            neutralButton.setBackgroundColor(Color.parseColor("#FF0000"));
        }
    }

    private void fileUpload() {
        Button cancel;
        Button sdCard;
        Button cloud;
        Button both;
        AlertDialog.Builder upload_pop = new AlertDialog.Builder(getActivity());

        View view = inflater.inflate(R.layout.upload_dialog, container, false);

        cancel = (Button) view.findViewById(R.id.bt_cancel);
        sdCard = (Button) view.findViewById(R.id.bt_sdcard);
        cloud = (Button) view.findViewById(R.id.bt_cloud);
        both = (Button) view.findViewById(R.id.bt_both);

        File file = saver.addData(fcalc.getFeatureData());

        final AlertDialog dialog = upload_pop.create();

        context = this.getContext();

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                file.delete();
                Toast.makeText(getActivity(), "Canceled", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        sdCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //saver.addData(fcalc.getSamplesClassifier(), selectedItems);
                Toast.makeText(getActivity(), "Saving on SDCARD!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        cloud.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cloudUpload.setDelete(true);

                Response.Listener<String> responseListener = new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            boolean success = jsonResponse.getBoolean("success");

                            if (success) {
                                Log.d("Success ", "uploading emg data");
                            } else {
                                Log.d("Failed ", "uploading emg data");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                };

                ArrayList<DataVector> featureRows = fcalc.getFeatureData();

                RequestQueue queue = Volley.newRequestQueue(context);
                for (int i = 0; i < featureRows.size(); i++) {
                    double trunc = i / 100;
                    exportEMGRequest emg = new exportEMGRequest(
                            featureRows.get(i),
                            ListElementsArrayList.get((int) trunc),
                            responseListener
                    );
                    queue.add(emg);
                }
                queue.start();

                Toast.makeText(getActivity(), "Saving on Cloud!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        both.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cloudUpload.setDelete(false);
                cloudUpload.beginUpload(file);
                Toast.makeText(getActivity(), "Saving on SDCARD and Cloud!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
        dialog.setView(view);
        dialog.show();
    }

    public void openFolder() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        getActivity().startActivityForResult(intent, 2);
    }

    public void givePath(Uri data, Context context) {
        ArrayList<String> TempGestures = new ArrayList<>();
        for (int j = 0; j < selectedItems.size(); j++) {
            Log.d("Selected Items:", String.valueOf(selectedItems.get(j)));
            TempGestures.add(j, selectedItems.get(j));
        }
        saver.givePath(data, TempGestures);
    }
}
