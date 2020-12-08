package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.label.ImageLabel;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.automl.FirebaseAutoMLLocalModel;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceAutoMLImageLabelerOptions;

import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    FirebaseAutoMLLocalModel localModel;
    FirebaseVisionImageLabeler labeler;
    FirebaseVisionImage image;

    ImageView imageView;
    int imagePic = 1000;
    int permissionCode = 1001;
    Button chooseButton;
    TextView textView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FirebaseApp.initializeApp(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        final Button button1 = findViewById(R.id.button);

        final TextView textView = findViewById(R.id.recycleCode);
        textView.setText("");
        final TextView decide = findViewById(R.id.textView7);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String x =  textView.getText().toString();
                try{
                    Integer recycleCode = Integer.parseInt(x);
                    if (recycleCode < 1 || recycleCode > 7){
                        decide.setText("Enter a number between 1 and 7 \n");
                    } else if (recycleCode == 1) {
                        decide.setText("Empty and rinse any food\nDon't recycle caps \n" );
                    } else if (recycleCode == 2){
                        decide.setText("Check with your rcycling program \n if they allow this " +
                                "plastic\n If not, look into programs like \n TerraCycle for recycling " +
                                "programs \n");
                    } else if (recycleCode == 3){
                        decide.setText("This can rarely be recycled. \nDon't burn it as it contains chlorine\n" +
                                "Ask your waste management center\nTry to reuse it possible \n");
                    } else if (recycleCode == 4){
                        decide.setText("Check with your local recycling program. \n");
                    } else if (recycleCode == 5){
                        decide.setText("Rinse and empty food,\nthrow caps in the garbage.\nAsk " +
                                "to make sure that your recycling\n program allows this \n");
                    } else if (recycleCode == 6){
                        decide.setText("you should place them in a bag,\n squeeze out the air, and tie" +
                                "\nit up before putting it in the trash to prevent " +
                                "\npellets from dispersing. \n");
                    } else if (recycleCode == 7){
                        decide.setText("Can't be recycled \n");
                    }
                } catch (NumberFormatException e) {
                    decide.setText("Enter a number between 1 and 7 \n");
                }



            }
        });

        chooseButton = findViewById(R.id.button2);
        chooseButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
                        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
                        requestPermissions(permissions,permissionCode);

                    } else {
                        pickImageFromGallery();

                    }
                }
                else {
                    pickImageFromGallery();
                }

            }
        });
    }

    private void pickImageFromGallery(){
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent,1000);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1001:{
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    pickImageFromGallery();
                } else {
                    Toast.makeText(this, "Permission was denied", Toast.LENGTH_SHORT).show();
                }
            }

        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        System.out.println("on activity");
        //super.onActivityResult(requestCode, resultCode, data);

            Uri uri = data.getData();
            System.out.println(uri.toString());

            setLabelerfromLocalModel(uri);
    }
    private void setLabelerfromLocalModel(Uri uri){
        System.out.println("set labeler");
        textView = findViewById(R.id.textView7);
        FirebaseApp.initializeApp(this);
        final ArrayList<Float> confidenceArray = new ArrayList<>();
        final ArrayList<String> labelArray = new ArrayList<>();
        localModel = new FirebaseAutoMLLocalModel.Builder().setAssetFilePath("model/manifest.json").build();
        try{
            FirebaseVisionOnDeviceAutoMLImageLabelerOptions options = new FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder(localModel).
                    setConfidenceThreshold(.0f).build();
            FirebaseApp.initializeApp(this);
            labeler = FirebaseVision.getInstance().getOnDeviceAutoMLImageLabeler(options);
            image = FirebaseVisionImage.fromFilePath(MainActivity.this,uri);
            labeler.processImage(image).addOnCompleteListener(new OnCompleteListener<List<FirebaseVisionImageLabel>>() {
                @Override
                public void onComplete(@NonNull Task<List<FirebaseVisionImageLabel>> task) {
                    //progressDialog.cancel();
                    for (FirebaseVisionImageLabel label : task.getResult()) {
                        String eachlabel = label.getText().toUpperCase();
                        float confidence = label.getConfidence();
                        System.out.println(eachlabel + confidence);
                        confidenceArray.add(confidence);
                        labelArray.add(eachlabel);
                    }
                    float max = confidenceArray.get(0);
                    for (int i = 0; i < confidenceArray.size(); i++) {
                        if (max < confidenceArray.get(i)){
                            max = confidenceArray.get(i);
                        }
                    }
                    if (max < .6){
                        textView.setText("Image not processed properly. Try again");
                    } else {
                        textView.setText(setMax(confidenceArray,labelArray));
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("OnFail", "" + e);
                    Toast.makeText(MainActivity.this, "Something went wrong! " + e, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (FirebaseMLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String setMax(ArrayList<Float> confidence, ArrayList<String> label){
        Hashtable<String, Float> my_dict = new Hashtable<>();
        for (int i = 0; i <6 ; i++) {
            my_dict.put(label.get(i),confidence.get(i));
        }
        float max = Float.MIN_VALUE;
        String maxString = "";
        String returnString = maxString + "\n";
        for (String key: my_dict.keySet()){
            if (my_dict.get(key) > max){
                max = my_dict.get(key);
                maxString = key;
            }
        }
        if (maxString.toLowerCase().equals("paper")){
            returnString += maxString + "\n Throw in the recycling bin\n " + "If paper is lined with plastic" +
                    "\n check with your local center \n";
        } else if (maxString.toLowerCase().equals("cardboard")){
            returnString += maxString + "\n "+"Try to break it down as much as possible\n. Then place in the recycling bin \n";
        } else if (maxString.toLowerCase().equals("glass")){
            returnString += maxString + "\n "+"Rinse and remove anything from inside the object\n Can be thrown in the recycling bin \n";
        } else if (maxString.toLowerCase().equals("plastic")){
            returnString += maxString + "\n "+"Rinse out any food, and remove bottle caps, if present\n For more accurate" +
                    "information, enter the recycling code \n";
        } else if (maxString.toLowerCase().equals("metal")){
            returnString += maxString + "\n "+"Throw in the recycling bin \n";
        } else if (maxString.toLowerCase().equals("trash")){
            returnString += maxString + "\n "+"Can't be thrown in the recycling.\n Ask your local recyling \ncenter for more" +
                    "information \n";
        }
        return returnString;
    }
}