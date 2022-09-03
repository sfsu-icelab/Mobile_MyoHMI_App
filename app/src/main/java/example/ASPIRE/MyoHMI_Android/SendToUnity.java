package example.ASPIRE.MyoHMI_Android;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import androidx.annotation.Nullable;
import android.util.Log;


public class SendToUnity extends Service {

    private static String gesture = "ZERO GESTURES";
    private static String defaultGesture = "ZERO GESTURES";
    private static String Quaternion = "0.00,0.01,0.02,0.03";
    private static String[] fullMessage;
    private final Handler handler = new Handler();

    private Runnable sendData = new Runnable() {
        // the specific method which will be executed by the handler
        public void run() {

            // sendIntent is the object that will be broadcast outside our app
            Intent sendIntent = new Intent();

            // Flags for the Intent to allow for background access
            sendIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_FROM_BACKGROUND | Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

            // Intent Identifier
            sendIntent.setAction("com.test.sendintent.IntentToUnity");


            fullMessage = new String[2];
            if(!gesture.equals("ZERO GESTURES")){
                fullMessage[0] = gesture;
            } else {
                fullMessage[0] = defaultGesture;
            }
            fullMessage[1] = Quaternion;

            //data of the Intent object
            sendIntent.putExtra(Intent.EXTRA_TEXT, fullMessage);


            // Broadcasts the Intent
            sendBroadcast(sendIntent);

            // In our case we run this method each 0.5 second with postDelayed
            for (int i = 0; i < fullMessage.length; i++) {
                Log.d("SendToUnityMessage", fullMessage[i]);
            }

            handler.removeCallbacks(this);
            handler.postDelayed(this, 120);
        }
    };

    public static void setGesture(String gesture1) {
        gesture = gesture1;
    }

    public static void setDefaultGesture(String gesture1) {
        defaultGesture = gesture1;
    }

    public static void setQuaternion(float w, float x, float y, float z) {
        Quaternion = String.valueOf(w) + "," + String.valueOf(x) + "," + String.valueOf(y) + "," + String.valueOf(z);
    }

    // When service is started
    @Override
    public void onStart(Intent intent, int startid) {
        // We first start the Handler
        handler.removeCallbacks(sendData);
        handler.postDelayed(sendData, 120);
        Log.d("SendToUnity", "Started");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {


        Intent sendIntent = new Intent();

        sendIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_FROM_BACKGROUND | Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        sendIntent.setAction("com.test.sendintent.IntentToUnity");


        gesture = "ZERO GESTURES";
        defaultGesture = "ZERO GESTURES";
        Quaternion = "0.00,0.01,0.02,0.03";

        fullMessage = new String[2];
        if(!gesture.equals("ZERO GESTURES")){
            fullMessage[0] = gesture;
        } else {
            fullMessage[0] = defaultGesture;
        }
        fullMessage[1] = Quaternion;

        //data of the Intent object
        sendIntent.putExtra(Intent.EXTRA_TEXT, fullMessage);

        // Broadcasts the Intent
        sendBroadcast(sendIntent);

        for (int i = 0; i < fullMessage.length; i++) {
            Log.d("SendToUnityMessage", fullMessage[i]);
        }

        super.onTaskRemoved(rootIntent);
        Log.d("SendToUnity", "Stopped");

        stopSelf();
    }


}