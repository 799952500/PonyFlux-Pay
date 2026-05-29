package com.payflow.cashier.config;

import com.payflow.common.crypto.AesEncryptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis TypeHandler：AES-256-GCM 加密字符串自动加解密（收银台库）。
 */
@Slf4j
public class EncryptedStringTypeHandler extends BaseTypeHandler<String> {

    private static volatile String masterKey;

    public EncryptedStringTypeHandler() {
    }

    public static void setMasterKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("加密主密钥不能为空");
        }
        masterKey = key;
        log.info("收银台 EncryptedStringTypeHandler 主密钥已配置");
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String param, JdbcType jdbcType)
            throws SQLException {
        if (masterKey == null) {
            throw new IllegalStateException("EncryptedStringTypeHandler 未配置主密钥");
        }
        ps.setString(i, AesEncryptor.encrypt(param, masterKey));
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
            return ciphertext;
        }
        try {
            return AesEncryptor.decrypt(ciphertext, masterKey);
        } catch (RuntimeException ex) {
            log.debug("字段解密失败，按明文返回: {}", ex.getMessage());
            return ciphertext;
        }
    }
}
