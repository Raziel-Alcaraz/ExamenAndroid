package com.example.examenandroid;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONObject;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
String TAG = "mapsAct";
    private GoogleMap mMap;
    FirebaseFirestore db;
    SQLiteDatabase peliculas;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        db = FirebaseFirestore.getInstance();
        peliculas = openOrCreateDatabase("peliculas", MODE_PRIVATE, null);

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    public void bajarUbicaciones(GoogleMap mMap){
        db.collection(generarUserParaFirebase())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() ) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                if(document.get("lon")!= null&& document.get("lat")!=null) {
                                    Log.d(TAG, document.getId() + " => " + document.getData());
                                    LatLng sydney = new LatLng((double) document.get("lat"), (double) document.get("lon"));
                                    mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
                                    mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
                                }
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        bajarUbicaciones(mMap);
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