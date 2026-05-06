package com.payflow.recon.storage;

import com.payflow.recon.config.ReconProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

/**
 * S3 兼容存储（MinIO / OSS S3 模式）。
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "payflow.recon.storage.type", havingValue = "s3")
public class S3ReconFileStorage implements ReconFileStorage {

    private final ReconProperties reconProperties;
    private final S3Client s3Client;
    private final S3Presigner presigner;

    public S3ReconFileStorage(ReconProperties reconProperties) {
        this.reconProperties = reconProperties;
        ReconProperties.S3 s3 = reconProperties.getStorage().getS3();
        AwsBasicCredentials creds = AwsBasicCredentials.create(s3.getAccessKey(), s3.getSecretKey());
        var builder = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .region(Region.of(s3.getRegion()));
        if (s3.getEndpoint() != null && !s3.getEndpoint().isBlank()) {
            builder.endpointOverride(java.net.URI.create(s3.getEndpoint()))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(s3.isPathStyleAccess())
                            .build());
        }
        this.s3Client = builder.build();
        var preBuilder = S3Presigner.builder()
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .region(Region.of(s3.getRegion()));
        if (s3.getEndpoint() != null && !s3.getEndpoint().isBlank()) {
            preBuilder.endpointOverride(java.net.URI.create(s3.getEndpoint()));
        }
        this.presigner = preBuilder.build();
    }

    @Override
    public String put(Path localFile, String logicalName) throws IOException {
        ReconProperties.S3 s3 = reconProperties.getStorage().getS3();
        String key = "recon/" + LocalDate.now() + "/" + UUID.randomUUID() + "_" + logicalName;
        s3Client.putObject(
                PutObjectRequest.builder().bucket(s3.getBucket()).key(key).build(),
                RequestBody.fromBytes(Files.readAllBytes(localFile)));
        log.info("对账文件已上传 S3: bucket={}, key={}", s3.getBucket(), key);
        return "s3:" + s3.getBucket() + ":" + key;
    }

    @Override
    public InputStream open(String storageKey) throws IOException {
        String[] bk = parseS3Key(storageKey);
        return s3Client.getObject(GetObjectRequest.builder().bucket(bk[0]).key(bk[1]).build());
    }

    @Override
    public String presignGet(String storageKey, Duration ttl) {
        String[] bk = parseS3Key(storageKey);
        GetObjectPresignRequest pre = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(g -> g.bucket(bk[0]).key(bk[1]))
                .build();
        return presigner.presignGetObject(pre).url().toString();
    }

    private static String[] parseS3Key(String storageKey) {
        if (storageKey == null || !storageKey.startsWith("s3:")) {
            throw new IllegalArgumentException("非法 S3 存储键: " + storageKey);
        }
        String rest = storageKey.substring(3);
        int colon = rest.indexOf(':');
        if (colon < 1) {
            throw new IllegalArgumentException("非法 S3 存储键: " + storageKey);
        }
        return new String[]{rest.substring(0, colon), rest.substring(colon + 1)};
    }
}
