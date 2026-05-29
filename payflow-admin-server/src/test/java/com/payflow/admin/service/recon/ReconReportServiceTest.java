package com.payflow.admin.service.recon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.admin.dto.recon.ReconReportSubscribeRequest;
import com.payflow.admin.entity.recon.ReconReportSubscriptionEntity;
import com.payflow.admin.mapper.SysUserMapper;
import com.payflow.admin.mapper.recon.ReconReportSnapshotEntityMapper;
import com.payflow.admin.mapper.recon.ReconReportSubscriptionEntityMapper;
import com.payflow.admin.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReconReportService 集成测试")
class ReconReportServiceTest {

    @Mock
    private ReconReportSubscriptionEntityMapper subscriptionMapper;
    @Mock
    private ReconReportSnapshotEntityMapper snapshotMapper;
    @Mock
    private ReconAggregationService aggregationService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SysUserMapper sysUserMapper;

    @Test
    @DisplayName("订阅 upsert 写入 subscriberId")
    void subscribeUpsert() {
        ReconReportService service = new ReconReportService(
                subscriptionMapper, snapshotMapper, aggregationService, notificationService, sysUserMapper,
                new ObjectMapper());
        when(subscriptionMapper.selectOne(any())).thenReturn(null);
        when(subscriptionMapper.insert(org.mockito.ArgumentMatchers.<ReconReportSubscriptionEntity>any()))
                .thenAnswer(inv -> {
            ReconReportSubscriptionEntity e = inv.getArgument(0);
            e.setId(1L);
            return 1;
        });
        ReconReportSubscribeRequest req = new ReconReportSubscribeRequest();
        req.setReportType("WEEKLY");
        req.setScope("OWNED");
        req.setEnabled(true);
        var dto = service.subscribe("admin", req);
        assertEquals("admin", dto.getSubscriberId());
        verify(subscriptionMapper).insert(org.mockito.ArgumentMatchers.<ReconReportSubscriptionEntity>any());
    }
}
