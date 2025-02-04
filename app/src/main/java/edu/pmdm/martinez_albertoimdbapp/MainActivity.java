package edu.pmdm.martinez_albertoimdbapp;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.facebook.AccessToken;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.squareup.picasso.Picasso;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.net.ssl.HttpsURLConnection;
import edu.pmdm.martinez_albertoimdbapp.database.DatabaseHelper;
import edu.pmdm.martinez_albertoimdbapp.databinding.ActivityMainBinding;
import edu.pmdm.martinez_albertoimdbapp.sync.SyncFavorites;
import edu.pmdm.martinez_albertoimdbapp.sync.UsersSync;
import edu.pmdm.martinez_albertoimdbapp.utils.KeystoreManager;

public class MainActivity extends AppCompatActivity implements Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {

    private static final String TAG = "MainActivity";
    public static final String PREF_NAME = "AppPreferences";
    public static final String KEY_LOGOUT_CANDIDATE = "logout_candidate";
    public static final String KEY_ACTIVE_UID = "active_uid";

    private AppBarConfiguration mAppBarConfiguration;
    private GoogleSignInClient mGoogleSignInClient;
    private String currentUserId = null;

    // Variables de instancia para el header
    private TextView headerUserName;
    private TextView headerUserEmail;
    private ImageView headerUserProfilePic;

    // Variables para gestionar el ciclo de vida
    private int activityReferences = 0; // Contador de actividades activas
    private boolean isActivityChangingConfigurations = false; // Indica si hay cambios de configuración
    private String lastLoggedInUid = null; // Para evitar múltiples registros de login

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Verificar si hay un logout pendiente al iniciar la app
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String candidate = prefs.getString(KEY_LOGOUT_CANDIDATE, null);
        String activeUid = prefs.getString(KEY_ACTIVE_UID, null);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (candidate != null && activeUid != null) {
            if (currentUser == null || currentUser.getUid().equals(activeUid)) {
                updateLogoutLocal(candidate, activeUid);
                Log.d(TAG, "Logout pendiente registrado al iniciar para uid " + activeUid + ": " + candidate);
                new UsersSync(this).syncLocalToRemote(activeUid, "logout");
            } else {
                Log.d(TAG, "Logout pendiente descartado por cambio de usuario.");
            }

            // Limpiar las marcas para la nueva sesión
            prefs.edit().remove(KEY_LOGOUT_CANDIDATE).remove(KEY_ACTIVE_UID).apply();
        }

        // Registrar los callbacks de ciclo de vida
        getApplication().registerActivityLifecycleCallbacks(this);
        getApplication().registerComponentCallbacks(this);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        View headerView = navigationView.getHeaderView(0);

        // Asignamos las vistas del header a las variables de instancia
        headerUserName = headerView.findViewById(R.id.textView);
        headerUserEmail = headerView.findViewById(R.id.user_email);
        headerUserProfilePic = headerView.findViewById(R.id.imageView);
        Button logoutButton = headerView.findViewById(R.id.logout_button);

        // Obtener el usuario autenticado y registrar/actualizar en la BD local.
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();

            if (candidate != null && activeUid != null) {
                if (user.getUid().equals(activeUid)) {
                    updateLogoutLocal(candidate, activeUid);
                    Log.d(TAG, "Logout pendiente registrado al iniciar para uid " + activeUid + ": " + candidate);
                    new UsersSync(this).syncLocalToRemote(activeUid, "logout");
                } else {
                    Log.d(TAG, "Logout pendiente descartado por cambio de usuario.");
                }
                prefs.edit().remove(KEY_LOGOUT_CANDIDATE).remove(KEY_ACTIVE_UID).apply();
            }

            insertUserInDb(user, null);
            setUserInfo(user, headerUserName, headerUserEmail, headerUserProfilePic);
        }

        mGoogleSignInClient = GoogleSignIn.getClient(this,
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .build()
        );

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_favorites, R.id.nav_buscar
        ).setOpenableLayout(binding.drawerLayout).build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        if (currentUserId != null) {
            SyncFavorites syncFavorites = new SyncFavorites(this, currentUserId);
            syncFavorites.syncFavorites();
            syncFavorites.startRealtimeSync();
        }

        logoutButton.setOnClickListener(v -> logout());
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    private void updateLoginLocal(String uid) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
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
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        try {
            ContentValues values = new ContentValues();
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

    private void saveLogoutCandidate(String logoutTime, String uid) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_LOGOUT_CANDIDATE, logoutTime)
                .putString(KEY_ACTIVE_UID, uid)
                .apply();
        Log.d(TAG, "Guardado logout candidato para uid " + uid + ": " + logoutTime);
    }

    private void saveActiveUid(String uid) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
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
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
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
            // Registrar login en Firestore solo si el UID ha cambiado
            DatabaseHelper dbHelper = new DatabaseHelper(getApplicationContext());
            String encryptedAddress = dbHelper.getUserEncryptedAddress(user.getUid());
            String encryptedPhone = dbHelper.getUserEncryptedPhone(user.getUid());

            // Actualizar el último UID registrado
            lastLoggedInUid = user.getUid();

            // Registrar el login localmente
            updateLoginLocal(lastLoggedInUid);

            // Sincronizar el login con Firestore
            new UsersSync(getApplicationContext()).syncLocalToRemote(
                    lastLoggedInUid,
                    "login",
                    user.getDisplayName() != null ? user.getDisplayName() : "Usuario",
                    user.getEmail() != null ? user.getEmail() : "No disponible",
                    encryptedAddress,
                    encryptedPhone
            );
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
                String currentTime = getCurrentTime();
                updateLogoutLocal(currentTime, user.getUid());
                new UsersSync(this).syncLocalToRemote(user.getUid(), "logout");
                saveLogoutCandidateIfAppInBackground();
            }
            Log.d(TAG, "onActivityStopped: app en background.");
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations();

        // Solo actuar si la actividad destruida es la principal y se está cerrando
        if (activity instanceof MainActivity && activity.isFinishing()) {
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String activeUid = prefs.getString(KEY_ACTIVE_UID, null);

            if (activeUid != null) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                // Registrar el logout solo si el usuario activo coincide con el UID guardado
                if (user != null && activeUid.equals(user.getUid())) {
                    String currentTime = getCurrentTime();
                    updateLogoutLocal(currentTime, activeUid);
                    new UsersSync(this).syncLocalToRemote(activeUid, "logout");
                    Log.d(TAG, "onActivityDestroyed: Logout registrado para uid " + activeUid + " a las " + currentTime);

                    // Guardar el logout como candidato en caso de reinicio
                    saveLogoutCandidate(currentTime, activeUid);
                } else {
                    Log.d(TAG, "onActivityDestroyed: No se registra logout porque el usuario activo ha cambiado o se ha eliminado.");
                }
            }
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        // Detectar cuando la UI se oculta (aplicación en segundo plano)
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                String currentTime = getCurrentTime();
                saveLogoutCandidate(currentTime, user.getUid());
                Log.d(TAG, "onTrimMemory: UI oculta, logout candidato guardado.");
            }
        }
    }

    private void saveLogoutCandidateIfAppInBackground() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String currentTime = getCurrentTime();
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putString(KEY_LOGOUT_CANDIDATE, currentTime)
                    .putString(KEY_ACTIVE_UID, user.getUid())
                    .apply();
            Log.d(TAG, "Guardado logout candidato: " + currentTime);
        }
    }

    private void registerLogoutIfAppIsClosing() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String currentTime = getCurrentTime();
            updateLogoutLocal(currentTime, user.getUid());
            new UsersSync(this).syncLocalToRemote(user.getUid(), "logout");
            Log.d(TAG, "Logout registrado en onDestroy: " + currentTime);
        }
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onLowMemory() {}

    /**
     * Inserta o actualiza el usuario en la BD local con los datos cifrados.
     */
    private void insertUserInDb(FirebaseUser user, String logoutCandidate) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        String userUid = user.getUid();
        String displayName = user.getDisplayName() != null ? user.getDisplayName() : "Usuario";
        String email = user.getEmail() != null ? user.getEmail() : "No disponible";

        // Registrar la fecha y hora del último login
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        values.put(DatabaseHelper.COLUMN_USER_ULTIMO_LOGIN, currentTime);
        values.put(DatabaseHelper.COLUMN_USER_ULTIMO_LOGOUT, logoutCandidate != null ? logoutCandidate : "");

        // Verificar si el usuario ya existe en la base de datos
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                new String[]{DatabaseHelper.COLUMN_USER_NAME, DatabaseHelper.COLUMN_USER_ADDRESS, DatabaseHelper.COLUMN_USER_PHONE},
                DatabaseHelper.COLUMN_USER_UID + " = ?",
                new String[]{userUid},
                null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
            // El usuario ya existe, NO actualizar el nombre, solo recuperar los datos cifrados
            String existingName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_NAME));
            String encryptedAddress = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ADDRESS));
            String encryptedPhone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_PHONE));

            // Mantener el nombre original y actualizar solo otros datos
            values.put(DatabaseHelper.COLUMN_USER_NAME, existingName);
            values.put(DatabaseHelper.COLUMN_USER_ADDRESS, encryptedAddress);
            values.put(DatabaseHelper.COLUMN_USER_PHONE, encryptedPhone);

            db.update(DatabaseHelper.TABLE_USERS, values, DatabaseHelper.COLUMN_USER_UID + " = ?", new String[]{userUid});
            Log.d("DATABASE", "Usuario ya existente, NO se actualiza el nombre");
        } else {
            // El usuario no existe, insertar un nuevo registro con su nombre original
            values.put(DatabaseHelper.COLUMN_USER_UID, userUid);
            values.put(DatabaseHelper.COLUMN_USER_NAME, displayName);
            values.put(DatabaseHelper.COLUMN_USER_EMAIL, email);
            values.put(DatabaseHelper.COLUMN_USER_ADDRESS, ""); // Dirección vacía
            values.put(DatabaseHelper.COLUMN_USER_PHONE, "");   // Teléfono vacío

            db.insert(DatabaseHelper.TABLE_USERS, null, values);
            Log.d("DATABASE", "Nuevo usuario insertado con nombre: " + displayName);
        }

        if (cursor != null) {
            cursor.close();
        }

        // Sincronizar los datos con Firestore
        String encryptedAddress = dbHelper.getUserEncryptedAddress(userUid);
        String encryptedPhone = dbHelper.getUserEncryptedPhone(userUid);
        new UsersSync(this).syncUser(userUid, displayName, email, encryptedAddress, encryptedPhone);
    }


    /**
     * Configura la información del usuario en el header.
     * Si el usuario se autenticó mediante correo (proveedor "password"),
     * se mostrará únicamente el e-mail y se asigna una imagen genérica.
     * De lo contrario, se utiliza la lógica existente para Facebook y Google.
     */
    private void setUserInfo(FirebaseUser user, TextView userName, TextView userEmail, ImageView userProfilePic) {
        boolean isEmailLogin = false;
        boolean hasFacebook = false;
        boolean hasGoogle = false;
        for (UserInfo info : user.getProviderData()) {
            String provider = info.getProviderId();
            if ("password".equals(provider)) {
                isEmailLogin = true;
            } else if ("facebook.com".equals(provider)) {
                hasFacebook = true;
            } else if ("google.com".equals(provider)) {
                hasGoogle = true;
            }
        }
        if (isEmailLogin) {
        // Usuario autenticado mediante correo: mostrar solo e-mail y una imagen genérica.
            userName.setText("");
            userEmail.setText(user.getEmail() != null ? user.getEmail() : "No disponible");
            userProfilePic.setImageResource(R.drawable.ic_android_black_24dp);
        } else if (hasFacebook && AccessToken.getCurrentAccessToken() != null && !AccessToken.getCurrentAccessToken().isExpired()) {
            fetchFacebookUserInfo(userProfilePic, userName, userEmail);
        } else {
        // En caso de Google u otro proveedor se usa la información por defecto.
            userName.setText(user.getDisplayName() != null ? user.getDisplayName() : "Usuario");
            userEmail.setText(user.getEmail() != null ? user.getEmail() : "No disponible");
            if (user.getPhotoUrl() != null) {
                String photoUrl = user.getPhotoUrl().toString();
        // Guardamos la URL en el tag del ImageView para usarla luego en EditUserActivity
                userProfilePic.setTag(photoUrl);
                Picasso.get().load(photoUrl).into(userProfilePic);
            }
        }
    }

    private void fetchFacebookUserInfo(ImageView userProfilePic, TextView userName, TextView userEmail) {
        AccessToken token = AccessToken.getCurrentAccessToken();
        if (token == null || token.isExpired()) {
            userName.setText("Usuario (FB Token Expirado)");
            userEmail.setText("Conectado con Facebook");
            return;
        }
        new Thread(() -> {
            try {
                String graphUrl = "https://graph.facebook.com/me?fields=name,email,picture.type(large)&access_token=" + token.getToken();
                URL url = new URL(graphUrl);
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                conn.connect();
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();
                JSONObject response = new JSONObject(sb.toString());
                final String fbName = response.optString("name", "Usuario");
                JSONObject pictureObj = response.optJSONObject("picture");
                JSONObject dataObj = (pictureObj != null) ? pictureObj.optJSONObject("data") : null;
                final String fbPhotoUrl = (dataObj != null) ? dataObj.optString("url") : null;
                runOnUiThread(() -> {
                    userName.setText(fbName);
                    userEmail.setText("Conectado con Facebook");
                    if (fbPhotoUrl != null) {
                        // Guardamos la URL en el tag del ImageView para usarla luego en EditUserActivity
                        userProfilePic.setTag(fbPhotoUrl);
                        Picasso.get().load(fbPhotoUrl).into(userProfilePic);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    private void logout() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUserId != null && user != null) {
            // Sincronizamos los datos fijos reales para asegurarnos de que el documento se crea/actualiza
            String displayName = user.getDisplayName() != null ? user.getDisplayName() : "Usuario";
            String email = user.getEmail() != null ? user.getEmail() : "No disponible";
            new UsersSync(this).syncUser(currentUserId, displayName, email, "", "");

            // Actualizamos la BD local y registramos el logout remoto
            updateUserLogoutInDb(currentUserId);
            new UsersSync(this).syncLocalToRemote(currentUserId, "logout");

            // Se elimina el uid activo para que el AppLifecycleManager no vuelva a registrar el logout
            SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().remove(KEY_ACTIVE_UID).remove(KEY_LOGOUT_CANDIDATE).apply();
        }

        // Cerrar sesión en Firebase, Facebook y Google
        FirebaseAuth.getInstance().signOut();
        LoginManager.getInstance().logOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void updateUserLogoutInDb(String uid) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        values.put(DatabaseHelper.COLUMN_USER_ULTIMO_LOGOUT, currentTime);
        db.update(DatabaseHelper.TABLE_USERS, values,
                DatabaseHelper.COLUMN_USER_UID + "=?", new String[]{uid});
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Intentar registrar el logout en onDestroy
        registerLogoutIfAppIsClosing();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_edit_user) {
            // Se crea el intent para iniciar EditUserActivity y se envían los datos del usuario
            Intent intent = new Intent(this, EditUserActivity.class);
            String name = headerUserName.getText().toString();
            String email = headerUserEmail.getText().toString();
            String photoUrl = "";
            if (headerUserProfilePic.getTag() != null) {
                photoUrl = headerUserProfilePic.getTag().toString();
            }
            intent.putExtra("user_name", name);
            intent.putExtra("user_email", email);
            intent.putExtra("user_photo_url", photoUrl);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
