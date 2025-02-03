package edu.pmdm.martinez_albertoimdbapp.utils;

import android.util.Base64;
import java.nio.charset.StandardCharsets;

public class KeystoreManager {

    /**
     * Cifra un texto utilizando Base64
     * @param plainText El texto sin cifrar
     * @return El texto cifrado en Base64
     */
    public static String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        byte[] encryptedBytes = Base64.encode(plainText.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
        return new String(encryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * Desencripta un texto cifrado en Base64
     * @param encryptedText El texto cifrado en Base64
     * @return El texto desencriptado
     */
    public static String decrypt(String encryptedText) {
        if (encryptedText == null) {
            return null;
        }
        byte[] decodedBytes = Base64.decode(encryptedText, Base64.DEFAULT);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }
}

