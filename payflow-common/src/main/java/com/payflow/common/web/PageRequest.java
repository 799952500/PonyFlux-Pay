package com.payflow.common.web;

/**
 * 统一分页请求参数，强制上限防止超大 pageSize 拖垮数据库。
 */
public final class PageRequest {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private final int page;
    private final int size;

    private PageRequest(int page, int size) {
        this.page = page;
        this.size = size;
    }

    /**
     * 从客户端入参构建分页请求，page 最小为 1，size 裁剪至 {@link #MAX_SIZE}。
     */
    public static PageRequest of(Integer page, Integer size) {
        int p = page == null || page < 1 ? DEFAULT_PAGE : page;
        int s = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return new PageRequest(p, s);
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    /** MyBatis-Plus 使用的 offset（0-based） */
    public long getOffset() {
        return (long) (page - 1) * size;
    }
}
