package com.oasystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oasystem.dto.FormTemplateCreateRequest;
import com.oasystem.dto.FormTemplateUpdateRequest;
import com.oasystem.entity.FormTemplate;
import com.oasystem.service.FormTemplateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 表单模板控制器集成测试
 * 测试阶段八：表单设计器 REST API 端点及权限控制
 * 覆盖所有端点、权限场景、参数校验及异常处理
 */
@SpringBootTest
@AutoConfigureMockMvc
class FormTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FormTemplateService formTemplateService;

    // ==================== GET /form-templates/all ====================

    @Test
    @DisplayName("未认证用户访问 /form-templates/all 应返回 401")
    void testListAllTemplatesUnauthenticated() throws Exception {
        mockMvc.perform(get("/form-templates/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @WithMockUser
    @DisplayName("已认证用户访问 /form-templates/all 应返回模板列表")
    void testListAllTemplatesAuthenticated() throws Exception {
        FormTemplate template = new FormTemplate();
        template.setId(1L);
        template.setName("请假表单");
        template.setCode("LEAVE_FORM");
        when(formTemplateService.listAllTemplates()).thenReturn(List.of(template));

        mockMvc.perform(get("/form-templates/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("请假表单"))
                .andExpect(jsonPath("$.data[0].code").value("LEAVE_FORM"));
    }

    // ==================== GET /form-templates ====================

    @Test
    @WithMockUser
    @DisplayName("分页查询模板应返回分页结果")
    void testListTemplates() throws Exception {
        com.oasystem.dto.PageResult<FormTemplate> pageResult = com.oasystem.dto.PageResult.of(
                List.of(), 0L, 1, 10
        );
        when(formTemplateService.listTemplates(1, 10)).thenReturn(pageResult);

        mockMvc.perform(get("/form-templates")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(10));
    }

    @Test
    @WithMockUser
    @DisplayName("分页查询使用默认参数")
    void testListTemplatesDefaultParams() throws Exception {
        com.oasystem.dto.PageResult<FormTemplate> pageResult = com.oasystem.dto.PageResult.of(
                List.of(), 0L, 1, 10
        );
        when(formTemplateService.listTemplates(1, 10)).thenReturn(pageResult);

        mockMvc.perform(get("/form-templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== GET /form-templates/{id} ====================

    @Test
    @WithMockUser
    @DisplayName("按ID查询存在的模板应返回详情")
    void testGetTemplateById() throws Exception {
        FormTemplate template = new FormTemplate();
        template.setId(1L);
        template.setName("测试表单");
        when(formTemplateService.getTemplateById(1L)).thenReturn(template);

        mockMvc.perform(get("/form-templates/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("测试表单"));
    }

    @Test
    @WithMockUser
    @DisplayName("按ID查询不存在的模板应抛异常并返回500")
    void testGetTemplateByIdNotFound() throws Exception {
        when(formTemplateService.getTemplateById(999L)).thenThrow(
                new com.oasystem.exception.BusinessException("表单模板不存在")
        );

        mockMvc.perform(get("/form-templates/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("表单模板不存在"));
    }

    // ==================== GET /form-templates/code/{code} ====================

    @Test
    @WithMockUser
    @DisplayName("按编码查询存在的模板应返回详情")
    void testGetTemplateByCode() throws Exception {
        FormTemplate template = new FormTemplate();
        template.setId(1L);
        template.setName("报销表单");
        template.setCode("EXPENSE_FORM");
        when(formTemplateService.getTemplateByCode("EXPENSE_FORM")).thenReturn(template);

        mockMvc.perform(get("/form-templates/code/EXPENSE_FORM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("报销表单"));
    }

    @Test
    @WithMockUser
    @DisplayName("按编码查询不存在的模板应返回data为null")
    void testGetTemplateByCodeNotFound() throws Exception {
        when(formTemplateService.getTemplateByCode("NOT_EXIST")).thenReturn(null);

        mockMvc.perform(get("/form-templates/code/NOT_EXIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    // ==================== POST /form-templates ====================

    @Test
    @WithMockUser(authorities = {"form_design"})
    @DisplayName("有form_design权限的用户可以创建模板")
    void testCreateTemplateWithPermission() throws Exception {
        FormTemplateCreateRequest request = new FormTemplateCreateRequest();
        request.setName("新表单");
        request.setCode("NEW_FORM");
        request.setFieldsConfig(List.of(Map.of("id", "f1", "type", "text", "label", "字段1", "name", "field1")));

        when(formTemplateService.createTemplate(any())).thenReturn(100L);

        mockMvc.perform(post("/form-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(100));
    }

    @Test
    @WithMockUser(authorities = {"all"})
    @DisplayName("有all权限的用户可以创建模板")
    void testCreateTemplateWithAllPermission() throws Exception {
        FormTemplateCreateRequest request = new FormTemplateCreateRequest();
        request.setName("新表单");
        request.setCode("NEW_FORM_ALL");

        when(formTemplateService.createTemplate(any())).thenReturn(101L);

        mockMvc.perform(post("/form-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(authorities = {"apply"})
    @DisplayName("无form_design权限的用户创建模板应返回403")
    void testCreateTemplateWithoutPermission() throws Exception {
        FormTemplateCreateRequest request = new FormTemplateCreateRequest();
        request.setName("新表单");
        request.setCode("NEW_FORM_NO_PERM");

        mockMvc.perform(post("/form-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("未认证用户创建模板应返回401")
    void testCreateTemplateUnauthenticated() throws Exception {
        FormTemplateCreateRequest request = new FormTemplateCreateRequest();
        request.setName("新表单");
        request.setCode("NEW_FORM_UNAUTH");

        mockMvc.perform(post("/form-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @WithMockUser(authorities = {"form_design"})
    @DisplayName("创建模板时名称为空应触发参数校验")
    void testCreateTemplateValidationNameBlank() throws Exception {
        FormTemplateCreateRequest request = new FormTemplateCreateRequest();
        request.setName(""); // 空名称
        request.setCode("VALIDATION_TEST");

        mockMvc.perform(post("/form-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @WithMockUser(authorities = {"form_design"})
    @DisplayName("创建模板时编码为空应触发参数校验")
    void testCreateTemplateValidationCodeBlank() throws Exception {
        FormTemplateCreateRequest request = new FormTemplateCreateRequest();
        request.setName("测试");
        request.setCode(""); // 空编码

        mockMvc.perform(post("/form-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ==================== POST /form-templates/{id}/update ====================

    @Test
    @WithMockUser(authorities = {"form_design"})
    @DisplayName("有form_design权限的用户可以更新模板")
    void testUpdateTemplateWithPermission() throws Exception {
        FormTemplateUpdateRequest request = new FormTemplateUpdateRequest();
        request.setName("更新后名称");
        request.setFieldsConfig(List.of(Map.of("id", "u1", "type", "textarea", "label", "更新字段", "name", "updateField")));

        when(formTemplateService.updateTemplate(eq(1L), any())).thenReturn(true);

        mockMvc.perform(post("/form-templates/1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @WithMockUser(authorities = {"apply"})
    @DisplayName("无form_design权限的用户更新模板应返回403")
    void testUpdateTemplateWithoutPermission() throws Exception {
        FormTemplateUpdateRequest request = new FormTemplateUpdateRequest();
        request.setName("更新名称");

        mockMvc.perform(post("/form-templates/1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @WithMockUser(authorities = {"form_design"})
    @DisplayName("更新模板时名称为空应触发参数校验")
    void testUpdateTemplateValidation() throws Exception {
        FormTemplateUpdateRequest request = new FormTemplateUpdateRequest();
        request.setName(""); // 空名称

        mockMvc.perform(post("/form-templates/1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @WithMockUser(authorities = {"form_design"})
    @DisplayName("更新不存在的模板应返回500业务异常")
    void testUpdateTemplateNotFound() throws Exception {
        FormTemplateUpdateRequest request = new FormTemplateUpdateRequest();
        request.setName("不存在");

        when(formTemplateService.updateTemplate(eq(999L), any())).thenThrow(
                new com.oasystem.exception.BusinessException("表单模板不存在")
        );

        mockMvc.perform(post("/form-templates/999/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("表单模板不存在"));
    }

    // ==================== POST /form-templates/{id}/delete ====================

    @Test
    @WithMockUser(authorities = {"form_design"})
    @DisplayName("有form_design权限的用户可以删除模板")
    void testDeleteTemplateWithPermission() throws Exception {
        when(formTemplateService.deleteTemplate(1L)).thenReturn(true);

        mockMvc.perform(post("/form-templates/1/delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @WithMockUser(authorities = {"all"})
    @DisplayName("有all权限的用户可以删除模板")
    void testDeleteTemplateWithAllPermission() throws Exception {
        when(formTemplateService.deleteTemplate(2L)).thenReturn(true);

        mockMvc.perform(post("/form-templates/2/delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(authorities = {"apply"})
    @DisplayName("无form_design权限的用户删除模板应返回403")
    void testDeleteTemplateWithoutPermission() throws Exception {
        mockMvc.perform(post("/form-templates/1/delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("未认证用户删除模板应返回401")
    void testDeleteTemplateUnauthenticated() throws Exception {
        mockMvc.perform(post("/form-templates/1/delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @WithMockUser(authorities = {"form_design"})
    @DisplayName("删除不存在的模板应返回500业务异常")
    void testDeleteTemplateNotFound() throws Exception {
        when(formTemplateService.deleteTemplate(999L)).thenThrow(
                new com.oasystem.exception.BusinessException("表单模板不存在")
        );

        mockMvc.perform(post("/form-templates/999/delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("表单模板不存在"));
    }

    // ==================== 边界与异常场景 ====================

    @Test
    @WithMockUser(authorities = {"form_design"})
    @DisplayName("创建模板时编码重复应返回500")
    void testCreateTemplateDuplicateCode() throws Exception {
        FormTemplateCreateRequest request = new FormTemplateCreateRequest();
        request.setName("重复编码");
        request.setCode("DUPLICATE_CODE");

        when(formTemplateService.createTemplate(any())).thenThrow(
                new com.oasystem.exception.BusinessException("表单编码已存在")
        );

        mockMvc.perform(post("/form-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("表单编码已存在"));
    }

    @Test
    @WithMockUser(authorities = {"form_design"})
    @DisplayName("创建模板：复杂的fieldsConfig JSON应正确传递")
    void testCreateTemplateComplexJson() throws Exception {
        String complexJson = """
                {
                    "name": "复杂表单",
                    "code": "COMPLEX_FORM",
                    "fieldsConfig": [
                        {"id": "f1", "type": "select", "label": "类型", "name": "type",
                         "options": [{"value": "a", "label": "A"}]},
                        {"id": "f2", "type": "checkbox", "label": "多选", "name": "multi",
                         "options": [{"value": "x", "label": "X"}]}
                    ],
                    "status": 1
                }
                """;

        when(formTemplateService.createTemplate(any())).thenReturn(200L);

        mockMvc.perform(post("/form-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(complexJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser
    @DisplayName("查询端点不需要特殊权限，仅需要认证")
    void testQueryEndpointsRequireAuthenticationOnly() throws Exception {
        when(formTemplateService.listAllTemplates()).thenReturn(List.of());
        when(formTemplateService.listTemplates(anyInt(), anyInt())).thenReturn(
                com.oasystem.dto.PageResult.of(List.of(), 0L, 1, 10)
        );

        // 普通认证用户（无任何特殊authority）应能访问查询接口
        mockMvc.perform(get("/form-templates/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/form-templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
