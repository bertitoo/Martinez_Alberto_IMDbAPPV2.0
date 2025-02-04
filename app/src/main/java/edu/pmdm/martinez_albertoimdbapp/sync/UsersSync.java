package edu.pmdm.martinez_albertoimdbapp.sync;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import edu.pmdm.martinez_albertoimdbapp.utils.KeystoreManager;

public class UsersSync {

    private static final String TAG = "UsersSync";
    private final Context context;
    private final FirebaseFirestore firestore;

    public UsersSync(Context context) {
        this.context = context.getApplicationContext();
        this.firestore = FirebaseFirestore.getInstance();
    }

    // Retorna la fecha/hora actual en formato "yyyy-MM-dd HH:mm:ss"
    private String getCurrentTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    /**
     * Sincroniza los datos fijos del usuario en Firestore, incluidos los datos cifrados de dirección y teléfono.
     */
    public void syncUser(String uid, String name, String email, String encryptedAddress, String encryptedPhone) {
        Map<String, Object> fixedData = new HashMap<>();
        fixedData.put("uid", uid);
        fixedData.put("name", name);
        fixedData.put("email", email);
        fixedData.put("address", encryptedAddress);   // Guardar la dirección cifrada
        fixedData.put("phone", encryptedPhone);       // Guardar el teléfono cifrado

        firestore.collection("users")
                .document(uid)
                .set(new HashMap<String, Object>() {{
                    put("activity_log", fixedData);
                }}, SetOptions.merge())
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Datos fijos sincronizados para uid " + uid))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error sincronizando datos fijos para uid " + uid, e));
    }

    /**
     * Registra un evento de login para el usuario.
     */
    public void startSession(String uid, String name, String email, String encryptedAddress, String encryptedPhone) {
        syncUser(uid, name, email, encryptedAddress, encryptedPhone);

        firestore.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    ArrayList<Map<String, Object>> history =
                            (ArrayList<Map<String, Object>>) documentSnapshot.get("activity_log.history");
                    if (history == null) {
                        history = new ArrayList<>();
                    }
                    Map<String, Object> loginEvent = new HashMap<>();
                    loginEvent.put("login_time", getCurrentTime());
                    loginEvent.put("logout_time", "");
                    history.add(loginEvent);

                    firestore.collection("users")
                            .document(uid)
                            .update("activity_log.history", history)
                            .addOnSuccessListener(aVoid ->
                                    Log.d(TAG, "Evento LOGIN registrado para uid " + uid + " (login_time: " + loginEvent.get("login_time") + ")"))
                            .addOnFailureListener(e ->
                                    Log.e(TAG, "Error registrando evento LOGIN para uid " + uid, e));
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error al leer el documento para uid " + uid, e));
    }

    /**
     * Registra un evento de logout para el usuario.
     */
    public void endSession(String uid) {
        firestore.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    ArrayList<Map<String, Object>> history =
                            (ArrayList<Map<String, Object>>) documentSnapshot.get("activity_log.history");

                    if (history == null) {
                        history = new ArrayList<>();
                    }

                    if (history.isEmpty() || (history.get(history.size() - 1).get("logout_time") != null
                            && !((String) history.get(history.size() - 1).get("logout_time")).isEmpty())) {
                        Map<String, Object> logoutEvent = new HashMap<>();
                        logoutEvent.put("login_time", "");
                        logoutEvent.put("logout_time", getCurrentTime());
                        history.add(logoutEvent);
                        firestore.collection("users")
                                .document(uid)
                                .update("activity_log.history", history)
                                .addOnSuccessListener(aVoid ->
                                        Log.d(TAG, "Evento LOGOUT registrado para uid " + uid + " (logout_time: " + logoutEvent.get("logout_time") + ")"))
                                .addOnFailureListener(e ->
                                        Log.e(TAG, "Error registrando evento LOGOUT para uid " + uid, e));
                    } else {
                        Map<String, Object> ultimoRegistro = history.get(history.size() - 1);
                        String logoutTime = (String) ultimoRegistro.get("logout_time");
                        if (logoutTime == null || logoutTime.isEmpty()) {
                            ultimoRegistro.put("logout_time", getCurrentTime());
                            firestore.collection("users")
                                    .document(uid)
                                    .update("activity_log.history", history)
                                    .addOnSuccessListener(aVoid ->
                                            Log.d(TAG, "Evento LOGOUT registrado para uid " + uid + " (logout_time: " + ultimoRegistro.get("logout_time") + ")"))
                                    .addOnFailureListener(e ->
                                            Log.e(TAG, "Error registrando evento LOGOUT para uid " + uid, e));
                        } else {
                            Log.w(TAG, "No hay sesión abierta para cerrar en uid " + uid);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error al leer el documento para uid " + uid, e));
    }

    /**
     * Sincroniza la BD local a Firestore registrando el evento indicado.
     */
    public void syncLocalToRemote(String uid, String eventType, String name, String email, String encryptedAddress, String encryptedPhone) {
        if ("login".equalsIgnoreCase(eventType)) {
            startSession(uid, name, email, encryptedAddress, encryptedPhone);
        } else if ("logout".equalsIgnoreCase(eventType)) {
            endSession(uid);
        } else {
            Log.w(TAG, "Tipo de evento no reconocido: " + eventType);
        }
    }

    // Sobrecarga para cuando se registra solo un logout.
    public void syncLocalToRemote(String uid, String eventType) {
        if ("logout".equalsIgnoreCase(eventType)) {
            endSession(uid);
        } else {
            Log.w(TAG, "Para el login se deben proporcionar los datos fijos (name y email).");
        }
    }
}
