package example.ASPIRE.MyoHMI_Android;

import android.os.AsyncTask;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaInvokerFactory;

/**
 * Created by Alex on 12/24/2017.
 */

public class Lambda {

    static CognitoCachingCredentialsProvider cognitoProvider;
    static LambdaInvokerFactory factory;

    static LambdaInterface myInterface;

    public Lambda(CognitoCachingCredentialsProvider cog, LambdaInvokerFactory factory) { //to pass context in from main activity
        cognitoProvider = cog;
        this.factory = factory;
        myInterface = factory.build(LambdaInterface.class);
    }

    public static class LTask extends AsyncTask<byte[], Void, Integer> {
        @Override
        protected Integer doInBackground(byte[]... params) {
            // invoke "echo" method. In case it fails, it will throw a
            // LambdaFunctionException.

            try {
                return myInterface.giveIncrement(params[0]);
            } catch (Exception lfe) {
                Log.e("Tag", "Failed to invoke echo", lfe);
                //System.out.println(lfe);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result != null) {
                int prediction = result.intValue();
                System.out.print("Lambda Prediction: !!!!! ");
                System.out.println(prediction);
            } else {
                System.out.println("result is null");
            }
        }
    }

}
