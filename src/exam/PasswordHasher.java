package exam;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Salted PBKDF2 password storage; plaintext passwords are never saved. */
public final class PasswordHasher {
    private static final int ITERATIONS = 600_000;
    private static final SecureRandom RANDOM = new SecureRandom();
    private PasswordHasher() { }
    public static String hash(char[] password) {
        byte[] salt = new byte[16]; RANDOM.nextBytes(salt);
        return "pbkdf2-sha256$" + ITERATIONS + "$" + encode(salt) + "$" + encode(derive(password, salt, ITERATIONS));
    }
    public static boolean verify(char[] password, String stored) {
        try {
            String[] parts = stored.split("\\$", -1);
            if (parts.length != 4 || !"pbkdf2-sha256".equals(parts[0])) return false;
            int rounds = Integer.parseInt(parts[1]);
            if (rounds < 100_000 || rounds > 1_000_000) return false;
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            if (salt.length != 16 || expected.length != 32) return false;
            return MessageDigest.isEqual(expected, derive(password, salt, rounds));
        } catch (IllegalArgumentException e) { return false; }
    }
    private static String encode(byte[] data) { return Base64.getEncoder().encodeToString(data); }
    private static byte[] derive(char[] password, byte[] salt, int rounds) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, rounds, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (GeneralSecurityException e) { throw new IllegalStateException("Password hashing is unavailable.", e); }
        finally { spec.clearPassword(); }
    }
}
