package edu.pmdm.martinez_albertoimdbapp.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Helper para gestionar la base de datos SQLite de películas favoritas.
 * Este helper se encarga de crear, actualizar y gestionar la tabla donde se almacenan las películas favoritas de los usuarios.
 *
 * @author Alberto Martínez Vadillo
 */
public class FavoritesDatabaseHelper extends SQLiteOpenHelper {

    // Nombre de la base de datos
    private static final String DATABASE_NAME = "favorites_db";

    // Versión de la base de datos
    private static final int DATABASE_VERSION = 2;

    // Nombre de la tabla y columnas
    public static final String TABLE_FAVORITES = "favorites";
    public static final String COLUMN_ID = "id";               // ID de la película (clave primaria)
    public static final String COLUMN_TITLE = "title";         // Título de la película
    public static final String COLUMN_IMAGE_URL = "image_url"; // URL de la imagen del póster
    public static final String COLUMN_USER_ID = "user_id";     // ID del usuario propietario de los favoritos

    // SQL para crear la tabla de favoritos
    private static final String CREATE_TABLE_FAVORITES =
            "CREATE TABLE " + TABLE_FAVORITES + " (" +
                    COLUMN_ID + " TEXT, " +
                    COLUMN_TITLE + " TEXT, " +
                    COLUMN_IMAGE_URL + " TEXT, " +
                    COLUMN_USER_ID + " TEXT, " +
                    "PRIMARY KEY (" + COLUMN_ID + ", " + COLUMN_USER_ID + "))";

    /**
     * Constructor del helper de base de datos.
     *
     * @param context El contexto de la aplicación donde se inicializa la base de datos.
     */
    public FavoritesDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Crea la base de datos y la tabla de favoritos.
     *
     * @param db La instancia de la base de datos.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_FAVORITES); // Ejecuta el comando SQL para crear la tabla
    }

    /**
     * Maneja las actualizaciones de la base de datos.
     * En este caso, elimina la tabla existente y la recrea.
     *
     * @param db         La instancia de la base de datos.
     * @param oldVersion La versión anterior de la base de datos.
     * @param newVersion La nueva versión de la base de datos.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES); // Elimina la tabla si ya existe
        onCreate(db); // Crea una nueva tabla
    }
}