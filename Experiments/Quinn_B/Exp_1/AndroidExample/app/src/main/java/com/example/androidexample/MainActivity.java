package com.example.androidexample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import android.widget.TextView;

import android.graphics.Color;            // To set text color
import android.graphics.Typeface;         // To change font family/style


import org.w3c.dom.Text;

/*

1. To run this project, open the directory "Android Example", otherwise it may not recognize the file structure properly

2. Ensure you are using a compatible version of gradle, to do so you need to check 2 files.

    AndroidExample/Gradle Scripts/build.gradle
    Here, you will have this block of code. Ensure it is set to a compatible version,
    in this case 8.12.2 should be sufficient:
        plugins {
            id 'com.android.application' version '8.12.2' apply false
        }

    Gradle Scripts/gradle-wrapper.properties

3. This file is what actually determines the Gradle version used, 8.13 should be sufficient.
    "distributionUrl=https\://services.gradle.org/distributions/gradle-8.13-bin.zip" ---Edit the version if needed

4. You might be instructed by the plugin manager to upgrade plugins, accept it and you may execute the default selected options.

5. Press "Sync project with gradle files" located at the top right of Android Studio,
   once this is complete you will be able to run the app

   This version is compatible with both JDK 17 and 21. The Java version you want to use can be
   altered in Android Studio->Settings->Build, Execution, Deployment->Build Tools->Gradle

 */

public class MainActivity extends AppCompatActivity {

    private TextView messageText;   // define message textview variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);             // link to Main activity XML

        // Changes the background color
        int bg = Color.BLACK;
        getWindow().getDecorView().setBackgroundColor(bg);

        /* initialize UI elements */
        messageText = findViewById(R.id.main_msg_txt);      // link to message textview in the Main activity XML
        messageText.setText("Hello Universe!");

        // Changes the text, font, color, and size
        messageText.setTypeface(Typeface.SANS_SERIF,
                Typeface.BOLD);
        messageText.setTextColor(Color.BLUE);
        messageText.setTextSize(60);
        messageText.setBackgroundColor(Color.WHITE);


        // Another line of text
        messageText = findViewById(R.id.extra_txt);
        messageText.setText("What is the meaning of life?");

        messageText.setTypeface(Typeface.SERIF,
                Typeface.ITALIC);
        messageText.setTextColor(Color.RED);
        messageText.setTextSize(40);

        messageText.setBackgroundColor(Color.GREEN);

    }
}