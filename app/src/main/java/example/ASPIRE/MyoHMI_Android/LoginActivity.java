package example.ASPIRE.MyoHMI_Android;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.examples.transfer.api.TransferLearningModel;

import java.util.concurrent.ExecutionException;

public class LoginActivity extends AppCompatActivity {

    public static boolean loggedIn = false;
    public static String user = "no_login";
    private CNN cnn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        final EditText etUsername = (EditText) findViewById(R.id.etUsername);
        final EditText etPassword = (EditText) findViewById(R.id.etPassword);
        final TextView tvRegisterLink = (TextView) findViewById(R.id.tvRegisterLink);
        final Button bLogin = (Button) findViewById(R.id.bSignIn);
        final TextView tvContinueLink = (TextView) findViewById(R.id.tvcontinue);
        //cnn.initialize().addOnFailureListener( e -> Log.e("CNN Error", "Error to setting up gesture classifier.", e) );

        /*cnn = new CNN(this);

        float[][][] f = new float[32][32][3];

        f[0][10][0] = 30F;

        TransferLearningModel.Prediction[] predictions = cnn.predict(f);

        for (TransferLearningModel.Prediction prediction : predictions) {
            Log.i("training" + prediction.getClassName(), String.valueOf(prediction.getConfidence()));
        }
        try {
            for(int i = 0; i < 1; i++)
                cnn.addSample(f, "1").get();
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to add sample to model", e.getCause());
        } catch (InterruptedException e) {
            // no-op
        }
        cnn.enableTraining((epoch, loss) -> {
            Log.i("training" + epoch, String.valueOf(loss));
            if(epoch >= 99) {
                cnn.disableTraining();
                TransferLearningModel.Prediction[] predict = cnn.predict(f);

                for (TransferLearningModel.Prediction prediction : predict) {
                    Log.i("training" + prediction.getClassName(), String.valueOf(prediction.getConfidence()));
                }
            }
        });*/
        tvRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //cnn.classifyAsync(new Float[]{21.6125F,66.125F,67.325F,56.4875F,40.1F,12.05F,15.4375F,19.9625F}).addOnSuccessListener(result -> Log.i("Gesture", result));
                Intent registerIntent = new Intent(LoginActivity.this, RegisterActivity.class);
                LoginActivity.this.startActivity(registerIntent);
            }
        });

        tvContinueLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent continueIntent = new Intent(LoginActivity.this, MainActivity.class);
                LoginActivity.this.startActivity(continueIntent);
            }
        });

        bLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String username = etUsername.getText().toString();
                user = username;
                final String password = etPassword.getText().toString();

                // Response received from the server
                Response.Listener<String> responseListener = new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            boolean success = jsonResponse.getBoolean("success");

                            if (success) {

                                //if sucesss lets also get the load data screen
                                loggedIn = true;

                                Log.d("User has logged in", "");

                                String name = jsonResponse.getString("name");
                                int age = jsonResponse.getInt("age");

                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                intent.putExtra("name", name);
                                intent.putExtra("age", age);
                                intent.putExtra("username", username);
                                LoginActivity.this.startActivity(intent);

                            } else {
                                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                                builder.setMessage("Login Failed")
                                        .setNegativeButton("Retry", null)
                                        .create()
                                        .show();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                };

                LoginRequest loginRequest = new LoginRequest(username, password, responseListener);
                RequestQueue queue = Volley.newRequestQueue(LoginActivity.this);
                queue.add(loginRequest);
            }
        });
    }

    public boolean getLoggedIn() {
        return loggedIn;
    }
}
