package com.oasystem.config;

import com.alibaba.cola.statemachine.StateMachine;
import com.alibaba.cola.statemachine.StateMachineFactory;
import com.alibaba.cola.statemachine.builder.StateMachineBuilder;
import com.alibaba.cola.statemachine.builder.StateMachineBuilderFactory;
import com.oasystem.enums.ApprovalEvent;
import com.oasystem.enums.ApprovalStatus;
import com.oasystem.statemachine.ApprovalContext;
import com.oasystem.statemachine.ApprovalStateMachineHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * COLA 状态机配置类
 * 定义审批工单的状态流转规则
 * <p>
 * 状态流转图：
 * DRAFT(草稿) --SUBMIT--> PROCESSING(审批中)
 * PROCESSING(审批中) --APPROVE--> APPROVED(已通过)
 * PROCESSING(审批中) --REJECT--> RETURNED(已打回)
 * PROCESSING(审批中) --REVOKE--> DRAFT(草稿)
 * APPROVED(已通过) --REEDIT--> DRAFT(草稿)
 * RETURNED(已打回) --REEDIT--> DRAFT(草稿)
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class StateMachineConfig {

    private final ApprovalStateMachineHelper stateMachineHelper;

    public static final String STATE_MACHINE_ID = "approvalStateMachine";

    /**
     * 创建并配置审批状态机
     * 如果状态机已存在（如测试中），则返回已存在的状态机
     */
    @Bean
    public StateMachine<ApprovalStatus, ApprovalEvent, ApprovalContext> approvalStateMachine() {
        // 首先检查并移除已存在的状态机（解决多测试类运行时上下文刷新问题）
        unregisterExistingStateMachine();

        try {
            // 尝试构建新的状态机
            StateMachineBuilder<ApprovalStatus, ApprovalEvent, ApprovalContext> builder =
                    StateMachineBuilderFactory.create();

            // 1. 草稿 -> 审批中（提交事件）
            builder.externalTransition()
                    .from(ApprovalStatus.DRAFT)
                    .to(ApprovalStatus.PROCESSING)
                    .on(ApprovalEvent.SUBMIT)
                    .when(stateMachineHelper::checkFormComplete)
                    .perform(stateMachineHelper::doSubmit);

            // 2. 审批中 -> 已通过（审批同意事件）
            builder.externalTransition()
                    .from(ApprovalStatus.PROCESSING)
                    .to(ApprovalStatus.APPROVED)
                    .on(ApprovalEvent.APPROVE)
                    .when(stateMachineHelper::checkApproverPermission)
                    .perform(stateMachineHelper::doApprove);

            // 3. 审批中 -> 已打回（审批拒绝事件）
            builder.externalTransition()
                    .from(ApprovalStatus.PROCESSING)
                    .to(ApprovalStatus.RETURNED)
                    .on(ApprovalEvent.REJECT)
                    .when(stateMachineHelper::checkApproverPermission)
                    .perform(stateMachineHelper::doReject);

            // 4. 审批中 -> 草稿（撤销事件）
            builder.externalTransition()
                    .from(ApprovalStatus.PROCESSING)
                    .to(ApprovalStatus.DRAFT)
                    .on(ApprovalEvent.REVOKE)
                    .when(stateMachineHelper::checkIsApplicant)
                    .perform(stateMachineHelper::doRevoke);

            // 5. 已通过 -> 草稿（重新编辑事件）
            builder.externalTransition()
                    .from(ApprovalStatus.APPROVED)
                    .to(ApprovalStatus.DRAFT)
                    .on(ApprovalEvent.REEDIT)
                    .when(stateMachineHelper::checkIsApplicant)
                    .perform(stateMachineHelper::doReedit);

            // 6. 已打回 -> 草稿（重新编辑事件）
            builder.externalTransition()
                    .from(ApprovalStatus.RETURNED)
                    .to(ApprovalStatus.DRAFT)
                    .on(ApprovalEvent.REEDIT)
                    .when(stateMachineHelper::checkIsApplicant)
                    .perform(stateMachineHelper::doReedit);

            // 构建状态机
            StateMachine<ApprovalStatus, ApprovalEvent, ApprovalContext> stateMachine =
                    builder.build(STATE_MACHINE_ID);

            log.info("审批状态机 [{}] 构建完成，包含以下状态转换规则：", STATE_MACHINE_ID);
            log.info("- DRAFT --SUBMIT--> PROCESSING");
            log.info("- PROCESSING --APPROVE--> APPROVED");
            log.info("- PROCESSING --REJECT--> RETURNED");
            log.info("- PROCESSING --REVOKE--> DRAFT");
            log.info("- APPROVED --REEDIT--> DRAFT");
            log.info("- RETURNED --REEDIT--> DRAFT");

            return stateMachine;
        } catch (Exception e) {
            // 状态机已存在，从工厂获取
            log.warn("状态机 [{}] 已存在，从工厂获取已注册实例", STATE_MACHINE_ID);
            return getExistingStateMachine();
        }
    }

    /**
     * 从 COLA StateMachineFactory 获取已注册的状态机
     * 使用反射访问工厂中的 stateMachineMap 字段
     */
    @SuppressWarnings("unchecked")
    private StateMachine<ApprovalStatus, ApprovalEvent, ApprovalContext> getExistingStateMachine() {
        try {
            // COLA 5.0 使用 StateMachineFactory.stateMachineMap 存储已构建的状态机
            Field stateMachineMapField = StateMachineFactory.class.getDeclaredField("stateMachineMap");
            stateMachineMapField.setAccessible(true);
            Map<String, StateMachine<?, ?, ?>> stateMachineMap =
                    (Map<String, StateMachine<?, ?, ?>>) stateMachineMapField.get(null);

            if (stateMachineMap != null) {
                StateMachine<?, ?, ?> machine = stateMachineMap.get(STATE_MACHINE_ID);
                if (machine != null) {
                    log.debug("成功从 StateMachineFactory 获取状态机 [{}]", STATE_MACHINE_ID);
                    return (StateMachine<ApprovalStatus, ApprovalEvent, ApprovalContext>) machine;
                }
            }
        } catch (Exception ex) {
            log.warn("无法从 StateMachineFactory 获取状态机: {}", ex.getMessage());
        }
        throw new RuntimeException("状态机 [" + STATE_MACHINE_ID + "] 已存在但无法获取");
    }

    /**
     * 从 StateMachineFactory 注销已存在的状态机
     * 用于解决多测试类运行时上下文刷新导致的旧状态机引用问题
     */
    @SuppressWarnings("unchecked")
    private void unregisterExistingStateMachine() {
        try {
            Field stateMachineMapField = StateMachineFactory.class.getDeclaredField("stateMachineMap");
            stateMachineMapField.setAccessible(true);
            Map<String, StateMachine<?, ?, ?>> stateMachineMap =
                    (Map<String, StateMachine<?, ?, ?>>) stateMachineMapField.get(null);

            if (stateMachineMap != null && stateMachineMap.containsKey(STATE_MACHINE_ID)) {
                stateMachineMap.remove(STATE_MACHINE_ID);
                log.info("已从 StateMachineFactory 移除旧的状态机 [{}]", STATE_MACHINE_ID);
            }
        } catch (Exception ex) {
            // 忽略异常，可能 Factory 还未初始化
            log.debug("无法从 StateMachineFactory 移除状态机: {}", ex.getMessage());
        }
    }
}
