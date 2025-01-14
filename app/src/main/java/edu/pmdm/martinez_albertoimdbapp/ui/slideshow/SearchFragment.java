package edu.pmdm.martinez_albertoimdbapp.ui.slideshow;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.pmdm.martinez_albertoimdbapp.FilteredMoviesActivity;
import edu.pmdm.martinez_albertoimdbapp.R;
import edu.pmdm.martinez_albertoimdbapp.models.TMDBMovie;
import edu.pmdm.martinez_albertoimdbapp.models.TMDBSearchResponse;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Fragmento para buscar películas por género y año usando la API de TMDB.
 * Permite filtrar películas y mostrar los resultados en otra actividad.
 *
 * @author Alberto Martínez Vadillo
 */
public class SearchFragment extends Fragment {

    private Spinner genreSpinner;
    private EditText yearEditText;
    private final Map<String, String> genreMap = new HashMap<>();

    private static final String TMDB_API_KEY = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJmOTEyOGUzMDUyMmVkOWQ3MDcyMjExYzkxNjY1NTU5MiIsIm5iZiI6MTczNjYyNDA4OS4wNDgsInN1YiI6IjY3ODJjN2Q5YmQ3OTNjMDM1NDRlNzZlNCIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.krzhLkfIyxWlbV7tlyUSBzCncsl5zbGA8kSFBGZx1Co";
    private static final String TMDB_GENRES_URL = "https://api.themoviedb.org/3/genre/movie/list?language=en";
    private static final String TMDB_SEARCH_URL = "https://api.themoviedb.org/3/discover/movie";
    private static final String TMDB_DETAILS_URL = "https://api.themoviedb.org/3/movie/";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflar el layout del fragmento
        View root = inflater.inflate(R.layout.fragment_buscar, container, false);

        genreSpinner = root.findViewById(R.id.genreSpinner);
        yearEditText = root.findViewById(R.id.yearEditText);
        Button searchButton = root.findViewById(R.id.searchButton);

        // Cargar géneros y configurar el botón de búsqueda
        loadGenres();
        searchButton.setOnClickListener(v -> performSearch());

        return root;
    }

    /**
     * Carga los géneros desde la API de TMDB y los muestra en un Spinner.
     */
    private void loadGenres() {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(TMDB_GENRES_URL)
                        .get()
                        .addHeader("accept", "application/json")
                        .addHeader("Authorization", TMDB_API_KEY)
                        .build();

                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JSONObject jsonObject = new JSONObject(responseBody);
                    JSONArray genresArray = jsonObject.getJSONArray("genres");

                    List<String> genreNames = new ArrayList<>();
                    for (int i = 0; i < genresArray.length(); i++) {
                        JSONObject genreObject = genresArray.getJSONObject(i);
                        String id = genreObject.getString("id");
                        String name = genreObject.getString("name");
                        genreNames.add(name);
                        genreMap.put(name, id);
                    }

                    requireActivity().runOnUiThread(() -> {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                                android.R.layout.simple_spinner_item, genreNames);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        genreSpinner.setAdapter(adapter);
                    });
                }
            } catch (Exception e) {
                Log.e("GENRE_ERROR", "Error al cargar géneros", e);
            }
        }).start();
    }

    private static final int MIN_YEAR = 1800; // Año mínimo permitido para la búsqueda

    /**
     * Realiza la búsqueda de películas basado en el género y el año seleccionado.
     */
    private void performSearch() {
        String selectedGenre = genreSpinner.getSelectedItem().toString();
        String selectedGenreId = genreMap.get(selectedGenre);
        String year = yearEditText.getText().toString();

        if (year.isEmpty()) {
            Toast.makeText(getContext(), "Por favor, ingrese un año válido.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar que el año sea numérico y mayor o igual al año mínimo
        try {
            int yearInt = Integer.parseInt(year);
            if (yearInt < MIN_YEAR) {
                Toast.makeText(getContext(), "Por favor, ingrese un año mayor o igual a " + MIN_YEAR + ".", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "El año debe ser un número válido.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Si la validación es exitosa, continuar con la búsqueda
        new Thread(() -> {
            try {
                String url = TMDB_SEARCH_URL + "?include_adult=false&language=en-US&with_genres=" + selectedGenreId + "&primary_release_year=" + year;

                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("accept", "application/json")
                        .addHeader("Authorization", TMDB_API_KEY)
                        .build();

                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    List<TMDBMovie> movies = parseMovies(responseBody);

                    // Para cada película, obtener el `imdb_id`
                    for (TMDBMovie movie : movies) {
                        fetchImdbId(movie);
                    }

                    // Pasar los resultados a la nueva actividad
                    if (!movies.isEmpty()) {
                        Intent intent = new Intent(getContext(), FilteredMoviesActivity.class);
                        String moviesJson = new Gson().toJson(movies);
                        intent.putExtra("MOVIES_LIST", moviesJson);
                        requireActivity().runOnUiThread(() -> startActivity(intent));
                    } else {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "No se encontraron resultados.", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            } catch (Exception e) {
                Log.e("SEARCH_ERROR", "Error en la búsqueda", e);
            }
        }).start();
    }

    /**
     * Obtiene el `imdb_id` de una película usando su ID de TMDB.
     *
     * @param movie Película de la que se desea obtener el `imdb_id`.
     */
    private void fetchImdbId(TMDBMovie movie) {
        try {
            String url = TMDB_DETAILS_URL + movie.getId() + "?language=en-US";

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("accept", "application/json")
                    .addHeader("Authorization", TMDB_API_KEY)
                    .build();

            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                JSONObject jsonObject = new JSONObject(responseBody);
                if (jsonObject.has("imdb_id")) {
                    movie.setImdbId(jsonObject.getString("imdb_id"));
                }
            } else {
                Log.e("IMDB_ID_ERROR", "Error al obtener el IMDB ID: " + response.code());
            }
        } catch (Exception e) {
            Log.e("FETCH_IMDB_ERROR", "Error al obtener IMDB ID", e);
        }
    }

    /**
     * Parsea el JSON de respuesta para obtener la lista de películas.
     *
     * @param responseBody Respuesta JSON de la búsqueda.
     * @return Lista de películas.
     */
    private List<TMDBMovie> parseMovies(String responseBody) {
        List<TMDBMovie> movies = new ArrayList<>();
        try {
            Gson gson = new Gson();
            TMDBSearchResponse searchResponse = gson.fromJson(responseBody, TMDBSearchResponse.class);

            for (TMDBSearchResponse.Movie movie : searchResponse.getResults()) {
                movies.add(new TMDBMovie(
                        movie.getId(),
                        movie.getTitle(),
                        "https://image.tmdb.org/t/p/w500" + movie.getPosterPath()
                ));
            }
        } catch (Exception e) {
            Log.e("PARSE_ERROR", "Error al parsear las películas", e);
        }
        return movies;
    }
}