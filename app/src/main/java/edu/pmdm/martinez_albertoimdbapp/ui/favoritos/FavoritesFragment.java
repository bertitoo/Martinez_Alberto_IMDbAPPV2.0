package edu.pmdm.martinez_albertoimdbapp.ui.favoritos;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import edu.pmdm.martinez_albertoimdbapp.MovieDetailsActivity;
import edu.pmdm.martinez_albertoimdbapp.R;
import edu.pmdm.martinez_albertoimdbapp.database.FavoritesManager;
import edu.pmdm.martinez_albertoimdbapp.models.Movie;

/**
 * Fragmento que muestra y gestiona las películas favoritas del usuario.
 * Permite visualizar las películas guardadas, eliminarlas y compartir la lista en formato JSON.
 *
 * @author Alberto Martínez Vadillo
 */
public class FavoritesFragment extends Fragment {

    private GridLayout gridLayout;
    private FavoritesManager favoritesManager;
    private String currentUserId;

    private ActivityResultLauncher<String> bluetoothPermissionLauncher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_favoritos, container, false);

        gridLayout = root.findViewById(R.id.gridLayout);
        Button btnShareJson = root.findViewById(R.id.btnShareJson);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
        } else {
            Toast.makeText(getContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return root;
        }

        favoritesManager = new FavoritesManager(getContext());
        loadFavorites();

        // Configuración del lanzador para permisos de Bluetooth
        bluetoothPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        showFavoritesJsonDialog();
                    } else {
                        Toast.makeText(getContext(), "Permiso de Bluetooth denegado.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Configurar el botón para compartir JSON
        btnShareJson.setOnClickListener(v -> requestBluetoothPermission());

        return root;
    }

    /**
     * Carga las películas favoritas del usuario desde la base de datos y las muestra en un GridLayout.
     */
    private void loadFavorites() {
        List<Movie> favorites = favoritesManager.getFavoritesForUser(currentUserId);
        gridLayout.removeAllViews();
        if (favorites.isEmpty()) {
            Toast.makeText(getContext(), "No tienes películas favoritas.", Toast.LENGTH_SHORT).show();
        } else {
            for (Movie movie : favorites) {
                addImageToGrid(movie);
            }
        }
    }

    /**
     * Añade una película al GridLayout como un ImageView.
     *
     * @param movie Objeto Movie que representa la película a añadir.
     */
    private void addImageToGrid(Movie movie) {
        ImageView imageView = new ImageView(getContext());
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 500;
        params.height = 750;
        params.setMargins(16, 16, 16, 16);
        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        new Thread(() -> {
            Bitmap bitmap = getBitmapFromURL(movie.getPosterUrl());
            if (bitmap != null) {
                if (isAdded() && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> imageView.setImageBitmap(bitmap));
                } else {
                    Log.e("FRAGMENT_ERROR", "Fragment no está adjunto a la actividad.");
                }
            }
        }).start();

        imageView.setOnClickListener(v -> {
            if (isAdded() && getActivity() != null) {
                Intent intent = new Intent(getActivity(), MovieDetailsActivity.class);
                intent.putExtra("IMDB_ID", movie.getImdbId());
                Toast.makeText(requireContext(), "Mostrando detalles de la película: " + movie.getTitle(), Toast.LENGTH_SHORT).show();
                startActivity(intent);
            } else {
                Log.e("FRAGMENT_ERROR", "Fragment no está adjunto a la actividad.");
            }
        });

        imageView.setOnLongClickListener(v -> {
            if (isAdded() && getActivity() != null) {
                favoritesManager.removeFavorite(movie.getImdbId(), currentUserId);
                Toast.makeText(requireContext(), "Película eliminada de favoritos: " + movie.getTitle(), Toast.LENGTH_LONG).show();
                loadFavorites();
            } else {
                Log.e("FRAGMENT_ERROR", "Fragment no está adjunto a la actividad.");
            }
            return true;
        });

        if (isAdded() && getActivity() != null) {
            gridLayout.addView(imageView);
        } else {
            Log.e("FRAGMENT_ERROR", "Fragment no está adjunto a la actividad. No se agregó la vista.");
        }
    }

    /**
     * Descarga una imagen desde una URL y la devuelve como un objeto Bitmap.
     *
     * @param imageUrl URL de la imagen.
     * @return Objeto Bitmap de la imagen descargada o null si ocurre un error.
     */
    private Bitmap getBitmapFromURL(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            Log.e("IMAGE_ERROR", "Error al descargar la imagen", e);
            return null;
        }
    }

    /**
     * Solicita permiso para usar Bluetooth.
     * Si el permiso ya está concedido, muestra el cuadro de diálogo con las películas favoritas en JSON.
     */
    private void requestBluetoothPermission() {
        if (requireContext().checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            showFavoritesJsonDialog();
        }
    }

    /**
     * Muestra un cuadro de diálogo con las películas favoritas del usuario en formato JSON.
     */
    private void showFavoritesJsonDialog() {
        List<Movie> favorites = favoritesManager.getFavoritesForUser(currentUserId);

        if (favorites.isEmpty()) {
            Toast.makeText(getContext(), "No tienes películas favoritas para compartir.", Toast.LENGTH_SHORT).show();
            return;
        }

        Gson gson = new Gson();
        String json = gson.toJson(favorites);

        if (isAdded() && getActivity() != null) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Películas Favoritas en JSON")
                    .setMessage(json)
                    .setPositiveButton("Cerrar", (dialog, which) -> dialog.dismiss())
                    .show();
        } else {
            Log.e("FRAGMENT_ERROR", "Fragment no está adjunto a la actividad.");
        }
    }
}