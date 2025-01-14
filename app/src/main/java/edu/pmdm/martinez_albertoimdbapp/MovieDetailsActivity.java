package edu.pmdm.martinez_albertoimdbapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.io.IOException;

import edu.pmdm.martinez_albertoimdbapp.models.MovieResponse;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Clase que muestra los detalles de una película, incluyendo su título, descripción,
 * año de lanzamiento, calificación y póster. Permite compartir los detalles de la película
 * mediante un mensaje SMS.
 *
 * @author Alberto Martínez Vadillo
 */

public class MovieDetailsActivity extends AppCompatActivity {

    private static final int CONTACTS_PERMISSION_CODE = 124; // Código para solicitar acceso a contactos

    private TextView titleTextView, plotTextView, releaseDateTextView, ratingTextView;
    private ImageView posterImageView;
    private Button shareButton;
    private String imdbId; // Identificador de IMDb de la película
    private String phoneNumber; // Número de teléfono del contacto seleccionado
    private String messageText; // Texto del mensaje a enviar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_details);

        // Inicializar vistas
        titleTextView = findViewById(R.id.titleTextView);
        plotTextView = findViewById(R.id.plotTextView);
        releaseDateTextView = findViewById(R.id.releaseDateTextView);
        ratingTextView = findViewById(R.id.ratingTextView);
        posterImageView = findViewById(R.id.posterImageView);
        shareButton = findViewById(R.id.smsButton);

        // Obtener el IMDb ID desde el intent
        Intent intent = getIntent();
        imdbId = intent.getStringExtra("IMDB_ID");

        Log.d("MOVIE_DETAILS", "ID recibido: " + imdbId);

        if (imdbId != null) {
            fetchMovieDetails(imdbId);
        } else {
            Toast.makeText(this, "Error: No se recibió el ID de la película.", Toast.LENGTH_SHORT).show();
        }

        // Configurar el botón para compartir detalles
        shareButton.setOnClickListener(v -> shareMovieDetails());
    }

    /**
     * Obtiene los detalles de la película usando su IMDb ID.
     *
     * @param imdbId El ID de IMDb de la película.
     */
    private void fetchMovieDetails(String imdbId) {
        if (!imdbId.startsWith("tt")) {
            Toast.makeText(this, "ID de película inválido: " + imdbId, Toast.LENGTH_SHORT).show();
            return;
        }

        OkHttpClient client = new OkHttpClient();
        String detailsUrl = "https://imdb-com.p.rapidapi.com/title/get-overview?tconst=" + imdbId;

        Request detailsRequest = new Request.Builder()
                .url(detailsUrl)
                .addHeader("x-rapidapi-key", "f7f23d7619msh83c94fd82b17f34p14e350jsn2d3cd99ac75c")
                .addHeader("x-rapidapi-host", "imdb-com.p.rapidapi.com")
                .build();

        client.newCall(detailsRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MovieDetailsActivity.this, "Error al obtener los detalles: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d("MOVIE_DETAILS", "Respuesta JSON: " + responseBody);
                    runOnUiThread(() -> parseAndUpdateUI(responseBody));
                    fetchMoviePlot(imdbId);
                } else {
                    Log.e("MOVIE_DETAILS", "Error HTTP: " + response.code() + " " + response.message());
                    runOnUiThread(() -> Toast.makeText(MovieDetailsActivity.this, "Error al obtener los detalles de la película.", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    /**
     * Obtiene la descripción de la película (plot) usando su IMDb ID.
     *
     * @param imdbId El ID de IMDb de la película.
     */
    private void fetchMoviePlot(String imdbId) {
        OkHttpClient client = new OkHttpClient();
        String plotUrl = "https://imdb-com.p.rapidapi.com/title/get-plot?tconst=" + imdbId;

        Request plotRequest = new Request.Builder()
                .url(plotUrl)
                .addHeader("x-rapidapi-key", "f7f23d7619msh83c94fd82b17f34p14e350jsn2d3cd99ac75c")
                .addHeader("x-rapidapi-host", "imdb-com.p.rapidapi.com")
                .build();

        client.newCall(plotRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MovieDetailsActivity.this, "Error al obtener la descripción: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d("MOVIE_PLOT", "Respuesta JSON (plot): " + responseBody);
                    runOnUiThread(() -> parsePlot(responseBody));
                } else {
                    Log.e("MOVIE_PLOT", "Error HTTP: " + response.code() + " " + response.message());
                    runOnUiThread(() -> Toast.makeText(MovieDetailsActivity.this, "Error al obtener la descripción de la película.", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    /**
     * Procesa y muestra la descripción de la película.
     *
     * @param plotJson JSON que contiene la descripción de la película.
     */
    private void parsePlot(String plotJson) {
        try {
            JSONObject jsonObject = new JSONObject(plotJson);
            String plot = "Descripción no disponible.";

            if (jsonObject.has("data")) {
                JSONObject dataObject = jsonObject.getJSONObject("data");
                if (dataObject.has("title")) {
                    JSONObject titleObject = dataObject.getJSONObject("title");
                    if (titleObject.has("plot")) {
                        plot = titleObject.getJSONObject("plot").getJSONObject("plotText").getString("plainText");
                    }
                }
            }

            plotTextView.setText(plot);
        } catch (Exception e) {
            Log.e("PLOT_PARSE_ERROR", "Error al procesar el JSON del plot", e);
            Toast.makeText(this, "Error al mostrar la descripción.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Analiza la respuesta JSON y actualiza la interfaz con los detalles de la película.
     *
     * @param jsonResponse Respuesta JSON que contiene los detalles de la película.
     */
    private void parseAndUpdateUI(String jsonResponse) {
        try {
            Gson gson = new Gson();
            MovieResponse movieResponse = gson.fromJson(jsonResponse, MovieResponse.class);

            String title = "Título no disponible.";
            String plot = "Descripción no disponible.";
            int releaseYear = 0;
            double rating = 0.0;
            String posterUrl = "";

            if (movieResponse.getData() != null) {
                MovieResponse.Data data = movieResponse.getData();
                if (data.getTitle() != null) {
                    MovieResponse.Title titleObject = data.getTitle();
                    title = titleObject.getTitleText() != null ? titleObject.getTitleText().getText() : title;
                    releaseYear = titleObject.getReleaseYear() != null ? titleObject.getReleaseYear().getYear() : releaseYear;
                    rating = titleObject.getRatingsSummary() != null ? titleObject.getRatingsSummary().getAggregateRating() : rating;
                    posterUrl = titleObject.getPrimaryImage() != null ? titleObject.getPrimaryImage().getUrl() : posterUrl;
                    if (titleObject.getPlot() != null && titleObject.getPlot().getPlotText() != null) {
                        plot = titleObject.getPlot().getPlotText().getPlainText();
                    }
                }
            }

            titleTextView.setText(title);
            plotTextView.setText(plot);
            releaseDateTextView.setText("Año de lanzamiento: " + releaseYear);
            ratingTextView.setText("Calificación: " + rating);
            Picasso.get().load(posterUrl).into(posterImageView);

        } catch (Exception e) {
            Log.e("PARSE_JSON_ERROR", "Error al procesar el JSON", e);
            Toast.makeText(this, "Error al mostrar los detalles de la película.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Permite compartir los detalles de la película mediante SMS.
     */
    private void shareMovieDetails() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, CONTACTS_PERMISSION_CODE);
        } else {
            String movieTitle = titleTextView.getText().toString();
            String movieRating = ratingTextView.getText().toString().replace("Calificación: ", "");
            messageText = "Esta película te gustará: " + movieTitle + " Rating: " + movieRating;
            seleccionarContacto();
        }
    }

    /**
     * Permite seleccionar un contacto de la lista de contactos.
     */
    private void seleccionarContacto() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(intent, CONTACTS_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CONTACTS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                seleccionarContacto();
            } else {
                Toast.makeText(this, "Permiso para acceder a contactos denegado.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("Range")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CONTACTS_PERMISSION_CODE && resultCode == RESULT_OK) {
            Uri contactUri = data.getData();
            String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER};
            try (Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    Intent smsIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + phoneNumber));
                    smsIntent.putExtra("sms_body", messageText);
                    startActivity(Intent.createChooser(smsIntent, "Selecciona una aplicación de mensajería"));
                }
            } catch (Exception e) {
                Toast.makeText(this, "Error al seleccionar el contacto: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("CONTACT_SELECTION", "Error al obtener el número de contacto", e);
            }
        }
    }
}