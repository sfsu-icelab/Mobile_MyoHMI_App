package example.ASPIRE.MyoHMI_Android;

/**
 * Created by Alex on 12/21/2017.
 */

import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunction;

public interface LambdaInterface {

    /**
     * Invoke the Lambda function "AndroidBackendLambdaFunction".
     * The function name is the method name.
     */
    @LambdaFunction
    int giveIncrement(byte[] data);

}

