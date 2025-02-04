package edu.pmdm.martinez_albertoimdbapp.utils;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.SharedPreferences;
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
    private int activityReferences = 0; // Contador de actividades activas
    private boolean isActivityChangingConfigurations = false; // Indica si hay cambios de configuración
    private String lastLoggedInUid = null; // Para evitar múltiples registros de login

    public AppLifecycleManager(Context context) {
        this.appContext = context.getApplicationContext();

        // Verificar si hay un logout pendiente al iniciar la app
        SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String candidate = prefs.getString(KEY_LOGOUT_CANDIDATE, null);
        String activeUid = prefs.getString(KEY_ACTIVE_UID, null);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (candidate != null && activeUid != null) {
            if (currentUser == null || currentUser.getUid().equals(activeUid)) {
                updateLogoutLocal(candidate, activeUid);
                Log.d(TAG, "Logout pendiente registrado al iniciar para uid " + activeUid + ": " + candidate);
                new UsersSync(appContext).syncLocalToRemote(activeUid, "logout");
            } else {
                Log.d(TAG, "Logout pendiente descartado por cambio de usuario.");
            }
            prefs.edit().remove(KEY_LOGOUT_CANDIDATE).remove(KEY_ACTIVE_UID).apply();
        }

        // Inicializar el último UID registrado
        if (currentUser != null) {
            lastLoggedInUid = currentUser.getUid();
        }
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

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
                    new String[]{uid}
            );
            Log.d(TAG, "Último login actualizado para uid " + uid + ": " + currentTime + " (" + rows + " filas afectadas)");
        } catch (Exception e) {
            Log.e(TAG, "Error al actualizar último login", e);
        }
    }

    private void updateLogoutLocal(String logoutTime, String uid) {
        DatabaseHelper dbHelper = new DatabaseHelper(appContext);
        try {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(DatabaseHelper.COLUMN_USER_ULTIMO_LOGOUT, logoutTime);
            int rows = dbHelper.getWritableDatabase().update(
                    DatabaseHelper.TABLE_USERS,
                    values,
                    DatabaseHelper.COLUMN_USER_UID + "=?",
                    new String[]{uid}
            );
            Log.d(TAG, "Último logout actualizado para uid " + uid + ": " + logoutTime + " (" + rows + " filas afectadas)");
        } catch (Exception e) {
            Log.e(TAG, "Error al actualizar último logout", e);
        }
    }

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

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(Activity activity) {
        activityReferences++;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String currentUid = user.getUid();
            SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String activeUid = prefs.getString(KEY_ACTIVE_UID, null);
            if (activeUid == null || !activeUid.equals(currentUid)) {
                saveActiveUid(currentUid);
            }
            if (!isActivityChangingConfigurations) {
                updateLoginLocal(currentUid);
            }
            prefs.edit().remove(KEY_LOGOUT_CANDIDATE).apply();
        }
        Log.d(TAG, "onActivityStarted: app en foreground (contador=" + activityReferences + ").");
    }

    @Override
    public void onActivityResumed(Activity activity) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && !user.getUid().equals(lastLoggedInUid)) {
            DatabaseHelper dbHelper = new DatabaseHelper(appContext);
            String encryptedAddress = dbHelper.getUserEncryptedAddress(user.getUid());
            String encryptedPhone = dbHelper.getUserEncryptedPhone(user.getUid());
            new UsersSync(appContext).syncLocalToRemote(
                    user.getUid(),
                    "login",
                    user.getDisplayName() != null ? user.getDisplayName() : "Usuario",
                    user.getEmail() != null ? user.getEmail() : "No disponible",
                    encryptedAddress,
                    encryptedPhone
            );
            lastLoggedInUid = user.getUid();
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {}

    @Override
    public void onActivityStopped(Activity activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations();
        activityReferences--;
        if (activityReferences == 0 && !isActivityChangingConfigurations) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                String uid = user.getUid();
                String currentTime = getCurrentTime();
                updateLogoutLocal(currentTime, uid);
                new UsersSync(appContext).syncLocalToRemote(uid, "logout");
                saveLogoutCandidate();
            }
            Log.d(TAG, "onActivityStopped: app en background.");
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations();
        if (activity instanceof edu.pmdm.martinez_albertoimdbapp.MainActivity && activity.isFinishing()) {
            SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String activeUid = prefs.getString(KEY_ACTIVE_UID, null);
            if (activeUid != null) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
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

    @Override
    public void onTrimMemory(int level) {
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            saveLogoutCandidate();
            Log.d(TAG, "onTrimMemory: UI oculta, logout candidato guardado.");
        }
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {}

    @Override
    public void onLowMemory() {}
}