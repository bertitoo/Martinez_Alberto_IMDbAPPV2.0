package edu.pmdm.martinez_albertoimdbapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.SignInButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Actividad de inicio de sesión con Google.
 * Permite a los usuarios autenticarse utilizando su cuenta de Google y, en caso de éxito,
 * redirige a la actividad principal de la aplicación.
 *
 * @author Alberto Martínez Vadillo
 */
public class LoginActivity extends AppCompatActivity {

    private GoogleSignInClient mGoogleSignInClient; // Cliente para la autenticación con Google
    private static final int RC_SIGN_IN = 1001;     // Código de solicitud para el inicio de sesión

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Verificar si ya hay un usuario autenticado en Firebase
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            navigateToMainActivity(); // Navegar directamente a la actividad principal
            return;
        }

        // Configurar opciones de inicio de sesión con Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Client ID definido en strings.xml
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Configurar el botón de inicio de sesión
        SignInButton signInButton = findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(v -> signIn());

        // Personalizar el texto del botón de inicio de sesión
        customizeSignInButton(signInButton);
    }

    /**
     * Personaliza el texto del botón de inicio de sesión con Google.
     *
     * @param signInButton Botón de inicio de sesión que se personalizará.
     */
    private void customizeSignInButton(SignInButton signInButton) {
        for (int i = 0; i < signInButton.getChildCount(); i++) {
            View child = signInButton.getChildAt(i);
            if (child instanceof TextView) {
                TextView textView = (TextView) child;
                textView.setText("Sign in with Google"); // Establecer texto personalizado
            }
        }
    }

    /**
     * Lanza el flujo de inicio de sesión de Google.
     */
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            GoogleSignIn.getSignedInAccountFromIntent(data)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            GoogleSignInAccount account = task.getResult();
                            firebaseAuthWithGoogle(account);
                        }
                    });
        }
    }

    /**
     * Autentica al usuario con Firebase usando el token de Google.
     *
     * @param account Objeto de la cuenta de Google autenticada.
     */
    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(account.getIdToken(), null))
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        navigateToMainActivity(); // Navegar a la actividad principal en caso de éxito
                    }
                });
    }

    /**
     * Navega a la actividad principal (MainActivity) tras un inicio de sesión exitoso.
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // Finalizar LoginActivity para evitar que el usuario vuelva con el botón "Atrás"
    }
}