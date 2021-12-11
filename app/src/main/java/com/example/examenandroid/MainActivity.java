package com.example.examenandroid;
    /* Pasos:
    0: enviar request y bajar JSON
        enviarRequest() hace lo que su nombre dice.
        Si se recibe el objeto JSON, pasa al paso 1, valga la rebuznancia.
        Si el resultado es error salta al paso 2

    1: procesar JSON
        Una vez se ha enviado el request, este se recibe y se
        procesa en recibirJSON() esta misma función hace la persistencia en SQLite

    2:Leer base de SQLite
        leerPersistencia() lee la base de datos y muestra los contenidos al usuario

3: Mapa: se creó un handler que escribe cada 30 minutos la ubicación del usuario en la consola de Firebase
    La función EnviarLocationFirebase se ejecuta cada 30 minutos.

        3.1: Se baja la ubicación del fusedLocationClient
        3.2: Se busca el id en la SQLite, si no existe se genera uno con base en el timestamp (milisegundos)
        3.3: Con el id se almacena la ubicación en firebase
        3.4: Con el FAB se abre la activity del mapa
            En el mapa se descargan y muestran las ubicaciones del usuario
4: Firebase storage:
    Se pueden subir imágenes (una a una) y ver la galería. to-do está en uploadsActivity.java
     */
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.provider.BaseColumns;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import im.delight.android.webview.AdvancedWebView;

public class MainActivity extends AppCompatActivity {
    String jsonText;
    SQLiteDatabase peliculas;
    int delayms = 1800000;
    FirebaseFirestore db;
    String TAG = "mainact";
    private FusedLocationProviderClient fusedLocationClient;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        enviarRequest();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        ActivityResultLauncher<String[]> locationPermissionRequest =
                registerForActivityResult(new ActivityResultContracts
                                .RequestMultiplePermissions(), result -> {
                            Boolean fineLocationGranted = result.getOrDefault(
                                    Manifest.permission.ACCESS_FINE_LOCATION, false);
                            Boolean coarseLocationGranted = result.getOrDefault(
                                    Manifest.permission.ACCESS_COARSE_LOCATION,false);
                            if (fineLocationGranted != null && fineLocationGranted) {

                                        enviarLocationFirebase();

                            } else if (coarseLocationGranted != null && coarseLocationGranted) {
                                enviarLocationFirebase();
                            } else {
                                Snackbar.make(findViewById(R.id.fab), "Abriendo mapa...", Snackbar.LENGTH_LONG);
                            }
                        }
                );
        locationPermissionRequest.launch(new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });

        peliculas = openOrCreateDatabase("peliculas", MODE_PRIVATE, null);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Abriendo mapa...", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                irAMapa();

            }
        });
        FloatingActionButton fab2 = findViewById(R.id.fab2);
        fab2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Abriendo mapa...", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                irAUploads();

            }
        });
    }

    public void irAMapa() {
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
    }
    public void irAUploads() {
        Intent intent = new Intent(this, uploadsActivity.class);
        startActivity(intent);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @SuppressLint("MissingPermission")
    public void enviarLocationFirebase() {
Log.d(TAG,"enviando location");

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null ) {
                            Map<String, Object> ubicacion = new HashMap<>();
                            ubicacion.put("lat", location.getLatitude());
                            ubicacion.put("lon", location.getLongitude());
                            String android_id = generarUserParaFirebase();
                            String milisegundos = String.valueOf(System.currentTimeMillis());
                            db.collection(android_id).document(milisegundos)
                                    .set(ubicacion)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {

                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.d(TAG, "DocumentSnapshot successfully written!");
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {

                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.w(TAG, "Error writing document", e);
                                        }
                                    });
                        }
                    }
                });



    final Handler handler = new Handler();
    handler.postDelayed(new Runnable() {
        @Override
        public void run() {
            enviarLocationFirebase();
        }
    }, delayms);

}

@Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    public void enviarRequest(){
        final TextView textView = (TextView) findViewById(R.id.text);

        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="https://api.themoviedb.org/3/trending/movie/day?api_key=4098516b1a0255c02f0a7f7cf4aae923";
                // Solicitar un JSON con una solicitud GET a la URL:
                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                        (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                recibirJSON(response);

                                Log.d("inicio-0", response.toString());
                            }
                        }, new Response.ErrorListener() {


                            @Override
                            public void onErrorResponse(VolleyError error) {
                              leerPersistencia();
                            }
                        });
                queue.add(jsonObjectRequest);
    }

    public void recibirJSON(JSONObject yeison){
        peliculas.execSQL("DROP TABLE IF EXISTS Populares;");
        peliculas.execSQL("CREATE TABLE IF NOT EXISTS Populares(Id INTEGER,Titulo VARCHAR,Descripcion VARCHAR, Valoracion REAL);");
            jsonText = (String)  yeison.toString();
            Log.d("inicio-1", jsonText);
        try {
            org.json.JSONArray listaPopulares = (org.json.JSONArray) (yeison.get("results"));
            Log.d("inicio-listapop", listaPopulares.toString());
            for(int i=0; i<listaPopulares.length(); i++) {
                JSONObject elemento = (JSONObject) listaPopulares.get(i);
                Log.d("inicio-elemento", elemento.toString());
                String titulo =(String) elemento.get("title");
                String descr =(String) elemento.get("overview");
                Double valoracion =(Double) elemento.get("vote_average");
                insertarEnTabla(i,titulo,descr,valoracion);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
leerPersistencia();
    }
    public boolean insertarEnTabla(int Id ,String Titulo,String Descripcion ,Double Valoracion){

        SQLiteDatabase dbX = peliculas;

        ContentValues contentValues = new ContentValues();
        contentValues.put("Id",Id);
        contentValues.put("Titulo",Titulo);
        contentValues.put("Descripcion",Descripcion);
        contentValues.put("Valoracion",Valoracion);
        dbX.insert("Populares",null,contentValues);

        return true;
    }
    public void leerPersistencia(){
        Cursor resultSet = peliculas.rawQuery("Select * from Populares",null);
        resultSet.moveToFirst();
        for(int i=0;i<resultSet.getCount();i++) {
            int id = resultSet.getInt(0);
            String titulo = resultSet.getString(1);
            String descr = resultSet.getString(2);
            String valoracion = resultSet.getString(3);
            Log.d("inicio-leerPers", "'" + titulo + "':" + valoracion + ". " + descr + "");
            Fragment peliculaFragmento = new peliculaFrag();
            Bundle args = new Bundle();
            args.putString("param1", titulo);
            args.putString("param2", valoracion);
            args.putString("param3", descr);
            peliculaFragmento.setArguments(args);
            LinearLayout linear = (LinearLayout) findViewById(R.id.linear);

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.linear, peliculaFragmento, String.valueOf(i))
                    .disallowAddToBackStack()
                    .commit();
            resultSet.moveToNext();
        }
    }

   public String generarUserParaFirebase(){
        SQLiteDatabase dbX = peliculas;

        ContentValues contentValues = new ContentValues();
       peliculas.execSQL("CREATE TABLE IF NOT EXISTS Usuario(Id VARCHAR);");
       Cursor resultSet = peliculas.rawQuery("Select * from Usuario",null);
       if(!(resultSet.getCount() >0)) {
           contentValues.put("Id",String.valueOf(System.currentTimeMillis()));
           dbX.insert("Usuario", null, contentValues);
           return String.valueOf(System.currentTimeMillis());
       }else{
           Cursor resultSet2 = peliculas.rawQuery("Select * from Usuario",null);
           resultSet2.moveToFirst();
               String id = resultSet2.getString(0);
         return id;
       }
    }

}


