package com.example.androidexample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Random;// For random 1–5
import android.graphics.Color;// For text color

import org.w3c.dom.Text;


public class MainActivity extends AppCompatActivity {

    private TextView messageText;     // define message textview variable
    private Button counterButton;     // define counter button variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);             // link to Main activity XML

        /* initialize UI elements */
        messageText = findViewById(R.id.main_msg_txt);      // link to message textview in the Main activity XML
        counterButton = findViewById(R.id.main_counter_btn);// link to counter button in the Main activity XML

        /* extract data passed into this activity from another activity */
        Bundle extras = getIntent().getExtras();
        if(extras == null) {
            messageText.setText("Intent Example");
        } else {
            String number = extras.getString("NUM");  // this will come from LoginActivity / CounterActivity
            messageText.setText("The number was " + number);

            // Compares picked number to a random 1–5 and shows result
            int picked = 0;
            try { picked = Integer.parseInt(number); } catch (Exception ignore) {}
            int secret = 1 + new Random().nextInt(5); // 1-5

            String result = (picked == secret) ? "Good job!" : "Try again.";
            messageText.setText("You picked " + picked + ". The number was " + secret + ". " + result);
            // Success = Green, Failure = Red
            messageText.setTextColor((picked == secret) ? Color.GREEN : Color.RED);
        }

        /* click listener on counter button pressed */
        counterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /* when counter button is pressed, use intent to switch to Counter Activity */
                Intent intent = new Intent(MainActivity.this, CounterActivity.class);
                startActivity(intent);
            }
        });
    }
}
