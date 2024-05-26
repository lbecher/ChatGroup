package br.unioeste;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Crypt {
    protected SecretKey generateAesKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        return keyGen.generateKey();
    }

    protected KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(1024);
        return keyPairGenerator.generateKeyPair();
    }

    protected String decodePublicKeyBase64(String publicKeyBase64) {
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
        String publicKeyString = new String(publicKeyBytes);
        System.out.println(publicKeyString);
        return publicKeyString;
    }

    protected String encodePublicKeyBase64(PublicKey publicKey) throws Exception {
        RSAPublicKey p = (RSAPublicKey) publicKey;
        System.out.println(p.toString());
        return Base64.getEncoder().encodeToString(p.getEncoded());
    }

    protected SecretKey decryptRsaBase64(String encryptedAesKeyBase64, PrivateKey privateKey) throws Exception {
        byte[] encryptedKey = Base64.getDecoder().decode(encryptedAesKeyBase64);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedKey = cipher.doFinal(encryptedKey);
        return new SecretKeySpec(decryptedKey, 0, decryptedKey.length, "AES");
    }

    protected String encryptRsaBase64(SecretKey aesKey, String publicKeyBase64) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
        PKCS8EncodedKeySpec publicKeySpec = new PKCS8EncodedKeySpec(publicKeyBytes);
        RSAPublicKey rasPublicKey = (RSAPublicKey) keyFactory.generatePrivate(publicKeySpec);

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, rasPublicKey);
        byte[] encryptedAesKey = cipher.doFinal(aesKey.toString().getBytes());

        System.out.println("Aqui!");

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
