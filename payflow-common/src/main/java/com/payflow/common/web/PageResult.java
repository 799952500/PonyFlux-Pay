package com.payflow.common.web;

import java.util.Collections;
import java.util.List;

/**
 * 统一分页结果包装。
 */
public final class PageResult<T> {

    private final List<T> list;
    private final long total;
    private final int page;
    private final int size;

    private PageResult(List<T> list, long total, int page, int size) {
        this.list = list == null ? Collections.emptyList() : list;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public static <T> PageResult<T> of(List<T> list, long total, PageRequest pageRequest) {
        return new PageResult<>(list, total, pageRequest.getPage(), pageRequest.getSize());
    }

    public List<T> getList() {
        return list;
    }

    public long getTotal() {
        return total;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }
}
