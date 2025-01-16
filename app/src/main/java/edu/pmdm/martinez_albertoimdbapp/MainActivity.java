package edu.pmdm.martinez_albertoimdbapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import android.view.Menu;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.facebook.login.LoginManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;

import edu.pmdm.martinez_albertoimdbapp.databinding.ActivityMainBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration; // Configuración para el AppBar y NavigationDrawer
    private GoogleSignInClient mGoogleSignInClient;   // Cliente para el inicio de sesión con Google

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflar el diseño usando View Binding
        edu.pmdm.martinez_albertoimdbapp.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Configuración del NavigationDrawer y NavigationView
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        // Inflar el encabezado del NavigationView
        View headerView = navigationView.getHeaderView(0);

        // Referencias a los componentes del encabezado
        TextView userName = headerView.findViewById(R.id.textView); // Nombre del usuario
        TextView userEmail = headerView.findViewById(R.id.user_email); // Correo del usuario
        ImageView userProfilePic = headerView.findViewById(R.id.imageView); // Foto de perfil
        Button logoutButton = headerView.findViewById(R.id.logout_button); // Botón de cerrar sesión

        // Obtener usuario autenticado de Firebase
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            // Actualizar la interfaz con los datos del usuario
            userName.setText(user.getDisplayName() != null ? user.getDisplayName() : "Usuario"); // Nombre
            userEmail.setText(user.getEmail() != null ? user.getEmail() : "No disponible"); // Correo
            if (user.getPhotoUrl() != null) {
                Picasso.get().load(user.getPhotoUrl()).into(userProfilePic); // Foto de perfil
            } else {
                Log.d("FOTO DE PERFIL", "No se ha podido cargar la foto");
            }
        }

        // Configurar GoogleSignInClient para manejar el cierre de sesión
        mGoogleSignInClient = GoogleSignIn.getClient(this,
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .build());

        // Configuración del NavigationDrawer con el controlador de navegación
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_favorites, R.id.nav_buscar) // IDs de los fragmentos
                .setOpenableLayout(drawer) // DrawerLayout asociado
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Configurar acción del botón de cerrar sesión
        logoutButton.setOnClickListener(v -> logout());
    }

    /**
     * Maneja el cierre de sesión del usuario, cerrando las sesiones de Firebase, Google y Facebook.
     * Redirige a la actividad de inicio de sesión.
     */
    private void logout() {
        // Cerrar sesión en Firebase
        FirebaseAuth.getInstance().signOut();
        LoginManager.getInstance().logOut();

        // Cerrar sesión de Google
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, task -> {
                    // Cerrar sesión de Facebook
                    LoginManager.getInstance().logOut();

                    // Redirigir a LoginActivity tras cerrar sesión
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
        // Manejar la navegación hacia arriba en el controlador de navegación
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}