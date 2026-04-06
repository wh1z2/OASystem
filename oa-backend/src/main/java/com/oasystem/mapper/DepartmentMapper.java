package com.oasystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oasystem.entity.Department;
import org.apache.ibatis.annotations.Mapper;

/**
 * 部门Mapper接口
 */
@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {
}
