package com.oasystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oasystem.entity.FormTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 表单模板 Mapper
 */
@Mapper
public interface FormTemplateMapper extends BaseMapper<FormTemplate> {

    /**
     * 根据编码查询表单模板
     */
    @Select("SELECT * FROM oa_form_template WHERE code = #{code} AND status = 1 LIMIT 1")
    FormTemplate selectByCode(String code);
}
