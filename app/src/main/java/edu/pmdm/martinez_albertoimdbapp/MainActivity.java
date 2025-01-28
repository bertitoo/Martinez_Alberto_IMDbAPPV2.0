package edu.pmdm.martinez_albertoimdbapp;

import android.content.Intent;
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

import javax.net.ssl.HttpsURLConnection;

import edu.pmdm.martinez_albertoimdbapp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration; // Configuración para el AppBar y NavigationDrawer
    private GoogleSignInClient mGoogleSignInClient;   // Cliente para el inicio de sesión con Google

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflar el diseño usando View Binding
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Configuración del NavigationDrawer y NavigationView
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        // Obtenemos la vista real del header (ya inflado por NavigationView)
        View headerView = navigationView.getHeaderView(0);

        // Referencias a los componentes del encabezado
        TextView userName = headerView.findViewById(R.id.textView);         // Nombre del usuario
        TextView userEmail = headerView.findViewById(R.id.user_email);      // Correo (o texto) del usuario
        ImageView userProfilePic = headerView.findViewById(R.id.imageView); // Foto de perfil
        Button logoutButton = headerView.findViewById(R.id.logout_button);  // Botón de cerrar sesión

        // Obtener usuario autenticado de Firebase
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Revisamos si tiene Facebook y/o Google
            boolean hasFacebook = false;
            boolean hasGoogle = false;

            for (UserInfo userInfo : user.getProviderData()) {
                if ("facebook.com".equals(userInfo.getProviderId())) {
                    hasFacebook = true;
                } else if ("google.com".equals(userInfo.getProviderId())) {
                    hasGoogle = true;
                }
            }

            // Comprobamos el token de Facebook
            AccessToken fbToken = AccessToken.getCurrentAccessToken();
            boolean isFacebookTokenValid = (fbToken != null && !fbToken.isExpired());

            // Si el usuario tiene Facebook como proveedor y el token es válido => Lógica Facebook
            if (hasFacebook && isFacebookTokenValid) {
                fetchFacebookUserInfo(userProfilePic, userName, userEmail);
            }
            // Si no, comprobamos Google
            else if (hasGoogle) {
                // Usar la info de Google
                userName.setText(user.getDisplayName() != null ? user.getDisplayName() : "Usuario");
                userEmail.setText(user.getEmail() != null ? user.getEmail() : "No disponible");

                if (user.getPhotoUrl() != null) {
                    Picasso.get().load(user.getPhotoUrl()).into(userProfilePic);
                }
            }
            // Caso: ni Facebook válido ni Google => por si existiera otro proveedor
            else {
                // Ajusta según tus necesidades
                userName.setText(user.getDisplayName() != null ? user.getDisplayName() : "Usuario");
                userEmail.setText(user.getEmail() != null ? user.getEmail() : "No disponible");

                if (user.getPhotoUrl() != null) {
                    Picasso.get().load(user.getPhotoUrl()).into(userProfilePic);
                }
            }
        }

        // Configurar GoogleSignInClient para manejar el cierre de sesión
        mGoogleSignInClient = GoogleSignIn.getClient(this,
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .build()
        );

        // Configuración del NavigationDrawer con el controlador de navegación
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_favorites, R.id.nav_buscar // IDs de tus fragments
        ).setOpenableLayout(drawer) // DrawerLayout asociado
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Configurar acción del botón de cerrar sesión
        logoutButton.setOnClickListener(v -> logout());
    }

    /**
     * Llamada a la Graph API de Facebook para obtener el nombre y la foto de perfil del usuario.
     * Se reemplaza el email con "Conectado con Facebook".
     *
     * @param userProfilePic ImageView donde se cargará la foto de perfil
     * @param userName       TextView para el nombre
     * @param userEmail      TextView para el correo/estado del usuario
     */
    private void fetchFacebookUserInfo(ImageView userProfilePic, TextView userName, TextView userEmail) {
        AccessToken token = AccessToken.getCurrentAccessToken();
        if (token == null || token.isExpired()) {
            // Si no hay token o está expirado, no podemos llamar a la Graph API
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

                // Nombre
                final String fbName = response.optString("name", "Usuario");

                // Foto
                JSONObject pictureObj = response.optJSONObject("picture");
                JSONObject dataObj = (pictureObj != null) ? pictureObj.optJSONObject("data") : null;
                final String fbPhotoUrl = (dataObj != null) ? dataObj.optString("url") : null;

                // Actualizar UI
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

    /**
     * Maneja el cierre de sesión del usuario, cerrando las sesiones de Firebase, Google y Facebook.
     * Redirige a la actividad de inicio de sesión.
     */
    private void logout() {
        // Cerrar sesión en Firebase
        FirebaseAuth.getInstance().signOut();

        // Cerrar sesión de Facebook
        LoginManager.getInstance().logOut();

        // Cerrar sesión de Google
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Finalizar MainActivity
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflar el menú; añade elementos al AppBar si están presentes
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }
}