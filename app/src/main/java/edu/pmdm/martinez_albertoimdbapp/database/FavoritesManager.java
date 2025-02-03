package edu.pmdm.martinez_albertoimdbapp.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.pmdm.martinez_albertoimdbapp.models.Movie;
import edu.pmdm.martinez_albertoimdbapp.sync.SyncFavorites;

public class FavoritesManager {

    private final DatabaseHelper dbHelper;
    private final Context context;

    public FavoritesManager(Context context) {
        this.context = context;
        dbHelper = new DatabaseHelper(context);
    }

    /**
     * Verifica si una película ya está en los favoritos de un usuario (en SQLite).
     */
    public boolean isFavorite(String id, String userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_FAVORITES,
                null,
                DatabaseHelper.COLUMN_MOVIE_ID + "=? AND " + DatabaseHelper.COLUMN_USER_ID + "=?",
                new String[]{id, userId},
                null,
                null,
                null
        );

        boolean isFavorite = cursor.moveToFirst();
        cursor.close();

        return isFavorite;
    }

    /**
     * Agrega una película a la lista de favoritos de un usuario en LOCAL y en FIRESTORE.
     * Se usan únicamente los campos: id, title e imageUrl.
     * Al finalizar, se invoca la sincronización a través de SyncFavorites.
     */
    public void addFavorite(String id, String title, String imageUrl, String userId) {
        // ---------------------------
        // 1) Guardar en la DB local
        // ---------------------------
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_MOVIE_ID, id);
        values.put(DatabaseHelper.COLUMN_TITLE, title);
        values.put(DatabaseHelper.COLUMN_IMAGE_URL, imageUrl);
        values.put(DatabaseHelper.COLUMN_USER_ID, userId);

        db.insertWithOnConflict(
                DatabaseHelper.TABLE_FAVORITES,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
        );

        // ---------------------------
        // 2) Subir a Firestore (solo con id, title e imageUrl)
        // ---------------------------
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        Map<String, Object> movieData = new HashMap<>();
        movieData.put("id", id);
        movieData.put("title", title);
        movieData.put("imageUrl", imageUrl);

        firestore
                .collection("favorites")
                .document(userId)
                .collection("movies")
                .document(id)
                .set(movieData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Después de guardar, sincronizamos
                        new SyncFavorites(context, userId).syncFavorites();
                    }
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                });
    }

    /**
     * Elimina una película de la lista de favoritos de un usuario en LOCAL y en FIRESTORE.
     * Al finalizar, se invoca la sincronización a través de SyncFavorites.
     */
    public void removeFavorite(String id, String userId) {
        // 1) Eliminar de la DB local
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(
                DatabaseHelper.TABLE_FAVORITES,
                DatabaseHelper.COLUMN_MOVIE_ID + "=? AND " + DatabaseHelper.COLUMN_USER_ID + "=?",
                new String[]{id, userId}
        );

        // 2) Eliminar de Firestore
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore
                .collection("favorites")
                .document(userId)
                .collection("movies")
                .document(id)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    new SyncFavorites(context, userId).syncFavorites();
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                });
    }

    /**
     * Obtiene la lista de películas favoritas de un usuario desde la DB LOCAL (SQLite).
     */
    public List<Movie> getFavoritesForUser(String userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Movie> movies = new ArrayList<>();

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_FAVORITES,
                null,
                DatabaseHelper.COLUMN_USER_ID + "=?",
                new String[]{userId},
                null,
                null,
                null
        );

        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range")
                String imdbId = cursor.getString(
                        cursor.getColumnIndex(DatabaseHelper.COLUMN_MOVIE_ID)
                );
                @SuppressLint("Range")
                String title = cursor.getString(
                        cursor.getColumnIndex(DatabaseHelper.COLUMN_TITLE)
                );
                @SuppressLint("Range")
                String posterUrl = cursor.getString(
                        cursor.getColumnIndex(DatabaseHelper.COLUMN_IMAGE_URL)
                );

                Movie movie = new Movie(imdbId, posterUrl);
                movie.setTitle(title);
                movies.add(movie);
            } while (cursor.moveToNext());
        }

        cursor.close();

        return movies;
    }
}