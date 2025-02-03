package edu.pmdm.martinez_albertoimdbapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.libraries.places.api.model.Place;

import java.util.Arrays;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private Place selectedPlace; // Lugar seleccionado

    // Launcher para el widget de Autocomplete
    private final ActivityResultLauncher<Intent> autocompleteLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedPlace = Autocomplete.getPlaceFromIntent(result.getData());
                    updateMapWithSelectedPlace();
                } else if (result.getResultCode() == AutocompleteActivity.RESULT_ERROR) {
                    Status status = Autocomplete.getStatusFromIntent(result.getData());
                    Toast.makeText(this, "Error: " + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Inicializar Places si aún no lo está
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyAER7D-uvYpBOG3wZjz9z3AeGulqAci-OU");
        }

        // Obtener el fragmento del mapa y solicitar la notificación cuando esté listo
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Botón para buscar otra dirección
        findViewById(R.id.buttonSearchAddress).setOnClickListener(v -> openPlacePicker());

        // Botón para confirmar la dirección seleccionada y devolverla a EditUserActivity
        findViewById(R.id.buttonConfirmAddress).setOnClickListener(v -> {
            if (selectedPlace != null && selectedPlace.getAddress() != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("selected_address", selectedPlace.getAddress());
                // Puedes agregar más datos (por ejemplo, latitud/longitud) si lo deseas
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "No se ha seleccionado ninguna dirección", Toast.LENGTH_SHORT).show();
            }
        });

        // Si no hay dirección seleccionada, lanza el Autocomplete de inmediato
        if (selectedPlace == null) {
            openPlacePicker();
        }
    }

    /**
     * Lanza el widget de Autocomplete para seleccionar una dirección.
     */
    private void openPlacePicker() {
        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG
        );
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                .build(this);
        autocompleteLauncher.launch(intent);
    }

    /**
     * Actualiza el mapa con un marcador en la ubicación seleccionada.
     */
    private void updateMapWithSelectedPlace() {
        if (googleMap != null && selectedPlace != null && selectedPlace.getLatLng() != null) {
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions()
                    .position(selectedPlace.getLatLng())
                    .title(selectedPlace.getAddress()));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedPlace.getLatLng(), 15f));
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        // Si ya se ha seleccionado una dirección, actualiza el mapa
        updateMapWithSelectedPlace();
    }
}