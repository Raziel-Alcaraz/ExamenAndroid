package com.example.examenandroid;

import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class uploadsActivity extends AppCompatActivity {
    FirebaseStorage storage;
    private final int PICK_IMAGE_REQUEST = 357;
    private Uri filePath;
String TAG = "upAct";
    private ImageView imageView;
    FirebaseFirestore db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uploads);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        db = FirebaseFirestore.getInstance();
        imageView = findViewById(R.id.imgView);
        TextView tv = (TextView) findViewById(R.id.porsubir);
        tv.setVisibility(View.INVISIBLE);
        storage = FirebaseStorage.getInstance();
        String TAG = "upAct";
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Abriendo galería", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                elegirImagen();
            }
        });
        galeria();
    }
    private void elegirImagen()
    {

        // Defining Implicit Intent to mobile gallery
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(
                Intent.createChooser(
                        intent,
                        "Select Image from here..."),
                PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data)
    {

        super.onActivityResult(requestCode,
                resultCode,
                data);

        // checking request code and result code
        // if request code is PICK_IMAGE_REQUEST and
        // resultCode is RESULT_OK
        // then set image in the image view
        if (requestCode == PICK_IMAGE_REQUEST
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {

            // Get the Uri of data
            filePath = data.getData();
            try {

                // Setting image on image view using Bitmap
                Bitmap bitmap = MediaStore
                        .Images
                        .Media
                        .getBitmap(
                                getContentResolver(),
                                filePath);
                imageView.setImageBitmap(bitmap);
                TextView tv = (TextView) findViewById(R.id.porsubir);
                tv.setVisibility(View.VISIBLE);
                FloatingActionButton fab = findViewById(R.id.fab);
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Snackbar.make(view, "Subiendo imagen...", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                        subirImagen();
                    }
                });

                fab.setImageResource(android.R.drawable.ic_menu_upload);
            }

            catch (IOException e) {
                // Log the exception
                e.printStackTrace();
            }
        }
    }
    public void subirImagen(){
        StorageReference storageRef = storage.getReference();
        Uri file = filePath;
        StorageReference ref = storageRef.child("images/"+generarUserParaFirebase()+"/"+file.getLastPathSegment());
        UploadTask uploadTask = ref.putFile(file);

        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }

                // Continue with the task to get the download URL
                return ref.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    crearRef(String.valueOf(downloadUri));
                    imageView.setImageBitmap(null);
                    TextView tv = (TextView) findViewById(R.id.porsubir);
                    tv.setVisibility(View.INVISIBLE);
                } else {
                    // Handle failures
                    // ...
                }
            }
        });
        regresarAlInicio();
    }
    public void crearRef(String link){
        Map<String, Object> ubicacion = new HashMap<>();
        Log.d("uploads", link);
        ubicacion.put(String.valueOf(System.currentTimeMillis()),link);
        Log.d("uploads",generarUserParaFirebase());
        db.collection(generarUserParaFirebase()).document("galeria")
                .update(ubicacion)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("uploadActRef", "DocumentSnapshot successfully written!");

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("uploadActRef", "Error writing document", e);
                        reintentoSet(ubicacion);
                    }
                });
        regresarAlInicio();
    }
    public void reintentoSet(Map ubicacion){
        db.collection(generarUserParaFirebase()).document("galeria")
                .set(ubicacion)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("uploadActRef", "DocumentSnapshot successfully written!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("uploadActRef", "Error writing document", e);

                    }
                });
    }
    public String generarUserParaFirebase(){
        SQLiteDatabase peliculas = openOrCreateDatabase("peliculas", MODE_PRIVATE, null);


        ContentValues contentValues = new ContentValues();
        peliculas.execSQL("CREATE TABLE IF NOT EXISTS Usuario(Id VARCHAR);");
        Cursor resultSet = peliculas.rawQuery("Select * from Usuario",null);
        if(!(resultSet.getCount() >0)) {
            contentValues.put("Id",String.valueOf(System.currentTimeMillis()));
            peliculas.insert("Usuario", null, contentValues);
            return String.valueOf(System.currentTimeMillis());
        }else{
            Cursor resultSet2 = peliculas.rawQuery("Select * from Usuario",null);
            resultSet2.moveToFirst();
            String id = resultSet2.getString(0);
            return id;
        }
    }

    public void galeria(){
        final DocumentReference docRef = db.collection(generarUserParaFirebase()).document("galeria");
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    LinearLayout linearlay = (LinearLayout) findViewById(R.id.fotosBajadas);
                    linearlay.removeAllViews();
                    Log.d(TAG, "Current data: " + snapshot.getData());
                    try {
                        JSONObject json = new JSONObject((Map) snapshot.getData());
                        Iterator<String> temp = json.keys();
                        do {
                            String key = temp.next();
                            String value =(String) json.get(key);
                            ImageView imageViewX = new ImageView(getApplicationContext());

                            linearlay.addView(imageViewX);
                            Glide.with(getApplicationContext()).load(value).into(imageViewX);
                        }while (temp.hasNext());

                    } catch (JSONException f) {
                        f.printStackTrace();
                    }
                    
                } else {
                    Log.d(TAG, "Current data: null");
                }
            }
        });
    }

    public void regresarAlInicio(){
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Abriendo galería...", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                elegirImagen();
            }
        });

        fab.setImageResource(android.R.drawable.ic_menu_search);
    }
        // Glide.with(this).load("http://goo.gl/gEgYUd").into(imageView);

}