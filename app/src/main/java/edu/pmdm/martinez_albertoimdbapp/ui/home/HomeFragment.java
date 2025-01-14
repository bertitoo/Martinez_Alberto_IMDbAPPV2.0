package edu.pmdm.martinez_albertoimdbapp.ui.home;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import edu.pmdm.martinez_albertoimdbapp.MovieDetailsActivity;
import edu.pmdm.martinez_albertoimdbapp.R;
import edu.pmdm.martinez_albertoimdbapp.api.IMDBApiService;
import edu.pmdm.martinez_albertoimdbapp.database.FavoritesManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Fragmento principal que muestra las películas más populares desde IMDb.
 */
public class HomeFragment extends Fragment {

    private GridLayout gridLayout;
    private IMDBApiService imdbApiService;
    private final Map<String, Bitmap> imageCache = new HashMap<>();
    private FavoritesManager favoritesManager;
    private String userId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        gridLayout = root.findViewById(R.id.gridLayout);
        imdbApiService = new IMDBApiService();
        favoritesManager = new FavoritesManager(requireContext());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userId = (user != null) ? user.getUid() : null;

        loadTopMovies();
        return root;
    }

    /**
     * Carga las películas más populares.
     */
    private void loadTopMovies() {
        new Thread(() -> {
            try {
                String response = imdbApiService.getTopMeterTitles(); // Una sola solicitud
                parseAndDisplayMovies(response);
            } catch (Exception e) {
                Log.e("IMDB_ERROR", "Error al cargar películas", e);
            }
        }).start();
    }

    /**
     * Procesa y muestra las películas en el GridLayout.
     *
     * @param response Respuesta JSON de la API.
     */
    private void parseAndDisplayMovies(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONObject dataObject = jsonObject.getJSONObject("data");
            JSONArray edges = dataObject.getJSONObject("topMeterTitles").getJSONArray("edges");

            int count = 0; // Contador para limitar a 10 películas

            for (int i = 0; i < edges.length() && count < 10; i++) { // Limitar a 10 iteraciones
                JSONObject node = edges.getJSONObject(i).getJSONObject("node");
                String tconst = node.getString("id");
                String imageUrl = node.getJSONObject("primaryImage").getString("url");
                String title = node.getJSONObject("titleText").getString("text");

                count++; // Incrementar el contador
                requireActivity().runOnUiThread(() -> addImageToGrid(imageUrl, tconst, title));
            }
        } catch (Exception e) {
            Log.e("PARSE_ERROR", "Error al procesar datos de películas", e);
        }
    }

    /**
     * Añade una imagen al GridLayout.
     */
    private void addImageToGrid(String imageUrl, String tconst, String movieTitle) {
        ImageView imageView = new ImageView(getContext());
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 500;
        params.height = 750;
        params.setMargins(16, 16, 16, 16);
        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        if (imageCache.containsKey(imageUrl)) {
            imageView.setImageBitmap(imageCache.get(imageUrl));
        } else {
            new Thread(() -> {
                Bitmap bitmap = getBitmapFromURL(imageUrl);
                if (bitmap != null) {
                    imageCache.put(imageUrl, bitmap);
                    requireActivity().runOnUiThread(() -> imageView.setImageBitmap(bitmap));
                }
            }).start();
        }

        imageView.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MovieDetailsActivity.class);
            intent.putExtra("IMDB_ID", tconst);
            startActivity(intent);
        });

        imageView.setOnLongClickListener(v -> {
            if (userId == null) {
                Toast.makeText(getContext(), "Por favor, inicia sesión para añadir películas a favoritos.", Toast.LENGTH_LONG).show();
                return true;
            }

            boolean isFavorite = favoritesManager.isFavorite(tconst, userId);
            if (isFavorite) {
                Toast.makeText(getContext(), "La película ya está en tus favoritos.", Toast.LENGTH_LONG).show();
            } else {
                favoritesManager.addFavorite(tconst, movieTitle, imageUrl, userId);
                Toast.makeText(getContext(), "Película añadida a favoritos.", Toast.LENGTH_LONG).show();
            }
            return true;
        });

        gridLayout.addView(imageView);
    }

    /**
     * Descarga una imagen desde una URL.
     */
    private Bitmap getBitmapFromURL(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            Log.e("IMAGE_ERROR", "Error al descargar la imagen", e);
            return null;
        }
    }
}