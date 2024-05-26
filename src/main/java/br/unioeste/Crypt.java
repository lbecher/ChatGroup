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

    public PublicKey decodePublicKeyBase64(String publicKeyBase64) throws Exception {
        // Decodifica a chave pública em Base64 para bytes
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);

        // Cria um X509EncodedKeySpec a partir dos bytes da chave pública
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);

        // Cria um KeyFactory para o algoritmo especificado (por exemplo, RSA)
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        // Gera um objeto PublicKey a partir do X509EncodedKeySpec
        return keyFactory.generatePublic(keySpec);
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

    protected String encryptRsaBase64(SecretKey aesKey, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedAesKey = cipher.doFinal(aesKey.toString().getBytes());
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
