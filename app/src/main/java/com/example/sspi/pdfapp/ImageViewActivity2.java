package com.example.sspi.pdfapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;

public class ImageViewActivity2 extends AppCompatActivity {

    private ImageView imageView;
    private Bitmap sampleImage;
    private SparseArray<TextBlock> textBlocks;

    // Variables to track text selection
    private float startX;
    private float startY;

    // Initialize the button coordinates
    float buttonLeft = 100; // Adjust the button position as needed
    float buttonTop = 150;  // Adjust the button position as needed
    float buttonRight = 250; // Adjust the button position as needed
    float buttonBottom = 250; // Adjust the button position as needed

    String selectedLine = null; //to store the selected line
    Boolean hasShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);

        String imgUri = getIntent().getStringExtra("imgUri");
        imageView = findViewById(R.id.img);

        Button ocrButton = findViewById(R.id.ocrButton);

        try {
            sampleImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.parse(imgUri));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Glide.with(this).load(imgUri).into(imageView);
        ocrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTextRecognition();
            }
        });

        // Initialize textBlocks
        textBlocks = new SparseArray<>();


        // Set a touch listener on the ImageView
        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                // Convert the view coordinates to image coordinates
                Matrix inverse = new Matrix();
                imageView.getImageMatrix().invert(inverse);
                float[] touchPoint = {motionEvent.getX(), motionEvent.getY()};
                inverse.mapPoints(touchPoint);

                startX = touchPoint[0];
                startY = touchPoint[1];

//                Log.d("Coordinates", "X " + startX + " Y " + startY + " height=" + view.getHeight());
                highlightLineContainingTouchPoint(startX, startY);
                return true;
            }
        });

    }

    private void startTextRecognition() {
        // Initialize the TextRecognizer
        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        if (!textRecognizer.isOperational()) {
            return;
        }

        // Create a Frame from the loaded image
        Frame frame = new Frame.Builder().setBitmap(sampleImage).build();

        // Perform text recognition
        textBlocks = textRecognizer.detect(frame);
    }



    public void highlightLineContainingTouchPoint(float x, float y) {
        // Initialize a new Bitmap for highlighting
        Bitmap highlightedImage = sampleImage.copy(sampleImage.getConfig(), true);
        Canvas canvas = new Canvas(highlightedImage);
        Paint paint = new Paint();
        paint.setColor(Color.YELLOW);
        paint.setAlpha(128);

        //for copy button
        Paint cPaint = new Paint();
        cPaint.setColor(Color.BLACK);
        cPaint.setAlpha(190);

        //for Text
        Paint tPaint = new Paint();
        tPaint.setColor(Color.WHITE);
        tPaint.setTextSize(38);
        tPaint.setAntiAlias(true);


        // Check if the touch point is within the copy button background
        if(!hasShown){
            if (x >= buttonLeft && x <= buttonRight && y >= buttonTop && y <= buttonBottom) {
                showToast(selectedLine);
                hasShown = true;
            }
        }



        // Iterate over textBlocks to find the line containing the touch point
        for (int i = 0; i < textBlocks.size(); i++) {
            TextBlock textBlock = textBlocks.valueAt(i);
            for (Text line : textBlock.getComponents()) {
                // Check if the touch point is within the bounding box of the line
                if (x >= line.getBoundingBox().left &&
                        x <= line.getBoundingBox().right &&
                        y >= line.getBoundingBox().top &&
                        y <= line.getBoundingBox().bottom) {
                    float left = line.getBoundingBox().left;
                    float top = line.getBoundingBox().top;
                    float bottom = line.getBoundingBox().bottom;
                    float right = line.getBoundingBox().right;
//                    Log.e("pdu", "Line top" + top + "Bottom" + bottom);

                    // Highlight the line containing the touch point
                    canvas.drawRect(left, top, right, bottom, paint);

                    //finding the coordinates of the button
                    buttonLeft = x;
                    buttonRight = x + 150;
                    buttonTop = y-150;
                    buttonBottom = y-50;

                    canvas.drawRoundRect(buttonLeft, buttonTop, buttonRight, buttonBottom, 12, 12, cPaint);

                    //Draw a "copy" text in it
                    float textX = (buttonLeft + buttonRight - tPaint.measureText("Copy"))/2;
                    float textY = (buttonTop + buttonBottom)/2;
                    canvas.drawText("Copy", textX, textY, tPaint);

                    // Store the selected line's text
                    selectedLine = line.getValue();
                    hasShown = false;
                }
            }
        }

        // Update the ImageView with the highlighted image
        imageView.setImageBitmap(highlightedImage);
    }



    public void showToast(String msg){
        if (msg != null) {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this, "Empty", Toast.LENGTH_SHORT).show();
        }
    }
}
