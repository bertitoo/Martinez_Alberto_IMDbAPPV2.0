package edu.pmdm.martinez_albertoimdbapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import edu.pmdm.martinez_albertoimdbapp.database.FavoritesManager;
import edu.pmdm.martinez_albertoimdbapp.models.TMDBMovie;

/**
 * Actividad para mostrar las películas filtradas en un diseño de cuadrícula.
 * Permite visualizar los detalles de cada película y añadirlas a favoritos.
 *
 * @author Alberto Martínez Vadillo
 */
public class FilteredMoviesActivity extends AppCompatActivity {

    private GridLayout gridLayout; // Layout para mostrar las películas en cuadrícula
    private FavoritesManager favoritesManager; // Gestión de favoritos
    private String currentUserId; // ID del usuario autenticado

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filtered_movies);

        gridLayout = findViewById(R.id.gridLayout);
        favoritesManager = new FavoritesManager(this);

        // Obtener el ID del usuario autenticado
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = (user != null) ? user.getUid() : null;

        // Obtener la lista de películas desde el Intent
        String moviesJson = getIntent().getStringExtra("MOVIES_LIST");
        if (moviesJson != null) {
            Type listType = new TypeToken<List<TMDBMovie>>() {}.getType();
            List<TMDBMovie> movies = new Gson().fromJson(moviesJson, listType);
            displayResults(movies);
        } else {
            Toast.makeText(this, "No se encontraron películas para mostrar.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Muestra los resultados de las películas en el GridLayout.
     *
     * @param movies Lista de películas a mostrar.
     */
    private void displayResults(List<TMDBMovie> movies) {
        gridLayout.removeAllViews();

        if (movies.isEmpty()) {
            Toast.makeText(this, "No se encontraron resultados.", Toast.LENGTH_SHORT).show();
            return;
        }

        for (TMDBMovie movie : movies) {
            addImageToGrid(movie);
        }
    }

    /**
     * Añade la imagen de una película al GridLayout.
     * Permite mostrar detalles al hacer clic y agregar a favoritos con una pulsación larga.
     *
     * @param movie Objeto TMDBMovie que contiene los detalles de la película.
     */
    private void addImageToGrid(TMDBMovie movie) {
        ImageView imageView = new ImageView(this);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 500;
        params.height = 750;
        params.setMargins(16, 16, 16, 16);
        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        // Descargar y establecer la imagen del póster
        new Thread(() -> {
            try {
                Bitmap bitmap = getBitmapFromURL(movie.getPosterPath());
                if (bitmap != null) {
                    runOnUiThread(() -> imageView.setImageBitmap(bitmap));
                }
            } catch (Exception e) {
                Log.e("IMAGE_ERROR", "Error al cargar imagen", e);
            }
        }).start();

        // Configurar clic para mostrar detalles
        imageView.setOnClickListener(v -> {
            Intent intent = new Intent(this, MovieDetailsActivity.class);
            intent.putExtra("IMDB_ID", movie.getImdbId()); // Pasar el ID de IMDb
            Toast.makeText(this, "Mostrando detalles de: " + movie.getTitle(), Toast.LENGTH_SHORT).show();
            startActivity(intent);
        });

        // Configurar pulsación larga para agregar a favoritos
        imageView.setOnLongClickListener(v -> {
            if (currentUserId == null) {
                Toast.makeText(this, "Por favor, inicia sesión para gestionar favoritos.", Toast.LENGTH_LONG).show();
                return true;
            }

            boolean isFavorite = favoritesManager.isFavorite(movie.getImdbId(), currentUserId);
            if (isFavorite) {
                Toast.makeText(this, "La película '" + movie.getTitle() + "' ya está en tus favoritos.", Toast.LENGTH_LONG).show();
            } else {
                favoritesManager.addFavorite(movie.getImdbId(), movie.getTitle(), movie.getPosterPath(), currentUserId);
                Toast.makeText(this, "Película añadida a favoritos: " + movie.getTitle(), Toast.LENGTH_LONG).show();
            }
            return true;
        });

        gridLayout.addView(imageView);
    }

    /**
     * Descarga un bitmap desde una URL y lo redimensiona.
     *
     * @param imageUrl URL de la imagen a descargar.
     * @return Bitmap redimensionado o null si ocurre un error.
     */
    private Bitmap getBitmapFromURL(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap originalBitmap = BitmapFactory.decodeStream(input);

            // Ajustar el tamaño de la imagen
            int maxWidth = 500; // Ancho máximo deseado
            int maxHeight = 750; // Alto máximo deseado
            return resizeBitmap(originalBitmap, maxWidth, maxHeight);
        } catch (Exception e) {
            Log.e("IMAGE_ERROR", "Error al descargar la imagen", e);
            return null;
        }
    }

    /**
     * Redimensiona un Bitmap manteniendo la relación de aspecto.
     *
     * @param bitmap    El Bitmap original.
     * @param maxWidth  Ancho máximo deseado.
     * @param maxHeight Alto máximo deseado.
     * @return Bitmap redimensionado.
     */
    private Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        if (bitmap == null) return null;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float aspectRatio = (float) width / height;

        if (width > maxWidth || height > maxHeight) {
            if (aspectRatio > 1) {
                // Imagen más ancha que alta
                width = maxWidth;
                height = Math.round(maxWidth / aspectRatio);
            } else {
                // Imagen más alta que ancha
                height = maxHeight;
                width = Math.round(maxHeight * aspectRatio);
            }
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }
}