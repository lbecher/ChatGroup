package br.unioeste;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Crypt {
    protected KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(1024);
        return keyPairGenerator.generateKeyPair();
    } 

    protected String decodePublicKeyBase64(String publicKeyBase64) {
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
        return new String(publicKeyBytes);
    }

    protected String encodePublicKeyBase64(PublicKey publicKey) {
        byte[] publicKeyBytes = publicKey.toString().getBytes();
        return Base64.getEncoder().encodeToString(publicKeyBytes);
    }

    protected SecretKey decryptRsaBase64(String encryptedAesKeyBase64, PrivateKey privateKey) throws Exception {
        byte[] encryptedKey = Base64.getDecoder().decode(encryptedAesKeyBase64);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedKey = cipher.doFinal(encryptedKey);
        return new SecretKeySpec(decryptedKey, 0, decryptedKey.length, "AES");
    }

    protected String encryptRsaBase64(String aesKey, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedAesKey = cipher.doFinal(aesKey.getBytes());
        return Base64.getEncoder().encodeToString(encryptedAesKey);
    }

    protected String encryptAes(String string, SecretKey aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        byte[] encryptedBytes = cipher.doFinal(string.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    protected String decryptAes(String string, SecretKey aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        byte[] decodedBytes = Base64.getDecoder().decode(string);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes);
    }
}
