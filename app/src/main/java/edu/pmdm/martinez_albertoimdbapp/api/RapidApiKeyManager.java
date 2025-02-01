package edu.pmdm.martinez_albertoimdbapp.api;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class RapidApiKeyManager {
    private static final String TAG = "RapidApiKeyManager";
    List<String> apiKeys = new ArrayList<>();
    private int currentKeyIndex = 0;

    public RapidApiKeyManager() {
        apiKeys.add("f7f23d7619msh83c94fd82b17f34p14e350jsn2d3cd99ac75c");
        apiKeys.add("784a9d8d51msh5221f53a175ce27p100fc2jsnf2e975ed90db");
        apiKeys.add("6736c5df30msh01f0f50742ae493p1ddc7fjsnac91e1d30459");
    }

    public String getCurrentKey() {
        String currentKey = apiKeys.get(currentKeyIndex);
        Log.d(TAG, "Using API key [" + (currentKeyIndex + 1) + "/" + apiKeys.size() + "]: " + currentKey);
        return currentKey;
    }

    public void switchToNextKey() {
        currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size();
        String nextKey = apiKeys.get(currentKeyIndex);
        Log.d(TAG, "Switched to API key [" + (currentKeyIndex + 1) + "/" + apiKeys.size() + "]: " + nextKey);
    }

    public int getTotalKeys() {
        return apiKeys.size();
    }
}