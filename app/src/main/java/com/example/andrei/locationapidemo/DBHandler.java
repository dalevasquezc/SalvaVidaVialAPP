package com.example.andrei.locationapidemo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.xml.sax.Parser;

import java.util.ArrayList;

/**
 * Created by Kashmir on 8/05/2016.
 */
public class DBHandler extends SQLiteOpenHelper {

    //DataBase Version
    private static final int DATABASE_VERSION = 1;

    //DataBase Name
    private static final String DATABASE_NAME = "SalvaVidaVial";

    //Localizacion table name
    private static final String TABLE_LOCALIZACION = "localizacion";

    //Localizacion table Localizacion column names
    private static final String KEY_ID_LOCALIZACION = "Id_localizacion";
    private static final String KEY_ID_RECORRIDO = "Id_recorrido";
    private static final String KEY_LATITUD = "Latitud";
    private static final String KEY_LONGITUD = "longitud";
    private static final String KEY_VELOCIDAD = "velocidad";
    private static final String KEY_TIME = "time";
    private static final String KEY_DELTA = "delta";
    private static final String KEY_DELTAPROMEDIO = "delta_promedio";
    private static final String KEY_PORCENTAJE_DIF = "porcentaje_diferencia";

    //Parametros table name
    private static final String TABLE_PARAMETRO = "parametro";

    //Columnas tabla parametro
    private static final String KEY_TIPO_PARAMETRO = "Tipo_parametro";
    private static final String KEY_CONTENIDO = "Contenido";

    //Recorrido Table name
    private static final String TABLE_RECORRIDO = "recorrido";

    //Columnas table recorrido
    private static final String KEY_ID_TABLE_RECORRIDO = "Id_recorrido";
    private static final String KEY_TIEMPO_TOTAL = "Tiempo_Total";
    private static final String KEY_VELOCIDAD_PROMEDIO = "Velocidad_Promedio";
    private static final String KEY_VELOCIDAD_MAXIMA = "Velocidad_Maxima";

    public DBHandler(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        String CREATE_LOCATION_TABLE = "CREATE TABLE " + TABLE_LOCALIZACION + "("
                + KEY_ID_LOCALIZACION + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_LATITUD + " FLOAT," + KEY_LONGITUD + " FLOAT," + KEY_VELOCIDAD + " FLOAT," + KEY_TIME + " FLOAT,"
                + KEY_DELTA + " FLOAT," + KEY_DELTAPROMEDIO + " FLOAT," + KEY_PORCENTAJE_DIF + " FLOAT" + ")";
        db.execSQL(CREATE_LOCATION_TABLE);
        String CREATE_PARAMETRO_TABLE = "CREATE TABLE " + TABLE_PARAMETRO + "(" + KEY_TIPO_PARAMETRO + " INTEGER PRIMARY KEY," + KEY_CONTENIDO + " INTEGER" + ")";
        db.execSQL(CREATE_PARAMETRO_TABLE);
        String CREATE_RECORRIDO_TABLE = "CREATE TABLE " + TABLE_RECORRIDO + "(" + KEY_ID_TABLE_RECORRIDO + " INTEGER PRIMARY KEY," + KEY_TIEMPO_TOTAL + " FLOAT," + KEY_VELOCIDAD_PROMEDIO
                + " FLOAT," + KEY_VELOCIDAD_MAXIMA + "FLOAT" + ")";
        db.execSQL(CREATE_RECORRIDO_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        //Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCALIZACION);
        //Creating table again
        onCreate(db);

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PARAMETRO);
        onCreate(db);

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECORRIDO);
        onCreate(db);
    }

    //Add new Location
    public void addLocation(Localizacion localizacion)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_LATITUD, localizacion.latitude); //Escribe latitud
        values.put(KEY_LONGITUD, localizacion.longtitude); //Escribe longitud
        values.put(KEY_VELOCIDAD, localizacion.speed);  //Escribe velocidad
        values.put(KEY_TIME, localizacion.time); // Escribe tiempo
        values.put(KEY_DELTA, localizacion.delta);
        values.put(KEY_DELTAPROMEDIO, localizacion.deltapromedio);
        values.put(KEY_PORCENTAJE_DIF, localizacion.porcentajeDif);

        //inserting row
        db.insert(TABLE_LOCALIZACION, null, values);
        db.close(); //Cerrando la conexion.
    }

    public void addParametro(int tipo, int contenido)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_TIPO_PARAMETRO, tipo);
        values.put(KEY_CONTENIDO, contenido);

        db.insert(TABLE_PARAMETRO, null, values);
        db.close();
    }

    public void addRecorrido(Recorrido recorrido)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_TIEMPO_TOTAL, recorrido.tiempoTotal);
        values.put(KEY_VELOCIDAD_PROMEDIO, recorrido.velocidadPromedi);
        values.put(KEY_VELOCIDAD_MAXIMA, recorrido.velocidadMaxima);

        db.insert(TABLE_RECORRIDO, null, values);
        db.close();
    }

    //Getting one Location
    public Localizacion getLocalizacion(int idLocalizacion)
    {
        String query = "SELECT * FROM " + TABLE_LOCALIZACION + " WHERE " + KEY_ID_LOCALIZACION + " = \"" + idLocalizacion + "\"";

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(query, null);

        Localizacion localizacion = new Localizacion();

        if(cursor.moveToFirst())
        {
            cursor.moveToFirst();
            localizacion.idLocalizacion = Integer.parseInt(cursor.getString(0));
            localizacion.latitude = Double.parseDouble(cursor.getString(1));
            localizacion.longtitude = Double.parseDouble(cursor.getString(2));
            localizacion.speed = Float.parseFloat(cursor.getString(3));
            localizacion.time = Long.parseLong(cursor.getString(4));
            localizacion.delta = Double.parseDouble(cursor.getString(5));
            localizacion.deltapromedio = Double.parseDouble(cursor.getString(6));
            localizacion.porcentajeDif = Double.parseDouble(cursor.getString(7));
            cursor.close();
        }
        else
        {
            localizacion = null;
        }
        db.close();
        return localizacion;
    }

    public Parametro getParametro(String tipo)
    {
        String query = "SELECT * FROM " + TABLE_PARAMETRO + " WHERE " + KEY_TIPO_PARAMETRO + " = \"" + tipo + "\"";

        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.rawQuery(query, null);

        Parametro parametro = new Parametro();

        if(cursor.moveToFirst())
        {
            cursor.moveToFirst();
            parametro.tipoParametro = cursor.getString(0);
            parametro.contenido = cursor.getString(1);
            cursor.close();
        }
        else
        {
            parametro = null;
        }
        db.close();
        return parametro;
    }

    //Actualizar registros en la base de datos
    public int updateLocalizacion(Localizacion localizacion)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_LATITUD, localizacion.latitude);
        values.put(KEY_LONGITUD, localizacion.longtitude);
        values.put(KEY_VELOCIDAD, localizacion.speed);
        values.put(KEY_TIME, localizacion.time);
        values.put(KEY_DELTA, localizacion.delta);
        values.put(KEY_DELTAPROMEDIO, localizacion.deltapromedio);
        values.put(KEY_PORCENTAJE_DIF, localizacion.porcentajeDif);

        //updating row
        return db.update(TABLE_LOCALIZACION, values, KEY_ID_LOCALIZACION + " = ?", new String[]{String.valueOf(localizacion.idLocalizacion)});
    }

    public int updateRecorrido(Recorrido recorrido)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_TIEMPO_TOTAL, recorrido.tiempoTotal);
        values.put(KEY_VELOCIDAD_PROMEDIO, recorrido.velocidadPromedi);
        values.put(KEY_VELOCIDAD_MAXIMA, recorrido.velocidadMaxima);

        return db.update(TABLE_RECORRIDO, values, KEY_ID_TABLE_RECORRIDO + " = ?", new String[]{String.valueOf(recorrido.idRecorrido)});
    }

    //Eliminar registros
    public void deleteLocation(Localizacion localizacion)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_LOCALIZACION, KEY_ID_LOCALIZACION + " = ?", new String[]{ String.valueOf(localizacion.idLocalizacion)});
        db.close();
    }

    public void deleteRecorrido(Recorrido recorrido)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_RECORRIDO, KEY_ID_TABLE_RECORRIDO + " = ?", new String[]{ String.valueOf(recorrido.idRecorrido)});
        db.close();
    }

    public ArrayList<Cursor> getData(String Query){
        //get writable database
        SQLiteDatabase sqlDB = this.getWritableDatabase();
        String[] columns = new String[] { "mesage" };
        //an array list of cursor to save two cursors one has results from the query
        //other cursor stores error message if any errors are triggered
        ArrayList<Cursor> alc = new ArrayList<Cursor>(2);
        MatrixCursor Cursor2= new MatrixCursor(columns);
        alc.add(null);
        alc.add(null);


        try{
            String maxQuery = Query ;
            //execute the query results will be save in Cursor c
            Cursor c = sqlDB.rawQuery(maxQuery, null);


            //add value to cursor2
            Cursor2.addRow(new Object[] { "Success" });

            alc.set(1,Cursor2);
            if (null != c && c.getCount() > 0) {


                alc.set(0,c);
                c.moveToFirst();

                return alc ;
            }
            return alc;
        } catch(SQLException sqlEx){
            Log.d("printing exception", sqlEx.getMessage());
            //if any exceptions are triggered save the error message to cursor an return the arraylist
            Cursor2.addRow(new Object[] { ""+sqlEx.getMessage() });
            alc.set(1,Cursor2);
            return alc;
        } catch(Exception ex){

            Log.d("printing exception", ex.getMessage());

            //if any exceptions are triggered save the error message to cursor an return the arraylist
            Cursor2.addRow(new Object[] { ""+ex.getMessage() });
            alc.set(1,Cursor2);
            return alc;
        }


    }
}
