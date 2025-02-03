package edu.pmdm.martinez_albertoimdbapp.utils;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;

import edu.pmdm.martinez_albertoimdbapp.MainActivity;
import edu.pmdm.martinez_albertoimdbapp.database.FavoritesDatabaseHelper;

public class AppLifecycleManager implements Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {

    private static final String TAG = "AppLifecycleManager";
    public static final String PREF_NAME = "AppPreferences";
    public static final String KEY_LOGOUT_CANDIDATE = "logout_candidate";
    public static final String KEY_ACTIVE_UID = "active_uid";

    private final Context appContext;
    private int activityReferences = 0;
    private boolean isActivityChangingConfigurations = false;

    public AppLifecycleManager(Context context) {
        this.appContext = context.getApplicationContext();
        // Al arrancar la app, comprobamos si había un logout pendiente para el uid activo
        SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String candidate = prefs.getString(KEY_LOGOUT_CANDIDATE, null);
        String activeUid = prefs.getString(KEY_ACTIVE_UID, null);
        if (candidate != null && activeUid != null) {
            updateLogout(candidate, activeUid);
            // Limpiar las marcas para la nueva sesión
            prefs.edit().remove(KEY_LOGOUT_CANDIDATE).remove(KEY_ACTIVE_UID).apply();
            Log.d(TAG, "Logout pendiente registrado al iniciar para uid " + activeUid + ": " + candidate);
        }
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    /**
     * Actualiza el "último login" en la BD para el uid indicado.
     */
    private void updateLogin(String uid) {
        FavoritesDatabaseHelper dbHelper = new FavoritesDatabaseHelper(appContext);
        try {
            android.content.ContentValues values = new android.content.ContentValues();
            String currentTime = getCurrentTime();
            values.put(FavoritesDatabaseHelper.COLUMN_USER_ULTIMO_LOGIN, currentTime);
            int rows = dbHelper.getWritableDatabase().update(
                    FavoritesDatabaseHelper.TABLE_USERS,
                    values,
                    FavoritesDatabaseHelper.COLUMN_USER_UID + "=?",
                    new String[]{uid});
            Log.d(TAG, "Último login actualizado para uid " + uid + ": " + currentTime + " (" + rows + " filas afectadas)");
        } catch (Exception e) {
            Log.e(TAG, "Error al actualizar último login", e);
        }
    }

    /**
     * Actualiza el "último logout" en la BD para el uid indicado usando la hora proporcionada.
     */
    private void updateLogout(String logoutTime, String uid) {
        FavoritesDatabaseHelper dbHelper = new FavoritesDatabaseHelper(appContext);
        try {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(FavoritesDatabaseHelper.COLUMN_USER_ULTIMO_LOGOUT, logoutTime);
            int rows = dbHelper.getWritableDatabase().update(
                    FavoritesDatabaseHelper.TABLE_USERS,
                    values,
                    FavoritesDatabaseHelper.COLUMN_USER_UID + "=?",
                    new String[]{uid});
            Log.d(TAG, "Último logout actualizado para uid " + uid + ": " + logoutTime + " (" + rows + " filas afectadas)");
        } catch (Exception e) {
            Log.e(TAG, "Error al actualizar último logout", e);
        }
    }

    /**
     * Guarda en SharedPreferences la hora actual como candidato a logout.
     */
    private void saveLogoutCandidate() {
        String currentTime = getCurrentTime();
        SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LOGOUT_CANDIDATE, currentTime).apply();
        Log.d(TAG, "Logout candidato guardado: " + currentTime);
    }

    /**
     * Guarda en SharedPreferences el uid actual como cuenta activa.
     */
    private void saveActiveUid(String uid) {
        SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ACTIVE_UID, uid).apply();
        Log.d(TAG, "UID activo guardado: " + uid);
    }

    // --- ActivityLifecycleCallbacks ---

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        // No se hace nada
    }

    @Override
    public void onActivityStarted(Activity activity) {
        activityReferences++;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String currentUid = user.getUid();
            SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String activeUid = prefs.getString(KEY_ACTIVE_UID, null);
            if (activeUid == null) {
                // Primera vez en la sesión; guardar el uid activo.
                saveActiveUid(currentUid);
            } else if (!activeUid.equals(currentUid)) {
                // Cambio de cuenta: si hay candidato, actualizar logout para el uid anterior.
                String candidate = prefs.getString(KEY_LOGOUT_CANDIDATE, null);
                if (candidate != null) {
                    updateLogout(candidate, activeUid);
                }
                // Guardar el nuevo uid activo.
                saveActiveUid(currentUid);
            }
            // Actualizar el login para el usuario actual.
            updateLogin(currentUid);
            // Limpiar el candidato a logout al volver a primer plano.
            prefs.edit().remove(KEY_LOGOUT_CANDIDATE).apply();
        }
        Log.d(TAG, "App en primer plano (onActivityStarted).");
    }

    @Override
    public void onActivityResumed(Activity activity) {
        // No se hace nada adicional
    }

    @Override
    public void onActivityPaused(Activity activity) {
        // No se hace nada
    }

    @Override
    public void onActivityStopped(Activity activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations();
        activityReferences--;
        if (activityReferences == 0 && !isActivityChangingConfigurations) {
            // La app pasó a background: actualizar el logout para el uid activo.
            SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String activeUid = prefs.getString(KEY_ACTIVE_UID, null);
            if (activeUid != null) {
                String currentTime = getCurrentTime();
                updateLogout(currentTime, activeUid);
            }
            // Guardar candidato a logout.
            saveLogoutCandidate();
            Log.d(TAG, "App en background (onActivityStopped).");
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        // No se hace nada
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations();
        // Si la actividad principal se destruye (por cierre o reinicio) y se está finalizando, registrar logout.
        if (activity instanceof MainActivity && activity.isFinishing()) {
            SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String activeUid = prefs.getString(KEY_ACTIVE_UID, null);
            if (activeUid != null) {
                String currentTime = getCurrentTime();
                updateLogout(currentTime, activeUid);
                Log.d(TAG, "onActivityDestroyed: Logout registrado para uid " + activeUid + " a las " + currentTime);
            }
        }
    }

    // --- ComponentCallbacks2 ---

    @Override
    public void onTrimMemory(int level) {
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            // Cuando la UI deja de ser visible, guardar candidato a logout.
            saveLogoutCandidate();
            Log.d(TAG, "UI oculta (onTrimMemory): logout candidato guardado.");
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // No se hace nada
    }

    @Override
    public void onLowMemory() {
        // No se hace nada
    }
}