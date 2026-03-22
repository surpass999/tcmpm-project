package cn.gemrun.base.module.declare.service.project.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import javax.annotation.Resource;

import cn.gemrun.base.framework.common.util.json.JsonUtils;
import cn.gemrun.base.module.declare.controller.admin.project.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.project.ProjectDO;
import cn.gemrun.base.module.declare.dal.dataobject.project.ProjectProcessDO;
import cn.gemrun.base.module.declare.dal.dataobject.project.ProcessIndicatorConfigDO;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorDO;
import cn.gemrun.base.module.declare.dal.mysql.project.ProjectMapper;
import cn.gemrun.base.module.declare.dal.mysql.project.ProjectProcessMapper;
import cn.gemrun.base.module.declare.service.project.ProcessFormService;
import cn.gemrun.base.module.declare.service.project.ProcessIndicatorConfigService;
import cn.gemrun.base.module.declare.service.indicator.DeclareIndicatorService;
import cn.gemrun.base.module.declare.service.indicator.DeclareIndicatorValueService;
import cn.gemrun.base.module.declare.enums.ProcessType;
import cn.gemrun.base.module.system.api.user.AdminUserApi;
import cn.gemrun.base.module.system.api.user.dto.AdminUserRespDTO;
import cn.gemrun.base.module.system.dal.dataobject.dict.DictDataDO;
import cn.gemrun.base.module.system.service.dict.DictDataService;
import cn.gemrun.base.module.bpm.service.task.BpmProcessInstanceService;
import cn.gemrun.base.module.bpm.api.event.ProcessStartedEvent;
import cn.gemrun.base.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.gemrun.base.framework.web.core.util.WebFrameworkUtils;
import org.springframework.context.ApplicationEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;

import static cn.gemrun.base.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.gemrun.base.module.declare.enums.ErrorCodeConstants.*;

/**
 * 过程填报 Service 实现类
 *
 * @author Gemini
 */
@Slf4j
@Service
public class ProcessFormServiceImpl implements ProcessFormService {

    /**
     * 过程类型字典类型
     */
    private static final String DICT_TYPE_PROCESS_TYPE = "declare_process_type";

    /**
     * 填报状态字典类型
     */
    private static final String DICT_TYPE_PROCESS_STATUS = "declare_process_status";

    /**
     * 项目状态字典类型
     */
    private static final String DICT_TYPE_PROJECT_STATUS = "declare_project_status";

    @Resource
    private ProjectProcessMapper projectProcessMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private ProcessIndicatorConfigService processIndicatorConfigService;
    @Resource
    private DeclareIndicatorService declareIndicatorService;
    @Resource
    private DeclareIndicatorValueService declareIndicatorValueService;
    @Resource
    private DictDataService dictDataService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private BpmProcessInstanceService bpmProcessInstanceService;
    @Resource
    private cn.gemrun.base.module.bpm.service.business.BpmBusinessTypeService bpmBusinessTypeService;
    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    // ========== 核心方法实现 ==========

    @Override
    public ProcessFormConfigRespVO getFormConfig(Long processId) {
        // 1. 获取过程记录
        ProjectProcessDO process = projectProcessMapper.selectById(processId);
        if (process == null) {
            throw exception(PROJECT_PROCESS_NOT_EXISTS);
        }

        // 2. 获取项目信息
        ProjectDO project = projectMapper.selectById(process.getProjectId());
        if (project == null) {
            throw exception(PROJECT_NOT_EXISTS);
        }

        // 3. 构建响应
        ProcessFormConfigRespVO resp = new ProcessFormConfigRespVO();

        // 3.1 过程记录信息
        resp.setProcessId(processId);
        resp.setProcessType(process.getProcessType());
        resp.setProcessTypeName(getDictText(DICT_TYPE_PROCESS_TYPE, String.valueOf(process.getProcessType())));
        resp.setProcessTitle(process.getProcessTitle());
        resp.setReportPeriodStart(process.getReportPeriodStart());
        resp.setReportPeriodEnd(process.getReportPeriodEnd());

        // 3.2 项目基本信息
        resp.setProjectId(project.getId());
        resp.setProjectName(project.getProjectName());
        resp.setProjectType(project.getProjectType());
        resp.setProjectTypeName(processIndicatorConfigService.getProjectTypeName(project.getProjectType()));
        resp.setProjectLeader(project.getLeaderName());
        // 建设周期
        if (project.getStartTime() != null && project.getPlanEndTime() != null) {
            String period = project.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    + " ~ " + project.getPlanEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            resp.setConstructionPeriod(period);
        }
        resp.setProjectStatus(project.getProjectStatus());
        resp.setProjectStatusName(getDictText(DICT_TYPE_PROJECT_STATUS, project.getProjectStatus()));

        // 3.3 指标配置
        List<ProcessIndicatorConfigDO> configs = processIndicatorConfigService
                .getConfigListByProcessTypeAndProjectType(process.getProcessType(), project.getProjectType());
        List<ProcessFormConfigRespVO.IndicatorFormItem> indicators = buildIndicatorFormItems(configs, process);
        resp.setIndicators(indicators);

        // 3.4 文本字段配置
        resp.setTextFields(getTextFieldConfigs(process.getProcessType()));

        // 3.5 填报数据
        resp.setStatus(process.getStatus());
        resp.setStatusName(getDictText(DICT_TYPE_PROCESS_STATUS, process.getStatus()));
        resp.setCreateName(process.getCreator());
        resp.setCreateTime(process.getCreateTime());
        resp.setProcessData(process.getProcessData());
        resp.setIndicatorValues(process.getIndicatorValues());
        resp.setAttachmentIds(process.getAttachmentIds());
        resp.setIndicatorVersion(process.getVersion());

        return resp;
    }

    @Override
    public ProcessFormDataRespVO getFormData(Long processId) {
        ProjectProcessDO process = projectProcessMapper.selectById(processId);
        if (process == null) {
            throw exception(PROJECT_PROCESS_NOT_EXISTS);
        }

        ProcessFormDataRespVO resp = new ProcessFormDataRespVO();
        resp.setProcessId(processId);
        resp.setFormData(parseJson(process.getProcessData(), new TypeReference<Map<String, Object>>() {}));
        resp.setIndicatorValues(parseJson(process.getIndicatorValues(), new TypeReference<Map<String, Object>>() {}));
        resp.setAttachmentIds(process.getAttachmentIds());
        resp.setStatus(process.getStatus());
        resp.setStatusName(getDictText(DICT_TYPE_PROCESS_STATUS, process.getStatus()));
        resp.setCreateName(process.getCreator());
        resp.setCreateTime(process.getCreateTime());
        resp.setIndicatorVersion(process.getVersion());
        resp.setUpdateTime(process.getUpdateTime());

        return resp;
    }

    /**
     * 保存填报数据
     *
     * 注意：本方法只负责数据持久化，不负责启动 BPM 流程。
     * BPM 流程由 submitProcessReview() 统一触发（前端列表页「发起流程」按钮），
     * 流程启动后由 DeclareProcessStartedListener → updateProjectProcessInstance() 设置 SUBMITTED。
     *
     * 状态语义：
     * - DRAFT：草稿，不验证必填项
     * - SAVED：已验证，保存待提交流程（发起人发起节点之前只能为 DRAFT 或 SAVED）
     * - SUBMITTED：已发起 BPM 流程（仅由事件回调设置，不在本方法处理）
     * - *_RETURNED：退回，可重新修改提交，需要验证表单
     * - APPROVED：审批通过
     */
    @Override
    public Long saveFormData(ProcessFormDataSaveReqVO reqVO) {
        log.info("[saveFormData] 开始保存, processId: {}, status: {}, formData: {}, indicatorValues: {}",
                reqVO.getProcessId(), reqVO.getStatus(), reqVO.getFormData(), reqVO.getIndicatorValues());

        // 1. 校验过程记录
        ProjectProcessDO process = projectProcessMapper.selectById(reqVO.getProcessId());
        if (process == null) {
            throw exception(PROJECT_PROCESS_NOT_EXISTS);
        }

        log.info("[saveFormData] 过程记录状态: {}", process.getStatus());

        // 2. 校验状态（DRAFT/SAVED/包含 RETURNED 的状态可以编辑和保存）
        if (!isEditable(process.getStatus())) {
            throw exception(PROCESS_STATUS_NOT_EDITABLE);
        }

        // 3. 获取当前用户ID
        String userId = getLoginUserId();

        // 4. 更新过程记录
        ProjectProcessDO update = new ProjectProcessDO();
        update.setId(reqVO.getProcessId());
        update.setProcessData(toJson(reqVO.getFormData()));
        update.setIndicatorValues(toJson(reqVO.getIndicatorValues()));
        update.setAttachmentIds(reqVO.getAttachmentIds());
        // 注意：更新时只设置 updater（使用用户ID），不设置 creator
        update.setUpdater(userId);
        update.setUpdateTime(LocalDateTime.now());

        // 5. 状态处理：前端直接传状态值
        if (reqVO.getStatus() != null) {
            update.setStatus(reqVO.getStatus());
            // 如果是提交状态，设置报告提交时间
            if ("SUBMITTED".equals(reqVO.getStatus())) {
                update.setReportTime(LocalDateTime.now());
            }
        }

        projectProcessMapper.updateById(update);

        // if ("SUBMITTED".equals(reqVO.getStatus())) {
        //     startProcess(reqVO.getProcessId(), process.getProjectId());
        // }

        log.info("[saveFormData] 保存填报数据成功, processId: {}, status: {}", reqVO.getProcessId(), reqVO.getStatus());
        return reqVO.getProcessId();
    }

    @Override
    public Map<String, Object> validateFormData(ProcessFormDataSaveReqVO reqVO) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();

        // 1. 校验过程记录存在
        ProjectProcessDO process = projectProcessMapper.selectById(reqVO.getProcessId());
        if (process == null) {
            errors.add("过程记录不存在");
            result.put("valid", false);
            result.put("errors", errors);
            return result;
        }

        // 2. 获取项目信息
        ProjectDO project = projectMapper.selectById(process.getProjectId());

        // 3. 获取指标配置
        List<ProcessIndicatorConfigDO> configs = processIndicatorConfigService
                .getConfigListByProcessTypeAndProjectType(process.getProcessType(), project.getProjectType());

        // 4. 校验必填指标
        Map<String, Object> indicatorValues = reqVO.getIndicatorValues();
        if (!CollectionUtils.isEmpty(configs)) {
            for (ProcessIndicatorConfigDO config : configs) {
                if (Boolean.TRUE.equals(config.getIsRequired())) {
                    String key = "indicator_" + config.getIndicatorId();
                    Object value = indicatorValues != null ? indicatorValues.get(key) : null;
                    if (value == null || !StringUtils.hasText(String.valueOf(value))) {
                        DeclareIndicatorDO indicator = declareIndicatorService.getIndicator(config.getIndicatorId());
                        String indicatorName = indicator != null ? indicator.getIndicatorName() : "指标" + config.getIndicatorId();
                        errors.add(indicatorName + "为必填项");
                    }
                }
            }
        }

        // 5. 校验文本字段
        Map<String, Object> formData = reqVO.getFormData();
        List<ProcessFormConfigRespVO.TextFieldConfig> textFields = getTextFieldConfigs(process.getProcessType());
        if (!CollectionUtils.isEmpty(textFields)) {
            for (ProcessFormConfigRespVO.TextFieldConfig field : textFields) {
                if (Boolean.TRUE.equals(field.getIsRequired())) {
                    Object value = formData != null ? formData.get(field.getFieldCode()) : null;
                    if (value == null || !StringUtils.hasText(String.valueOf(value))) {
                        errors.add(field.getFieldName() + "为必填项");
                    }
                }
            }
        }

        result.put("valid", errors.isEmpty());
        result.put("errors", errors);
        return result;
    }

    @Override
    public void withdrawFormData(Long processId) {
        ProjectProcessDO process = projectProcessMapper.selectById(processId);
        if (process == null) {
            throw exception(PROJECT_PROCESS_NOT_EXISTS);
        }

        // 只有已提交状态可以撤回
        if (!"SUBMITTED".equals(process.getStatus())) {
            throw exception(PROCESS_STATUS_NOT_WITHDRAWABLE);
        }

        // 撤回后回到 SAVED 状态，可以重新修改并提交
        ProjectProcessDO update = new ProjectProcessDO();
        update.setId(processId);
        update.setStatus("SAVED");
        update.setUpdater(getLoginUserName());
        update.setUpdateTime(LocalDateTime.now());
        projectProcessMapper.updateById(update);

        log.info("[withdrawFormData] 撤回填报数据成功, processId: {}, 原状态: SUBMITTED, 新状态: SAVED", processId);
    }

    @Override
    public void syncIndicatorToProject(Long processId) {
        // 1. 获取过程记录
        ProjectProcessDO process = projectProcessMapper.selectById(processId);
        if (process == null) {
            log.warn("[syncIndicatorToProject] 过程记录不存在, processId: {}", processId);
            return;
        }

        // 2. 只有审批通过状态才同步
        if (!"APPROVED".equals(process.getStatus())) {
            log.warn("[syncIndicatorToProject] 过程记录未审批通过, processId: {}, status: {}", processId, process.getStatus());
            return;
        }

        // 3. 解析指标值
        Map<String, Object> indicatorValues = parseJson(process.getIndicatorValues(),
                new TypeReference<Map<String, Object>>() {});

        if (CollectionUtils.isEmpty(indicatorValues)) {
            log.info("[syncIndicatorToProject] 指标值为空, processId: {}", processId);
            return;
        }

        // 4. 获取项目
        ProjectDO project = projectMapper.selectById(process.getProjectId());
        if (project == null) {
            log.warn("[syncIndicatorToProject] 项目不存在, projectId: {}", process.getProjectId());
            return;
        }

        // 5. 获取指标配置
        List<ProcessIndicatorConfigDO> configs = processIndicatorConfigService
                .getConfigListByProcessTypeAndProjectType(process.getProcessType(), project.getProjectType());

        if (CollectionUtils.isEmpty(configs)) {
            log.info("[syncIndicatorToProject] 无指标配置, processId: {}, processType: {}, projectType: {}",
                    processId, process.getProcessType(), project.getProjectType());
            return;
        }

        // 6. 计算新版本号
        int newVersion = (process.getVersion() == null ? 0 : process.getVersion()) + 1;

        // 7. 获取当前用户ID
        Long userId = WebFrameworkUtils.getLoginUserId();
        if (userId == null) {
            userId = 0L;
        }

        // 8. 同步每个指标到指标值表
        for (ProcessIndicatorConfigDO config : configs) {
            String key = "indicator_" + config.getIndicatorId();
            Object value = indicatorValues.get(key);
            if (value == null) {
                continue;
            }

            // 获取指标信息
            DeclareIndicatorDO indicator = declareIndicatorService.getIndicator(config.getIndicatorId());
            if (indicator == null) {
                continue;
            }

            // 业务类型: 直接使用 processType（1=建设过程, 2=半年报, 3=年度总结, 4=中期评估, 5=整改记录, 6=验收申请）
            int businessType = process.getProcessType();

            // 保存指标值（每次提交都是新记录，保留历史）
            declareIndicatorValueService.saveIndicatorValue(
                    businessType,
                    project.getId(),  // business_id = project_id
                    config.getIndicatorId(),
                    indicator.getIndicatorCode(),
                    indicator.getValueType(),
                    value,
                    userId
            );

            log.debug("[syncIndicatorToProject] 同步指标, indicatorId: {}, value: {}", config.getIndicatorId(), value);
        }

        // 9. 更新过程记录的版本号
        ProjectProcessDO update = new ProjectProcessDO();
        update.setId(processId);
        update.setVersion(newVersion);
        projectProcessMapper.updateById(update);

        log.info("[syncIndicatorToProject] 同步指标完成, processId: {}, version: {}", processId, newVersion);
    }

    @Override
    public void sendReminderNotification(Long processId) {
        // TODO: 实现催办通知逻辑
        // 1. 获取过程记录
        // 2. 根据过程类型获取对应的通知模板
        // 3. 发送通知
        log.info("[sendReminderNotification] 发送催办通知, processId: {}", processId);
    }

    // ========== 私有方法 ==========

    /**
     * 构建指标表单项
     */
    private List<ProcessFormConfigRespVO.IndicatorFormItem> buildIndicatorFormItems(
            List<ProcessIndicatorConfigDO> configs, ProjectProcessDO process) {

        if (CollectionUtils.isEmpty(configs)) {
            return Collections.emptyList();
        }

        // 解析已填报的指标值
        Map<String, Object> indicatorValues = parseJson(process.getIndicatorValues(),
                new TypeReference<Map<String, Object>>() {});

        List<ProcessFormConfigRespVO.IndicatorFormItem> items = new ArrayList<>();
        for (ProcessIndicatorConfigDO config : configs) {
            DeclareIndicatorDO indicator = declareIndicatorService.getIndicator(config.getIndicatorId());
            if (indicator == null) {
                continue;
            }

            ProcessFormConfigRespVO.IndicatorFormItem item = new ProcessFormConfigRespVO.IndicatorFormItem();
            item.setConfigId(config.getId());
            item.setIndicatorId(indicator.getId());
            item.setIndicatorCode(indicator.getIndicatorCode());
            item.setIndicatorName(indicator.getIndicatorName());
            item.setCategory(indicator.getCategory());
            item.setValueType(indicator.getValueType());
            item.setUnit(indicator.getUnit());
            item.setIsRequired(config.getIsRequired());
            item.setSort(config.getSort());
            item.setFillRequire(indicator.getExtraConfig());

            // 当前已填报值
            if (indicatorValues != null) {
                item.setCurrentValue(indicatorValues.get("indicator_" + indicator.getId()));
            }

            items.add(item);
        }

        return items;
    }

    /**
     * 获取文本字段配置（硬编码）
     */
    private List<ProcessFormConfigRespVO.TextFieldConfig> getTextFieldConfigs(Integer processType) {
        List<ProcessFormConfigRespVO.TextFieldConfig> configs = new ArrayList<>();

        // 通用字段：建设进展
        ProcessFormConfigRespVO.TextFieldConfig progress = new ProcessFormConfigRespVO.TextFieldConfig();
        progress.setFieldCode("progress");
        progress.setFieldName("建设进展");
        progress.setIsRequired(false);
        progress.setMaxLength(2000);
        progress.setFieldType(2);  // 多行文本
        configs.add(progress);

        // 通用字段：存在问题
        ProcessFormConfigRespVO.TextFieldConfig problems = new ProcessFormConfigRespVO.TextFieldConfig();
        problems.setFieldCode("problems");
        problems.setFieldName("存在问题");
        problems.setIsRequired(false);
        problems.setMaxLength(1000);
        problems.setFieldType(2);  // 多行文本
        configs.add(problems);

        // 通用字段：下一步计划
        ProcessFormConfigRespVO.TextFieldConfig nextPlan = new ProcessFormConfigRespVO.TextFieldConfig();
        nextPlan.setFieldCode("nextPlan");
        nextPlan.setFieldName("下一步计划");
        nextPlan.setIsRequired(false);
        nextPlan.setMaxLength(1000);
        nextPlan.setFieldType(2);  // 多行文本
        configs.add(nextPlan);

        // 根据流程类型添加特定字段
        if (processType != null) {
            switch (processType) {
                case 4: // 中期评估
                    addTextField(configs, "selfEvaluation", "自评结论", true, 2000, 2);
                    addTextField(configs, "rectificationPlan", "整改措施", false, 1000, 2);
                    break;
                case 5: // 整改记录
                    addTextField(configs, "rectifyContent", "整改内容", true, 1000, 2);
                    addTextField(configs, "rectifyMeasures", "整改措施", true, 1000, 2);
                    addTextField(configs, "rectifyResult", "整改结果", false, 1000, 2);
                    break;
                case 6: // 验收申请
                    addTextField(configs, "acceptanceSummary", "建设总结", true, 5000, 2);
                    addTextField(configs, "achievementDesc", "建设成果说明", false, 2000, 2);
                    break;
                default:
                    break;
            }
        }

        return configs;
    }

    private void addTextField(List<ProcessFormConfigRespVO.TextFieldConfig> configs,
                              String fieldCode, String fieldName, Boolean isRequired, Integer maxLength, Integer fieldType) {
        ProcessFormConfigRespVO.TextFieldConfig field = new ProcessFormConfigRespVO.TextFieldConfig();
        field.setFieldCode(fieldCode);
        field.setFieldName(fieldName);
        field.setIsRequired(isRequired);
        field.setMaxLength(maxLength);
        field.setFieldType(fieldType);
        configs.add(field);
    }

    /**
     * 判断状态是否可编辑
     * 可编辑状态：DRAFT、SAVED、RECTIFICATION_SUBMITTED（整改提交后）、以及包含 RETURNED 的状态（如 EXPERT_RETURNED）
     * 不可编辑：SUBMITTED(已提交)、APPROVED(已通过)等
     */
    private boolean isEditable(String status) {
        if (status == null) {
            return true;
        }
        // DRAFT/SAVED/RECTIFICATION_SUBMITTED/包含 RETURNED 的状态可编辑
        return "DRAFT".equals(status)
                || "SAVED".equals(status)
                || "RECTIFICATION_SUBMITTED".equals(status)
                || status.contains("RETURNED");
    }

    /**
     * 触发流程
     */
    private void startProcess(Long processId, Long projectId) {
        // 1. 获取过程记录
        ProjectProcessDO process = projectProcessMapper.selectById(processId);
        if (process == null) {
            log.error("[startProcess] 过程记录不存在, processId: {}", processId);
            return;
        }

        // 2. 获取项目信息
        ProjectDO project = projectMapper.selectById(projectId);
        if (project == null) {
            log.error("[startProcess] 项目不存在, projectId: {}", projectId);
            return;
        }

        // 3. 获取流程定义Key (基于过程类型)
        ProcessType processType = ProcessType.valueOf(process.getProcessType());
        if (processType == null) {
            log.error("[startProcess] 过程类型不存在, processType: {}", process.getProcessType());
            return;
        }

        // 4. 根据 businessType 查询 bpm_business_type 表获取流程定义Key
        // 格式: project_process:type:{processType}
        String businessTypeKey = "project_process:type:" + process.getProcessType();
        String processDefinitionKey = bpmBusinessTypeService.getProcessDefinitionKey(businessTypeKey);
        if (processDefinitionKey == null) {
            log.error("[startProcess] 流程定义不存在, businessType: {}", businessTypeKey);
            return;
        }

        // 5. 构建 businessKey: {processDefinitionKey}_{processId}
        // 格式与 BpmStartProcessController 保持一致: processDefinitionKey + "_" + businessId
        String businessKey = processDefinitionKey + "_" + processId;

        // 6. 构建流程变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("businessKey", businessKey);
        variables.put("businessType", "project");
        variables.put("projectId", projectId);
        variables.put("processId", processId);
        variables.put("processType", process.getProcessType());

        // 7. 构建流程创建请求 (使用 DTO 以支持 businessKey)
        BpmProcessInstanceCreateReqDTO createReqDTO = new BpmProcessInstanceCreateReqDTO();
        createReqDTO.setProcessDefinitionKey(processDefinitionKey);
        createReqDTO.setBusinessKey(businessKey);
        createReqDTO.setVariables(variables);

        try {
            // 8. 启动流程
            Long userId = WebFrameworkUtils.getLoginUserId();
            String processInstanceId = bpmProcessInstanceService.createProcessInstance(userId, createReqDTO);

            // 9. 发布流程发起事件（状态更新由 DeclareProcessStartedListener 统一处理）
            applicationEventPublisher.publishEvent(new ProcessStartedEvent(
                    processInstanceId,
                    businessTypeKey,
                    businessKey,
                    processId,
                    processDefinitionKey,
                    userId
            ));

            log.info("[startProcess] 启动流程成功, processId: {}, processInstanceId: {}, businessKey: {}",
                    processId, processInstanceId, businessKey);
        } catch (Exception e) {
            log.error("[startProcess] 启动流程失败, processId: {}, error: {}", processId, e.getMessage(), e);
        }
    }

    /**
     * 获取登录用户名称
     */
    /**
     * 获取当前登录用户的ID
     */
    private String getLoginUserId() {
        try {
            Long userId = WebFrameworkUtils.getLoginUserId();
            if (userId != null) {
                return String.valueOf(userId);
            }
        } catch (Exception e) {
            log.warn("获取登录用户ID失败: {}", e.getMessage());
        }
        return "0";
    }

    /**
     * 获取当前登录用户的昵称
     */
    private String getLoginUserName() {
        try {
            Long userId = WebFrameworkUtils.getLoginUserId();
            if (userId != null) {
                AdminUserRespDTO user = adminUserApi.getUser(userId);
                if (user != null) {
                    return user.getNickname();
                }
            }
        } catch (Exception e) {
            log.warn("获取登录用户信息失败: {}", e.getMessage());
        }
        return "系统";
    }

    /**
     * 获取字典文本
     */
    private String getDictText(String dictType, String value) {
        if (!StringUtils.hasText(dictType) || !StringUtils.hasText(value)) {
            return "";
        }
        try {
            DictDataDO dictData = dictDataService.getDictData(dictType, value);
            return dictData != null ? dictData.getLabel() : value;
        } catch (Exception e) {
            log.warn("获取字典文本失败, dictType: {}, value: {}, error: {}", dictType, value, e.getMessage());
            return value;
        }
    }

    /**
     * 对象转JSON
     */
    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return JsonUtils.toJsonPrettyString(obj);
        } catch (Exception e) {
            log.error("JSON序列化失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JSON转对象
     */
    private <T> T parseJson(String json, TypeReference<T> typeRef) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return JsonUtils.parseObject(json, typeRef);
        } catch (Exception e) {
            log.error("JSON反序列化失败: {}", e.getMessage());
            return null;
        }
    }

}
