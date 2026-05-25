package com.payflow.common.crypto;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM 加解密工具类，用于敏感数据（商户密钥、证书密码等）的加密存储。
 *
 * <p>算法：AES/GCM/NoPadding（256 位密钥，12 字节 IV，128 位 Tag）。</p>
 *
 * <p>密文格式：Base64(IV + ciphertext + tag)，解密时自动提取 IV。</p>
 *
 * <h3>使用示例</h3>
 * <pre>
 *   // 加密
 *   String masterKey = "dGVzdC1rZXktMzItYnl0ZXMtbG9uZy0xMjM0NTY="; // Base64 编码的 256 位密钥
 *   String encrypted = AesEncryptor.encrypt("my-secret-api-key", masterKey);
 *
 *   // 解密
 *   String decrypted = AesEncryptor.decrypt(encrypted, masterKey);
 * </pre>
 *
 * @author Lucas
 */
@Slf4j
public final class AesEncryptor {

    /** AES-GCM 算法标识 */
    private static final String ALGORITHM = "AES/GCM/NoPadding";

    /** GCM IV 长度（字节） */
    private static final int IV_LENGTH = 12;

    /** GCM Tag 长度（位） */
    private static final int TAG_LENGTH_BITS = 128;

    /** 安全随机数生成器 */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private AesEncryptor() {
        // 工具类禁止实例化
    }

    /**
     * AES-GCM 加密。
     *
     * @param plaintext 明文
     * @param base64Key Base64 编码的 AES-256 密钥
     * @return Base64 编码的密文（含 IV）
     * @throws IllegalArgumentException 参数为空或密钥无效
     */
    public static String encrypt(String plaintext, String base64Key) {
        if (plaintext == null || plaintext.isBlank()) {
            throw new IllegalArgumentException("明文不能为空");
        }
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalArgumentException("密钥不能为空");
        }
        try {
            SecretKeySpec keySpec = decodeKey(base64Key);
            byte[] iv = generateIv();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // 拼接 IV + ciphertext
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("AES-GCM 加密失败", e);
            throw new IllegalStateException("加密失败", e);
        }
    }

    /**
     * AES-GCM 解密。
     *
     * @param ciphertext Base64 编码的密文（含 IV）
     * @param base64Key  Base64 编码的 AES-256 密钥
     * @return 明文
     * @throws IllegalArgumentException 参数为空或密钥无效
     */
    public static String decrypt(String ciphertext, String base64Key) {
        if (ciphertext == null || ciphertext.isBlank()) {
            throw new IllegalArgumentException("密文不能为空");
        }
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalArgumentException("密钥不能为空");
        }
        try {
            SecretKeySpec keySpec = decodeKey(base64Key);
            byte[] combined = Base64.getDecoder().decode(ciphertext);

            // 提取 IV 和密文
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("AES-GCM 解密失败", e);
            throw new IllegalStateException("解密失败", e);
        }
    }

    /**
     * 生成一个新的 AES-256 主密钥（Base64 编码）。
     *
     * @return Base64 编码的 32 字节密钥
     */
    public static String generateMasterKey() {
        byte[] key = new byte[32];
        SECURE_RANDOM.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    /**
     * 校验 Base64 主密钥是否为 AES-256 要求的 32 字节（应用启动时调用）。
     *
     * @param base64Key Base64 编码密钥
     * @throws IllegalArgumentException 解码后长度不是 32 字节
     */
    public static void validateMasterKey(String base64Key) {
        decodeKey(base64Key.trim());
    }

    /**
     * 解码 Base64 密钥为 SecretKeySpec。
     *
     * @param base64Key Base64 编码的密钥
     * @return SecretKeySpec
     * @throws IllegalArgumentException 密钥长度不是 256 位
     */
    private static SecretKeySpec decodeKey(String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "AES-256 密钥长度必须为 32 字节，当前: " + keyBytes.length);
        }
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * 生成随机 IV。
     *
     * @return 12 字节 IV
     */
    private static byte[] generateIv() {
        byte[] iv = new byte[IV_LENGTH];
        SECURE_RANDOM.nextBytes(iv);
        return iv;
    }
}
