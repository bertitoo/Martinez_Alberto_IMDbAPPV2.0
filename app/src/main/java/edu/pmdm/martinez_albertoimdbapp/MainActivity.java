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
import android.util.Log;
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

import edu.pmdm.martinez_albertoimdbapp.database.FavoritesDatabaseHelper;
import edu.pmdm.martinez_albertoimdbapp.databinding.ActivityMainBinding;
import edu.pmdm.martinez_albertoimdbapp.sync.SyncFavorites;
import edu.pmdm.martinez_albertoimdbapp.utils.AppLifecycleManager;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private GoogleSignInClient mGoogleSignInClient;
    private String currentUserId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Registrar el AppLifecycleManager
        // Registrar el AppLifecycleManager para gestionar login/logout globalmente.
        AppLifecycleManager appLifecycleManager = new AppLifecycleManager(this);
        getApplication().registerActivityLifecycleCallbacks(appLifecycleManager);
        getApplication().registerComponentCallbacks(appLifecycleManager);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        View headerView = navigationView.getHeaderView(0);

        TextView userName = headerView.findViewById(R.id.textView);
        TextView userEmail = headerView.findViewById(R.id.user_email);
        ImageView userProfilePic = headerView.findViewById(R.id.imageView);
        Button logoutButton = headerView.findViewById(R.id.logout_button);

        // Obtener el usuario autenticado
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
            // Antes de registrar el usuario, usar logout pendiente si existe
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String logoutCandidate = prefs.getString(KEY_LOGOUT_CANDIDATE, null);
            insertUserInDb(user, logoutCandidate);
            setUserInfo(user, userName, userEmail, userProfilePic);
        }

        mGoogleSignInClient = GoogleSignIn.getClient(this,
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .build()
        );

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_favorites, R.id.nav_buscar
        ).setOpenableLayout(drawer).build();

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
     * Inserta o actualiza el usuario en la BD.
     * Si hay un logout pendiente (logoutCandidate no es vacío), se guarda ese valor en la columna "último logout".
     */
    private void insertUserInDb(FirebaseUser user, String logoutCandidate) {
        FavoritesDatabaseHelper dbHelper = new FavoritesDatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FavoritesDatabaseHelper.COLUMN_USER_UID, user.getUid());
        values.put(FavoritesDatabaseHelper.COLUMN_USER_NAME,
                user.getDisplayName() != null ? user.getDisplayName() : "Usuario");
        values.put(FavoritesDatabaseHelper.COLUMN_USER_EMAIL,
                user.getEmail() != null ? user.getEmail() : "No disponible");

        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        values.put(FavoritesDatabaseHelper.COLUMN_USER_ULTIMO_LOGIN, currentTime);
        // Solo se guarda el logoutCandidate si no es null; de lo contrario, se inserta vacío.
        values.put(FavoritesDatabaseHelper.COLUMN_USER_ULTIMO_LOGOUT,
                logoutCandidate != null ? logoutCandidate : "");

        db.insertWithOnConflict(FavoritesDatabaseHelper.TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private void setUserInfo(FirebaseUser user, TextView userName, TextView userEmail, ImageView userProfilePic) {
        boolean hasFacebook = false;
        boolean hasGoogle = false;
        for (UserInfo info : user.getProviderData()) {
            if ("facebook.com".equals(info.getProviderId())) {
                hasFacebook = true;
            } else if ("google.com".equals(info.getProviderId())) {
                hasGoogle = true;
            }
        }
        if (hasFacebook && AccessToken.getCurrentAccessToken() != null && !AccessToken.getCurrentAccessToken().isExpired()) {
            fetchFacebookUserInfo(userProfilePic, userName, userEmail);
        } else if (hasGoogle) {
            userName.setText(user.getDisplayName() != null ? user.getDisplayName() : "Usuario");
            userEmail.setText(user.getEmail() != null ? user.getEmail() : "No disponible");
            if (user.getPhotoUrl() != null) {
                Picasso.get().load(user.getPhotoUrl()).into(userProfilePic);
            }
        } else {
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
        if (currentUserId != null) {
            updateUserLogoutInDb(currentUserId);
            SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().remove(KEY_ACTIVE_UID).remove(KEY_LOGOUT_CANDIDATE).apply();
        }
        FirebaseAuth.getInstance().signOut();
        LoginManager.getInstance().logOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void updateUserLogoutInDb(String uid) {
        FavoritesDatabaseHelper dbHelper = new FavoritesDatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        values.put(FavoritesDatabaseHelper.COLUMN_USER_ULTIMO_LOGOUT, currentTime);
        db.update(FavoritesDatabaseHelper.TABLE_USERS, values,
                FavoritesDatabaseHelper.COLUMN_USER_UID + "=?", new String[]{uid});
    }

    @Override
    protected void onDestroy() {
        // Al destruir la actividad, si se está finalizando (cierre o reinicio), se registra el logout.
        if (isFinishing() && currentUserId != null) {
            updateUserLogoutInDb(currentUserId);
            Log.d("MainActivity", "onDestroy: Logout registrado para uid " + currentUserId);
        }
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