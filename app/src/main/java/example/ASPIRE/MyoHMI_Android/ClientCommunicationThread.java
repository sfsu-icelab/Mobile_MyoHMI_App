package example.ASPIRE.MyoHMI_Android;

import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class ClientCommunicationThread extends Thread {

    public final static int TCP_SERVER_PORT = 9941;
    static int lastpredC = 0;
    static int lastpredR = 0;
    static long regTime = 0;
    private final String ec2ip = "34.215.131.221";
    private final String alexHomeip = "2601:645:c100:b669:ad86:cf34:9b81:48e3";
    private final String icelabip = "192.168.0.100";//"34.213.61.15";
    private final String dragonip = "2601:645:c100:b669:0:2bff:feed:2e50";
    private final String sfStateip = "10.143.132.221";

    byte[] buffer = new byte[512];

    int length;
    private ArrayList<byte[]> mMessages = new ArrayList<>();
    private String mServer;
    private boolean mRun = true;

    public ClientCommunicationThread() {
        this.mServer = ec2ip;
    }

    public static void calculateDiff(int choice, int cloudOrReg) {

        if (cloudOrReg == 0) {//cloud
            if (lastpredC != choice && lastpredR == choice) {
                Log.d("Print Time Diff: ", String.valueOf(System.currentTimeMillis() - regTime));
            }
            lastpredC = choice;
        } else {//regular
            if (lastpredR != choice) {
                regTime = System.currentTimeMillis();
            }
            lastpredR = choice;
        }
    }

    @Override
    public void run() {

        while (mRun) {
            Socket s = null;
            try {

                s = new Socket(mServer, TCP_SERVER_PORT);
                DataInputStream input = new DataInputStream(s.getInputStream());

                while (mRun) {
                    if ((length = input.read(buffer)) != -1)
                        Log.d("Cloud Prediction: ", String.valueOf(buffer[0]) + "  :  " + String.valueOf(System.currentTimeMillis()));
//                        calculateDiff((int)buffer[0], 0);
                    FeatureCalculator.getThing(System.nanoTime());
                }

            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                //close connection
                if (s != null) {
                    try {
                        s.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void close() {
        mRun = false;
    }
}