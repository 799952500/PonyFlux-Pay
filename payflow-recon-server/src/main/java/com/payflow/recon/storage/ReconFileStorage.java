package com.payflow.recon.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;

/**
 * 对账原始文件存储（本地目录或 S3 兼容对象存储）。
 *
 * @author PayFlow Team
 */
public interface ReconFileStorage {

    /**
     * 上传本地文件，返回存储键（由实现解释）。
     */
    String put(Path localFile, String logicalName) throws IOException;

    /**
     * 读取已存储文件。
     */
    InputStream open(String storageKey) throws IOException;

    /**
     * 生成短期下载 URL（S3 预签名）；本地实现可返回 null。
     */
    String presignGet(String storageKey, Duration ttl);
}
