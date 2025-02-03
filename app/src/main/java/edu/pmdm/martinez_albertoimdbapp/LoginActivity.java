package edu.pmdm.martinez_albertoimdbapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {
    // Variables de autenticación
    private FirebaseAuth mAuth;
    private GoogleSignInClient gClient;
    private CallbackManager callbackManager;
    private final String TAG = "LOGIN ACTIVITY";
    private AuthCredential facebookCredential;
    private ActivityResultLauncher<Intent> activityResultLauncherGoogleSignIn;
    private ActivityResultLauncher<Intent> activityResultLauncherGoogleLinking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Inicialización de CallbackManager y Firebase
        callbackManager = CallbackManager.Factory.create();
        mAuth = FirebaseAuth.getInstance();

        // Configuración de Google Sign-In
        GoogleSignInOptions gOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        gClient = GoogleSignIn.getClient(this, gOptions);

        // Si ya hay usuario autenticado, se navega directamente a MainActivity
        FirebaseUser usuarioActual = mAuth.getCurrentUser();
        if (usuarioActual != null) {
            navigateToMainActivity();
        }

        // Configuración de launcher para Google Sign-In
        activityResultLauncherGoogleSignIn = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK) {
                            Intent data = result.getData();
                            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                            try {
                                GoogleSignInAccount account = task.getResult(ApiException.class);
                                if (account != null) {
                                    firebaseAuthWithGoogle(account.getIdToken());
                                }
                            } catch (ApiException e) {
                                Toast.makeText(LoginActivity.this, "Algo ha ido mal", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });

        activityResultLauncherGoogleLinking = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Intent data = result.getData();
                            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                            try {
                                GoogleSignInAccount account = task.getResult(ApiException.class);
                                if (account != null) {
                                    vincularGoogleConFacebook(account);
                                }
                            } catch (ApiException e) {
                                Log.e(TAG, "Google sign in failed for linking", e);
                                Toast.makeText(LoginActivity.this, "Error al iniciar sesión con Google para vincular.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Inicio de sesión con Google cancelado.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // Configuración del botón de Google Sign-In
        SignInButton buttonGoogle = findViewById(R.id.sign_in_button);
        buttonGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent signInIntent = gClient.getSignInIntent();
                activityResultLauncherGoogleSignIn.launch(signInIntent);
            }
        });

        // Configuración del botón de Facebook
        LoginButton loginButtonFacebook = findViewById(R.id.fb_button);
        loginButtonFacebook.setReadPermissions("email", "public_profile");
        loginButtonFacebook.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "facebook:onSuccess:" + loginResult);
                handleFacebookAccessToken(loginResult.getAccessToken());
            }
            @Override
            public void onCancel() {
                Log.d(TAG, "facebook:onCancel");
            }
            @Override
            public void onError(@NonNull FacebookException e) {
                Log.d(TAG, "facebook:onError", e);
            }
        });

        // Referencias a los campos de e-mail y contraseña
        EditText etEmail = findViewById(R.id.et_email);
        EditText etPassword = findViewById(R.id.et_password);
        // Botón para REGISTRARSE (crear cuenta nueva)
        Button btnRegister = findViewById(R.id.btn_register);
        // Botón para INICIAR SESIÓN (login con cuenta existente)
        Button btnLogin = findViewById(R.id.btn_login);

        // Flujo de REGISTRO con correo y contraseña (solo si el usuario quiere crear una cuenta)
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString();

                if (email.isEmpty() || password.isEmpty()){
                    Toast.makeText(LoginActivity.this, "Email y contraseña son obligatorios", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Validar que el correo tenga el formato adecuado
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                    Toast.makeText(LoginActivity.this, "Formato de correo inválido", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()){
                                Toast.makeText(LoginActivity.this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                                navigateToMainActivity();
                            } else {
                                if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                    Toast.makeText(LoginActivity.this, "El correo ya está registrado", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(LoginActivity.this, "Error en el registro: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

        // Flujo de LOGIN con correo y contraseña (sólo funciona si la cuenta ya existe)
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString();

                if (email.isEmpty() || password.isEmpty()){
                    Toast.makeText(LoginActivity.this, "Email y contraseña son obligatorios", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Validar formato del correo
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                    Toast.makeText(LoginActivity.this, "Formato de correo inválido", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()){
                                Toast.makeText(LoginActivity.this, "Login exitoso.", Toast.LENGTH_SHORT).show();
                                navigateToMainActivity();
                            } else {
                                // Si falla, probablemente el usuario no existe o la contraseña es incorrecta
                                Toast.makeText(LoginActivity.this, "Error en el login.", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        navigateToMainActivity();
                    } else {
                        Toast.makeText(LoginActivity.this, "Autenticación fallida.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToMainActivity() {
        finish();
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser usuarioActual = mAuth.getCurrentUser();
        if (usuarioActual != null) {
            navigateToMainActivity();
        }
    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        Toast.makeText(LoginActivity.this, "Autenticación con Facebook exitosa.", Toast.LENGTH_SHORT).show();
                        navigateToMainActivity();
                    } else {
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            FirebaseAuthUserCollisionException exception = (FirebaseAuthUserCollisionException) task.getException();
                            String email = exception.getEmail();
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            mostrarDialogoVinculacion(email, credential);
                        } else {
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Autenticación con Facebook fallida.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void mostrarDialogoVinculacion(String email, AuthCredential credential) {
        new AlertDialog.Builder(this)
                .setTitle("Colisión de cuentas")
                .setMessage("El correo " + email + " ya está vinculado a una cuenta de Google. ¿Quieres vincular ambas cuentas a este correo?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    facebookCredential = credential;
                    activityResultLauncherGoogleLinking.launch(gClient.getSignInIntent());
                })
                .setNegativeButton("No", (dialog, which) -> {
                    Toast.makeText(LoginActivity.this, "No se han vinculado las cuentas.", Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false)
                .show();
    }

    private void vincularGoogleConFacebook(GoogleSignInAccount account) {
        AuthCredential googleCredential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(googleCredential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        mAuth.getCurrentUser().linkWithCredential(facebookCredential)
                                .addOnCompleteListener(this, linkTask -> {
                                    if (linkTask.isSuccessful()) {
                                        Log.d(TAG, "linkWithCredential:success");
                                        Toast.makeText(LoginActivity.this, "Cuentas de Google y Facebook vinculadas exitosamente.", Toast.LENGTH_SHORT).show();
                                        navigateToMainActivity();
                                    } else {
                                        Log.w(TAG, "linkWithCredential:failure", linkTask.getException());
                                        Toast.makeText(LoginActivity.this, "Vinculación de cuentas fallida.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Log.w(TAG, "signInWithCredential:failure during linking", task.getException());
                        Toast.makeText(LoginActivity.this, "Autenticación con Google fallida para vincular.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}