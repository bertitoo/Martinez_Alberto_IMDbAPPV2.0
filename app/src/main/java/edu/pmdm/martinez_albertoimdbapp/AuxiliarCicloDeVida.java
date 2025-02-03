package edu.pmdm.martinez_albertoimdbapp;

import android.app.Application;

import edu.pmdm.martinez_albertoimdbapp.utils.AppLifecycleManager;

public class AuxiliarCicloDeVida extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Se crea y se registra el AppLifecycleManager para gestionar globalmente los eventos de login/logout.
        AppLifecycleManager lifecycleManager = new AppLifecycleManager(this);
        registerActivityLifecycleCallbacks(lifecycleManager);
        registerComponentCallbacks(lifecycleManager);
    }
}