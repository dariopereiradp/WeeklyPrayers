package dp.wkp.utils;

import android.util.Base64;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import dp.wkp.R;
import dp.wkp.activities.MainActivity;

/**
 * Aes encryption
 * It uses ECB encryption, that is not actually safe. But, for the purpose of the app, is fine.
 */
public class AESCryptography {

    private static SecretKeySpec secretKey;
    private static final String CHARSET = "ISO_8859_1";

    private static String decryptedString;
    private static String encryptedString;

    public static void setKey(String myKey) {

        MessageDigest sha;
        try {
            byte[] key = myKey.getBytes(CHARSET);
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16); // use only first 128 bit
            secretKey = new SecretKeySpec(key, "AES");

        } catch (Exception e) {
            Toast.makeText(MainActivity.getInstance(), MainActivity.getInstance().getString(R.string.erroChave), Toast.LENGTH_SHORT).show();
        }
    }

    public static String getDecryptedString() {
        return decryptedString;
    }

    public static void setDecryptedString(String decryptedString) {
        AESCryptography.decryptedString = decryptedString;
    }

    public static String getEncryptedString() {
        return encryptedString;
    }

    public static void setEncryptedString(String encryptedString) {
        AESCryptography.encryptedString = encryptedString;
    }

    public static void encrypt(String strToEncrypt) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, UnsupportedEncodingException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS7Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        setEncryptedString(Base64.encodeToString(cipher.doFinal(strToEncrypt.getBytes(CHARSET)), Base64.DEFAULT));
    }

    public static void decrypt(String strToDecrypt) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS7PADDING");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        setDecryptedString(new String(cipher.doFinal(Base64.decode(strToDecrypt.getBytes(CHARSET), Base64.DEFAULT)), CHARSET));
    }
}
