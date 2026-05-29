package com.payflow.recon.parser;

import com.payflow.recon.entity.ReconBillRecord;
import com.payflow.recon.parser.impl.AlipayBillParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("AlipayBillParser 冒烟")
class AlipayBillParserTest {

  private final AlipayBillParser parser = new AlipayBillParser();

  @Test
  @DisplayName("解析 fixture CSV 生成账单行")
  void parseFixtureCsv() throws Exception {
    var url = getClass().getClassLoader().getResource("fixtures/alipay_bill_sample.csv");
    assertNotNull(url);
    Path csv = Path.of(url.toURI());
    List<ReconBillRecord> records = parser.parse(csv, "task-fixture", "ALIPAY");

    assertFalse(records.isEmpty());
    ReconBillRecord first = records.get(0);
    assertEquals("202601010001", first.getChannelTradeNo());
    assertEquals("ORDER_CH_ONLY", first.getOutTradeNo());
    assertEquals(1000L, first.getAmountFen());
    assertFalse(first.getParseError());
  }
}
