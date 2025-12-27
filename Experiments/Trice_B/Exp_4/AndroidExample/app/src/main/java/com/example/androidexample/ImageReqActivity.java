package com.example.androidexample;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;

public class ImageReqActivity extends AppCompatActivity {

    // Declare the Button and ImageView variables
    private Button btnImageReq;
    private ImageView imageView;

    // Define the URL of the image to be requested
    public static final String URL_IMAGE = "http://sharding.org/outgoing/temp/testimg3.jpg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the layout for this activity
        setContentView(R.layout.activity_image_req);

        // Initialize the button and image view using findViewById
        btnImageReq = findViewById(R.id.btnImageReq);
        imageView = findViewById(R.id.imgView);

        // Set an OnClickListener for the button to trigger the image request when clicked
        btnImageReq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call method to make image request when the button is clicked
                makeImageRequest();
            }
        });
    }

    /**
     * Method to make an image request using Volley
     */
    private void makeImageRequest() {
        // Create a new ImageRequest object to request the image from the URL
        ImageRequest imageRequest = new ImageRequest(
                URL_IMAGE, // URL of the image to fetch
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap response) {
                        // Called when the image is successfully fetched
                        // Set the fetched image (Bitmap) to the ImageView
                        imageView.setImageBitmap(response);
                        // Log a success message
                        Log.d("Image Request", "Image loaded successfully.");
                    }
                },
                0, // Width of the image (0 to use the original width)
                0, // Height of the image (0 to use the original height)
                ImageView.ScaleType.FIT_XY, // Scale type for the image (adjust to fit)
                Bitmap.Config.RGB_565, // Bitmap configuration for better memory usage
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Called when an error occurs during the image request
                        // Log the error for debugging purposes
                        Log.e("Volley Error", error.toString());
                        // Display a Toast message to the user about the error
                        Toast.makeText(ImageReqActivity.this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Add the ImageRequest to the Volley request queue for processing
        VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(imageRequest);
    }
}
