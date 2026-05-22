package com.payflow.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.entity.ChurnAlert;
import com.payflow.admin.mapper.ChurnAlertMapper;
import com.payflow.admin.mapper.MerchantMapper;
import com.payflow.admin.mapper.cashier.OrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ChurnAlertService 单元测试
 * 验证流失检测算法（7d vs 前7d，下降 50%/70%/90% 分别触发 yellow/orange/red）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChurnAlertService 测试")
class ChurnAlertServiceTest {

    @Mock
    private ChurnAlertMapper churnAlertMapper;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private MerchantMapper merchantMapper;

    private ChurnAlertService service;

    @BeforeEach
    void setUp() {
        service = new ChurnAlertService(churnAlertMapper, merchantMapper, orderMapper);
    }

    @Test
    @DisplayName("下降55%触发黄色预警")
    void detectYellowAlert() {
        mockOrderCounts(4.5, 10.0); // 当前日均4.5, 基线日均10 → 下降55%

        when(churnAlertMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(0L); // 无已存在预警
        when(churnAlertMapper.insert(any(ChurnAlert.class)))
                .thenReturn(1);

        int count = service.detectChurn();

        assertEquals(1, count);
        ArgumentCaptor<ChurnAlert> captor = ArgumentCaptor.forClass(ChurnAlert.class);
        verify(churnAlertMapper).insert(captor.capture());
        assertEquals("yellow", captor.getValue().getAlertLevel());
        assertTrue(captor.getValue().getDeclinePct().doubleValue() > 50);
    }

    @Test
    @DisplayName("下降75%触发橙色预警")
    void detectOrangeAlert() {
        mockOrderCounts(2.5, 10.0); // 基线10, 当前2.5 → 下降75%

        when(churnAlertMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(0L);
        when(churnAlertMapper.insert(any(ChurnAlert.class)))
                .thenReturn(1);

        int count = service.detectChurn();

        assertEquals(1, count);
        ArgumentCaptor<ChurnAlert> captor = ArgumentCaptor.forClass(ChurnAlert.class);
        verify(churnAlertMapper).insert(captor.capture());
        assertEquals("orange", captor.getValue().getAlertLevel());
    }

    @Test
    @DisplayName("下降95%触发红色预警")
    void detectRedAlert() {
        mockOrderCounts(0.5, 10.0); // 基线10, 当前0.5 → 下降95%

        when(churnAlertMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(0L);
        when(churnAlertMapper.insert(any(ChurnAlert.class)))
                .thenReturn(1);

        int count = service.detectChurn();

        assertEquals(1, count);
        ArgumentCaptor<ChurnAlert> captor = ArgumentCaptor.forClass(ChurnAlert.class);
        verify(churnAlertMapper).insert(captor.capture());
        assertEquals("red", captor.getValue().getAlertLevel());
    }

    @Test
    @DisplayName("下降不足50%不触发预警")
    void noAlertBelowThreshold() {
        mockOrderCounts(10.0, 6.0); // 下降40%，不足50%

        int count = service.detectChurn();

        assertEquals(0, count);
        verify(churnAlertMapper, never()).insert(any(ChurnAlert.class));
    }

    @Test
    @DisplayName("当前日均高于基线不触发预警")
    void noAlertWhenIncrease() {
        mockOrderCounts(10.0, 15.0); // 上涨50%

        int count = service.detectChurn();

        assertEquals(0, count);
        verify(churnAlertMapper, never()).insert(any(ChurnAlert.class));
    }

    @Test
    @DisplayName("已存在pending预警时不重复生成")
    void skipExistingPendingAlert() {
        mockOrderCounts(2.0, 10.0); // 下降80%

        when(churnAlertMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(1L); // 已有pending预警

        int count = service.detectChurn();

        assertEquals(0, count);
        verify(churnAlertMapper, never()).insert(any(ChurnAlert.class));
    }

    @Test
    @DisplayName("统计超时未处理预警数")
    void countOverdueAlerts() {
        when(churnAlertMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(5L);

        int count = service.countOverdueAlerts(48);

        assertEquals(5, count);
    }

    private void mockOrderCounts(double currentAvg, double baselineAvg) {
        when(orderMapper.merchantOrderCountsInRange(
                any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(
                        List.of(Map.of(
                                "merchantId", 1L,
                                "dailyAvg", currentAvg,
                                "consecutiveDays", 3L
                        )),
                        List.of(Map.of(
                                "merchantId", 1L,
                                "dailyAvg", baselineAvg,
                                "consecutiveDays", 1L
                        ))
                );
    }
}
