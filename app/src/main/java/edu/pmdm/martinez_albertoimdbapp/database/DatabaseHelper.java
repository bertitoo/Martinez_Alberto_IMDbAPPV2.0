package edu.pmdm.martinez_albertoimdbapp.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
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
    private static final int DATABASE_VERSION = 6; // Incrementar la versión para reflejar los cambios

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
    public static final String COLUMN_USER_UID = "uid";             // UID del usuario (clave primaria)
    public static final String COLUMN_USER_NAME = "nombre";         // Nombre del usuario
    public static final String COLUMN_USER_EMAIL = "email";         // E-mail del usuario
    public static final String COLUMN_USER_ADDRESS = "address";     // Dirección del usuario
    public static final String COLUMN_USER_PHONE = "phone";         // Teléfono del usuario
    public static final String COLUMN_USER_IMAGE_URL = "image_url"; // URL de la imagen del perfil
    public static final String COLUMN_USER_ULTIMO_LOGIN = "ultimo_login";  // Último login
    public static final String COLUMN_USER_ULTIMO_LOGOUT = "ultimo_logout"; // Último logout

    // SQL para crear la tabla de usuarios
    private static final String CREATE_TABLE_USERS =
            "CREATE TABLE " + TABLE_USERS + " (" +
                    COLUMN_USER_UID + " TEXT PRIMARY KEY, " +
                    COLUMN_USER_NAME + " TEXT, " +
                    COLUMN_USER_EMAIL + " TEXT, " +
                    COLUMN_USER_ADDRESS + " TEXT, " +
                    COLUMN_USER_PHONE + " TEXT, " +
                    COLUMN_USER_IMAGE_URL + " TEXT, " +
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
        db.execSQL(CREATE_TABLE_FAVORITES);  // Crear tabla de favoritos
        db.execSQL(CREATE_TABLE_USERS);      // Crear tabla de usuarios
    }

    /**
     * Maneja las actualizaciones de la base de datos.
     * En este caso, añadimos las nuevas columnas a la tabla `users` si no existen.
     *
     * @param db         La instancia de la base de datos.
     * @param oldVersion La versión anterior de la base de datos.
     * @param newVersion La nueva versión de la base de datos.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 6) {
            // Comprobar si las columnas ya existen antes de intentar agregarlas
            if (!columnExists(db, TABLE_USERS, COLUMN_USER_ADDRESS)) {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_USER_ADDRESS + " TEXT;");
            }
            if (!columnExists(db, TABLE_USERS, COLUMN_USER_PHONE)) {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_USER_PHONE + " TEXT;");
            }
            if (!columnExists(db, TABLE_USERS, COLUMN_USER_IMAGE_URL)) {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_USER_IMAGE_URL + " TEXT;");
            }
        }
    }

    public void deleteUsersByEmail(String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Eliminamos todos los usuarios con el correo especificado
        db.delete(TABLE_USERS, COLUMN_USER_EMAIL + " = ?", new String[]{email});
    }

    /**
     * Comprueba si una columna existe en una tabla específica.
     *
     * @param db          La instancia de la base de datos.
     * @param tableName   El nombre de la tabla.
     * @param columnName  El nombre de la columna a comprobar.
     * @return true si la columna existe, false si no.
     */
    private boolean columnExists(SQLiteDatabase db, String tableName, String columnName) {
        Cursor cursor = db.rawQuery("PRAGMA table_info(" + tableName + ");", null);
        while (cursor.moveToNext()) {
            @SuppressLint("Range") String column = cursor.getString(cursor.getColumnIndex("name"));
            if (column.equals(columnName)) {
                cursor.close();
                return true;
            }
        }
        cursor.close();
        return false;
    }

    /**
     * Obtiene la dirección cifrada del usuario.
     *
     * @param uid El UID del usuario.
     * @return La dirección cifrada.
     */
    public String getUserEncryptedAddress(String uid) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_USER_ADDRESS},
                COLUMN_USER_UID + "=?", new String[]{uid}, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            @SuppressLint("Range") String encryptedAddress = cursor.getString(cursor.getColumnIndex(COLUMN_USER_ADDRESS));
            cursor.close();
            return encryptedAddress;
        }
        return null;
    }

    /**
     * Obtiene el número de teléfono cifrado del usuario.
     *
     * @param uid El UID del usuario.
     * @return El número de teléfono cifrado.
     */
    public String getUserEncryptedPhone(String uid) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_USER_PHONE},
                COLUMN_USER_UID + "=?", new String[]{uid}, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            @SuppressLint("Range") String encryptedPhone = cursor.getString(cursor.getColumnIndex(COLUMN_USER_PHONE));
            cursor.close();
            return encryptedPhone;
        }
        return null;
    }

    /**
     * Guarda los datos cifrados del usuario (dirección y teléfono).
     *
     * @param uid               El UID del usuario.
     * @param encryptedAddress  La dirección cifrada.
     * @param encryptedPhone    El número de teléfono cifrado.
     */
    public void saveEncryptedUserData(String uid, String encryptedAddress, String encryptedPhone, String photoUrl) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ADDRESS, encryptedAddress);
        values.put(COLUMN_USER_PHONE, encryptedPhone);
        values.put(COLUMN_USER_IMAGE_URL, photoUrl);

        db.update(TABLE_USERS, values, COLUMN_USER_UID + "=?", new String[]{uid});
    }

    public SQLiteDatabase getDatabase(boolean writable) {
        return writable ? getWritableDatabase() : getReadableDatabase();
    }
}