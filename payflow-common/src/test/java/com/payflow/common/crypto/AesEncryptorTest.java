package com.payflow.common.crypto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AesEncryptorTest {

    @Test
    void shouldEncryptAndDecryptBack() {
        String masterKey = AesEncryptor.generateMasterKey();
        String plainText = "Hello PayFlow!";

        String cipherText = AesEncryptor.encrypt(plainText, masterKey);
        assertNotNull(cipherText);
        assertNotEquals(plainText, cipherText);

        String decrypted = AesEncryptor.decrypt(cipherText, masterKey);
        assertEquals(plainText, decrypted);
    }

    @Test
    void shouldFailWithWrongKey() {
        String key1 = AesEncryptor.generateMasterKey();
        String key2 = AesEncryptor.generateMasterKey();
        String plainText = "sensitive data";

        String cipherText = AesEncryptor.encrypt(plainText, key1);
        assertThrows(Exception.class, () -> AesEncryptor.decrypt(cipherText, key2));
    }

    @Test
    void shouldHandleNonEmptyPlaintext() {
        String masterKey = AesEncryptor.generateMasterKey();
        String cipherText = AesEncryptor.encrypt("test", masterKey);
        String decrypted = AesEncryptor.decrypt(cipherText, masterKey);
        assertEquals("test", decrypted);
    }

    @Test
    void shouldGenerateUniqueMasterKeys() {
        String key1 = AesEncryptor.generateMasterKey();
        String key2 = AesEncryptor.generateMasterKey();
        assertNotEquals(key1, key2);
    }
}
