package edu.pmdm.martinez_albertoimdbapp.models;

/**
 * Representa una película con atributos básicos como título, ID de IMDb y URL del póster.
 *
 * Esta clase sirve como modelo para gestionar información básica de películas
 * dentro de la aplicación.
 *
 * @author Alberto Martínez Vadillo
 */
public class Movie {
    private String title;         // Título de la película
    private String imdbId;        // ID único de IMDb
    private String posterUrl;     // URL de la imagen del póster

    /**
     * Constructor para inicializar una instancia de la clase con los datos básicos de la película.
     *
     * @param imdbId    ID único de IMDb de la película.
     * @param posterUrl URL de la imagen del póster de la película.
     */
    public Movie(String imdbId, String posterUrl) {
        this.imdbId = imdbId;
        this.posterUrl = posterUrl;
    }

    /**
     * Obtiene el título de la película.
     *
     * @return El título de la película.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Establece el título de la película.
     *
     * @param title Título de la película.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Obtiene el ID único de IMDb de la película.
     *
     * @return El ID de IMDb de la película.
     */
    public String getImdbId() {
        return imdbId;
    }

    /**
     * Establece el ID único de IMDb de la película.
     *
     * @param imdbId ID único de IMDb de la película.
     */
    public void setImdbId(String imdbId) {
        this.imdbId = imdbId;
    }

    /**
     * Obtiene la URL del póster de la película.
     *
     * @return La URL de la imagen del póster.
     */
    public String getPosterUrl() {
        return posterUrl;
    }

}