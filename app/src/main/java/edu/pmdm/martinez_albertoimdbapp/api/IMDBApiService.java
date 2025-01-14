package edu.pmdm.martinez_albertoimdbapp.api;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para interactuar con la API de IMDb.
 */
public class IMDBApiService {

    private static final String API_KEY = "f7f23d7619msh83c94fd82b17f34p14e350jsn2d3cd99ac75c";
    private static final String HOST = "imdb-com.p.rapidapi.com";
    private final Map<String, String> cache = new HashMap<>(); // Caché en memoria
    private final OkHttpClient client = new OkHttpClient();

    /**
     * Obtiene los títulos más populares de IMDb.
     */
    public String getTopMeterTitles() throws IOException {
        String cacheKey = "top_meter_titles";
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey); // Retorna de caché si existe
        }

        Request request = new Request.Builder()
                .url("https://imdb-com.p.rapidapi.com/title/get-top-meter?topMeterTitlesType=ALL")
                .get()
                .addHeader("x-rapidapi-key", API_KEY)
                .addHeader("x-rapidapi-host", HOST)
                .build();

        Response response = client.newCall(request).execute();
        try {
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                cache.put(cacheKey, responseBody); // Guardar en caché
                return responseBody;
            } else {
                throw new IOException("Error en la respuesta: " + response.code());
            }
        } finally {
            if (response.body() != null) {
                response.body().close();
            }
        }
    }

    /**
     * Obtiene los detalles de una película usando su tconst.
     */
    public String getTitleDetails(String tconst) throws IOException {
        if (cache.containsKey(tconst)) {
            return cache.get(tconst); // Retorna de caché si existe
        }

        Request request = new Request.Builder()
                .url("https://imdb-com.p.rapidapi.com/title/get-overview?tconst=" + tconst)
                .get()
                .addHeader("x-rapidapi-key", API_KEY)
                .addHeader("x-rapidapi-host", HOST)
                .build();

        Response response = client.newCall(request).execute();
        try {
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                cache.put(tconst, responseBody); // Guardar en caché
                return responseBody;
            } else {
                throw new IOException("Error en la respuesta: " + response.code());
            }
        } finally {
            if (response.body() != null) {
                response.body().close();
            }
        }
    }
}