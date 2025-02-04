package edu.pmdm.martinez_albertoimdbapp.api;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase que gestiona un conjunto de claves API para ser utilizadas de forma cíclica.
 * Se pueden obtener las claves disponibles y cambiar entre ellas de manera secuencial.
 * El sistema cambiará automáticamente a la siguiente clave cuando se llame al método
 * `switchToNextKey`, y devolverá la clave actual cuando se llame a `getCurrentKey`.
 *
 * @author Alberto Martínez Vadillo
 */
public class RapidApiKeyManager {

    // Etiqueta para los registros de depuración
    private static final String TAG = "RapidApiKeyManager";

    // Lista de claves API disponibles
    List<String> apiKeys = new ArrayList<>();

    // Índice de la clave API actual
    private int currentKeyIndex = 0;

    /**
     * Constructor de la clase RapidApiKeyManager. Inicializa el conjunto de claves API.
     * Se añaden tres claves API predefinidas a la lista.
     */
    public RapidApiKeyManager() {
        apiKeys.add("f7f23d7619msh83c94fd82b17f34p14e350jsn2d3cd99ac75c");
        apiKeys.add("784a9d8d51msh5221f53a175ce27p100fc2jsnf2e975ed90db");
        apiKeys.add("6736c5df30msh01f0f50742ae493p1ddc7fjsnac91e1d30459");
    }

    /**
     * Obtiene la clave API actual desde la lista de claves.
     * Muestra un mensaje de depuración con la clave API actual y el índice en uso.
     *
     * @return La clave API que está siendo utilizada actualmente.
     */
    public String getCurrentKey() {
        String currentKey = apiKeys.get(currentKeyIndex);
        Log.d(TAG, "Using API key [" + (currentKeyIndex + 1) + "/" + apiKeys.size() + "]: " + currentKey);
        return currentKey;
    }

    /**
     * Cambia a la siguiente clave API en la lista, de forma cíclica.
     * Cuando se llega al final de la lista, vuelve a la primera clave.
     * Muestra un mensaje de depuración con la nueva clave API en uso.
     */
    public void switchToNextKey() {
        currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size();
        String nextKey = apiKeys.get(currentKeyIndex);
        Log.d(TAG, "Switched to API key [" + (currentKeyIndex + 1) + "/" + apiKeys.size() + "]: " + nextKey);
    }

    /**
     * Obtiene el total de claves API disponibles en la lista.
     *
     * @return El número total de claves API disponibles.
     */
    public int getTotalKeys() {
        return apiKeys.size();
    }
}
