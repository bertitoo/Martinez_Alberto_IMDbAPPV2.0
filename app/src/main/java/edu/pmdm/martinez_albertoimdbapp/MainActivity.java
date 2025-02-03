package edu.pmdm.martinez_albertoimdbapp;

import static edu.pmdm.martinez_albertoimdbapp.utils.AppLifecycleManager.KEY_ACTIVE_UID;
import static edu.pmdm.martinez_albertoimdbapp.utils.AppLifecycleManager.KEY_LOGOUT_CANDIDATE;
import static edu.pmdm.martinez_albertoimdbapp.utils.AppLifecycleManager.PREF_NAME;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
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

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private GoogleSignInClient mGoogleSignInClient;
    private String currentUserId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Se asume que el AppLifecycleManager ya se registró en AuxiliarCicloDeVida

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        View headerView = navigationView.getHeaderView(0);

        TextView userName = headerView.findViewById(R.id.textView);
        TextView userEmail = headerView.findViewById(R.id.user_email);
        ImageView userProfilePic = headerView.findViewById(R.id.imageView);
        Button logoutButton = headerView.findViewById(R.id.logout_button);

        // Obtener el usuario autenticado y registrar/actualizar en la BD local.
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String logoutCandidate = prefs.getString(KEY_LOGOUT_CANDIDATE, null);
            insertUserInDb(user, logoutCandidate);
            setUserInfo(user, userName, userEmail, userProfilePic);
            // NOTA: El registro de login en Firestore se realiza desde AppLifecycleManager (en onActivityResumed)
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

    /**
     * Inserta o actualiza el usuario en la BD local.
     */
    private void insertUserInDb(FirebaseUser user, String logoutCandidate) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USER_UID, user.getUid());
        values.put(DatabaseHelper.COLUMN_USER_NAME,
                user.getDisplayName() != null ? user.getDisplayName() : "Usuario");
        values.put(DatabaseHelper.COLUMN_USER_EMAIL,
                user.getEmail() != null ? user.getEmail() : "No disponible");

        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        values.put(DatabaseHelper.COLUMN_USER_ULTIMO_LOGIN, currentTime);
        values.put(DatabaseHelper.COLUMN_USER_ULTIMO_LOGOUT,
                logoutCandidate != null ? logoutCandidate : "");

        db.insertWithOnConflict(DatabaseHelper.TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
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
                Picasso.get().load(user.getPhotoUrl()).into(userProfilePic);
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
            new UsersSync(this).syncUser(currentUserId, displayName, email);

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
    public void onDestroy() {
        super.onDestroy();
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
}