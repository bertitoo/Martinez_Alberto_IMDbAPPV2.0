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
     * Sincroniza los datos fijos del usuario en Firestore.
     * Se guarda en "users/{uid}" el campo "activity_log" con las claves "name", "uid" y "email".
     */
    public void syncUser(String uid, String name, String email) {
        Map<String, Object> fixedData = new HashMap<>();
        fixedData.put("name", name);
        fixedData.put("uid", uid);
        fixedData.put("email", email);

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
     * Siempre añade un nuevo objeto al arreglo "activity_log.history" con "login_time" (y "logout_time" vacío).
     */
    public void startSession(String uid, String name, String email) {
        // Primero, sincroniza los datos fijos.
        syncUser(uid, name, email);

        firestore.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    ArrayList<Map<String, Object>> history =
                            (ArrayList<Map<String, Object>>) documentSnapshot.get("activity_log.history");
                    if (history == null) {
                        history = new ArrayList<>();
                    }
                    // Crear nuevo registro de login.
                    Map<String, Object> loginEvent = new HashMap<>();
                    loginEvent.put("login_time", getCurrentTime());
                    loginEvent.put("logout_time", ""); // Aún no se ha registrado el logout

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
     * Se busca el último registro en el arreglo "activity_log.history" que tenga "logout_time" vacío
     * y se le asigna la hora actual.
     */
    public void endSession(String uid) {
        firestore.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    ArrayList<Map<String, Object>> history =
                            (ArrayList<Map<String, Object>>) documentSnapshot.get("activity_log.history");

                    if (history == null) {
                        history = new ArrayList<>();
                    }

                    // Si el historial está vacío o el último registro ya tiene logout_time, entonces agregamos un nuevo registro
                    if (history.isEmpty() || (history.get(history.size() - 1).get("logout_time") != null
                            && !((String) history.get(history.size() - 1).get("logout_time")).isEmpty())) {
                        // Creamos un registro vacío de login (o dejamos el campo login_time vacío) y asignamos el logout_time
                        Map<String, Object> logoutEvent = new HashMap<>();
                        logoutEvent.put("login_time", ""); // O bien podrías omitirlo
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
                        // Si existe un registro abierto (sin logout_time) lo actualizamos
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
     * Sincroniza la BD local a Firestore registrando el evento indicado:
     * - "login": se llama a startSession (se añade un nuevo registro de login).
     * - "logout": se llama a endSession (se actualiza el último registro abierto con logout_time).
     */
    public void syncLocalToRemote(String uid, String eventType, String name, String email) {
        if ("login".equalsIgnoreCase(eventType)) {
            startSession(uid, name, email);
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