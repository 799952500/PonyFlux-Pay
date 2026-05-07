package com.payflow.recon.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 对账业务号生成。
 *
 * @author PayFlow Team
 */
public final class ReconIdGenerator {

    private static final DateTimeFormatter D = DateTimeFormatter.BASIC_ISO_DATE;

    private ReconIdGenerator() {
    }

    public static String newTaskId() {
        int n = ThreadLocalRandom.current().nextInt(1_000_000, 10_000_000);
        return "REC" + LocalDate.now().format(D) + n;
    }

    /** 商户对账子任务号 */
    public static String newMerchantTaskId() {
        int n = ThreadLocalRandom.current().nextInt(1_000_000, 10_000_000);
        return "MRC" + LocalDate.now().format(D) + n;
    }
}
