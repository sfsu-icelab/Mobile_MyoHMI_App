package example.ASPIRE.MyoHMI_Android;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static java.lang.Float.isFinite;

/**
 * Created by Alex on 6/19/2017.
 */

public class FeatureCalculator {

    // HACKberry arm Bluetooth Connection
    public static BluetoothConnection mBluetoothConnection;

    public static Activity classAct;
    public static TextView liveView, status;
    public static ProgressBar progressBar;
    public static ImageButton uploadButton;
    public static ImageButton resetButton;
    public static ImageButton trainButton;
    public static int prediction;
    public static ArrayList<Integer> classes = new ArrayList<>();
    public static twoDimArray featemg;
    public static twoDimArray featimu;
    public static boolean train = false;
    public static boolean classify = false;
    public static Context context;
    public static DataVector[] aux;//does it have to be public?
    static int nFeatures = 6;
    static int nIMUSensors = 0;
    static boolean[] featSelected = {true, true, true, true, true, true};
    static boolean[] imuSelected = {false, false, false, false, false, false, false, false, false, false};
    static ArrayList<DataVector> samplesClassifier = new ArrayList<DataVector>();
    static ArrayList<DataVector> featureData = new ArrayList<DataVector>();
    static long startCalc = System.currentTimeMillis();
    static long startClass = System.currentTimeMillis();
    static long startFeature = System.currentTimeMillis();
    static long time1 = 0;
    static File predFile;
    private static Classifier classifier = new Classifier();
    private static int currentClass = 0;
    private static View view;
    private static List<String> gestures;      // Defined gesture classes
    private static List<String> hackCommands;  // Commands to HACKberry Arm
    private static int[] decisions = new int[10];  // Stores 10 recent predictions
    private static int counter;                    // Best out of 10 predictions gets sent to the HACKberry Arm
    private static ServerCommunicationThread thread;
    private static ClientCommunicationThread clientThread;
    int threshold = 3; //According to Ian using 3 gives better results
    int nIMUFeatures = 1;
    int nSensors = ListActivity.getNumChannels();
    int bufsize = 128;
    int ibuf = 0;
    int imuibuf = 0;
    int nDimensions = 10;
    int lastCall, firstCall;
    int winsize = 40;    //window size
    int winincr = 8;    //separation length between windows
    int winnext = winsize + 1;    //winsize + 2 samples until first feature
    int numFeatSelected = 6;
    int nSamples = 100; //Kattia: Should be set by the user and have interaction in GUI
    int CNN_BATCH = 5;
    int batchIncrement = 0;
    private int[][] windowRaw = new int[52][8];
    private static int[][][] set = new int[50][32][32];
    private static ArrayList<int[][]> cnnTrainSet = new ArrayList<>();
    private int rawIncrement = 0;
    byte[] sendWindow = new byte[0];
    private String TAG = "FeatureCalculator";
//    private static SaveData saver;
    private ArrayList<DataVector> samplebuffer = new ArrayList<>(bufsize);
    private ArrayList<DataVector> imusamplebuffer = new ArrayList<>(bufsize);
    private LinkedHashMap<Integer, Integer> freq;
    private twoDimArray featureVector;
    private twoDimArray imuFeatureVector;
    private Plotter plotter;
    private byte[] sendBytes = new byte[0];
    private ArrayList<byte[]> samplebufferbytes = new ArrayList<>(bufsize);
    //Lambda lambda = new Lambda();//need to feed context in mainactivity
    private Lambda.LTask ltask;

    public FeatureCalculator() {
    }

    public FeatureCalculator(View v, Activity act) {
        classAct = act;
        view = v;
        liveView = (TextView) view.findViewById(R.id.gesture_detected);
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        uploadButton = (ImageButton) view.findViewById(R.id.im_upload);
        resetButton = (ImageButton) view.findViewById(R.id.im_reset);
        trainButton = (ImageButton) v.findViewById(R.id.bt_train);

//        saver = new SaveData(act);

//        predFile = saver.makeFile("predictions");
//        connect();
    }

    public FeatureCalculator(Plotter plot) {
        plotter = plot;
    }

    public static boolean getTrain() {
        return train;
    }

    public static void setTrain(boolean inTrain) {
        train = inTrain;
    }

    public static boolean getClassify() {
        return classify;
    }

    public static void setClassify(boolean inClassify) {
        classify = inClassify;
    }

    public static void getThing(long time) {
        time1 = time;
//        System.out.println("GOT NEW TIME: " + time);
    }

    public static byte[] longToBytes(long l) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (l & 0xFF);
            l >>= 8;
        }
        return result;
    }

    //Making the 100 x 40 matrix
    public static void pushClassifyTrainer(DataVector[] inFeatemg) {
        int featSize = featureData.size();
        int sampSize = samplesClassifier.size();
        int classSize = classes.size();
        if((featSize != sampSize) || (featSize != classSize)) {
            int minSize1 = Math.min(featSize, sampSize);
            int minSize = Math.min(minSize1, classSize);

            while(featureData.size() != minSize) {
                featureData.remove(featureData.size()-1);
            }
            while(samplesClassifier.size() != minSize) {
                samplesClassifier.remove(samplesClassifier.size()-1);
            }
            while(classes.size() != minSize) {
                classes.remove(classes.size()-1);
            }
        }
        featureData.add(inFeatemg[1]);
        samplesClassifier.add(inFeatemg[0]);
        classes.add(currentClass);
    }

    public static void pushClassifier(int[][] windowRaw, DataVector inFeatemg) {

        startClass = System.nanoTime();

        if(classifier.useRaw()) {
            prediction = classifier.predictRaw(windowRaw);
            //prediction = classifier.predictRaw2(windowRaw);
        } else {
            prediction = classifier.predict(inFeatemg);
        }

        //Log.d("FeatureCalculator", String.valueOf(prediction));
        Log.d("FeatureCalculator", gestures.get(prediction));

        //Sends predicted Gesture to Unity
        SendToUnity.setGesture(gestures.get(prediction));
        //SendToUnity.setQuaternion((float) inFeatemg.getValue(0).byteValue(), (float) inFeatemg.getValue(1).byteValue(), (float) inFeatemg.getValue(2).byteValue(), (float) inFeatemg.getValue(3).byteValue());

        // Send predicted Gesture (Best out of 10 predictions) to Bluetooth (HACKberry Arm)
        //mBluetoothConnection.sendPredictions(gestures.get(prediction));  // Send predicted gestures
        if(mBluetoothConnection != null) {
            if(counter == 10){
                mBluetoothConnection.sendPredictions(hackCommands.get(getMostFrequent())); // Send commands
                counter = 0;
                decisions[counter] = prediction;
            }else{
                decisions[counter] = prediction;
                counter++;
            }
        }


        if (prediction == -1) {
            return;
        }
        if (liveView != null) {
            classAct.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    liveView.setText(gestures.get(prediction));
                    progressBar.setVisibility(View.INVISIBLE);
                    uploadButton.setVisibility(View.VISIBLE);
                    resetButton.setVisibility(View.VISIBLE);
                    trainButton.setVisibility(View.INVISIBLE);
                }
            });
        }
        ClientCommunicationThread.calculateDiff(prediction, 1);
    }

    public static void sendClasses(List<String> classes) {
        gestures = classes;
    }

    public static void sendCommands(List<String> commands){ hackCommands = commands;}

//    ArrayList<Number> sendList = new ArrayList<Number>();

    public static void Train() {
        /* To save training data to file for server comp time analysis */
//        File file = saver.addData(samplesClassifier);

        classifier.Train(cnnTrainSet, samplesClassifier, classes);
        //cnnTrainSet.clear();
    }

    public static void reset() {
        setClassify(false);
        setTrain(false);
        samplesClassifier = new ArrayList<>();
        aux = null;
        classes = new ArrayList<>();
        currentClass = 0;
        classifier.reset();
        liveView.setText("");
        trainButton.setVisibility(View.INVISIBLE);
//        if(thread != null)
//            thread.close();
//            thread.start();
//
//        if(clientThread != null)
//            clientThread.close();
//            clientThread.start();
    }

    public static void setClasses(ArrayList<Integer> c) {
        classes = c;
    }

    public ArrayList<DataVector> getSamplesClassifier() {
        return samplesClassifier;
    }

    public static void setSamplesClassifier(ArrayList<DataVector> s) {
        samplesClassifier = s;
    }

    public ArrayList<DataVector> getFeatureData() {
        return featureData;
    }

    public int getGesturesSize() {
        return gestures.size();
    }

    public void connect() {
        thread = new ServerCommunicationThread();
        thread.start();
        clientThread = new ClientCommunicationThread();
        clientThread.start();
    }

    public static int getMostFrequent(){
        int count = 1;
        int popular = decisions[0];
        int temp;
        int tempCount;

        for (int i = 0; i < (decisions.length - 1); i++) {
            temp = decisions[i];
            tempCount = 0;
            for (int j = 1; j < decisions.length; j++) {
                if (temp == decisions[j])
                    tempCount++;
            }

            if (tempCount > count) {
                popular = temp;
                count = tempCount;
            }
        }
        return popular;
    }

    public void pushFeatureBuffer(byte[] dataBytes) { //actively accepts single EMG arrays and runs calculations when window is reached

        // System.out.println(samplesClassifier.size());
        sendWindow = ArrayUtils.addAll(sendWindow, dataBytes);

        Number[] dataObj = ArrayUtils.toObject(dataBytes);
        ArrayList<Number> emg_data_list = new ArrayList<Number>(Arrays.asList(dataObj));
        DataVector data = new DataVector(true, 1, dataBytes.length, emg_data_list, System.currentTimeMillis());

        samplebuffer.add(ibuf, data);

        for(int i = 0; i < dataBytes.length; i++) {
            windowRaw[rawIncrement][i] = dataBytes[i];
        }
        if(rawIncrement < 51) {
            rawIncrement++;
        } else {
            if(train) {
                classifier.addToRaw(windowRaw, currentClass);
                windowRaw = new int[52][8];
                batchIncrement++;
            } else if (classify && classifier.useRaw()) {
                pushClassifier(windowRaw, aux[0]);
                windowRaw = new int[52][8];
            }
            rawIncrement = 0;
        }

        if (samplebuffer.size() > bufsize)//limit size of buffer to bufsize
            samplebuffer.remove(samplebuffer.size() - 1);

        if (train) {
            aux[0].setFlag(currentClass);
        }

        if (ibuf == winnext)//start calculating
        {

            /*********************************************** Beginning of cloud stuff ***********************************************/

//            startCalc = System.nanoTime();
//            byte cloudControl = 0;
//            if (getClassify()) {
//                cloudControl = 1;
//            } else if (getTrain()) {
//                cloudControl = 2;
//            }
//            long clientTime = (System.nanoTime() - time1);
//            sendWindow = ArrayUtils.addAll(sendWindow, longToBytes(clientTime));
//            Log.d("window length: ", String.valueOf(sendWindow.length));
//            thread.send(sendWindow); //TCP Connection
//            //new Lambda.LTask().execute(sendWindow); //Calls Lambda
//            sendWindow = new byte[1];
//            sendWindow[0] = cloudControl;

            /********************************************** End of cloud stuff ***********************************************/

            /*********************************************** Start of Local Process***********************************************/

            lastCall = winnext;
            firstCall = (lastCall - winsize + bufsize + 1) % bufsize;
            startFeature = System.nanoTime();
            featureVector = featCalc(samplebuffer);

            imuFeatureVector = featCalcIMU(imusamplebuffer);
//            imuFeatureVector = new twoDimArray();
            aux = buildDataVector(featureVector, imuFeatureVector);

            aux[0].setTimestamp(data.getTimestamp());
            if (train) {
                aux[0].setFlag(currentClass);//dont need this?
                if (aux != null) {
                    pushClassifyTrainer(aux);
                }
                if(classifier.useRaw()) {
                    Log.i("cnnTrainset" + cnnTrainSet.size(), ":" + CNN_BATCH*(currentClass+1));
                    if(batchIncrement >= CNN_BATCH * (currentClass+1)) {
                        setTrain(false);
                        while (cnnTrainSet.size() > CNN_BATCH * (currentClass+1)) {
                            cnnTrainSet.remove(CNN_BATCH * (currentClass+1) - 1);
                        }
                        currentClass++;
                    }
                } else {
                    if (samplesClassifier.size() % (nSamples) == 0 && samplesClassifier.size() != 0) { //triggers
                        setTrain(false);
                        currentClass++;
                    }
                }
            } else if (classify && !classifier.useRaw()) {
                pushClassifier(windowRaw, aux[0]);
            }

            /*********************************************** End of Local Processes ***********************************************/

            winnext = (winnext + winincr) % bufsize;
        }
        ibuf = ++ibuf & (bufsize - 1); //make buffer circular
    }

    private DataVector[] buildDataVector(twoDimArray featureVector, twoDimArray imuFeatureVector)//ignoring grid and imu for now, assuming all features are selected
    {
        // Count total EMG features to send

        int emgct = numFeatSelected * nSensors;
        numFeatSelected = 6; //Resets the number of features selected to 6

        ArrayList<Number> temp = new ArrayList<Number>(emgct);
        DataVector dvec1 = null;

        int n = 0;
        int k = 0;
        int tempIndex = 0;
        int temp1Index = 0;

        for (int i = 0; i < nFeatures; i++) {
            //group features per sensor
            if (featSelected[i]) {
                for (int j = 0; j < nSensors; j++) {
                    temp.add(n, featureVector.getMatrixValue(tempIndex, j));
                    n++;
                }
            }
            tempIndex++;
        }

        for (int j = 0; j < nDimensions; j++) {
            if (imuSelected[j]) {
                for (int i = 0; i < nIMUFeatures; i++) {
                    temp.add(n, imuFeatureVector.getMatrixValue(i, j));
                    n++;
                }
            }
        }

        if (getTrain()) {//during training we wan to save all 8 sensor data
            ArrayList<Number> temp1 = new ArrayList<Number>(emgct);
            for (int i = 0; i < nFeatures; i++) {
                //group features per sensor
                for (int j = 0; j < nSensors; j++) {
                    temp1.add(k, featureVector.getMatrixValue(temp1Index, j));
                    k++;
                }
                temp1Index++;
            }
            for (int i = 0; i < nIMUFeatures; i++) {
                for (int j = 0; j < nDimensions; j++) {
                    temp1.add(k, imuFeatureVector.getMatrixValue(i, j));
                    k++;
                }
            }

            dvec1 = new DataVector(true, 0, temp.size(), temp1, 0000000);
        }

        DataVector dvec = new DataVector(true, 0, temp.size(), temp, 0000000);//nIMU must become dynamic with UI

        DataVector dvecArr[] = {dvec, dvec1};
        return dvecArr;
    }

    private twoDimArray featCalc(ArrayList<DataVector> samplebuf) {
        ArrayList<ArrayList<Float>> AUMatrix = new ArrayList<>();
        byte signLast;
        byte slopLast;
        int j, k;
        double Delta_2;
        float[] sMAVS = new float[nSensors];//Used to store the values of the MAV from all 8 channels and used by the sMAV feature
        float MMAV = 0;

        featemg = new twoDimArray();
        featemg.createMatrix(6, nSensors);

        //for each sensor calculate features
        for (int sensor = 0; sensor < nSensors; sensor++) {//loop through each EMG pod (8)
            k = (firstCall + bufsize - 1) % bufsize;    //one before window start   // (41 - 40 + 1 = 2) - 1
            j = (k + bufsize - 1) % bufsize;    //        two before ws(firstCall)  // 0
            ArrayList<Float> tempAU = new ArrayList<>();

            signLast = 0;
            slopLast = 0;

            //Some threshold for zero crossings and slope changes
            Delta_2 = samplebuf.get(k).getVectorData().get(sensor).floatValue() - samplebuf.get(j).getVectorData().get(sensor).floatValue(); //index out of bounds exception

            if (Delta_2 > threshold) {
                slopLast += 4;
            }
            if (Delta_2 < -threshold) {
                slopLast += 8;
            }

            //Beginning of Window???
            if (samplebuf.get(j).getVectorData().get(sensor).floatValue() > threshold) {
                signLast = (byte) (nSensors/2);
            } //Set to a high value?
            if (samplebuf.get(j).getVectorData().get(sensor).floatValue() < -threshold) {
                signLast = (byte) nSensors;
            }//set to a low value?

            for (int i = 0; i < (winsize); i++) //-2
            {
                j = k;                 //prev     //1 - 40
                k = (j + 1) % bufsize; //current  //2 - 41

                Delta_2 = samplebuf.get(k).getVectorData().get(sensor).floatValue() - samplebuf.get(j).getVectorData().get(sensor).floatValue();

                if (samplebuf.get(k).getVectorData().get(sensor).floatValue() > threshold) {
                    signLast += 1;
                }
                if (samplebuf.get(k).getVectorData().get(sensor).floatValue() < -threshold) {
                    signLast += 2;
                }
                if (Delta_2 > threshold) {
                    slopLast += 1;
                }
                if (Delta_2 < -threshold) {
                    slopLast += 2;
                }
                if ((signLast == 9 || signLast == 6)) {
                    featemg.setMatrixValue(2, sensor, featemg.getMatrixValue(2, sensor) + 1);
                }
                if ((slopLast == 9 || slopLast == 6)) {
                    featemg.setMatrixValue(3, sensor, featemg.getMatrixValue(3, sensor) + 1);
                }

                signLast = (byte) ((byte) (signLast << 2) & (byte) 15);
                slopLast = (byte) ((byte) (slopLast << 2) & (byte) 15);

                featemg.setMatrixValue(0, sensor, featemg.getMatrixValue(0, sensor) + Math.abs(samplebuf.get(k).getVectorData().get(sensor).floatValue()));
                featemg.setMatrixValue(1, sensor, featemg.getMatrixValue(1, sensor) + (float) Math.abs(Delta_2));
                tempAU.add(samplebuf.get(k).getVectorData().get(sensor).floatValue());
            }

            featemg.setMatrixValue(0, sensor, featemg.getMatrixValue(0, sensor) / winsize);
            featemg.setMatrixValue(1, sensor, featemg.getMatrixValue(1, sensor) / winsize);
            featemg.setMatrixValue(2, sensor, featemg.getMatrixValue(2, sensor) * 100 / winsize);
            featemg.setMatrixValue(3, sensor, featemg.getMatrixValue(3, sensor) * 100 / winsize);

            //Feature 4 smav
            sMAVS[sensor] = featemg.getMatrixValue(0, sensor);
            MMAV += featemg.getMatrixValue(0, sensor);

            if (sensor == (nSensors - 1)) {//don't want to use all
                for (int l = 0; l < nSensors; l++) {
                    featemg.setMatrixValue(4, l, (sMAVS[l] / (MMAV / nSensors)) * 25);
                }

                featemg.setMatrixValue(4, nSensors - 1, MMAV / nSensors);
                plotter.pushFeaturePlotter(featemg);
            }
            AUMatrix.add(tempAU);
        }

        for (int sensorIt = 0; sensorIt < nSensors; sensorIt++) {
            int sensorNext = sensorIt + 1;
            if (sensorNext == nSensors) {
                sensorNext = 0;
            }
            float tempValue = 0;
            for (int it = 0; it < winsize; it++) {
                tempValue += Math.abs((AUMatrix.get(sensorIt).get(it).floatValue() / featemg.getMatrixValue(0, sensorIt)) -
                        (AUMatrix.get(sensorNext).get(it).floatValue() / featemg.getMatrixValue(0, sensorNext)));
            }
            //Feature 5 Adjacency Uniqueness
            if(!isFinite(tempValue)) {
                tempValue = 0;
            }
            featemg.setMatrixValue(5, sensorIt, (tempValue / winsize) * 25); // multiply by 25 to scale the value of tempValue/winsize
        }

        return featemg;
    }

    private void setWindowSize(int newWinsize) {
        winsize = newWinsize;
        if (winsize + 10 > bufsize) {
            bufsize = winsize + 10;
            samplebuffer = null;//delete[] samplebuf;
            samplebuffer = new ArrayList<DataVector>(bufsize); //samplebuf = new DataVector[bufsize]; //arraylist holding bufsize amount of datavectors
        }
        ibuf = 0;
        winnext = winsize + 1;
    }

    private void setWindowIncrement(int newWinincr) {
        if (winincr + 10 > bufsize) {
            bufsize = winincr + 10;
            samplebuffer = null;//delete[] samplebuf;
            samplebuffer = new ArrayList<DataVector>(bufsize); //samplebuf = new DataVector[bufsize]; //arraylist holding bufsize amount of datavectors
        }
        winincr = newWinincr;
    }
//reset

    public void pushIMUFeatureBuffer(DataVector data) {
        imusamplebuffer.add(imuibuf, data);
        if (imusamplebuffer.size() > bufsize)//limit size of buffer to bufsize
            imusamplebuffer.remove(samplebuffer.size() - 1);
        imuibuf = ++imuibuf % (bufsize);

        if (data.getFlag() == 1) {
//            float w, x, y,z;
//            short int16 = (short)(((((byte)data.getValue(0)) & 0xFF) << 8) | (((byte) data.getValue(1)) & 0xFF));
//            w = int16;
//            int16 = (short)(((((byte)data.getValue(2)) & 0xFF) << 8) | (((byte) data.getValue(3)) & 0xFF));
//            x = int16;
//            int16 = (short)(((((byte)data.getValue(4)) & 0xFF) << 8) | (((byte) data.getValue(5)) & 0xFF));
//            y = int16;
//            int16 = (short)(((((byte)data.getValue(6)) & 0xFF) << 8) | (((byte) data.getValue(7)) & 0xFF));
//            z = int16;
            //SendToUnity.setQuaternion((float) data.getValue(0).byteValue(), (float) data.getValue(1).byteValue(), (float) data.getValue(2).byteValue(), (float) data.getValue(3).byteValue());
            //SendToUnity.setQuaternion(w, x, y, z);
        }
    }

    public twoDimArray featCalcIMU(ArrayList<DataVector> imusamplebuf) {
        int i;
        float sum;
        featimu = new twoDimArray();
        featimu.createMatrix(nIMUFeatures, nDimensions);
        for (int ft = 0; ft < nIMUFeatures; ft++) {
            for (int d = 0; d < nDimensions; d++) {
                i = (imuibuf + bufsize - (winsize / 4)) % bufsize;
                sum = 0;
                while (i != imuibuf) {
//                    sum += imusamplebuf.get(i).getValue(d).floatValue();
                    i = (i + bufsize + 1) % bufsize;
                }
                featimu.setMatrixValue(ft, d, sum / (winsize / 4));
            }
        }
        return featimu;
    }

    public void setFeatSelected(boolean[] boos) {
        featSelected = boos;
    }

    public void setIMUSelected(boolean[] boos) {
        imuSelected = boos;
    }

    public void setNumIMUSelected(int imus) {
        nIMUSensors = imus;
    }

    public void setNumFeatSelected(int feats) {
        nFeatures = feats;
    }

    public void startBTConnection(String mac){
        mBluetoothConnection = new BluetoothConnection(mac);
    }
}

//Two dimensional array class made to help in the implementation of featEMG
class twoDimArray {

    //matrix is our featEMG matrix
    ArrayList<ArrayList<Number>> matrix = new ArrayList<ArrayList<Number>>();
    int numRow;
    int numCol;

    //Init matrix to the desired dimensions all with 0
    //Note: row refers to nFeatures and columns refers to nSensors
    public void createMatrix(int numRow, int numCol) {
        this.numRow = numRow;
        this.numCol = numCol;
        for (int i = 0; i < numRow; i++) {
            ArrayList<Number> innerArray = new ArrayList<Number>();
            matrix.add(innerArray);
            for (int j = 0; j < numCol; j++) {
                innerArray.add((float) 0);
            }
        }
    }

    //Get value at specified row and column
    public float getMatrixValue(int inRow, int inCol) {
        return matrix.get(inRow).get(inCol).floatValue();
    }

    //Set value at specified row and column
    public void setMatrixValue(int numRow, int numCol, float data) {
        ArrayList<Number> temp;
        temp = matrix.get(numRow);
        temp.set(numCol, data);
        matrix.set(numRow, temp);
    }

    public ArrayList<DataVector> getDataVector() {
        ArrayList<DataVector> data = new ArrayList<>();
        for (int i = 0; i < numRow; i++) {
            ArrayList<Number> row = this.getInnerArray(i);
            data.add(new DataVector(0, row.size(), row));
        }
        return data;
    }

    //Return specific ROW
    public ArrayList<Number> getInnerArray(int inRow) {
        return matrix.get(inRow);
    }

    public void addRow(ArrayList inRow) {
        matrix.add(inRow);
    }

    public int getNumRow() {
        return this.numRow;
    }
    public int getNumCol() {
        return this.numCol;
    }
}