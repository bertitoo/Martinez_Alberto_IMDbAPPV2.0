package edu.pmdm.martinez_albertoimdbapp.models;

import java.io.Serializable;

/**
 * Clase que representa una película obtenida de TMDB (The Movie Database).
 * Implementa {@link Serializable} para permitir la serialización de objetos de esta clase.
 * Contiene información básica como el ID de TMDB, el título, la URL del póster y el ID de IMDB.
 *
 * @author Alberto Martínez Vadillo
 */
public class TMDBMovie implements Serializable {

    private String id;          // TMDB ID
    private String title;       // Título de la película
    private String posterPath;  // URL del póster
    private String imdbId;      // ID de IMDB

    /**
     * Constructor para inicializar una película con su ID, título y URL del póster.
     *
     * @param id         ID de TMDB.
     * @param title      Título de la película.
     * @param posterPath URL del póster de la película.
     */
    public TMDBMovie(String id, String title, String posterPath) {
        this.id = id;
        this.title = title;
        this.posterPath = posterPath;
    }

    /**
     * Obtiene el ID de TMDB.
     *
     * @return ID de TMDB.
     */
    public String getId() {
        return id;
    }

    /**
     * Establece el ID de TMDB.
     *
     * @param id Nuevo ID de TMDB.
     */
    public void setId(String id) {
        this.id = id;
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
     * Establece el título de la película.
     *
     * @param title Nuevo título de la película.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Obtiene la URL del póster de la película.
     *
     * @return URL del póster.
     */
    public String getPosterPath() {
        return posterPath;
    }

    /**
     * Obtiene el ID de IMDB.
     *
     * @return ID de IMDB.
     */
    public String getImdbId() {
        return imdbId;
    }

    /**
     * Establece el ID de IMDB.
     *
     * @param imdbId Nuevo ID de IMDB.
     */
    public void setImdbId(String imdbId) {
        this.imdbId = imdbId;
    }
}