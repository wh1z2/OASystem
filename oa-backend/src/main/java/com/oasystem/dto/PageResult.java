package com.oasystem.dto;

import lombok.Data;

import java.util.List;

/**
 * 分页查询结果封装
 */
@Data
public class PageResult<T> {

    /**
     * 当前页数据列表（与前端保持一致，使用records）
     */
    private List<T> records;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码（与前端保持一致，使用current）
     */
    private Integer current;

    /**
     * 每页大小（与前端保持一致，使用size）
     */
    private Integer size;

    /**
     * 总页数
     */
    private Integer pages;

    public static <T> PageResult<T> of(List<T> records, Long total, Integer current, Integer size) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setCurrent(current);
        result.setSize(size);
        result.setPages((int) Math.ceil((double) total / size));
        return result;
    }
}
