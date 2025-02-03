package edu.pmdm.martinez_albertoimdbapp.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Helper para gestionar la base de datos SQLite de películas favoritas y usuarios.
 * Se encarga de crear, actualizar y gestionar la tabla de favoritos y la tabla de usuarios.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // Nombre de la base de datos
    private static final String DATABASE_NAME = "imdb_db";

    // Versión de la base de datos (incrementada para forzar la actualización)
    private static final int DATABASE_VERSION = 4;

    // Tabla favorites y sus columnas
    public static final String TABLE_FAVORITES = "favorites";
    public static final String COLUMN_MOVIE_ID = "id";         // ID de la película (clave primaria compuesta junto con user_id)
    public static final String COLUMN_TITLE = "title";           // Título de la película
    public static final String COLUMN_IMAGE_URL = "image_url";     // URL de la imagen del póster
    public static final String COLUMN_USER_ID = "user_id";         // ID del usuario propietario de los favoritos

    // SQL para crear la tabla de favoritos
    private static final String CREATE_TABLE_FAVORITES =
            "CREATE TABLE " + TABLE_FAVORITES + " (" +
                    COLUMN_MOVIE_ID + " TEXT, " +
                    COLUMN_TITLE + " TEXT, " +
                    COLUMN_IMAGE_URL + " TEXT, " +
                    COLUMN_USER_ID + " TEXT, " +
                    "PRIMARY KEY (" + COLUMN_MOVIE_ID + ", " + COLUMN_USER_ID + "))";

    // Tabla users y sus columnas
    public static final String TABLE_USERS = "users";
    // Usamos "uid" para el user id de Firebase
    public static final String COLUMN_USER_UID = "uid";         // UID del usuario (clave primaria)
    public static final String COLUMN_USER_NAME = "nombre";       // Nombre del usuario
    public static final String COLUMN_USER_EMAIL = "email";       // E-mail del usuario
    public static final String COLUMN_USER_ULTIMO_LOGIN = "ultimo_login";   // Último login
    public static final String COLUMN_USER_ULTIMO_LOGOUT = "ultimo_logout";   // Último logout

    // SQL para crear la tabla de usuarios
    private static final String CREATE_TABLE_USERS =
            "CREATE TABLE " + TABLE_USERS + " (" +
                    COLUMN_USER_UID + " TEXT PRIMARY KEY, " +
                    COLUMN_USER_NAME + " TEXT, " +
                    COLUMN_USER_EMAIL + " TEXT, " +
                    COLUMN_USER_ULTIMO_LOGIN + " TEXT, " +
                    COLUMN_USER_ULTIMO_LOGOUT + " TEXT)";

    /**
     * Constructor del helper de base de datos.
     *
     * @param context El contexto de la aplicación donde se inicializa la base de datos.
     */
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Crea la base de datos y las tablas necesarias.
     *
     * @param db La instancia de la base de datos.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_FAVORITES);
        db.execSQL(CREATE_TABLE_USERS);
    }

    /**
     * Maneja las actualizaciones de la base de datos.
     * En este caso, elimina las tablas existentes y las recrea.
     *
     * @param db         La instancia de la base de datos.
     * @param oldVersion La versión anterior de la base de datos.
     * @param newVersion La nueva versión de la base de datos.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    public SQLiteDatabase getDatabase(boolean writable) {
        return writable ? getWritableDatabase() : getReadableDatabase();
    }
}