package com.payflow.recon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件下载地址响应。
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "对账文件下载")
public class FileUrlResponse {

    @Schema(description = "对象存储预签名 URL，可能为空")
    private String presignedUrl;

    @Schema(description = "对账服务内部下载路径（需带内部令牌）")
    private String internalDownloadPath;
}
