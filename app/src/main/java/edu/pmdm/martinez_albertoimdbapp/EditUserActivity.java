package edu.pmdm.martinez_albertoimdbapp;

import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.squareup.picasso.Picasso;

import com.hbb20.CountryCodePicker;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

import edu.pmdm.martinez_albertoimdbapp.database.DatabaseHelper;
import edu.pmdm.martinez_albertoimdbapp.utils.KeystoreManager;

public class EditUserActivity extends AppCompatActivity {

    private EditText editTextName;
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

        // Referencias a los elementos del layout
        editTextName = findViewById(R.id.editTextTextName);
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

        // Referencia al botón de selección de imagen
        Button buttonSelectImage = findViewById(R.id.buttonSelectImage);
        // Configurar el listener del botón
        buttonSelectImage.setOnClickListener(v -> showPhotoOptionsDialog());
    }

    private void showPhotoOptionsDialog() {
        // Crear el cuadro de diálogo
        CharSequence[] items = {"Cámara", "Dispositivo", "URL de web"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Seleccionar foto")
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0: // Opción "Cámara"
                            openCamera();
                            break;
                        case 1: // Opción "Dispositivo"
                            openGallery();
                            break;
                        case 2: // Opción "URL de web"
                            enterImageUrl();
                            break;
                    }
                });
        builder.create().show();
    }

    private String currentImageBase64 = ""; // Variable para almacenar la imagen en Base64

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data"); // Obtener la imagen capturada

                    // Mostrar la imagen en el ImageView
                    ImageView imageView = findViewById(R.id.imageView);
                    imageView.setImageBitmap(imageBitmap);

                    // Convertir el Bitmap a Base64
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                    byte[] byteArray = byteArrayOutputStream.toByteArray();
                    currentImageBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT);
                }
            });

    private void openCamera() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 100);
        } else {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(cameraIntent);
        }
    }

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    Uri imageUri = data.getData(); // Obtener la URI de la imagen seleccionada

                    // Mostrar la imagen en el ImageView
                    ImageView imageView = findViewById(R.id.imageView);
                    imageView.setImageURI(imageUri);

                    // Convertir la URI a Base64 (si es necesario)
                    currentImageBase64 = imageToBase64(imageUri);
                }
            });

    private void openGallery() {
        // Abrir la galería usando ACTION_OPEN_DOCUMENT
        Intent galleryIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        galleryIntent.addCategory(Intent.CATEGORY_OPENABLE);
        galleryIntent.setType("image/*"); // Filtrar solo imágenes
        galleryLauncher.launch(galleryIntent);
    }

    private void enterImageUrl() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Ingresar URL de imagen");

        final EditText input = new EditText(this);
        input.setHint("https://...");
        builder.setView(input);

        builder.setPositiveButton("Aceptar", (dialog, which) -> {
            String url = input.getText().toString();
            if (!url.isEmpty() && isValidUrl(url)) {
                // Mostrar un mensaje de carga
                Toast.makeText(this, "Cargando imagen...", Toast.LENGTH_SHORT).show();

                // Convertir la URL a Base64
                new Thread(() -> {
                    String base64Image = urlToBase64(url);
                    if (base64Image != null) {
                        runOnUiThread(() -> {
                            // Guardar la imagen en Base64
                            currentImageBase64 = base64Image;

                            // Mostrar la imagen en el ImageView
                            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            ImageView imageView = findViewById(R.id.imageView);
                            imageView.setImageBitmap(decodedBitmap);

                            Toast.makeText(EditUserActivity.this, "Imagen cargada correctamente", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(EditUserActivity.this, "Error al cargar la imagen", Toast.LENGTH_SHORT).show());
                    }
                }).start();
            } else {
                Toast.makeText(this, "Por favor, ingresa una URL válida", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.create().show();
    }

    // Método para validar que la URL sea válida
    private boolean isValidUrl(String url) {
        try {
            new URL(url).toURI(); // Intentar convertir la cadena en una URL válida
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Método para convertir una URL de imagen a Base64
    private String urlToBase64(String imageUrl) {
        try {
            InputStream inputStream = new URL(imageUrl).openStream();
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();

            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String imageToBase64(Uri imageUri) {
        try {
            // Leer la imagen desde la URI
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

            // Comprimir el Bitmap en un arreglo de bytes
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();

            // Convertir el arreglo de bytes a Base64
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Método para guardar los datos después de verificar el número de teléfono.
     * Solo ciframos la dirección y el número de teléfono antes de guardarlos.
     */
    private void saveData() {
        // Recoger los datos del usuario
        String name = editTextName.getText().toString();
        String email = ((EditText) findViewById(R.id.editTextTextEmail)).getText().toString();
        String address = editTextAddress.getText().toString();
        String phoneNumber = editTextNumberPhone.getText().toString();

        // Validar que el nombre no esté vacío
        if (name.isEmpty()) {
            Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar el número de teléfono
        if (!verifyPhoneNumber(phoneNumber)) {
            return;
        }

        // Obtener el UID del usuario autenticado
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "No hay usuario autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        String userUid = user.getUid();
        String countryCode = countryCodePicker.getSelectedCountryCodeWithPlus();
        String fullPhoneNumber = countryCode + phoneNumber;

        // Cifrar los datos de dirección y teléfono
        String encryptedAddress = KeystoreManager.encrypt(address);
        String encryptedPhone = KeystoreManager.encrypt(fullPhoneNumber);

        // Guardar los datos en la base de datos
        saveUserDataToDatabase(userUid, name, email, encryptedAddress, encryptedPhone, currentImageBase64);

        // Mostrar un mensaje de éxito
        Toast.makeText(this, "Datos guardados correctamente", Toast.LENGTH_SHORT).show();

        // Redirigir a MainActivity
        Intent intent = new Intent(EditUserActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void saveUserDataToDatabase(String userUid, String name, String email, String encryptedAddress,
                                        String encryptedPhone, String photoBase64) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USER_NAME, name);
        values.put(DatabaseHelper.COLUMN_USER_EMAIL, email);
        values.put(DatabaseHelper.COLUMN_USER_ADDRESS, encryptedAddress);
        values.put(DatabaseHelper.COLUMN_USER_PHONE, encryptedPhone);
        values.put(DatabaseHelper.COLUMN_USER_IMAGE_URL, photoBase64); // Guardar la imagen en Base64

        Cursor cursor = db.query(DatabaseHelper.TABLE_USERS, new String[]{DatabaseHelper.COLUMN_USER_UID},
                DatabaseHelper.COLUMN_USER_UID + " = ?", new String[]{userUid}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int rowsUpdated = db.update(DatabaseHelper.TABLE_USERS, values, DatabaseHelper.COLUMN_USER_UID + " = ?", new String[]{userUid});
            Log.d("DATABASE", "Filas actualizadas: " + rowsUpdated);
        } else {
            values.put(DatabaseHelper.COLUMN_USER_UID, userUid);
            long newRowId = db.insert(DatabaseHelper.TABLE_USERS, null, values);
            Log.d("DATABASE", "Usuario insertado con ID: " + newRowId);
        }
        if (cursor != null) {
            cursor.close();
        }
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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        String userUid = user.getUid();
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] projection = {
                DatabaseHelper.COLUMN_USER_NAME,
                DatabaseHelper.COLUMN_USER_ADDRESS,
                DatabaseHelper.COLUMN_USER_PHONE,
                DatabaseHelper.COLUMN_USER_IMAGE_URL // Columna que contiene la imagen en Base64
        };

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                projection,
                DatabaseHelper.COLUMN_USER_UID + " = ?",
                new String[]{userUid},
                null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_NAME));
            String encryptedAddress = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ADDRESS));
            String encryptedPhone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_PHONE));
            String imageBase64 = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_IMAGE_URL)); // Imagen en Base64

            if (name != null) {
                editTextName.setText(name);
            }

            String decryptedAddress = KeystoreManager.decrypt(encryptedAddress);
            String decryptedPhone = KeystoreManager.decrypt(encryptedPhone);
            editTextAddress.setText(decryptedAddress);
            configureCountryCodePickerAndPhoneNumber(decryptedPhone);

            // Decodificar y mostrar la imagen en Base64
            if (imageBase64 != null && !imageBase64.isEmpty()) {
                byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                ImageView imageView = findViewById(R.id.imageView);
                imageView.setImageBitmap(decodedBitmap);
            }

            cursor.close();
        } else {
            Log.d("DATABASE", "No se encontró el usuario en la base de datos");
        }
    }

    private void configureCountryCodePickerAndPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return;
        }

        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        try {
            // Analizar el número de teléfono
            Phonenumber.PhoneNumber parsedNumber = phoneNumberUtil.parse(phoneNumber, "");

            // Obtener el código de región (por ejemplo, "ES" para España)
            String regionCode = phoneNumberUtil.getRegionCodeForNumber(parsedNumber);

            // Configurar el CountryCodePicker con el código de región
            countryCodePicker.setCountryForNameCode(regionCode);

            // Extraer el número nacional (sin el prefijo)
            String nationalNumber = String.valueOf(parsedNumber.getNationalNumber());

            // Mostrar el número nacional en el campo EditText
            editTextNumberPhone.setText(nationalNumber);

        } catch (NumberParseException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al parsear el número de teléfono", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera(); // Si se concede el permiso de cámara, abrir la cámara
        }
    }

}