package com.payflow.payment.union;

import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

/**
 * 银联 RSA-SHA256 签名工具。
 * <p>
 * 签名流程：字段按 key ASCII 升序排序 → 拼接 "key=value&..." → SHA256 摘要 →
 * 商户私钥 RSA 签名 → Base64 编码。
 * </p>
 *
 * @author PayFlow Team
 */
@Slf4j
public final class UnionPaySignature {

    private UnionPaySignature() {
    }

    // ---- 签名 ----

    /**
     * 对参数 Map 计算 RSA-SHA256 签名。
     *
     * @param params         待签名参数（不含 signature 字段本身）
     * @param pfxPath        .pfx 证书文件路径
     * @param pfxPassword    证书密码
     * @return Base64 编码的签名字符串
     */
    public static String sign(Map<String, String> params, String pfxPath, String pfxPassword) {
        String signPlain = buildSignPlain(params);
        log.debug("银联签名原文: {}", signPlain);
        try {
            byte[] sha256 = sha256(signPlain);
            byte[] signed = rsaSign(sha256, loadPrivateKey(pfxPath, pfxPassword));
            return Base64.getEncoder().encodeToString(signed);
        } catch (Exception e) {
            throw new RuntimeException("银联签名失败", e);
        }
    }

    // ---- 验签 ----

    /**
     * 验证银联回调签名。
     *
     * @param params          通知参数（含 signature 字段）
     * @param publicKeyPath   银联公钥文件路径（.cer）
     * @return true=验签通过
     */
    public static boolean verify(Map<String, String> params, String publicKeyPath) {
        String signature = params.get("signature");
        if (signature == null || signature.isEmpty()) {
            log.error("银联通知缺少 signature 字段");
            return false;
        }
        // 移除 signature 字段后重新拼接
        TreeMap<String, String> sorted = new TreeMap<>(params);
        sorted.remove("signature");
        String signPlain = buildSignPlain(sorted);
        try {
            byte[] sha256 = sha256(signPlain);
            byte[] sigBytes = Base64.getDecoder().decode(signature);
            return rsaVerify(sha256, sigBytes, loadPublicKey(publicKeyPath));
        } catch (Exception e) {
            log.error("银联通知验签异常", e);
            return false;
        }
    }

    // ---- 工具方法 ----

    /**
     * 按 key ASCII 升序拼接签名原文: key1=val1&key2=val2&...
     */
    static String buildSignPlain(Map<String, String> params) {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder(512);
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return sb.toString();
    }

    static byte[] sha256(String plain) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(plain.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 不可用", e);
        }
    }

    // ---- 密钥加载 ----

    static PrivateKey loadPrivateKey(String pfxPath, String password) throws Exception {
        try (InputStream is = new FileInputStream(pfxPath)) {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(is, password.toCharArray());
            String alias = ks.aliases().nextElement();
            return (PrivateKey) ks.getKey(alias, password.toCharArray());
        }
    }

    static PublicKey loadPublicKey(String cerPath) throws Exception {
        try (InputStream is = new FileInputStream(cerPath)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(is);
            return cert.getPublicKey();
        }
    }

    // ---- RSA 操作 ----

    static byte[] rsaSign(byte[] digest, PrivateKey privateKey) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(privateKey);
        sig.update(digest);
        return sig.sign();
    }

    static boolean rsaVerify(byte[] digest, byte[] signature, PublicKey publicKey) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(publicKey);
        sig.update(digest);
        return sig.verify(signature);
    }
}
