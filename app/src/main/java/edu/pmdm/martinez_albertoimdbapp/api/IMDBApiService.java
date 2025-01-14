package edu.pmdm.martinez_albertoimdbapp.api;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para interactuar con la API de IMDb, con manejo de rotación de claves.
 */
public class IMDBApiService {

    private static final String HOST = "imdb-com.p.rapidapi.com";
    private final Map<String, String> cache = new HashMap<>();
    private final OkHttpClient client = new OkHttpClient();
    private final RapidApiKeyManager apiKeyManager = new RapidApiKeyManager(); // Manejo de claves API

    /**
     * Obtiene los títulos más populares desde IMDb.
     *
     * @return JSON con los títulos más populares.
     * @throws IOException Si todas las claves fallan o ocurre otro error.
     */
    public String getTopMeterTitles(String apiKey) throws IOException {
        // Crear un cliente HTTP
        OkHttpClient client = new OkHttpClient();

        // Construir la solicitud HTTP
        Request request = new Request.Builder()
                .url("https://imdb-com.p.rapidapi.com/title/get-top-meter?topMeterTitlesType=ALL&limit=10")
                .get()
                .addHeader("x-rapidapi-key", apiKey) // Usa la clave API proporcionada
                .addHeader("x-rapidapi-host", "imdb-com.p.rapidapi.com")
                .build();

        // Ejecutar la solicitud y manejar la respuesta
        Response response = client.newCall(request).execute();
        try {
            if (response.isSuccessful()) {
                return response.body().string(); // Retorna el cuerpo de la respuesta
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
     *
     * @param tconst Identificador único de la película.
     * @return JSON con los detalles de la película.
     * @throws IOException Si todas las claves fallan o ocurre otro error.
     */
    public String getTitleDetails(String tconst) throws IOException {
        IOException lastException = null;

        // Intentar todas las claves disponibles
        for (int i = 0; i < apiKeyManager.apiKeys.size(); i++) {
            String apiKey = apiKeyManager.getCurrentKey();

            Request request = new Request.Builder()
                    .url("https://imdb-com.p.rapidapi.com/title/get-overview?tconst=" + tconst)
                    .get()
                    .addHeader("x-rapidapi-key", apiKey)
                    .addHeader("x-rapidapi-host", HOST)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    return response.body().string();
                } else if (response.code() == 429) { // Límite alcanzado
                    apiKeyManager.switchToNextKey();
                    logKeySwitch(apiKey);
                } else {
                    throw new IOException("Error en la respuesta: " + response.code());
                }
            } catch (IOException e) {
                lastException = e;
                apiKeyManager.switchToNextKey(); // Cambiar clave si hay error
                logKeySwitch(apiKey);
            }
        }

        // Si todas las claves fallaron, lanzar excepción
        throw lastException != null ? lastException : new IOException("Todas las claves API fallaron.");
    }

    /**
     * Método para registrar cambios de clave API.
     *
     * @param previousKey La clave anterior que fue usada.
     */
    private void logKeySwitch(String previousKey) {
        System.out.println("Clave API agotada: " + previousKey + ". Cambiando a la siguiente clave.");
    }
}