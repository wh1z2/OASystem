package com.oasystem.config;

import com.alibaba.cola.statemachine.StateMachine;
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
     */
    @Bean
    public StateMachine<ApprovalStatus, ApprovalEvent, ApprovalContext> approvalStateMachine() {
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
    }
}
