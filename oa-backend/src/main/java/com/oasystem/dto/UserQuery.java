package com.oasystem.dto;

import lombok.Data;

/**
 * 用户查询条件DTO
 */
@Data
public class UserQuery {

    /**
     * 用户名/姓名/邮箱模糊查询
     */
    private String keyword;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 状态：0禁用，1启用
     */
    private Integer status;

    /**
     * 当前页码
     */
    private Integer current = 1;

    /**
     * 每页大小
     */
    private Integer size = 10;

    public Integer getPageNum() {
        return current;
    }

    public Integer getPageSize() {
        return size;
    }
}
