package com.payflow.admin.service;

/**
 * 存量数据隔离分类扫描。
 */
public interface DataIsolationInventoryScanService {

    /**
     * 执行全量扫描并刷新检查项统计。
     *
     * @return 更新或新增的检查项数量
     */
    int runFullScan();
}
