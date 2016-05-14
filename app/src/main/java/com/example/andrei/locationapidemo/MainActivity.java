package com.example.andrei.locationapidemo;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener{


    private static final String TAG = MainActivity.class.getSimpleName();
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;

    private Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;

    private boolean mRequestLocationUpdates = false;

    private LocationRequest mLocationRequest;

    private static int UPDATE_INTERVAL = 10000;
    private static int FATEST_INTERVAL = 5000;
    private static int DISPLACEMENT = 10;

    private TextView lblLocation, lblmensaje;
    private Button btnShowLocation, btnStartLocationUpdates;

    //Variables funcionalidades
    boolean firstItem = false;  //Primer elemento de localización registrado (No tiene antecesor de comparación).
    private List<Localizacion> listaLocalizacion = null;
    private Localizacion ultimaLocalizacion = new Localizacion();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DBHandler db = new DBHandler(this);

        //Instancio objeto que captura localizaciones
        listaLocalizacion = new ArrayList<Localizacion>();

        lblLocation = (TextView) findViewById(R.id.lblLocation);
        btnShowLocation = (Button) findViewById(R.id.buttonShowLocation);
        btnStartLocationUpdates = (Button) findViewById(R.id.buttonLocationUpdates);

        if(checkPlayServices()) {
            buildGoogleApiClient();
            createLocationRequest();
        }

        btnShowLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayLocation();
            }
        });

        btnStartLocationUpdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePeriodLocationUpdates();
            }
        });

        Button btnDataBase =(Button) findViewById(R.id.btn_database);

        btnDataBase.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
//                Intent dbmanager = new Intent(getApplicationContext(), AndroidDatabaseManager.class);
//                startActivity(dbmanager);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkPlayServices();
        if(mGoogleApiClient.isConnected() && mRequestLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void displayLocation() {
        //Defino variable para solicitar datos de ultima localización
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        //Definición de variables para conectar BD
        DBHandler dbHandler = new DBHandler(this);

        if(mLastLocation != null) {

            //Objeto Localización para añadir a la lista
            ultimaLocalizacion.latitude = mLastLocation.getLatitude();
            ultimaLocalizacion.longtitude = mLastLocation.getLongitude();
            ultimaLocalizacion.time = mLastLocation.getTime();
            ultimaLocalizacion.speed = mLastLocation.getSpeed();

            //Añado elemento a la lista
            listaLocalizacion.add(ultimaLocalizacion);

            int posUltElement = listaLocalizacion.size() - 1; //La ultima posición de la lista es la cantidad total de elementos menos 1 porque arranca desde el elemento "0".

            lblLocation.setText(listaLocalizacion.get(posUltElement).latitude + ", " + listaLocalizacion.get(posUltElement).longtitude);

            //Calcular delta para la ultima localización
            if(listaLocalizacion.size() == 1)
            {
                ultimaLocalizacion.delta = 0;
                listaLocalizacion.set(posUltElement, ultimaLocalizacion);
            }
            else
            {
                listaLocalizacion.get(posUltElement).delta = getDelta(listaLocalizacion.get(posUltElement - 1).time, listaLocalizacion.get(posUltElement - 1).speed,
                        listaLocalizacion.get(posUltElement).time, listaLocalizacion.get(posUltElement).speed);

            }



            //crear registro de la ultima localización en la BD
            dbHandler.addLocation(listaLocalizacion.get(posUltElement));
            //registrarLocalizacion(latitude, longitude, time, speed, contLocalizacion);

        }
        else {
            lblLocation.setText("Couldn't get the location. Make sure location is enabled on the device");
        }
    }

    private void togglePeriodLocationUpdates() {
        if(!mRequestLocationUpdates) {
            btnStartLocationUpdates.setText(getString(R.string.btn_stop_location_updates));

            mRequestLocationUpdates = true;

            startLocationUpdates();
        } else {
            btnStartLocationUpdates.setText(getString(R.string.btn_start_location_updates));

            mRequestLocationUpdates = false;

            stopLocationUpdates();
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(resultCode != ConnectionResult.SUCCESS) {
            if(GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(), "This device is not supported", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onConnected(Bundle bundle) {
        displayLocation();

        if(mRequestLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {

        mLastLocation = location;

        //Muestra mensaje temporal
        Toast.makeText(getApplicationContext(), "Location changed!", Toast.LENGTH_SHORT).show();

        displayLocation();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed: " + connectionResult.getErrorCode());
    }


    private void CalcDelta(){
//        locations.size() - 1;
        double deltaMax = 0d;
        double deltaMin = 0d;
        double deltaAvg = 0d;

//        for (){
//            //Clacular los deltas acá hhhhh
//
//        }
        //Y Luego calcular el porcentaje de diferencua que hay dentro de cada delta.
    }

    private double getDelta(long timeOne, float speedOne, long timeTwo, float speedTwo)
    {
        double delta;

        delta = (speedTwo - speedOne)/(timeTwo - timeOne);
        return delta;
    }
}
