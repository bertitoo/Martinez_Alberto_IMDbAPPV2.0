package edu.pmdm.martinez_albertoimdbapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {
    //Declaramos las variables de Firebase y GoogleSignInClient.
    private FirebaseAuth mAuth;
    private GoogleSignInClient gClient;
    private CallbackManager callbackManager;
    private String TAG = "LOGIN ACTIVITY";
    private AuthCredential facebookCredential;
    private ActivityResultLauncher<Intent> activityResultLauncherGoogleSignIn;
    private ActivityResultLauncher<Intent> activityResultLauncherGoogleLinking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        callbackManager = CallbackManager.Factory.create();

        //Obtenemos la instancia de Firebase.
        mAuth = FirebaseAuth.getInstance();
        //Declaramos una variable de tipo GoogleSignInOptions y la inicializamos al inicio de sesión por defecto.
        GoogleSignInOptions gOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build();
        //Inicializamos la variable de GoogleSignInClient utilizando las opciones de inicio de sesión por defecto.
        gClient = GoogleSignIn.getClient(this, gOptions);
        //Declaramos e inicializamos la variable usuarioActual a el usuario que recogemos con la variable de Firebase.
        FirebaseUser usuarioActual = mAuth.getCurrentUser();
        //Comprobamos que el usuario no sea nulo y navegamos al main.
        if (usuarioActual != null) {
            navigateToMainActivity();
        }
        //Launcher para lanzar la pestaña de selección de cuenta de Google.
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

        activityResultLauncherGoogleLinking = registerForActivityResult
                (new ActivityResultContracts.StartActivityForResult(),
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
                                        Toast.makeText(LoginActivity.this, "Algo ha ido mal durante el inicio de sesión con Google para vincular.", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(LoginActivity.this, "Inicio de sesión con Google cancelado.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

        //Declaramos e inicializamos la variable botón.
        SignInButton button = findViewById(R.id.sign_in_button);
        //Le añadimos el OnClickListener.
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Declaramos e incializamos el intent a la pestaña de selección de cuentas de google de gClient.
                Intent signInIntent = gClient.getSignInIntent();
                //Utilizando el launcher lanzamos el intent de inicio de sesión.
                activityResultLauncherGoogleSignIn.launch(signInIntent);
            }
        });

        LoginButton loginButton = findViewById(R.id.fb_button);
        loginButton.setReadPermissions("email", "public_profile");
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);

    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            navigateToMainActivity();
                        } else {
                            Toast.makeText(LoginActivity.this, "Autentificación fallida.", Toast.LENGTH_SHORT).show();
                        }
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
        FirebaseUser user = mAuth.getCurrentUser();
        // Usuario no autenticado, intenta iniciar sesión con Facebook
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Inicio de sesión exitoso
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser newUser = mAuth.getCurrentUser();
                        Toast.makeText(LoginActivity.this, "Autenticación con Facebook exitosa.", Toast.LENGTH_SHORT).show();
                        navigateToMainActivity();
                    } else {
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            // Manejar la colisión de cuentas
                            FirebaseAuthUserCollisionException exception = (FirebaseAuthUserCollisionException) task.getException();
                            String email = exception.getEmail();
                            Log.w(TAG, "signInWithCredential:failure", task.getException());

                            // Mostrar un diálogo al usuario para iniciar sesión con Google y luego vincular
                            mostrarDialogoVinculacion(email, credential);
                        } else {
                            // Otro tipo de error
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
                    // Almacenar la credencial de Facebook para vinculación posterior
                    facebookCredential = credential;
                    // Iniciar el flujo de Google Sign-In para vincular
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

        // Reautenticar con Google
        mAuth.signInWithCredential(googleCredential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Vincular la credencial de Facebook con la cuenta de Google
                        mAuth.getCurrentUser().linkWithCredential(facebookCredential)
                                .addOnCompleteListener(this, linkTask -> {
                                    if (linkTask.isSuccessful()) {
                                        Log.d(TAG, "linkWithCredential:success");
                                        FirebaseUser linkedUser = linkTask.getResult().getUser();
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