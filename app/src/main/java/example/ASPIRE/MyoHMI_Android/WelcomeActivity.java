package example.ASPIRE.MyoHMI_Android;

import static example.ASPIRE.MyoHMI_Android.Classifier.activity;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.concurrent.ExecutionException;

public class WelcomeActivity extends AppCompatActivity {
    private static int SPLASH_TIME_OUT = 3000;

    TextView title;
    ImageView logo;

    CountDownTimer downTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        title = findViewById(R.id.title);
        logo = findViewById(R.id.logo);

            /*final MediaPlayer welcome_sound = MediaPlayer.create(getApplicationContext(), R.raw.welcome_sound);
            welcome_sound.start();*/

        timer();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {


                Intent homeIntent = new Intent(WelcomeActivity.this, MainActivity.class);
                startActivity(homeIntent);
                finish();
            }


        }, SPLASH_TIME_OUT);
    }

    public void timer() {
        final MediaPlayer welcome_sound = MediaPlayer.create(getApplicationContext(), R.raw.welcome_sound);

        final long timeChal = 1000;
        final int countDown = 1000;
        downTimer = new CountDownTimer(timeChal, countDown) { // adjust the milli seconds here
            //31000 30 sec
            //61000 1 min
            //91000 90 sec
            //121000 2 min
            //181000 3 min
            public void onTick(long millisUntilFinished) {

            }

            public void onFinish() {
                title.setVisibility(View.VISIBLE);
                logo.setVisibility(View.VISIBLE);
//                welcome_sound.start();

            }
        };
        downTimer.start();
    }

}


