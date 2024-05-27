import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class Crypt {
    protected KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(1024);
        return keyPairGenerator.generateKeyPair();
    }

    protected PublicKey publicKeyFromBytes(byte[] bytes) throws Exception {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(bytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    protected SecretKey secretKeyKeyFromBytes(byte[] bytes) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(bytes, "AES");
        return (SecretKey) keySpec;
    }

    protected SecretKey generateAesKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        return keyGen.generateKey();
    }

    protected byte[] decodeBase64(String string) {
        return Base64.getDecoder().decode(string);
    }

    protected String encodeBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    protected byte[] decryptRsa(byte[] bytes, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(bytes);
    }

    protected byte[] encryptRsa(byte[] bytes, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(bytes);
    }

    protected byte[] encryptAes(byte[] bytes, SecretKey aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        return cipher.doFinal(bytes);
    }

    protected byte[] decryptAes(byte[] bytes, SecretKey aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        return cipher.doFinal(bytes);
    }
}
