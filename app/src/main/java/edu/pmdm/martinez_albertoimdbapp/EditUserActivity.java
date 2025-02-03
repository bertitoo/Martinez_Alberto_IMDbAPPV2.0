package edu.pmdm.martinez_albertoimdbapp;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.squareup.picasso.Picasso;

import com.hbb20.CountryCodePicker;

import edu.pmdm.martinez_albertoimdbapp.database.DatabaseHelper;
import edu.pmdm.martinez_albertoimdbapp.utils.KeystoreManager;

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

            // Cargar dirección y teléfono desde la base de datos
            loadUserDataFromDatabase();
        }

        // Al pulsar el botón se lanza MapActivity para seleccionar la dirección
        findViewById(R.id.buttonSelectDirection).setOnClickListener(v -> {
            Intent mapIntent = new Intent(EditUserActivity.this, MapActivity.class);
            mapActivityLauncher.launch(mapIntent);
        });

        // Al pulsar el botón de "SAVE", verificamos los datos y guardamos
        Button saveButton = findViewById(R.id.buttonSave);
        saveButton.setOnClickListener(v -> saveData());
    }

    /**
     * Método para guardar los datos después de verificar el número de teléfono.
     * Solo ciframos la dirección y el número de teléfono antes de guardarlos.
     */
    private void saveData() {
        // Verificar el número de teléfono antes de guardar los datos
        String phoneNumber = editTextNumberPhone.getText().toString();
        if (!verifyPhoneNumber(phoneNumber)) {
            // Si el número de teléfono no es válido, no guardar los datos
            return;
        }

        // Recoger los datos del usuario
        String name = ((EditText) findViewById(R.id.editTextTextName)).getText().toString();
        String email = ((EditText) findViewById(R.id.editTextTextEmail)).getText().toString();
        String address = editTextAddress.getText().toString();
        String photoUrl = (findViewById(R.id.imageView).getTag() != null)
                ? findViewById(R.id.imageView).getTag().toString()
                : "";  // Usamos "" en caso de que no haya URL

        // Obtener el UID del usuario autenticado
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            // Si no hay usuario autenticado, mostramos un mensaje y terminamos
            Toast.makeText(this, "No hay usuario autenticado", Toast.LENGTH_SHORT).show();
            return;
        }
        String userUid = user.getUid(); // Obtener el UID del usuario autenticado

        // Cifrar los datos de dirección y teléfono utilizando KeystoreManager
        String encryptedAddress = KeystoreManager.encrypt(address);
        String encryptedPhone = KeystoreManager.encrypt(phoneNumber);

        // Guardar los datos en la base de datos (sin cifrar el nombre, email y photoUrl)
        saveUserDataToDatabase(userUid, name, email, encryptedAddress, encryptedPhone, photoUrl);

        // Mostrar un mensaje de éxito
        Toast.makeText(this, "Datos guardados correctamente", Toast.LENGTH_SHORT).show();

        // Después de guardar los datos, redirigir a MainActivity
        Intent intent = new Intent(EditUserActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // Finalizamos esta actividad para evitar que el usuario regrese a esta pantalla de edición
    }

    private void saveUserDataToDatabase(String userUid, String name, String email, String encryptedAddress,
                                        String encryptedPhone, String photoUrl) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USER_NAME, name); // No ciframos el nombre
        values.put(DatabaseHelper.COLUMN_USER_EMAIL, email); // No ciframos el correo
        values.put(DatabaseHelper.COLUMN_USER_ADDRESS, encryptedAddress); // Dirección cifrada
        values.put(DatabaseHelper.COLUMN_USER_PHONE, encryptedPhone); // Teléfono cifrado
        values.put(DatabaseHelper.COLUMN_USER_IMAGE_URL, photoUrl); // URL de la imagen (no cifrada)

        // Verificar si el usuario ya existe en la base de datos
        Cursor cursor = db.query(DatabaseHelper.TABLE_USERS, new String[]{DatabaseHelper.COLUMN_USER_UID},
                DatabaseHelper.COLUMN_USER_UID + " = ?", new String[]{userUid}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            // El usuario ya existe, actualizamos sus datos
            db.update(DatabaseHelper.TABLE_USERS, values, DatabaseHelper.COLUMN_USER_UID + " = ?", new String[]{userUid});
        } else {
            // El usuario no existe, insertamos un nuevo registro
            values.put(DatabaseHelper.COLUMN_USER_UID, userUid); // Asegúrate de incluir el UID
            db.insert(DatabaseHelper.TABLE_USERS, null, values);
        }

        cursor.close();
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

    /**
     * Método para cargar la dirección y teléfono del usuario desde la base de datos.
     * Si existen datos, se desencriptan y se muestran en los campos correspondientes.
     */
    private void loadUserDataFromDatabase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser(); // Obtener el usuario autenticado
        if (user == null) {
            return; // Si no hay usuario autenticado, no hacer nada
        }

        String userUid = user.getUid(); // Obtener el UID del usuario autenticado

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Consultar la base de datos para obtener la dirección y el teléfono cifrados
        String[] projection = {
                DatabaseHelper.COLUMN_USER_ADDRESS,
                DatabaseHelper.COLUMN_USER_PHONE
        };

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,   // La tabla a consultar
                projection,                   // Las columnas a devolver
                DatabaseHelper.COLUMN_USER_UID + " = ?", // Filtro WHERE
                new String[]{userUid},        // Los valores del filtro
                null,                         // No agrupamos filas
                null,                         // No filtramos por grupos
                null                          // No ordenamos filas
        );

        if (cursor != null && cursor.moveToFirst()) {
            // Obtener los valores cifrados
            String encryptedAddress = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ADDRESS));
            String encryptedPhone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_PHONE));

            // Desencriptar los valores
            String decryptedAddress = KeystoreManager.decrypt(encryptedAddress);
            String decryptedPhone = KeystoreManager.decrypt(encryptedPhone);

            // Mostrar los valores desencriptados en los campos correspondientes
            editTextAddress.setText(decryptedAddress);
            editTextNumberPhone.setText(decryptedPhone);

            cursor.close();
        }
    }
}