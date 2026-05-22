import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Arrays;

public class CryptoUtils {
    // Demo key -- all peers/loadbalancer/client must use same key
    private static final String SECRET_KEY = "Review3SecretKey!"; // will be hashed to 16 bytes

    private static SecretKeySpec getKey() throws Exception {
        byte[] key = SECRET_KEY.getBytes("UTF-8");
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16); // use first 16 bytes (128-bit AES)
        return new SecretKeySpec(key, "AES");
    }

    public static byte[] encrypt(byte[] data) throws Exception {
        SecretKeySpec key = getKey();
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    public static byte[] decrypt(byte[] encrypted) throws Exception {
        SecretKeySpec key = getKey();
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(encrypted);
    }

    public static String getMD5Hash(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
