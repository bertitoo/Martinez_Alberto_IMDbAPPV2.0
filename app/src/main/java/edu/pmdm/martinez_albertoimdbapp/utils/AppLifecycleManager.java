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

import edu.pmdm.martinez_albertoimdbapp.database.DatabaseHelper;
import edu.pmdm.martinez_albertoimdbapp.sync.UsersSync;

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
        // Si existe un logout pendiente (por ejemplo, por un cierre previo de la app) se actualiza la BD local
        SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String candidate = prefs.getString(KEY_LOGOUT_CANDIDATE, null);
        String activeUid = prefs.getString(KEY_ACTIVE_UID, null);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (candidate != null && activeUid != null) {
            // Solo se registra el logout pendiente si el usuario activo coincide con el uid guardado
            if (currentUser == null || currentUser.getUid().equals(activeUid)) {
                updateLogoutLocal(candidate, activeUid);
                Log.d(TAG, "Logout pendiente registrado al iniciar para uid " + activeUid + ": " + candidate);
            } else {
                Log.d(TAG, "Logout pendiente descartado por cambio de usuario.");
            }
            // Limpiar las marcas para la nueva sesión
            prefs.edit().remove(KEY_LOGOUT_CANDIDATE).remove(KEY_ACTIVE_UID).apply();
        }
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    // Actualiza el último login en la BD local (para fines de registro interno)
    private void updateLoginLocal(String uid) {
        DatabaseHelper dbHelper = new DatabaseHelper(appContext);
        try {
            android.content.ContentValues values = new android.content.ContentValues();
            String currentTime = getCurrentTime();
            values.put(DatabaseHelper.COLUMN_USER_ULTIMO_LOGIN, currentTime);
            int rows = dbHelper.getWritableDatabase().update(
                    DatabaseHelper.TABLE_USERS,
                    values,
                    DatabaseHelper.COLUMN_USER_UID + "=?",
                    new String[]{uid});
            Log.d(TAG, "Último login actualizado para uid " + uid + ": " + currentTime + " (" + rows + " filas afectadas)");
        } catch (Exception e) {
            Log.e(TAG, "Error al actualizar último login", e);
        }
    }

    // Actualiza el último logout en la BD local
    private void updateLogoutLocal(String logoutTime, String uid) {
        DatabaseHelper dbHelper = new DatabaseHelper(appContext);
        try {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(DatabaseHelper.COLUMN_USER_ULTIMO_LOGOUT, logoutTime);
            int rows = dbHelper.getWritableDatabase().update(
                    DatabaseHelper.TABLE_USERS,
                    values,
                    DatabaseHelper.COLUMN_USER_UID + "=?",
                    new String[]{uid});
            Log.d(TAG, "Último logout actualizado para uid " + uid + ": " + logoutTime + " (" + rows + " filas afectadas)");
        } catch (Exception e) {
            Log.e(TAG, "Error al actualizar último logout", e);
        }
    }

    // Guarda en preferencias un logout candidato para que, en caso de reinicio, se actualice la BD local
    private void saveLogoutCandidate() {
        String currentTime = getCurrentTime();
        SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LOGOUT_CANDIDATE, currentTime).apply();
        Log.d(TAG, "Logout candidato guardado: " + currentTime);
    }

    private void saveActiveUid(String uid) {
        SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ACTIVE_UID, uid).apply();
        Log.d(TAG, "UID activo guardado: " + uid);
    }

    // --- ActivityLifecycleCallbacks ---

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        // No se requiere acción
    }

    @Override
    public void onActivityStarted(Activity activity) {
        activityReferences++;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String currentUid = user.getUid();
            SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String activeUid = prefs.getString(KEY_ACTIVE_UID, null);
            if (activeUid == null || !activeUid.equals(currentUid)) {
                // Si cambia el usuario activo, se guarda el nuevo uid
                saveActiveUid(currentUid);
            }
            // Se actualiza localmente el login (para fines de control interno)
            updateLoginLocal(currentUid);
            // Al iniciar la actividad se elimina cualquier logout candidato previo
            prefs.edit().remove(KEY_LOGOUT_CANDIDATE).apply();
        }
        Log.d(TAG, "onActivityStarted: app en foreground (contador=" + activityReferences + ").");
    }

    @Override
    public void onActivityResumed(Activity activity) {
        // Cada vez que la app pasa a primer plano se registra un login en Firestore.
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Para el login remoto es importante enviar también los datos fijos del usuario.
            DatabaseHelper dbHelper = new DatabaseHelper(appContext);
            String encryptedAddress = dbHelper.getUserEncryptedAddress(user.getUid());  // Obtener dirección cifrada de la base de datos
            String encryptedPhone = dbHelper.getUserEncryptedPhone(user.getUid());      // Obtener teléfono cifrado de la base de datos

            new UsersSync(appContext).syncLocalToRemote(
                    user.getUid(),
                    "login",
                    user.getDisplayName() != null ? user.getDisplayName() : "Usuario",
                    user.getEmail() != null ? user.getEmail() : "No disponible",
                    encryptedAddress,
                    encryptedPhone
            );
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        // No se requiere acción
    }

    @Override
    public void onActivityStopped(Activity activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations();
        activityReferences--;
        if (activityReferences == 0 && !isActivityChangingConfigurations) {
            // Cuando no hay actividades visibles, la app pasa a background:
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                String uid = user.getUid();
                String currentTime = getCurrentTime();
                updateLogoutLocal(currentTime, uid);
                // Se registra el logout en Firestore
                new UsersSync(appContext).syncLocalToRemote(uid, "logout");
                // Se guarda el logout candidato en SharedPreferences para casos de reinicio
                saveLogoutCandidate();
            }
            Log.d(TAG, "onActivityStopped: app en background.");
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        // No se requiere acción
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        // Si la actividad destruida es la principal y se está cerrando (no por un cambio de configuración)
        isActivityChangingConfigurations = activity.isChangingConfigurations();
        if (activity instanceof edu.pmdm.martinez_albertoimdbapp.MainActivity && activity.isFinishing()) {
            // Solo se registra el logout si aún existe un uid activo en SharedPreferences.
            SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String activeUid = prefs.getString(KEY_ACTIVE_UID, null);
            if (activeUid != null) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                // Se registra el logout únicamente si el usuario activo coincide con el que está en FirebaseAuth
                if (user != null && activeUid.equals(user.getUid())) {
                    String currentTime = getCurrentTime();
                    updateLogoutLocal(currentTime, activeUid);
                    new UsersSync(appContext).syncLocalToRemote(activeUid, "logout");
                    Log.d(TAG, "onActivityDestroyed: Logout registrado para uid " + activeUid + " a las " + currentTime);
                } else {
                    Log.d(TAG, "onActivityDestroyed: No se registra logout porque el usuario activo ha cambiado o se ha eliminado.");
                }
            }
        }
    }

    // --- ComponentCallbacks2 ---

    @Override
    public void onTrimMemory(int level) {
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            // Si la UI se oculta, también se guarda un logout candidato.
            saveLogoutCandidate();
            Log.d(TAG, "onTrimMemory: UI oculta, logout candidato guardado.");
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // No se requiere acción
    }

    @Override
    public void onLowMemory() {
        // No se requiere acción
    }
}