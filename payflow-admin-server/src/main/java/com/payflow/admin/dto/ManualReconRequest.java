package com.payflow.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 管理端手动触发对账请求（转发至对账服务）。
 *
 * @author PayFlow Team
 */
@Data
public class ManualReconRequest {

    @NotBlank
    private String reconChannel;

    @NotBlank
    private String accountCode;

    @NotNull
    private LocalDate billDate;
}
