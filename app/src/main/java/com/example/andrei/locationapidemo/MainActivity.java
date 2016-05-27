package com.example.andrei.locationapidemo;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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

    private static int UPDATE_INTERVAL = 5000; //Intervalo de tiempo para solicitar localización
    private static int FATEST_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;

    private TextView lblLocation, lblmensaje;
    private Button btnShowLocation, btnStartLocationUpdates;

    private User userCurrent;

    //Variables funcionalidades
    boolean firstItem = false;  //Primer elemento de localización registrado (No tiene antecesor de comparación).
    private List<Localizacion> listaLocalizacion = null;
    //private Localizacion ultimaLocalizacion = new Localizacion();

    //Variables de prueba
    int contLocalizaciones = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "EJECUCIÓN: onCreate()");

        DBHandler db = new DBHandler(this);

        //Instancio objeto que captura localizaciones
        listaLocalizacion = new ArrayList<Localizacion>();

        //Reconocer el usuario logueado en el aplicativo.
        userCurrent = new User();
        userCurrent.idUser = 123; //LLega el ID del usuario que se loguea en la aplicación.
        userCurrent = db.getUser(userCurrent.idUser);
        Log.d(TAG, "Valida existencia del usuario en BD");

        //Verifica si los servicios de localización estan disponibles para el dispositivo. El metodo onResume() requiere conexion establecida y request location declarado
        if (checkPlayServices()) {
            buildGoogleApiClient();//Establece conexión con el servicio de localizacion
            createLocationRequest();//Crea el objeto encargado de hacer peticiones al servicio de localizacion
        }

        lblLocation = (TextView) findViewById(R.id.lblLocation);
        btnShowLocation = (Button) findViewById(R.id.buttonShowLocation);
        btnStartLocationUpdates = (Button) findViewById(R.id.buttonLocationUpdates);

        //Control de Login: Si el número de identificación existe en la BD
        if(userCurrent != null) {

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

            Button btnDataBase = (Button) findViewById(R.id.btn_database);

            btnDataBase.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent dbmanager = new Intent(getApplicationContext(), AndroidDatabaseManager.class);
                    startActivity(dbmanager);
                }
            });
        }
        else
        {
            setContentView(R.layout.user_not_authenticate);
            Log.d(TAG, "Usuario no registrado en BD");
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "EJECUCIÓN: onStart()");

        if(mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "EJECUCIÓN: onResume()");
        checkPlayServices();
        if(mGoogleApiClient.isConnected() && mRequestLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "EJECUCIÓN: onStop()");
        if(mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "EJECUCIÓN: onPause()");
        stopLocationUpdates();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void displayLocation() {
        Log.d(TAG, "EJECUCIÓN: displayLocation()");
        //Defino variable para solicitar datos de ultima localización
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        Localizacion ultimaLocalizacion = new Localizacion();

        //Definición de variables para conectar BD
        DBHandler dbHandler = new DBHandler(this);

        //Determinar si existen diferencias con la ultima localización reconocida
        boolean localizacionDiferente = false;

        if(mLastLocation != null) {

            Log.d(TAG, "Cantidad de cambios de localización: " +  contLocalizaciones);
            contLocalizaciones++;

            //Objeto Localización para añadir a la lista
            ultimaLocalizacion.latitude = mLastLocation.getLatitude();
            ultimaLocalizacion.longtitude = mLastLocation.getLongitude();
            ultimaLocalizacion.time = mLastLocation.getTime();

            int posUltElement = listaLocalizacion.size() - 1;//La ultima posición de la lista es la cantidad total de elementos menos 1 porque arranca desde el elemento "0".

            //Calcular delta para la ultima localización
            if(listaLocalizacion.size() == 0)
            {
                ultimaLocalizacion.delta = 0;
                ultimaLocalizacion.deltapromedio = 0;
                ultimaLocalizacion.speed = getVelocidad(); //Velocidad en Ft/Seg

                //Añadiendo el primer elemento a la lista;
                listaLocalizacion.add(ultimaLocalizacion);

                dbHandler.addLocation(listaLocalizacion.get(0));
            }
            else if(ultimaLocalizacion.time != listaLocalizacion.get(posUltElement).time) //Compara la ultima localización versus la anterior
            {
                ultimaLocalizacion.delta = getDelta(listaLocalizacion.get(posUltElement).time, listaLocalizacion.get(posUltElement).speed,
                        ultimaLocalizacion.time, ultimaLocalizacion.speed);

                //Registro ultima localización con cambios reales.
                listaLocalizacion.add(ultimaLocalizacion);
                localizacionDiferente = true;
            }
            else
            {
                Log.d(TAG, "No existe cambio en tiempos de localización");
            }

            //Calcula delta promedio solo si existen cambios con la ultima localización conocida
            if(localizacionDiferente == true)
            {
                //Actualizo posición del ultimo elemento
                posUltElement = listaLocalizacion.size() - 1;

                ultimaLocalizacion.deltapromedio = getDeltaPromedio();
                ultimaLocalizacion.porcentajeDif = getPorcentajeDiferencia();
                ultimaLocalizacion.speed = getVelocidad();

                //Actualizar atributos de localización
                listaLocalizacion.set(posUltElement, ultimaLocalizacion);

                //crear registro de la ultima localización en la BD
                dbHandler.addLocation(listaLocalizacion.get(posUltElement));
                //registrarLocalizacion(latitude, longitude, time, speed, contLocalizacion);
            }

            posUltElement = listaLocalizacion.size() - 1;//Actualizo posición del ultimo elemento
            lblLocation.setText(listaLocalizacion.get(posUltElement).latitude + ", " + listaLocalizacion.get(posUltElement).longtitude);

        }
        else {
            lblLocation.setText("Couldn't get the location. Make sure location is enabled on the device");
        }
    }

    private void togglePeriodLocationUpdates() {
        Log.d(TAG, "EJECUCIÓN: togglePeriodLocationUpdates()");
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
        Log.d(TAG, "EJECUCIÓN: buildGoogleApiClient()");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    protected void createLocationRequest() {
        Log.d(TAG, "EJECUCIÓN: createLocationRequest()");
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private boolean checkPlayServices() {
        Log.d(TAG, "EJECUCIÓN: checkPlayServices()");
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
        Log.d(TAG, "EJECUCIÓN: startLocationUpdates()");
        //Borrar localizaciones en la lista, garantizar un nuevo recorrido
        if(listaLocalizacion.size() != 0) {
            listaLocalizacion.clear();
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        Log.d(TAG, "EJECUCIÓN: stopLocationUpdates()");
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

        //Instanciar objeto para registrar recorrido en BD
        DBHandler dbHandler = new DBHandler(this);

        //Agregar metodos para obtener datos de recorrido.
        Recorrido nuevoRecorrido = new Recorrido();

        nuevoRecorrido.tiempoTotal = getTiempoTotal();
        nuevoRecorrido.velocidadPromedi = getVelocidadPromedio();
        nuevoRecorrido.velocidadMaxima = getVelocidadMaxima();

        //Registrando datos generales del ultimo recorrido
        dbHandler.addRecorrido(nuevoRecorrido);

    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "EJECUCIÓN: onConnected(Bundle bundle)");
        displayLocation();

        if(mRequestLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "EJECUCIÓN: onConnectionSuspended(int i)");
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "EJECUCIÓN: onLocationChanged(Location location)");
        mLastLocation = location;

        //Muestra mensaje temporal
        Toast.makeText(getApplicationContext(), "Location changed!", Toast.LENGTH_SHORT).show();

        displayLocation();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "EJECUCIÓN: onConnectionFailed(ConnectionResult connectionResult)");
        Log.i(TAG, "Connection failed: " + connectionResult.getErrorCode());
    }

    private double getDelta(long timeOne, double speedOne, long timeTwo, double speedTwo)
    {
        double delta, difVelocidad, difTime;

        difVelocidad = speedTwo - speedOne;
        difTime = timeTwo - timeOne;
        delta = (speedTwo - speedOne)/(timeTwo - timeOne);

        //Imprimir la delta calculada.
        return delta;
    }

    private double getDeltaPromedio()
    {
        double deltaPromedio, sumatoriaDeltas = 0;
        int registrosMaximos = 15, deltasLeidas = 0; //IMPLEMENTAR COMO PARAMETRO

        //Leer menos elementos de la lista en caso que aún no existan 15 localizaciones.
        if(listaLocalizacion.size() < 15)
        {
            registrosMaximos = listaLocalizacion.size();
        }

        //lee las ultimas "registrosMaximos" deltas
        //Variable i, con el recorro la lista en el orden: Ultimo - Primero
        //Variable deltasLeidas, leo la cantidad maxima de registros.
        for(int i = (listaLocalizacion.size() - 1); deltasLeidas < registrosMaximos; i--)
        {
            sumatoriaDeltas = sumatoriaDeltas + listaLocalizacion.get(i).delta;
            deltasLeidas++;
        }

        //Delta promedio de la localización
        deltaPromedio = sumatoriaDeltas/registrosMaximos;

        return deltaPromedio;
    }

    private double getPorcentajeDiferencia()
    {
        //Este metodo determina el porcentaje de diferencia entre el delta de la localización vs el promedio de deltas de localizaciones


        //Defino variables
        double porcentajeDiferencia;
        int ultPos = listaLocalizacion.size() - 1;

        porcentajeDiferencia= (listaLocalizacion.get(ultPos).delta * 100) / listaLocalizacion.get(ultPos).deltapromedio;

        return porcentajeDiferencia;
    }

    private double getTiempoTotal()
    {
        double tiempoTotal = 0;
        int ultPos = listaLocalizacion.size() - 1;

        if(listaLocalizacion != null) {
            tiempoTotal = listaLocalizacion.get(ultPos).time - listaLocalizacion.get(0).time;
        }
        else
        {
            Log.d(TAG, "La lista se encuentra vacia: Sin localizaciones en memoria");
        }

        return tiempoTotal;
    }

    private double getVelocidadPromedio()
    {
        double velocidadPromedio, sumatoriaVelocidad = 0;

        if(listaLocalizacion != null)
        {
            for (int i = 0; i < listaLocalizacion.size(); i++) {
                sumatoriaVelocidad = sumatoriaVelocidad + listaLocalizacion.get(i).speed;
            }
        }
        else
        {
            Log.d(TAG, "La lista se encuentra vacia: Sin localizaciones en memoria");
        }

        velocidadPromedio = sumatoriaVelocidad/listaLocalizacion.size();

        return velocidadPromedio;
    }

    private double getVelocidadMaxima()
    {
        double maxVelocidad = 0;

        if(listaLocalizacion != null)
        {
            for (int i = 0; i < listaLocalizacion.size(); i++)
            {
                if(listaLocalizacion.get(i).speed > maxVelocidad)
                {
                    maxVelocidad = listaLocalizacion.get(i).speed;
                }
            }
        }
        else
        {
            Log.d(TAG, "La lista se encuentra vacia: Sin localizaciones en memoria");
        }
        return maxVelocidad;
    }

    private double getVelocidad()
    {
        double velocidad;
        int ultPos = listaLocalizacion.size() - 1;


        if(listaLocalizacion.size() <= 1)
        {
            velocidad = 0;
        }
        else
        {
            velocidad = Math.sqrt(Math.pow(listaLocalizacion.get(ultPos).latitude - listaLocalizacion.get(ultPos - 1).latitude, 2) + Math.pow(listaLocalizacion.get(ultPos).longtitude -
            listaLocalizacion.get(ultPos - 1).longtitude,2)) / (listaLocalizacion.get(ultPos).time - listaLocalizacion.get(ultPos - 1).time);
        }

        return velocidad;
    }
}
