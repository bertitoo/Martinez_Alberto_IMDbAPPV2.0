package edu.pmdm.martinez_albertoimdbapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;

import com.squareup.picasso.Picasso;

import com.hbb20.CountryCodePicker;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.google.i18n.phonenumbers.NumberParseException;

public class EditUserActivity extends AppCompatActivity {

    private EditText editTextAddress;
    private EditText editTextNumberPhone;
    private CountryCodePicker countryCodePicker;

    // Launcher para recibir el resultado de MapActivity
    private final ActivityResultLauncher<Intent> mapActivityLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    // Se obtiene la dirección confirmada desde MapActivity
                    String address = result.getData().getStringExtra("selected_address");
                    editTextAddress.setText(address);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user);

        // Aplicar insets al layout principal (opcional)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Referencias a los elementos del layout
        EditText editTextName = findViewById(R.id.editTextTextName);
        EditText editTextEmail = findViewById(R.id.editTextTextEmail);
        editTextAddress = findViewById(R.id.editTextTextAddress);
        editTextAddress.setEnabled(false);
        ImageView imageViewUser = findViewById(R.id.imageView);
        editTextNumberPhone = findViewById(R.id.editTextNumberPhone);
        countryCodePicker = findViewById(R.id.countryCodePicker);

        // Recuperar datos enviados desde otra actividad (por ejemplo, MainActivity)
        Intent intent = getIntent();
        if (intent != null) {
            String name = intent.getStringExtra("user_name");
            String email = intent.getStringExtra("user_email");
            String photoUrl = intent.getStringExtra("user_photo_url");

            if (name != null) {
                editTextName.setText(name);
            }
            if (email != null) {
                editTextEmail.setText(email);
                editTextEmail.setEnabled(false);
            }
            if (photoUrl != null && !photoUrl.isEmpty()) {
                Picasso.get().load(photoUrl).into(imageViewUser);
            }
        }

        // Al pulsar el botón se lanza MapActivity para seleccionar la dirección
        findViewById(R.id.buttonSelectDirection).setOnClickListener(v -> {
            Intent mapIntent = new Intent(EditUserActivity.this, MapActivity.class);
            mapActivityLauncher.launch(mapIntent);
        });

        // Al pulsar el botón de "SAVE", verificamos los datos
        Button saveButton = findViewById(R.id.buttonSave);
        saveButton.setOnClickListener(v -> saveData());
    }

    /**
     * Método para guardar los datos después de verificar el número de teléfono
     */
    private void saveData() {
        // Verificar el número de teléfono antes de guardar los datos
        String phoneNumber = editTextNumberPhone.getText().toString();
        if (!verifyPhoneNumber(phoneNumber)) {
            // Si el número de teléfono no es válido, no guardar los datos
            return;
        }

        // Aquí puedes agregar lógica para guardar los datos (por ejemplo, en una base de datos)
        // Si los datos son correctos, mostramos un mensaje de éxito
        Toast.makeText(this, "Datos guardados correctamente", Toast.LENGTH_SHORT).show();
        // Continuar con el flujo (por ejemplo, volver a la actividad anterior)
    }

    /**
     * Método para verificar el número de teléfono usando libphonenumber.
     */
    private boolean verifyPhoneNumber(String phoneNumber) {
        String countryCode = countryCodePicker.getSelectedCountryCodeWithPlus();

        // Construir el número completo con el prefijo del país
        String fullPhoneNumber = countryCode + phoneNumber;

        // Validar el número de teléfono
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        try {
            Phonenumber.PhoneNumber number = phoneNumberUtil.parse(fullPhoneNumber, "");
            boolean isValid = phoneNumberUtil.isValidNumber(number);

            if (isValid) {
                String formattedNumber = phoneNumberUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
                Toast.makeText(this, "Número válido: " + formattedNumber, Toast.LENGTH_SHORT).show();
                return true;
            } else {
                Toast.makeText(this, "Número no válido", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberParseException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al parsear el número", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}