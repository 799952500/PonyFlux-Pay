package com.payflow.cashier.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商户路由信息 DTO
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelRouteDTO {

    private Long routeId;
    private Long channelAccountId;
    private String merchantId;
    private Boolean enabled;
    private Integer priority;
}
