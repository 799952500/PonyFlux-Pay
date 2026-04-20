package com.payflow.cashier.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 渠道账户信息 DTO（返回给前端，敏感信息脱敏）
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelAccountDTO {

    private Long id;
    private Long channelId;
    private String channelCode;
    private String channelName;
    private String accountCode;
    private String accountName;
    private String status;
    private String remark;

    /** 脱敏后的配置（appId、mchId 等可见，appSecret 隐藏） */
    private Map<String, String> safeConfig;
}
