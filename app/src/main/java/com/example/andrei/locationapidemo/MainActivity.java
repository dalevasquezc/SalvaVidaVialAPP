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
            Localizacion listaLocalizaciones[] = new Localizacion[10];

    private static int UPDATE_INTERVAL = 10000;
    private static int FATEST_INTERVAL = 5000;
    private static int DISPLACEMENT = 10;

    private TextView lblLocation, lblmensaje;
    private Button btnShowLocation, btnStartLocationUpdates;
    private List<Location> locations = null;

    //Variables funcionalidades
    boolean firstItem = false;  //Primer elemento de localización registrado (No tiene antecesor de comparación).
    int contLocalizacion = 0; //Registro posición en objeto localización, del ultimo registro.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locations = new ArrayList<>();

        DBHandler db = new DBHandler(this);

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

            //Capturando valores de ultima localizacion en objeto
            listaLocalizaciones[contLocalizacion] = new Localizacion();

            listaLocalizaciones[contLocalizacion].time = mLastLocation.getTime();
            listaLocalizaciones[contLocalizacion].latitude = mLastLocation.getLatitude();
            listaLocalizaciones[contLocalizacion].longtitude = mLastLocation.getLongitude();
            listaLocalizaciones[contLocalizacion].speed = mLastLocation.getSpeed();

            lblLocation.setText(listaLocalizaciones[contLocalizacion].latitude + ", " + listaLocalizaciones[contLocalizacion].longtitude);

            if((contLocalizacion == 0) && (firstItem == false))
            {
                listaLocalizaciones[contLocalizacion].delta = 0; //El primer elemento no tiene antecesor contra el cual obtener delta
                firstItem = true;
            }
            else
            {
                Log.d(TAG, "CALCULA DELTA--------");
                listaLocalizaciones[contLocalizacion].delta = getDelta(listaLocalizaciones[contLocalizacion-1].time, listaLocalizaciones[contLocalizacion-1].speed,
                        listaLocalizaciones[contLocalizacion].time, listaLocalizaciones[contLocalizacion].speed);
                String deltastring = String.valueOf(listaLocalizaciones[contLocalizacion].delta);
                Log.d(TAG, "Delta CALCULADO " + deltastring );
            }

            //crear registro de localización en la BD
            dbHandler.addLocation(listaLocalizaciones[contLocalizacion]);
            //registrarLocalizacion(latitude, longitude, time, speed, contLocalizacion);

            //El objeto Localizacion "listaLocalizaciones" solo tendra cargado los ultimos diez registros
            contLocalizacion = contLocalizacion+1;
            //En la variable ListaLocalizaciones solo se almacenaran las ultimas 10 localizaciones identificadas.
            if(contLocalizacion > 9)
            {
                contLocalizacion = 0;
            }
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

        locations.add(location);


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
//            //Clacular los deltas acá
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
