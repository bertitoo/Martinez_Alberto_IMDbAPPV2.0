package edu.pmdm.martinez_albertoimdbapp.models;

import java.util.List;

/**
 * Clase que representa la respuesta de búsqueda de películas en TMDB (The Movie Database).
 * Incluye una lista de resultados que contienen información básica de las películas.
 *
 * @author Alberto Martínez Vadillo
 */
public class TMDBSearchResponse {

    private List<Movie> results; // Lista de resultados de la búsqueda

    /**
     * Obtiene la lista de películas de los resultados de la búsqueda.
     *
     * @return Lista de objetos {@link Movie}.
     */
    public List<Movie> getResults() {
        return results;
    }

    /**
     * Clase interna que representa una película dentro de los resultados de búsqueda.
     */
    public static class Movie {
        private String id; // ID de la película en TMDB
        private String title; // Título de la película
        private String poster_path; // Ruta del póster de la película

        /**
         * Obtiene el ID de la película.
         *
         * @return ID de la película.
         */
        public String getId() {
            return id;
        }

        /**
         * Obtiene el título de la película.
         *
         * @return Título de la película.
         */
        public String getTitle() {
            return title;
        }

        /**
         * Obtiene la ruta del póster de la película.
         *
         * @return Ruta del póster de la película.
         */
        public String getPosterPath() {
            return poster_path;
        }
    }
}