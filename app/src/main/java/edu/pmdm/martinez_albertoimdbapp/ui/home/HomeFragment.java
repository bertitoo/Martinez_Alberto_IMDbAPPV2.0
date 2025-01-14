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

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.pmdm.martinez_albertoimdbapp.MovieDetailsActivity;
import edu.pmdm.martinez_albertoimdbapp.R;
import edu.pmdm.martinez_albertoimdbapp.api.IMDBApiService;
import edu.pmdm.martinez_albertoimdbapp.database.FavoritesManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Fragmento principal que muestra las películas más populares desde IMDb.
 * Permite al usuario añadir películas a favoritos o ver más detalles.
 *
 * @author Alberto Martínez Vadillo
 */
public class HomeFragment extends Fragment {

    private GridLayout gridLayout;
    private IMDBApiService imdbApiService;
    private final Map<String, Bitmap> imageCache = new HashMap<>(); // Caché de imágenes
    private final Map<String, String> titleCache = new HashMap<>(); // Caché de títulos
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

        loadTopMeterImages();
        return root;
    }

    /**
     * Carga las imágenes de las películas más populares desde IMDb y las muestra en el GridLayout.
     */
    private void loadTopMeterImages() {
        new Thread(() -> {
            try {
                String response = imdbApiService.getTopMeterTitles();
                List<String> tconstList = parseTconsts(response);

                for (String tconst : tconstList) {
                    String detailsResponse = imdbApiService.getTitleDetails(tconst);
                    String imageUrl = parseImageUrl(detailsResponse);
                    String movieTitle = parseMovieTitle(detailsResponse);

                    if (imageUrl != null && movieTitle != null) {
                        titleCache.put(tconst, movieTitle); // Guardar el título en caché
                        requireActivity().runOnUiThread(() -> addImageToGrid(imageUrl, tconst, movieTitle));
                    }
                }
            } catch (Exception e) {
                Log.e("IMDB_ERROR", "Error al cargar imágenes", e);
            }
        }).start();
    }

    /**
     * Extrae los IDs de IMDb (tconst) de la respuesta de IMDb.
     *
     * @param response Respuesta en formato JSON de IMDb.
     * @return Lista de IDs tconst.
     */
    private List<String> parseTconsts(String response) {
        List<String> tconsts = new ArrayList<>();
        String regex = "tt\\d{7,8}";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(response);
        while (matcher.find() && tconsts.size() < 10) {
            tconsts.add(matcher.group());
        }
        return tconsts;
    }

    /**
     * Extrae la URL de la imagen del póster desde la respuesta JSON.
     *
     * @param response Respuesta en formato JSON de IMDb.
     * @return URL de la imagen o null si no se encuentra.
     */
    private String parseImageUrl(String response) {
        String regex = "https://.*?\\.jpg";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(response);
        return matcher.find() ? matcher.group() : null;
    }

    /**
     * Extrae el título de la película desde la respuesta JSON.
     *
     * @param response Respuesta en formato JSON de IMDb.
     * @return Título de la película o null si no se encuentra.
     */
    private String parseMovieTitle(String response) {
        String regex = "\"titleText\":\\{\"text\":\"(.*?)\"";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(response);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * Añade una imagen al GridLayout.
     *
     * @param imageUrl   URL de la imagen del póster.
     * @param tconst     ID de IMDb de la película.
     * @param movieTitle Título de la película.
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
            Toast.makeText(getContext(), "Mostrando detalles de la película: " + movieTitle, Toast.LENGTH_SHORT).show();
            startActivity(intent);
        });

        imageView.setOnLongClickListener(v -> {
            if (userId == null) {
                Toast.makeText(getContext(), "Por favor, inicia sesión para añadir películas a favoritos.", Toast.LENGTH_LONG).show();
                return true;
            }

            boolean isFavorite = favoritesManager.isFavorite(tconst, userId);
            if (isFavorite) {
                Toast.makeText(getContext(), "La película '" + movieTitle + "' ya está en tus favoritos.", Toast.LENGTH_LONG).show();
            } else {
                favoritesManager.addFavorite(tconst, movieTitle, imageUrl, userId);
                Toast.makeText(getContext(), "Película añadida a favoritos: " + movieTitle, Toast.LENGTH_LONG).show();
            }
            return true;
        });

        gridLayout.addView(imageView);
    }

    /**
     * Descarga una imagen desde una URL y la devuelve como un objeto Bitmap.
     *
     * @param imageUrl URL de la imagen.
     * @return Objeto Bitmap de la imagen descargada o null si ocurre un error.
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