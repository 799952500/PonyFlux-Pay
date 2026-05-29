package com.payflow.admin.service.recon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ReconSlaService 计算测试")
class ReconSlaServiceTest {

    @Test
    @DisplayName("due-soon：剩余时间占比低于阈值")
    void dueSoonWhenRemainingRatioLow() {
        ReconSlaService service = new ReconSlaService(null, null, null, null, null);
        LocalDateTime created = LocalDateTime.now().minusHours(8);
        LocalDateTime due = created.plusHours(10);
        LocalDateTime now = created.plusHours(9);
        assertTrue(service.isDueSoon(now, created, due, new BigDecimal("0.2")));
    }

    @Test
    @DisplayName("due-soon：已过期不算 due-soon")
    void notDueSoonWhenOverdue() {
        ReconSlaService service = new ReconSlaService(null, null, null, null, null);
        LocalDateTime created = LocalDateTime.now().minusHours(10);
        LocalDateTime due = created.plusHours(5);
        LocalDateTime now = LocalDateTime.now();
        assertFalse(service.isDueSoon(now, created, due, new BigDecimal("0.2")));
    }
}
