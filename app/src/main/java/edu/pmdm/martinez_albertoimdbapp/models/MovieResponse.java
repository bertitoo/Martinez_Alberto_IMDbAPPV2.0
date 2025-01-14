package edu.pmdm.martinez_albertoimdbapp.models;

/**
 * Clase que representa la respuesta del API de IMDb para detalles de una película.
 * Esta clase mapea los datos relevantes de la respuesta JSON a estructuras de Java.
 *
 * @author Alberto Martínez Vadillo
 */
public class MovieResponse {

    private Data data;

    /**
     * Obtiene los datos de la película.
     *
     * @return Un objeto {@link Data} que contiene los detalles de la película.
     */
    public Data getData() {
        return data;
    }

    /**
     * Clase interna que representa el nivel de datos principal de la respuesta.
     */
    public static class Data {
        private Title title;

        /**
         * Obtiene los detalles del título de la película.
         *
         * @return Un objeto {@link Title} que contiene información del título.
         */
        public Title getTitle() {
            return title;
        }
    }

    /**
     * Clase que representa los detalles del título de la película.
     */
    public static class Title {
        private TitleText titleText;
        private ReleaseYear releaseYear;
        private PrimaryImage primaryImage;
        private RatingsSummary ratingsSummary;
        private Plot plot;

        /**
         * Obtiene el texto del título de la película.
         *
         * @return Un objeto {@link TitleText} que contiene el texto del título.
         */
        public TitleText getTitleText() {
            return titleText;
        }

        /**
         * Obtiene el año de lanzamiento de la película.
         *
         * @return Un objeto {@link ReleaseYear} con el año de lanzamiento.
         */
        public ReleaseYear getReleaseYear() {
            return releaseYear;
        }

        /**
         * Obtiene la imagen principal de la película.
         *
         * @return Un objeto {@link PrimaryImage} que contiene la URL de la imagen.
         */
        public PrimaryImage getPrimaryImage() {
            return primaryImage;
        }

        /**
         * Obtiene el resumen de calificaciones de la película.
         *
         * @return Un objeto {@link RatingsSummary} con la calificación agregada.
         */
        public RatingsSummary getRatingsSummary() {
            return ratingsSummary;
        }

        /**
         * Obtiene el argumento de la película.
         *
         * @return Un objeto {@link Plot} que contiene el argumento.
         */
        public Plot getPlot() {
            return plot;
        }
    }

    /**
     * Clase que representa el texto del título.
     */
    public static class TitleText {
        private String text;

        /**
         * Obtiene el texto del título.
         *
         * @return El texto del título.
         */
        public String getText() {
            return text;
        }
    }

    /**
     * Clase que representa el año de lanzamiento de la película.
     */
    public static class ReleaseYear {
        private int year;

        /**
         * Obtiene el año de lanzamiento.
         *
         * @return El año de lanzamiento.
         */
        public int getYear() {
            return year;
        }
    }

    /**
     * Clase que representa la imagen principal de la película.
     */
    public static class PrimaryImage {
        private String url;

        /**
         * Obtiene la URL de la imagen principal.
         *
         * @return La URL de la imagen.
         */
        public String getUrl() {
            return url;
        }
    }

    /**
     * Clase que representa el resumen de calificaciones de la película.
     */
    public static class RatingsSummary {
        private double aggregateRating;

        /**
         * Obtiene la calificación agregada.
         *
         * @return La calificación agregada.
         */
        public double getAggregateRating() {
            return aggregateRating;
        }
    }

    /**
     * Clase para mapear el argumento de la película.
     */
    public static class Plot {
        private PlotText plotText;

        /**
         * Obtiene el texto del argumento.
         *
         * @return Un objeto {@link PlotText} que contiene el argumento en texto plano.
         */
        public PlotText getPlotText() {
            return plotText;
        }
    }

    /**
     * Clase para mapear el texto del argumento.
     */
    public static class PlotText {
        private String plainText;

        /**
         * Obtiene el argumento en texto plano.
         *
         * @return El texto plano del argumento.
         */
        public String getPlainText() {
            return plainText;
        }
    }
}