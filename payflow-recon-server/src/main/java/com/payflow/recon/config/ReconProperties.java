package com.payflow.recon.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 对账服务配置。
 *
 * @author PayFlow Team
 */
@Data
@Component
@ConfigurationProperties(prefix = "payflow.recon")
public class ReconProperties {

    /** 内部 API 令牌（与 admin 调用方一致） */
    private String internalToken = "";

    private Poller poller = new Poller();

    private MerchantPoller merchantPoller = new MerchantPoller();

    private Storage storage = new Storage();

    @Data
    public static class Poller {
        private boolean enabled = true;
        private long fixedDelayMs = 5000L;
    }

    @Data
    public static class MerchantPoller {
        private boolean enabled = true;
        private long fixedDelayMs = 5000L;
    }

    @Data
    public static class Storage {
        /** local | s3 */
        private String type = "local";
        private String localPath = "./data/recon-files";
        private S3 s3 = new S3();
    }

    @Data
    public static class S3 {
        private String endpoint = "";
        private String region = "us-east-1";
        private String bucket = "";
        private String accessKey = "";
        private String secretKey = "";
        private boolean pathStyleAccess = true;
    }
}
