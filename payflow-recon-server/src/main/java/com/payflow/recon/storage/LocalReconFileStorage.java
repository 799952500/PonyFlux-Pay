package com.payflow.recon.storage;

import com.payflow.recon.config.ReconProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 本地磁盘存储。
 *
 * @author PayFlow Team
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payflow.recon.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalReconFileStorage implements ReconFileStorage {

    private final ReconProperties reconProperties;

    @Override
    public String put(Path localFile, String logicalName) throws IOException {
        Path base = Path.of(reconProperties.getStorage().getLocalPath()).toAbsolutePath().normalize();
        Files.createDirectories(base);
        String name = (logicalName == null || logicalName.isBlank()) ? "bill" : logicalName;
        Path target = base.resolve(LocalDate.now() + "_" + UUID.randomUUID() + "_" + name);
        Files.copy(localFile, target, StandardCopyOption.REPLACE_EXISTING);
        return target.toAbsolutePath().toString();
    }

    @Override
    public InputStream open(String storageKey) throws IOException {
        return Files.newInputStream(Path.of(storageKey));
    }

    @Override
    public String presignGet(String storageKey, Duration ttl) {
        return null;
    }
}
