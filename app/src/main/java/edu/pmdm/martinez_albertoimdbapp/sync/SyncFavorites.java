package edu.pmdm.martinez_albertoimdbapp.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import edu.pmdm.martinez_albertoimdbapp.database.FavoritesManager;
import edu.pmdm.martinez_albertoimdbapp.models.Movie;

/**
 * Clase que gestiona la sincronización de los favoritos de un usuario entre la base de datos local (SQLite)
 * y Cloud Firestore. Permite tanto la sincronización manual como la sincronización en tiempo real.
 *
 * @author Alberto Martínez Vadillo
 */
public class SyncFavorites {

    private final String userId;
    private final FavoritesManager favoritesManager;
    private final FirebaseFirestore firestore;

    /**
     * Constructor para inicializar el objeto SyncFavorites con el contexto y el ID del usuario.
     *
     * @param context El contexto de la aplicación.
     * @param userId El ID del usuario cuyas películas favoritas se deben sincronizar.
     */
    public SyncFavorites(Context context, String userId) {
        this.userId = userId;
        this.favoritesManager = new FavoritesManager(context);
        this.firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Sincroniza los favoritos entre la base de datos local y Cloud Firestore.
     * Se invoca al iniciar la aplicación o después de modificar la base (añadir o eliminar película).
     * Se mantiene la diferenciación de usuarios usando el userId.
     */
    public void syncFavorites() {
        // 1) Obtener los favoritos locales
        List<Movie> localFavorites = favoritesManager.getFavoritesForUser(userId);

        // 2) Obtener los favoritos almacenados en Firestore para este usuario
        firestore.collection("favorites")
                .document(userId)
                .collection("movies")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        List<Movie> remoteFavorites = new ArrayList<>();

                        // Convertir cada documento de Firestore en un objeto Movie (solo id, title e imageUrl)
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            String id = doc.getString("id");
                            String title = doc.getString("title");
                            String imageUrl = doc.getString("imageUrl");

                            Movie movie = new Movie(id, imageUrl);
                            movie.setTitle(title);
                            remoteFavorites.add(movie);
                        }

                        // 3) Sincronizar: Si hay favoritos remotos que NO están en la base local, insertarlos localmente
                        for (Movie remoteMovie : remoteFavorites) {
                            boolean existsLocally = false;
                            for (Movie localMovie : localFavorites) {
                                if (localMovie.getImdbId().equals(remoteMovie.getImdbId())) {
                                    existsLocally = true;
                                    break;
                                }
                            }
                            if (!existsLocally) {
                                // Insertamos en la base local (solo con id, title e imageUrl)
                                favoritesManager.addFavorite(remoteMovie.getImdbId(), remoteMovie.getTitle(), remoteMovie.getPosterUrl(), userId);
                                Log.d("SyncFavorites", "Agregado a local (desde cloud): " + remoteMovie.getTitle());
                            }
                        }

                        // 4) Sincronizar: Si hay favoritos locales que NO están en Firestore, subirlos a la nube
                        for (Movie localMovie : localFavorites) {
                            boolean existsRemotely = false;
                            for (Movie remoteMovie : remoteFavorites) {
                                if (remoteMovie.getImdbId().equals(localMovie.getImdbId())) {
                                    existsRemotely = true;
                                    break;
                                }
                            }
                            if (!existsRemotely) {
                                favoritesManager.addFavorite(localMovie.getImdbId(), localMovie.getTitle(), localMovie.getPosterUrl(), userId);
                                Log.d("SyncFavorites", "Agregado a cloud (desde local): " + localMovie.getTitle());
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("SyncFavorites", "Error al sincronizar favoritos: ", e);
                    }
                });
    }

    /**
     * Opcional: Inicia un listener en tiempo real en Firestore para detectar cambios en los favoritos.
     * Cuando se detecte un cambio, se puede invocar syncFavorites() para actualizar la base local.
     */
    public void startRealtimeSync() {
        firestore.collection("favorites")
                .document(userId)
                .collection("movies")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e("SyncFavorites", "Error en sync realtime: ", e);
                        return;
                    }
                    if (queryDocumentSnapshots != null) {
                        Log.d("SyncFavorites", "Cambio detectado en cloud favorites. Re-sincronizando...");
                        syncFavorites();
                    }
                });
    }
}