package com.payflow.admin.config;

import com.payflow.common.crypto.AesEncryptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis TypeHandler：AES-256-GCM 加密字符串自动加解密。
 *
 * <p>数据库存储密文，Java 对象读写明文。主密钥通过
 * {@link #setMasterKey(String)} 在应用启动时注入。</p>
 *
 * <p>使用方式——实体字段注解：
 * <pre>{@code
 *   @TableField(typeHandler = EncryptedStringTypeHandler.class)
 *   private String appSecret;
 * }</pre>
 *
 * @author PayFlow Team
 */
@Slf4j
public class EncryptedStringTypeHandler extends BaseTypeHandler<String> {

    /** 主密钥持有器，由 Spring 启动时通过 CryptoProperties 注入 */
    private static volatile String masterKey;

    private EncryptedStringTypeHandler() {
        // MyBatis 通过反射创建实例
    }

    /**
     * 设置 AES-256 主密钥（应用启动时调用）。
     *
     * @param key Base64 编码的 256 位密钥
     */
    public static void setMasterKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("加密主密钥不能为空");
        }
        masterKey = key;
        log.info("EncryptedStringTypeHandler 主密钥已配置");
    }

    static String getMasterKey() {
        return masterKey;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String param, JdbcType jdbcType)
            throws SQLException {
        if (masterKey == null) {
            throw new IllegalStateException("EncryptedStringTypeHandler 未配置主密钥，请先调用 setMasterKey()");
        }
        String ciphertext = AesEncryptor.encrypt(param, masterKey);
        ps.setString(i, ciphertext);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return decryptIfNotNull(rs.getString(columnName));
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return decryptIfNotNull(rs.getString(columnIndex));
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return decryptIfNotNull(cs.getString(columnIndex));
    }

    private String decryptIfNotNull(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return null;
        }
        if (masterKey == null) {
            log.warn("主密钥未配置，返回密文原文");
            return ciphertext;
        }
        try {
            return AesEncryptor.decrypt(ciphertext, masterKey);
        } catch (RuntimeException ex) {
            // 兼容演示库/历史数据中的明文密钥（非 AES-GCM 密文）
            log.debug("字段解密失败，按明文返回: {}", ex.getMessage());
            return ciphertext;
        }
    }
}
