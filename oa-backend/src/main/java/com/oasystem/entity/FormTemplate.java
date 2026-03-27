package com.oasystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 表单模板实体类
 * 存储表单设计器的配置信息
 */
@Data
@TableName("oa_form_template")
public class FormTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 表单名称
     */
    private String name;

    /**
     * 表单编码
     */
    private String code;

    /**
     * 表单描述
     */
    private String description;

    /**
     * 字段配置（JSON格式）
     */
    private String fieldsConfig;

    /**
     * 流程配置
     */
    private String flowConfig;

    /**
     * 状态：0禁用，1启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
