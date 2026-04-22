package com.oasystem.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oasystem.dto.FormTemplateCreateRequest;
import com.oasystem.dto.FormTemplateUpdateRequest;
import com.oasystem.dto.PageResult;
import com.oasystem.entity.FormTemplate;
import com.oasystem.exception.BusinessException;
import com.oasystem.mapper.FormTemplateMapper;
import com.oasystem.service.FormTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 表单模板服务实现
 */
@Service
@RequiredArgsConstructor
public class FormTemplateServiceImpl implements FormTemplateService {

    private final FormTemplateMapper formTemplateMapper;

    @Override
    public List<FormTemplate> listAllTemplates() {
        LambdaQueryWrapper<FormTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FormTemplate::getStatus, 1);
        wrapper.orderByAsc(FormTemplate::getId);
        return formTemplateMapper.selectList(wrapper);
    }

    @Override
    public PageResult<FormTemplate> listTemplates(Integer current, Integer size) {
        Page<FormTemplate> page = new Page<>(current, size);
        LambdaQueryWrapper<FormTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(FormTemplate::getCreateTime);
        Page<FormTemplate> result = formTemplateMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), (Long) result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public FormTemplate getTemplateById(Long id) {
        FormTemplate template = formTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("表单模板不存在");
        }
        return template;
    }

    @Override
    public FormTemplate getTemplateByCode(String code) {
        return formTemplateMapper.selectByCode(code);
    }

    @Override
    public Long createTemplate(FormTemplateCreateRequest request) {
        FormTemplate exist = formTemplateMapper.selectOne(
                new LambdaQueryWrapper<FormTemplate>().eq(FormTemplate::getCode, request.getCode()));
        if (exist != null) {
            throw new BusinessException("表单编码已存在");
        }

        FormTemplate template = new FormTemplate();
        BeanUtils.copyProperties(request, template);
        if (request.getFieldsConfig() != null) {
            template.setFieldsConfig(JSON.toJSONString(request.getFieldsConfig()));
        } else {
            template.setFieldsConfig("[]");
        }
        if (template.getStatus() == null) {
            template.setStatus(1);
        }
        template.setCreateTime(LocalDateTime.now());
        formTemplateMapper.insert(template);
        return template.getId();
    }

    @Override
    public Boolean updateTemplate(Long id, FormTemplateUpdateRequest request) {
        FormTemplate template = formTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("表单模板不存在");
        }
        BeanUtils.copyProperties(request, template);
        if (request.getFieldsConfig() != null) {
            template.setFieldsConfig(JSON.toJSONString(request.getFieldsConfig()));
        }
        formTemplateMapper.updateById(template);
        return true;
    }

    @Override
    public Boolean deleteTemplate(Long id) {
        FormTemplate template = formTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("表单模板不存在");
        }
        formTemplateMapper.deleteById(id);
        return true;
    }
}
