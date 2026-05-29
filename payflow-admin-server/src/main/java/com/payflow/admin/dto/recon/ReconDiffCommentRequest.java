package com.payflow.admin.dto.recon;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 工单留言请求。
 */
@Data
public class ReconDiffCommentRequest {

    @NotBlank(message = "content 不能为空")
    @Size(min = 5, message = "content 至少 5 个字符")
    private String content;
}

