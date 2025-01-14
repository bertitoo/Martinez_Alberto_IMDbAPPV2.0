package edu.pmdm.martinez_albertoimdbapp.api;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

/**
 * Servicio para interactuar con la API de IMDb.
 * Proporciona métodos para obtener información sobre las películas más populares
 * y los detalles específicos de una película.
 *
 * @author Alberto Martínez Vadillo
 */
public class IMDBApiService {

    // Clave de la API para acceder a los servicios de IMDb
    private static final String API_KEY = "9bd454f9f5msh0ba5340b67f82bdp1c304bjsnbcd864c2f55d";

    // Host requerido para las solicitudes de IMDb API
    private static final String HOST = "imdb-com.p.rapidapi.com";

    /**
     * Obtiene los títulos más populares de IMDb (Top Meter Titles).
     *
     * @return Una cadena JSON con los títulos más populares.
     * @throws IOException Si hay un error en la conexión o en la respuesta de la API.
     */
    public String getTopMeterTitles() throws IOException {
        // Crear un cliente HTTP
        OkHttpClient client = new OkHttpClient();

        // Construir la solicitud HTTP
        Request request = new Request.Builder()
                .url("https://imdb-com.p.rapidapi.com/title/get-top-meter?topMeterTitlesType=ALL")
                .get()
                .addHeader("x-rapidapi-key", API_KEY)
                .addHeader("x-rapidapi-host", HOST)
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
            // Asegurarse de cerrar el cuerpo de la respuesta para evitar fugas de recursos
            if (response.body() != null) {
                response.body().close();
            }
        }
    }

    /**
     * Obtiene los detalles de una película específica usando su identificador `tconst`.
     *
     * @param tconst Identificador único de la película en IMDb (por ejemplo, "tt0120338").
     * @return Una cadena JSON con los detalles de la película.
     * @throws IOException Si hay un error en la conexión o en la respuesta de la API.
     */
    public String getTitleDetails(String tconst) throws IOException {
        // Crear un cliente HTTP
        OkHttpClient client = new OkHttpClient();

        // Construir la solicitud HTTP
        Request request = new Request.Builder()
                .url("https://imdb-com.p.rapidapi.com/title/get-overview?tconst=" + tconst)
                .get()
                .addHeader("x-rapidapi-key", API_KEY)
                .addHeader("x-rapidapi-host", HOST)
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
            // Asegurarse de cerrar el cuerpo de la respuesta para evitar fugas de recursos
            if (response.body() != null) {
                response.body().close();
            }
        }
    }
}