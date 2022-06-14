package example.ASPIRE.MyoHMI_Android;

import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

public class exportEMGRequest extends StringRequest {

    private static final String EMG_REQUEST_URL = "http://ec2-52-13-172-17.us-west-2.compute.amazonaws.com/exportEMGData.php";
    //for each of columns in EMG mysql base
    String[] columnNames = {"s1_MAV", "s2_MAV", "s3_MAV", "s4_MAV", "s5_MAV", "s6_MAV", "s7_MAV", "s8_MAV",
            "s1_WAV", "s2_WAV", "s3_WAV", "s4_WAV", "s5_WAV", "s6_WAV", "s7_WAV", "s8_WAV",
            "s1_Turns", "s2_Turns", "s3_Turns", "s4_Turns", "s5_Turns", "s6_Turns", "s7_Turns", "s8_Turns",
            "s1_Zeros", "s2_Zeros", "s3_Zeros", "s4_Zeros", "s5_Zeros", "s6_Zeros", "s7_Zeros", "s8_Zeros",
            "s1_SMAV", "s2_SMAV", "s3_SMAV", "s4_SMAV", "s5_SMAV", "s6_SMAV", "s7_SMAV", "s8_SMAV",
            "s1_AdjUnique", "s2_AdjUnique", "s3_AdjUnique", "s4_AdjUnique", "s5_AdjUnique", "s6_AdjUnique", "s7_AdjUnique", "s8_AdjUnique"
    };
    private Map<String, String> params;

    public exportEMGRequest(DataVector data, String gesture, Response.Listener<String> listener) {
        super(Method.POST, EMG_REQUEST_URL, listener, null);
        params = new HashMap<>();

        params.put("username", LoginActivity.user);
        System.out.println("username:" + LoginActivity.user);
        params.put("gesture_output", gesture);

        for (int x = 0; x < columnNames.length; x++) {
            params.put(columnNames[x], data.getValue(x).toString());
        }
    }

    @Override
    public Map<String, String> getParams() {
        return params;
    }
}
