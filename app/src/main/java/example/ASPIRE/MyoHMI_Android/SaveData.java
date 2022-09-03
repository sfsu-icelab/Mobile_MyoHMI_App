package example.ASPIRE.MyoHMI_Android;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.lang3.ArrayUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by Charles on 7/12/17.
 */

public class SaveData extends Activity {

    private static final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 1;
    private static final int MY_PERMISSIONS_READ_EXTERNAL_STORAGE = 2;
    private static SaveData saver;
    String FileName;
    CloudUpload cloudUpload;
    private Context context;
    private ArrayList<DataVector> trainData = new ArrayList<>();
    private FeatureCalculator fcalc;

    public SaveData(Context context) {
        this.context = context;
        checkWriteExternalStoragePermission();//move to initial upload file button
        checkReadExternalStoragePermission();
        cloudUpload = new CloudUpload(context);
    }

    public File addData(ArrayList<DataVector> trainData) {

        File file = null;
        String state;
        state = Environment.getExternalStorageState();

        String date = new SimpleDateFormat("yyyy-MM-dd-hh-mm").format(new Date());

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File Root = Environment.getExternalStorageDirectory();
            File Dir = new File(Root.getAbsolutePath() + "/MyoAppFile");
            if (!Dir.exists()) {
                Dir.mkdir();
            }

            FileName = date + ".txt";

            file = new File(Dir, FileName);

            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file, true);
                OutputStreamWriter osw = new OutputStreamWriter(fileOutputStream);

                for (int i = 0; i < trainData.size(); i++) {
                    DataVector data = trainData.get(i);
                    double trunc = i / 100;
                    //            saver.addData(selectedItems.get((int)trunc), data.getVectorData().toString() + "\t" + String.valueOf(data.getTimestamp()));
                    osw.append((int) trunc + "\t" + data.getVectorData().toString() + "\t" + String.valueOf(data.getTimestamp()));
                    osw.append("\n");
//                    Log.d("To be saved: ", selectedItems.get((int)trunc) + data.getVectorData().toString() + "\t" + String.valueOf(data.getTimestamp()));
                }
                osw.flush();
                osw.close();

//                cloudUpload.beginUpload(file);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.d("EXTERNAL STRG", "No SD card found");
        }
        return file;
    }

    public File makeFile(String filename) {
        File file = null;
        String state;
        state = Environment.getExternalStorageState();

        String date = new SimpleDateFormat("yyyy-MM-dd-hh-mm").format(new Date());

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File Root = Environment.getExternalStorageDirectory();
            File Dir = new File(Root.getAbsolutePath() + "/MyoAppFile");
            if (!Dir.exists()) {
                Dir.mkdir();
            }

            FileName = filename + " " + date + ".txt";

            file = new File(Dir, FileName);
        } else {
            Log.d("EXTERNAL STRG", "No SD card found");
        }
        return file;
    }

    public void addToFile(File file, String line) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file, true);
            OutputStreamWriter osw = new OutputStreamWriter(fileOutputStream);

            osw.append(line + "\n");

            osw.flush();
            osw.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void checkWriteExternalStoragePermission() {
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
        }
    }

    public void checkReadExternalStoragePermission() {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_READ_EXTERNAL_STORAGE);
        }
    }

    public void givePath(Uri data, ArrayList<String> TempGestures) {
        try {
            File Root = Environment.getExternalStorageDirectory();
            String[] elements = data.getPath().toString().split("/");
            BufferedReader reader = new BufferedReader(new FileReader(Root + "/MyoAppFile/" + elements[elements.length - 1]));
            String text;
            String[] column;
            String[] emgData;
            double[] lineData = new double[48];
            ArrayList<Integer> classes = new ArrayList<>();

            while ((text = reader.readLine()) != null) {
                column = text.split("\t");
                classes.add(Integer.parseInt(column[0]));
                emgData = column[1].split(",");
                for (int j = 0; j < emgData.length; j++) {
                    lineData[j] = Double.parseDouble(emgData[j].replaceAll("[^\\d.]", ""));
                }
                Number[] feat_dataObj = ArrayUtils.toObject(lineData);
                ArrayList<Number> LineData = new ArrayList<Number>(Arrays.asList(feat_dataObj));
                DataVector dvec = new DataVector(Integer.parseInt(column[0]), lineData.length, LineData);
                trainData.add(dvec);
            }
            Log.d("clases len, samples len", String.valueOf(classes.size()) + ", " + String.valueOf(trainData.size()));
            fcalc.setClasses(classes);
            fcalc.setSamplesClassifier(trainData);
            fcalc.sendClasses(TempGestures);
            fcalc.Train();
            fcalc.setClassify(true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Log.d("REQUEST_CODE", String.valueOf(requestCode));
        switch (requestCode) {
            case MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(context, "Write Storage Permission (already) Granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            case MY_PERMISSIONS_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(context, "Write Storage Permission (already) Granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }
}