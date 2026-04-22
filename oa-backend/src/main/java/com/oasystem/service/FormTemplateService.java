package com.oasystem.service;

import com.oasystem.dto.FormTemplateCreateRequest;
import com.oasystem.dto.FormTemplateUpdateRequest;
import com.oasystem.dto.PageResult;
import com.oasystem.entity.FormTemplate;

import java.util.List;

/**
 * 表单模板服务接口
 */
public interface FormTemplateService {

    /**
     * 查询所有启用的表单模板列表
     */
    List<FormTemplate> listAllTemplates();

    /**
     * 分页查询表单模板列表
     */
    PageResult<FormTemplate> listTemplates(Integer current, Integer size);

    /**
     * 根据ID查询表单模板详情
     */
    FormTemplate getTemplateById(Long id);

    /**
     * 根据编码查询表单模板
     */
    FormTemplate getTemplateByCode(String code);

    /**
     * 创建表单模板
     */
    Long createTemplate(FormTemplateCreateRequest request);

    /**
     * 更新表单模板
     */
    Boolean updateTemplate(Long id, FormTemplateUpdateRequest request);

    /**
     * 删除表单模板
     */
    Boolean deleteTemplate(Long id);
}
