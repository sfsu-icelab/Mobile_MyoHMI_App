package example.ASPIRE.MyoHMI_Android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ReceiveFromUnity extends BroadcastReceiver {

    // text is updated by Unity
    public static String text = "No Vibrate";
    private static MyoGattCallback mMyoCallback;
    private MyoCommandList myoCommandList = new MyoCommandList();

    // static method to create our receiver object
    //Unity scripts create this object

    public ReceiveFromUnity() {

    }
    public static void OnStart() {
        Log.d("ReceiveFromUnity", "Started");
    }

    public static void setmMyoCallback(MyoGattCallback mMyoCallback1) {
        ReceiveFromUnity.mMyoCallback = mMyoCallback1;
    }

    // Triggered when an Intent is catched
    @Override
    public void onReceive(Context context, Intent intent) {
        // We get the data the Intent has
        String receivedIntent = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (receivedIntent != null) {
            // We assigned it to our static variable
            text = receivedIntent.toUpperCase();
            //Log.d("ReceiveFromUnity", text);

            if (text.equals("LONG")) {
                mMyoCallback.setMyoControlCommand(myoCommandList.sendVibration3());
            } else if (text.equals("SHORT")) {
                mMyoCallback.setMyoControlCommand(myoCommandList.sendVibration1());
            } else if (text.equals("MEDIUM")) {
                mMyoCallback.setMyoControlCommand(myoCommandList.sendVibration2());
            }
        }
    }
}