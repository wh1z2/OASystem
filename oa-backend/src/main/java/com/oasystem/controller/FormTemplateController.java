package com.oasystem.controller;

import com.oasystem.dto.FormTemplateCreateRequest;
import com.oasystem.dto.FormTemplateUpdateRequest;
import com.oasystem.dto.PageResult;
import com.oasystem.dto.Result;
import com.oasystem.entity.FormTemplate;
import com.oasystem.service.FormTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 表单模板管理控制器
 */
@RestController
@RequestMapping("/form-templates")
@RequiredArgsConstructor
public class FormTemplateController {

    private final FormTemplateService formTemplateService;

    /**
     * 查询所有启用的表单模板列表
     */
    @GetMapping("/all")
    @PreAuthorize("isAuthenticated()")
    public Result<List<FormTemplate>> listAllTemplates() {
        List<FormTemplate> templates = formTemplateService.listAllTemplates();
        return Result.success(templates);
    }

    /**
     * 分页查询表单模板列表
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<FormTemplate>> listTemplates(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        PageResult<FormTemplate> result = formTemplateService.listTemplates(current, size);
        return Result.success(result);
    }

    /**
     * 根据ID查询表单模板详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<FormTemplate> getTemplateById(@PathVariable Long id) {
        FormTemplate template = formTemplateService.getTemplateById(id);
        return Result.success(template);
    }

    /**
     * 根据编码查询表单模板
     */
    @GetMapping("/code/{code}")
    @PreAuthorize("isAuthenticated()")
    public Result<FormTemplate> getTemplateByCode(@PathVariable String code) {
        FormTemplate template = formTemplateService.getTemplateByCode(code);
        if (template == null) {
            return Result.success(null);
        }
        return Result.success(template);
    }

    /**
     * 创建表单模板
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('form_design', 'all')")
    public Result<Long> createTemplate(@RequestBody @Valid FormTemplateCreateRequest request) {
        Long id = formTemplateService.createTemplate(request);
        return Result.success(id);
    }

    /**
     * 更新表单模板
     */
    @PostMapping("/{id}/update")
    @PreAuthorize("hasAnyAuthority('form_design', 'all')")
    public Result<Boolean> updateTemplate(@PathVariable Long id, @RequestBody @Valid FormTemplateUpdateRequest request) {
        Boolean success = formTemplateService.updateTemplate(id, request);
        return Result.success(success);
    }

    /**
     * 删除表单模板
     */
    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAnyAuthority('form_design', 'all')")
    public Result<Boolean> deleteTemplate(@PathVariable Long id) {
        Boolean success = formTemplateService.deleteTemplate(id);
        return Result.success(success);
    }
}
