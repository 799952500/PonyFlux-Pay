package com.payflow.recon.service;

import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.payflow.recon.config.ReconProperties;
import com.payflow.recon.entity.ReconBillRecord;
import com.payflow.recon.entity.ReconDiff;
import com.payflow.recon.mapper.ReconBillRecordMapper;
import com.payflow.recon.mapper.ReconDiffMapper;
import com.payflow.recon.mapper.cashier.CashierReconPaymentMapper;
import com.payflow.recon.mapper.cashier.CashierReconPaymentRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReconCompareService 四类差异")
class ReconCompareServiceTest {

    private static final String TASK_ID = "task-compare-1";
    private static final LocalDate BILL_DATE = LocalDate.of(2026, 1, 15);

    @Mock
    private ReconBillRecordMapper reconBillRecordMapper;
    @Mock
    private ReconDiffMapper reconDiffMapper;
    @Mock
    private CashierReconPaymentMapper cashierReconPaymentMapper;
    @Mock
    private ReconProperties reconProperties;

    @InjectMocks
    private ReconCompareService reconCompareService;

    @BeforeEach
    void setUp() {
        when(reconProperties.getBatchSize()).thenReturn(500);
    }

    @Test
    @DisplayName("CHANNEL_ONLY：渠道有、本地无")
    void channelOnlyDiff() {
        when(reconBillRecordMapper.selectList(any())).thenReturn(List.of(
                bill("TXN_CH", 1000L, "成功")));
        when(cashierReconPaymentMapper.listSuccessByBillDate(any(), any(), any())).thenReturn(List.of());

        try (MockedStatic<Db> db = mockStatic(Db.class)) {
            db.when(() -> Db.saveBatch(any(), anyInt())).thenReturn(true);
            int count = reconCompareService.compareAndPersist(TASK_ID, "alipay", BILL_DATE);
            assertEquals(1, count);
            assertDiffType(db, ReconCompareService.DIFF_CHANNEL_ONLY);
        }
    }

    @Test
    @DisplayName("LOCAL_ONLY：本地有、渠道无")
    void localOnlyDiff() {
        when(reconBillRecordMapper.selectList(any())).thenReturn(List.of());
        when(cashierReconPaymentMapper.listSuccessByBillDate(any(), any(), any())).thenReturn(List.of(
                local("TXN_LOC", "ORD-1", "M001", 2000L, "SUCCESS")));

        try (MockedStatic<Db> db = mockStatic(Db.class)) {
            db.when(() -> Db.saveBatch(any(), anyInt())).thenReturn(true);
            int count = reconCompareService.compareAndPersist(TASK_ID, "alipay", BILL_DATE);
            assertEquals(1, count);
            assertDiffType(db, ReconCompareService.DIFF_LOCAL_ONLY);
        }
    }

    @Test
    @DisplayName("AMOUNT_MISMATCH：金额不一致")
    void amountMismatchDiff() {
        when(reconBillRecordMapper.selectList(any())).thenReturn(List.of(
                bill("TXN_AMT", 1000L, "成功")));
        when(cashierReconPaymentMapper.listSuccessByBillDate(any(), any(), any())).thenReturn(List.of(
                local("TXN_AMT", "ORD-2", "M002", 999L, "SUCCESS")));

        try (MockedStatic<Db> db = mockStatic(Db.class)) {
            db.when(() -> Db.saveBatch(any(), anyInt())).thenReturn(true);
            int count = reconCompareService.compareAndPersist(TASK_ID, "alipay", BILL_DATE);
            assertEquals(1, count);
            assertDiffType(db, ReconCompareService.DIFF_AMOUNT_MISMATCH);
        }
    }

    @Test
    @DisplayName("STATUS_MISMATCH：状态不一致")
    void statusMismatchDiff() {
        when(reconBillRecordMapper.selectList(any())).thenReturn(List.of(
                bill("TXN_ST", 500L, "成功")));
        when(cashierReconPaymentMapper.listSuccessByBillDate(any(), any(), any())).thenReturn(List.of(
                local("TXN_ST", "ORD-3", "M003", 500L, "PROCESSING")));

        try (MockedStatic<Db> db = mockStatic(Db.class)) {
            db.when(() -> Db.saveBatch(any(), anyInt())).thenReturn(true);
            int count = reconCompareService.compareAndPersist(TASK_ID, "alipay", BILL_DATE);
            assertEquals(1, count);
            assertDiffType(db, ReconCompareService.DIFF_STATUS_MISMATCH);
        }
    }

    private static ReconBillRecord bill(String tradeNo, long fen, String status) {
        return ReconBillRecord.builder()
                .taskId(TASK_ID)
                .channelTradeNo(tradeNo)
                .amountFen(fen)
                .channelStatus(status)
                .parseError(false)
                .build();
    }

    private static CashierReconPaymentRow local(String txn, String orderId, String merchantId, long amount, String status) {
        CashierReconPaymentRow row = new CashierReconPaymentRow();
        row.setChannelTransactionId(txn);
        row.setOrderId(orderId);
        row.setMerchantId(merchantId);
        row.setAmount(amount);
        row.setStatus(status);
        return row;
    }

    @SuppressWarnings("unchecked")
    private static void assertDiffType(MockedStatic<Db> db, String expectedType) {
        ArgumentCaptor<List<ReconDiff>> captor = ArgumentCaptor.forClass(List.class);
        db.verify(() -> Db.saveBatch(captor.capture(), eq(500)));
        assertTrue(captor.getValue().stream().anyMatch(d -> expectedType.equals(d.getDiffType())));
    }
}
