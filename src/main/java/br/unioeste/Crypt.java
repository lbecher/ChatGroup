package br.unioeste;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Crypt {
    protected KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(1024);
        return keyPairGenerator.generateKeyPair();
    }

    protected PublicKey publicKeyFromBytes(byte[] bytes) throws Exception {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(bytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        return publicKey;
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
        byte[] bytes = Base64.getDecoder().decode(string);
        return bytes;
    }

    protected String encodeBase64(byte[] bytes) {
        String string = Base64.getEncoder().encodeToString(bytes);
        return string;
    }

    protected byte[] decryptRsa(byte[] bytes, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] decryptedBytes = cipher.doFinal(bytes);
        return decryptedBytes;
    }

    protected byte[] encryptRsa(byte[] bytes, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] encryptedBytes = cipher.doFinal(bytes);
        return encryptedBytes;
    }

    protected byte[] encryptAes(byte[] bytes, SecretKey aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);

        byte[] encryptedBytes = cipher.doFinal(bytes);
        return encryptedBytes;
    }

    protected byte[] decryptAes(byte[] bytes, SecretKey aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        
        byte[] decryptedBytes = cipher.doFinal(bytes);
        return decryptedBytes;
    }
}
