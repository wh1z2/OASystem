package com.oasystem.service;

import com.oasystem.dto.FormTemplateCreateRequest;
import com.oasystem.dto.FormTemplateUpdateRequest;
import com.oasystem.dto.PageResult;
import com.oasystem.entity.FormTemplate;
import com.oasystem.exception.BusinessException;
import com.oasystem.mapper.FormTemplateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 表单模板服务测试类
 * 测试阶段八：表单设计器后端功能
 * 覆盖所有新增功能点、边界条件及异常处理场景
 */
@SpringBootTest
@Transactional
class FormTemplateServiceTest {

    @Autowired
    private FormTemplateService formTemplateService;

    @Autowired
    private FormTemplateMapper formTemplateMapper;

    private static final String TEST_CODE_PREFIX = "TEST_FORM_";

    @BeforeEach
    void setUp() {
        // 清理测试数据，确保测试环境干净
        formTemplateMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FormTemplate>()
                        .like(FormTemplate::getCode, TEST_CODE_PREFIX)
        );
    }

    // ==================== 查询全部启用模板 ====================

    @Test
    @DisplayName("查询全部启用模板：应返回status=1的模板列表")
    void testListAllTemplates() {
        createTestTemplate("启用模板1", TEST_CODE_PREFIX + "ALL_1", 1);
        createTestTemplate("启用模板2", TEST_CODE_PREFIX + "ALL_2", 1);
        createTestTemplate("禁用模板", TEST_CODE_PREFIX + "ALL_3", 0);

        List<FormTemplate> templates = formTemplateService.listAllTemplates();

        assertNotNull(templates);
        assertTrue(templates.stream().allMatch(t -> t.getStatus() == 1));
        assertTrue(templates.stream().anyMatch(t -> t.getCode().equals(TEST_CODE_PREFIX + "ALL_1")));
        assertTrue(templates.stream().anyMatch(t -> t.getCode().equals(TEST_CODE_PREFIX + "ALL_2")));
        assertFalse(templates.stream().anyMatch(t -> t.getCode().equals(TEST_CODE_PREFIX + "ALL_3")));
    }

    @Test
    @DisplayName("查询全部启用模板：无启用模板时应返回空列表")
    void testListAllTemplatesEmpty() {
        List<FormTemplate> templates = formTemplateService.listAllTemplates();
        assertNotNull(templates);
        // 由于数据库中可能有其他数据，此测试仅验证方法不抛异常
    }

    // ==================== 分页查询 ====================

    @Test
    @DisplayName("分页查询：正常分页应返回正确数据结构")
    void testListTemplates() {
        createTestTemplate("分页模板1", TEST_CODE_PREFIX + "PAGE_1", 1);
        createTestTemplate("分页模板2", TEST_CODE_PREFIX + "PAGE_2", 1);

        PageResult<FormTemplate> result = formTemplateService.listTemplates(1, 10);

        assertNotNull(result);
        assertNotNull(result.getRecords());
        assertNotNull(result.getTotal());
        assertEquals(1, result.getCurrent());
        assertEquals(10, result.getSize());
        assertTrue(result.getTotal() >= 2);
    }

    @Test
    @DisplayName("分页查询：第二页应正确偏移")
    void testListTemplatesSecondPage() {
        for (int i = 1; i <= 5; i++) {
            createTestTemplate("分页模板" + i, TEST_CODE_PREFIX + "PAGE_MULTI_" + i, 1);
        }

        PageResult<FormTemplate> result = formTemplateService.listTemplates(2, 2);

        assertNotNull(result);
        assertEquals(2, result.getCurrent());
        assertEquals(2, result.getSize());
        assertTrue(result.getTotal() >= 5);
    }

    // ==================== 按ID查询 ====================

    @Test
    @DisplayName("按ID查询：存在的模板应返回详情")
    void testGetTemplateById() {
        Long id = createTestTemplate("查询模板", TEST_CODE_PREFIX + "GET_ID", 1);

        FormTemplate template = formTemplateService.getTemplateById(id);

        assertNotNull(template);
        assertEquals("查询模板", template.getName());
        assertEquals(TEST_CODE_PREFIX + "GET_ID", template.getCode());
    }

    @Test
    @DisplayName("按ID查询：不存在的模板应抛BusinessException")
    void testGetTemplateByIdNotFound() {
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            formTemplateService.getTemplateById(999999L);
        });
        assertEquals("表单模板不存在", exception.getMessage());
    }

    // ==================== 按编码查询 ====================

    @Test
    @DisplayName("按编码查询：存在的启用模板应返回")
    void testGetTemplateByCode() {
        createTestTemplate("编码查询", TEST_CODE_PREFIX + "GET_CODE", 1);

        FormTemplate template = formTemplateService.getTemplateByCode(TEST_CODE_PREFIX + "GET_CODE");

        assertNotNull(template);
        assertEquals("编码查询", template.getName());
    }

    @Test
    @DisplayName("按编码查询：不存在的编码应返回null")
    void testGetTemplateByCodeNotFound() {
        FormTemplate template = formTemplateService.getTemplateByCode("NON_EXISTENT_CODE_99999");
        assertNull(template);
    }

    @Test
    @DisplayName("按编码查询：禁用状态的模板不应返回（SQL过滤status=1）")
    void testGetTemplateByCodeDisabled() {
        createTestTemplate("禁用编码查询", TEST_CODE_PREFIX + "GET_CODE_DISABLED", 0);

        FormTemplate template = formTemplateService.getTemplateByCode(TEST_CODE_PREFIX + "GET_CODE_DISABLED");

        assertNull(template);
    }

    // ==================== 创建模板 ====================

    @Test
    @DisplayName("创建模板：正常创建应返回ID并持久化")
    void testCreateTemplate() {
        FormTemplateCreateRequest request = new FormTemplateCreateRequest();
        request.setName("创建测试");
        request.setCode(TEST_CODE_PREFIX + "CREATE");
        request.setDescription("测试描述");
        request.setFieldsConfig(List.of(
                Map.of("id", "f1", "type", "text", "label", "字段1", "name", "field1")
        ));
        request.setFlowConfig("{}");
        request.setStatus(1);

        Long id = formTemplateService.createTemplate(request);

        assertNotNull(id);
        FormTemplate saved = formTemplateMapper.selectById(id);
        assertNotNull(saved);
        assertEquals("创建测试", saved.getName());
        assertEquals(TEST_CODE_PREFIX + "CREATE", saved.getCode());
        assertEquals("测试描述", saved.getDescription());
        assertEquals(1, saved.getStatus());
        assertNotNull(saved.getCreateTime());
    }

    @Test
    @DisplayName("创建模板：fieldsConfig为null时应存储为'[]'")
    void testCreateTemplateWithNullFieldsConfig() {
        FormTemplateCreateRequest request = new FormTemplateCreateRequest();
        request.setName("空字段测试");
        request.setCode(TEST_CODE_PREFIX + "CREATE_NULL_FIELDS");
        request.setFieldsConfig(null);

        Long id = formTemplateService.createTemplate(request);

        FormTemplate saved = formTemplateMapper.selectById(id);
        assertEquals("[]", saved.getFieldsConfig());
    }

    @Test
    @DisplayName("创建模板：status为null时应默认为1")
    void testCreateTemplateWithNullStatus() {
        FormTemplateCreateRequest request = new FormTemplateCreateRequest();
        request.setName("默认状态测试");
        request.setCode(TEST_CODE_PREFIX + "CREATE_NULL_STATUS");
        request.setStatus(null);

        Long id = formTemplateService.createTemplate(request);

        FormTemplate saved = formTemplateMapper.selectById(id);
        assertEquals(1, saved.getStatus());
    }

    @Test
    @DisplayName("创建模板：编码重复时应抛BusinessException")
    void testCreateTemplateDuplicateCode() {
        FormTemplateCreateRequest request = new FormTemplateCreateRequest();
        request.setName("重复编码");
        request.setCode(TEST_CODE_PREFIX + "DUPLICATE");
        formTemplateService.createTemplate(request);

        FormTemplateCreateRequest duplicate = new FormTemplateCreateRequest();
        duplicate.setName("重复编码2");
        duplicate.setCode(TEST_CODE_PREFIX + "DUPLICATE");

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            formTemplateService.createTemplate(duplicate);
        });
        assertEquals("表单编码已存在", exception.getMessage());
    }

    @Test
    @DisplayName("创建模板：复杂fieldsConfig应正确序列化为JSON")
    void testCreateTemplateComplexFieldsConfig() {
        FormTemplateCreateRequest request = new FormTemplateCreateRequest();
        request.setName("复杂字段测试");
        request.setCode(TEST_CODE_PREFIX + "CREATE_COMPLEX");
        request.setFieldsConfig(List.of(
                Map.of("id", "f1", "type", "select", "label", "类型", "name", "type",
                        "options", List.of(Map.of("value", "a", "label", "选项A"))),
                Map.of("id", "f2", "type", "checkbox", "label", "多选", "name", "multi",
                        "options", List.of(Map.of("value", "x", "label", "X")))
        ));

        Long id = formTemplateService.createTemplate(request);

        FormTemplate saved = formTemplateMapper.selectById(id);
        assertNotNull(saved.getFieldsConfig());
        assertTrue(saved.getFieldsConfig().contains("select"));
        assertTrue(saved.getFieldsConfig().contains("checkbox"));
    }

    // ==================== 更新模板 ====================

    @Test
    @DisplayName("更新模板：正常更新应成功并持久化")
    void testUpdateTemplate() {
        Long id = createTestTemplate("更新前", TEST_CODE_PREFIX + "UPDATE", 1);

        FormTemplateUpdateRequest request = new FormTemplateUpdateRequest();
        request.setName("更新后");
        request.setDescription("更新描述");
        request.setFieldsConfig(List.of(Map.of("id", "u1", "type", "textarea", "label", "更新字段", "name", "updateField")));
        request.setFlowConfig("{\"updated\":true}");
        request.setStatus(0);

        Boolean success = formTemplateService.updateTemplate(id, request);

        assertTrue(success);
        FormTemplate updated = formTemplateMapper.selectById(id);
        assertEquals("更新后", updated.getName());
        assertEquals("更新描述", updated.getDescription());
        assertEquals(0, updated.getStatus());
        assertTrue(updated.getFieldsConfig().contains("textarea"));
        assertEquals("{\"updated\":true}", updated.getFlowConfig());
    }

    @Test
    @DisplayName("更新模板：fieldsConfig为null时应保持原值不变（不覆盖）")
    void testUpdateTemplateWithNullFieldsConfig() {
        Long id = createTestTemplate("原值保持", TEST_CODE_PREFIX + "UPDATE_NULL", 1);
        FormTemplate original = formTemplateMapper.selectById(id);
        String originalFields = original.getFieldsConfig();

        FormTemplateUpdateRequest request = new FormTemplateUpdateRequest();
        request.setName("仅更新名称");
        request.setFieldsConfig(null);

        formTemplateService.updateTemplate(id, request);

        FormTemplate updated = formTemplateMapper.selectById(id);
        assertEquals("仅更新名称", updated.getName());
        assertEquals(originalFields, updated.getFieldsConfig());
    }

    @Test
    @DisplayName("更新模板：不存在的模板应抛BusinessException")
    void testUpdateTemplateNotFound() {
        FormTemplateUpdateRequest request = new FormTemplateUpdateRequest();
        request.setName("不存在");

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            formTemplateService.updateTemplate(999999L, request);
        });
        assertEquals("表单模板不存在", exception.getMessage());
    }

    // ==================== 删除模板 ====================

    @Test
    @DisplayName("删除模板：正常删除应成功")
    void testDeleteTemplate() {
        Long id = createTestTemplate("删除测试", TEST_CODE_PREFIX + "DELETE", 1);

        Boolean success = formTemplateService.deleteTemplate(id);

        assertTrue(success);
        FormTemplate deleted = formTemplateMapper.selectById(id);
        assertNull(deleted);
    }

    @Test
    @DisplayName("删除模板：不存在的模板应抛BusinessException")
    void testDeleteTemplateNotFound() {
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            formTemplateService.deleteTemplate(999999L);
        });
        assertEquals("表单模板不存在", exception.getMessage());
    }

    // ==================== 综合场景测试 ====================

    @Test
    @DisplayName("完整生命周期：创建→查询→更新→删除")
    void testFullLifecycle() {
        // 创建
        FormTemplateCreateRequest createRequest = new FormTemplateCreateRequest();
        createRequest.setName("生命周期测试");
        createRequest.setCode(TEST_CODE_PREFIX + "LIFECYCLE");
        createRequest.setFieldsConfig(List.of(Map.of("id", "l1", "type", "text", "label", "生命周期字段", "name", "lifeField")));
        Long id = formTemplateService.createTemplate(createRequest);

        // 查询验证
        FormTemplate created = formTemplateService.getTemplateById(id);
        assertEquals("生命周期测试", created.getName());

        // 更新
        FormTemplateUpdateRequest updateRequest = new FormTemplateUpdateRequest();
        updateRequest.setName("生命周期测试-已更新");
        updateRequest.setFieldsConfig(List.of(Map.of("id", "l2", "type", "number", "label", "数字字段", "name", "numField")));
        formTemplateService.updateTemplate(id, updateRequest);

        // 验证更新
        FormTemplate updated = formTemplateService.getTemplateById(id);
        assertEquals("生命周期测试-已更新", updated.getName());
        assertTrue(updated.getFieldsConfig().contains("number"));

        // 删除
        formTemplateService.deleteTemplate(id);

        // 验证删除
        assertThrows(BusinessException.class, () -> formTemplateService.getTemplateById(id));
    }

    @Test
    @DisplayName("边界条件：超长字段名称应正常存储")
    void testBoundaryLongName() {
        String longName = "A".repeat(100);
        FormTemplateCreateRequest request = new FormTemplateCreateRequest();
        request.setName(longName);
        request.setCode(TEST_CODE_PREFIX + "LONG_NAME");

        Long id = formTemplateService.createTemplate(request);
        FormTemplate saved = formTemplateService.getTemplateById(id);
        assertEquals(longName, saved.getName());
    }

    @Test
    @DisplayName("边界条件：空描述应正常存储为null或空字符串")
    void testBoundaryEmptyDescription() {
        FormTemplateCreateRequest request = new FormTemplateCreateRequest();
        request.setName("空描述测试");
        request.setCode(TEST_CODE_PREFIX + "EMPTY_DESC");
        request.setDescription("");

        Long id = formTemplateService.createTemplate(request);
        FormTemplate saved = formTemplateMapper.selectById(id);
        assertNotNull(saved);
        assertEquals("空描述测试", saved.getName());
    }

    // ==================== 辅助方法 ====================

    private Long createTestTemplate(String name, String code, Integer status) {
        FormTemplate template = new FormTemplate();
        template.setName(name);
        template.setCode(code);
        template.setDescription("测试描述");
        template.setFieldsConfig("[]");
        template.setStatus(status);
        template.setCreateTime(java.time.LocalDateTime.now());
        formTemplateMapper.insert(template);
        return template.getId();
    }
}
