package edu.pmdm.martinez_albertoimdbapp.sync;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsersSync {

    private static final String TAG = "UsersSync";
    private final FirebaseFirestore firestore;

    public UsersSync(Context context) {
        // Usamos el contexto de la aplicación (aunque no lo necesitemos en este ejemplo)
        Context appContext = context.getApplicationContext();
        this.firestore = FirebaseFirestore.getInstance();
    }

    // Retorna la fecha/hora actual en formato "yyyy-MM-dd HH:mm:ss"
    private String getCurrentTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    /**
     * Sincroniza los datos del usuario en Firestore (dentro del documento con id = uid)
     * guardando los campos "name", "uid" y "email" a nivel raíz, no anidados.
     */
    public void syncUser(String uid, String name, String email) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("uid", uid);
        data.put("email", email);

        firestore.collection("users")
                .document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Datos sincronizados para uid " + uid))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error sincronizando datos para uid " + uid, e));
    }

    /**
     * Registra el evento de login.
     * - Se comprueba si el último objeto del arreglo "activity_log" tiene ya un logout registrado.
     * - Si no hay sesión abierta (último objeto con logout_time lleno o inexistente), se añade
     *   un nuevo objeto con login_time = hora actual y logout_time vacío.
     */
    public void startSession(String uid) {
        String currentTime = getCurrentTime();
        DocumentReference userRef = firestore.collection("users").document(uid);

        // Leemos el documento para verificar el estado actual del arreglo
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            List<Map<String, Object>> activityLog = (List<Map<String, Object>>) documentSnapshot.get("activity_log");
            boolean tieneSesionAbierta = false;

            if (activityLog != null && !activityLog.isEmpty()) {
                Map<String, Object> ultimoRegistro = activityLog.get(activityLog.size() - 1);
                String logoutTime = (String) ultimoRegistro.get("logout_time");
                // Si el campo de logout está vacío o es nulo, se entiende que la sesión está abierta
                if (logoutTime == null || logoutTime.isEmpty()) {
                    tieneSesionAbierta = true;
                }
            }

            if (!tieneSesionAbierta) {
                Map<String, Object> loginEvent = new HashMap<>();
                loginEvent.put("login_time", currentTime);
                loginEvent.put("logout_time", ""); // Se deja vacío

                // Se utiliza arrayUnion para añadir la sesión sin eliminar las anteriores
                userRef.update("activity_log", FieldValue.arrayUnion(loginEvent))
                        .addOnSuccessListener(aVoid ->
                                Log.d(TAG, "Evento LOGIN registrado para uid " + uid + " (login_time: " + currentTime + ")"))
                        .addOnFailureListener(e ->
                                Log.e(TAG, "Error registrando evento LOGIN para uid " + uid, e));
            } else {
                Log.d(TAG, "Ya existe una sesión activa para uid " + uid + ". No se registra un nuevo login.");
            }
        }).addOnFailureListener(e ->
                Log.e(TAG, "Error al leer el documento para uid " + uid, e));
    }

    /**
     * Registra el evento de logout.
     * - Se lee el documento y se revisa el último objeto del arreglo "activity_log".
     * - Si ese objeto tiene el campo logout_time vacío, se actualiza dicho objeto asignándole
     *   la hora actual, y se actualiza el arreglo completo en Firestore.
     */
    public void endSession(String uid) {
        String currentTime = getCurrentTime();
        DocumentReference userRef = firestore.collection("users").document(uid);

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            List<Map<String, Object>> activityLog = (List<Map<String, Object>>) documentSnapshot.get("activity_log");
            if (activityLog != null && !activityLog.isEmpty()) {
                Map<String, Object> ultimoRegistro = activityLog.get(activityLog.size() - 1);
                String logoutTime = (String) ultimoRegistro.get("logout_time");
                if (logoutTime == null || logoutTime.isEmpty()) {
                    // Se actualiza el registro de login abierto con la hora de logout
                    ultimoRegistro.put("logout_time", currentTime);

                    // Se actualiza el documento completo con el arreglo modificado
                    userRef.update("activity_log", activityLog)
                            .addOnSuccessListener(aVoid ->
                                    Log.d(TAG, "Evento LOGOUT registrado para uid " + uid + " (logout_time: " + currentTime + ")"))
                            .addOnFailureListener(e ->
                                    Log.e(TAG, "Error registrando evento LOGOUT para uid " + uid, e));
                } else {
                    Log.w(TAG, "No existe una sesión abierta para cerrar en uid " + uid);
                }
            } else {
                Log.w(TAG, "No hay registros en activity_log para uid " + uid);
            }
        }).addOnFailureListener(e ->
                Log.e(TAG, "Error al leer el documento para uid " + uid, e));
    }

    /**
     * Dependiendo del evento recibido:
     * - "login": se registra un evento de login (nueva sesión) si no hay una sesión abierta.
     * - "logout": se registra el logout en la sesión abierta, completando el campo logout_time.
     */
    public void syncLocalToRemote(String uid, String eventType) {
        if ("login".equalsIgnoreCase(eventType)) {
            startSession(uid);
        } else if ("logout".equalsIgnoreCase(eventType)) {
            endSession(uid);
        } else {
            Log.w(TAG, "Tipo de evento no reconocido: " + eventType);
        }
    }
}