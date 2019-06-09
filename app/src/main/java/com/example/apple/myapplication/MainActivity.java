package com.example.apple.myapplication;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    static{
        System.loadLibrary("tensorflow_inference");
    }
    private String MODEL_PATH = "new_densenet.pb";
    private String INPUT_NAME = "input_1";
    private String OUTPUT_NAME = "output_1";
    //private String INPUT_NAME = "module_apply_default/hub_input/Mul";
    //private String OUTPUT_NAME = "final_result";
    private TensorFlowInferenceInterface tf;
    private String label="";
    private Uri imageUri;
    private static final int PICK_FROM_GALLERY = 1;
    private Bitmap bitmap;
    private static final int CAPTURE_CAMEIA = 2;
    private File picture;

    float[] PREDICTIONS = new float[1000];
    private float[] floatValues;
    private int[] INPUT_SIZE = {224,224,3};

    ImageView imageView;
    TextView resultView;
    ImageButton buttonSub;
    ImageButton buttonMap;
    ImageButton buttonUpdate;
    ImageButton buttonCamera;
    ProgressBar barLoading;

    private long timeSeconds;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tf = new TensorFlowInferenceInterface(getAssets(),MODEL_PATH);

        imageView=(ImageView)findViewById(R.id.imageView1);
        resultView=(TextView)findViewById(R.id.text_show);
        buttonSub=(ImageButton)findViewById(R.id.predictButton);
        buttonMap=(ImageButton)findViewById(R.id.mapbutton);
        buttonUpdate=(ImageButton)findViewById(R.id.updateButton);
        buttonCamera=(ImageButton)findViewById(R.id.cameraButton);
        barLoading=(ProgressBar)findViewById(R.id.loading) ;

        buttonCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resultView.setText("Result");
                File file=new File(Environment.getExternalStorageDirectory(), "/temp/"+System.currentTimeMillis() + ".jpg");
                if (!file.getParentFile().exists())file.getParentFile().mkdirs();
                imageUri = FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID+".provider", file);
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(intent, CAPTURE_CAMEIA );//

            }
        });

        buttonUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resultView.setText("Result");
                try {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PICK_FROM_GALLERY);
                    } else {
                        Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(galleryIntent, PICK_FROM_GALLERY);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


        buttonSub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("Records Time:","Click");
                if (bitmap == null){
                    showNormalDialog("You should first upload the image!");

                }else{

                    try{
                        resultView.setText("Predicting");
                        barLoading.setVisibility(View.VISIBLE);



                        predict(bitmap);

                    }catch(Exception e){

                    }
                }
                }

        });

        Button.OnClickListener button_listener = new Button.OnClickListener(){
            @Override
            public void onClick(View view) {
                if (label.equals("")) {
                    showNormalDialog("You should predict first!");
                }else{
                        try{
                            ArrayList coor = ImageUtils.getCoordinates(getAssets().open("map.json"),label);
                            coor.add(label);
                            Intent intent = new Intent();
                            intent.setClass(MainActivity.this,MapsActivity.class);
                            Bundle args = new Bundle();
                            args.putSerializable("ARRAYLIST",(Serializable)coor);
                            intent.putExtra("BUNDLE",args);
                            startActivity(intent);
                        }catch (Exception e){

                        }
                    }
                }

            };
        buttonMap.setOnClickListener(button_listener);
    }

    private void showNormalDialog(String message){
        AlertDialog.Builder normalDialog = new AlertDialog.Builder(this);
        normalDialog.setTitle("Hint!");
        normalDialog.setIcon(R.drawable.new_upload);
        normalDialog.setMessage(message);
        normalDialog.setPositiveButton("Sure", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        normalDialog.create().show();
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults)
    {
        switch (requestCode) {
            case PICK_FROM_GALLERY:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(galleryIntent, PICK_FROM_GALLERY);
                } else {
                    //do something like displaying a message that he didn`t allow the app to access gallery and you wont be able to let him select from gallery
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK && data != null) {
            Toast.makeText(MainActivity.this, "ActivityResult resultCode error", Toast.LENGTH_SHORT).show();
            return;
        }
        switch (requestCode) {
            case PICK_FROM_GALLERY:
                Uri selectedImage = data.getData();
                String[] filePathColumns = {MediaStore.Images.Media.DATA};
                Cursor c = getContentResolver().query(selectedImage, filePathColumns, null, null, null);
                c.moveToFirst();
                int columnIndex = c.getColumnIndex(filePathColumns[0]);
                String imagePath = c.getString(columnIndex);
                showImage(imagePath);
                c.close();
                break;

            case CAPTURE_CAMEIA:

                    try {
                        bitmap = BitmapFactory.decodeStream(
                                getContentResolver().openInputStream(imageUri));
                        saveImageToGallery(MainActivity.this, bitmap);
                        imageView.setImageBitmap(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                break;

        }
    }

    public static void saveImageToGallery(Context context, Bitmap bmp) {

        File appDir = new File(Environment.getExternalStorageDirectory(), "PredictApp");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            MediaStore.Images.Media.insertImage(context.getContentResolver(),
                    file.getAbsolutePath(), fileName, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

//        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + path)));
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(file.getPath()))));
    }



    private void showImage(String imagePath){
        bitmap = BitmapFactory.decodeFile(imagePath);
        ((ImageView)findViewById(R.id.imageView1)).setImageBitmap(bitmap);
    }


    public Object[] argmax(float[] array){

        int best = -1;
        float best_confidence = 0.0f;
        for(int i = 0;i < array.length;i++){
            float value = array[i];
            if (value > best_confidence){
                best_confidence = value;
                best = i;
            }
        }
        return new Object[]{best,best_confidence};
    }


    public void predict(final Bitmap bitmap){
        
        //Runs inference in background thread
        new AsyncTask<Integer,Integer,Integer>(){

            @Override
            protected Integer doInBackground(Integer ...params){
                //Resize the image into 224 x 224

                Log.d("Records Time","before processBitmap");

                Bitmap resized_image = ImageUtils.processBitmap(bitmap,224);

                //Normalize the pixels
                Log.d("Records Time","before normaliza Bitmap");
                floatValues = ImageUtils.normalizeBitmap(resized_image,224,127.5f,1.0f);

                //Pass input into the tensorflow
                tf.feed(INPUT_NAME,floatValues,1,224,224,3);
                //compute predictions
                tf.run(new String[]{OUTPUT_NAME});
                //copy the output into the PREDICTIONS array
                tf.fetch(OUTPUT_NAME,PREDICTIONS);
                //Obtained highest prediction
                Object[] results = argmax(PREDICTIONS);
                int class_index = (Integer) results[0];
                float confidence = (Float) results[1];
                try{
                    final String conf = String.valueOf(confidence * 100).substring(0,5);
                    //Convert predicted class index into actual label name
                    label = ImageUtils.getLabel(getAssets().open("label.json"),class_index);
                    //Display result on UI
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            barLoading.setVisibility(View.GONE);
                            resultView.setText(label);
                        }
                    });
                } catch (Exception e){
                }

                return 0;
            }

        }.execute(0);

    }

}

