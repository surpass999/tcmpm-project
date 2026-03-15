package cn.gemrun.base.module.declare.service.filing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;

import java.util.*;
import java.util.stream.Collectors;

import cn.gemrun.base.module.declare.controller.admin.filing.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.filing.FilingDO;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorValueDO;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.module.declare.dal.mysql.filing.FilingMapper;
import cn.gemrun.base.module.declare.dal.mysql.indicator.DeclareIndicatorValueMapper;
import cn.gemrun.base.module.declare.service.indicator.DeclareIndicatorValueService;
import cn.gemrun.base.module.declare.service.project.ProjectService;
import cn.gemrun.base.module.declare.controller.admin.project.vo.ProjectSaveReqVO;
import cn.gemrun.base.module.declare.enums.ProjectStatus;
import cn.gemrun.base.module.bpm.controller.admin.task.vo.instance.BpmProcessInstanceCreateReqVO;
import cn.gemrun.base.module.bpm.service.task.BpmProcessInstanceService;
import cn.gemrun.base.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.gemrun.base.framework.web.core.util.WebFrameworkUtils;
import cn.gemrun.base.module.system.api.user.AdminUserApi;
import cn.gemrun.base.module.system.api.user.dto.AdminUserRespDTO;
import cn.gemrun.base.framework.security.core.util.SecurityFrameworkUtils;
import org.flowable.engine.repository.ProcessDefinition;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import static cn.gemrun.base.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.gemrun.base.module.declare.enums.ErrorCodeConstants.*;

/**
 * 项目备案核心信息 Service 实现类
 *
 * 数据权限规则（简化版）：
 * - 可见 = 发起人(creator=当前用户) OR 参与人(initiator_ids包含当前用户)
 * - 发起人：创建/提交的人 → 始终能看到自己提交的
 * - 参与人：审批过的人 → 审批过就能看到
 *
 * @author 杜春渔
 */
@Service
@Slf4j
public class FilingServiceImpl implements FilingService {

    @Resource
    private FilingMapper filingMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeclareIndicatorValueService indicatorValueService;
    @Resource
    private DeclareIndicatorValueMapper indicatorValueMapper;
    @Resource
    private ProjectService projectService;
    @Resource
    private BpmProcessInstanceService bpmProcessInstanceService;
    @Resource
    private BpmProcessDefinitionService bpmProcessDefinitionService;

    @Override
    public Long createFiling(FilingSaveReqVO createReqVO) {
        // 获取当前登录用户的部门ID，用于数据权限控制
        Long deptId = null;
        Long userId = null;
        try {
            userId = WebFrameworkUtils.getLoginUserId();
            if (userId != null) {
                AdminUserRespDTO user = adminUserApi.getUser(userId);
                if (user != null) {
                    deptId = user.getDeptId();
                    log.info("[createFiling] 用户 {} 部门ID: {}", userId, deptId);
                }
            }
        } catch (Exception e) {
            log.warn("[createFiling] 获取用户部门ID失败: {}", e.getMessage());
        }

        // 插入基本信息
        FilingDO filing = BeanUtils.toBean(createReqVO, FilingDO.class);
        filing.setDeptId(deptId);  // 设置部门ID，用于数据权限控制
        filing.setFilingStatus("DRAFT");  // 初始状态为草稿

        // 生成备案编号：PRNT + 年月日 + 序号（如 PRNT202603140001）
        String filingCode = generateFilingCode();
        filing.setFilingCode(filingCode);
        log.info("[createFiling] 生成备案编号: {}", filingCode);

        filingMapper.insert(filing);

        // 保存指标值
        saveIndicatorValues(createReqVO, filing.getId(), userId);

        // 返回
        return filing.getId();
    }

    @Override
    public void updateFiling(FilingSaveReqVO updateReqVO) {
        // 校验存在
        validateFilingExists(updateReqVO.getId());

        // 打印日志调试
        log.info("更新备案信息 ID: {}, validStartTime: {}, validEndTime: {}",
                updateReqVO.getId(), updateReqVO.getValidStartTime(), updateReqVO.getValidEndTime());

        // 使用 BeanUtils 转换
        FilingDO updateObj = BeanUtils.toBean(updateReqVO, FilingDO.class);
        filingMapper.updateById(updateObj);

        // 保存指标值
        Long userId = null;
        try {
            userId = SecurityFrameworkUtils.getLoginUserId();
        } catch (Exception e) {
            log.warn("[updateFiling] 获取用户ID失败: {}", e.getMessage());
        }
        saveIndicatorValues(updateReqVO, updateReqVO.getId(), userId);
    }

    /**
     * 保存指标值
     */
    private void saveIndicatorValues(FilingSaveReqVO reqVO, Long filingId, Long userId) {
        if (reqVO.getIndicatorValues() == null || reqVO.getIndicatorValues().isEmpty()) {
            return;
        }

        // 转换为 DO 对象
        List<DeclareIndicatorValueDO> indicatorValues = BeanUtils.toBean(
                reqVO.getIndicatorValues(), DeclareIndicatorValueDO.class);

        // 批量保存指标值（业务类型为1，代表备案）
        indicatorValueService.batchSaveIndicatorValues(1, filingId, indicatorValues, userId);
        log.info("保存指标值完成，备案ID: {}, 指标数量: {}", filingId, indicatorValues.size());
    }

    @Override
    public void deleteFiling(Long id) {
        // 校验存在
        validateFilingExists(id);
        // 删除备案基本信息
        filingMapper.deleteById(id);
        // 逻辑删除关联的指标值（业务类型1=备案）
        DeclareIndicatorValueDO indicatorValue = new DeclareIndicatorValueDO();
        indicatorValue.setDeleted(true);
        indicatorValueMapper.update(indicatorValue, new QueryWrapper<DeclareIndicatorValueDO>()
                .eq("business_type", 1)
                .eq("business_id", id));
    }

    @Override
    public void deleteFilingListByIds(List<Long> ids) {
        // 批量逻辑删除关联的指标值（业务类型1=备案）
        for (Long id : ids) {
            DeclareIndicatorValueDO indicatorValue = new DeclareIndicatorValueDO();
            indicatorValue.setDeleted(true);
            indicatorValueMapper.update(indicatorValue, new QueryWrapper<DeclareIndicatorValueDO>()
                    .eq("business_type", 1)
                    .eq("business_id", id));
        }
        // 批量删除备案基本信息
        filingMapper.deleteByIds(ids);
    }


    private FilingDO validateFilingExists(Long id) {
        FilingDO filing = filingMapper.selectById(id);
        if (filing == null) {
            throw exception(FILING_NOT_EXISTS);
        }
        return filing;
    }

    @Override
    public FilingDO getFiling(Long id) {
        return filingMapper.selectById(id);
    }

    @Override
    public PageResult<FilingDO> getFilingPage(FilingPageReqVO pageReqVO) {
        // 直接调用 mapper，系统根据用户角色配置自动处理数据权限
        // 角色配置的数据权限会自动过滤 dept_id 字段
        return filingMapper.selectPage(pageReqVO);
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        try {
            return SecurityFrameworkUtils.getLoginUserId();
        } catch (Exception e) {
            log.warn("[getCurrentUserId] 获取用户ID失败: {}", e.getMessage());
            return null;
        }
    }

    // ========== 流程相关方法已移至 BpmProcess AOP 自动处理 ==========

    @Override
    public void updateFilingStatus(Long id, String bizStatus) {
        FilingDO filing = filingMapper.selectById(id);
        if (filing == null) {
            log.warn("[updateFilingStatus] 备案不存在: id={}", id);
            return;
        }

        String oldStatus = filing.getFilingStatus();
        filing.setFilingStatus(bizStatus);
        filingMapper.updateById(filing);
        log.info("[updateFilingStatus] 更新备案状态: id={}, filingStatus={}", id, bizStatus);

        // ========== 自动立项逻辑 ==========
        // 当备案状态变为 NATION_APPROVED（国家局审核通过）时，自动创建项目
        // 支持带条件的格式：NATION_APPROVED | TO_PROJECT
        String actualStatus = bizStatus;
        if (bizStatus != null && bizStatus.contains("|")) {
            actualStatus = bizStatus.split("\\|")[0].trim();
        }
        if ("NATION_APPROVED".equals(actualStatus) && !"NATION_APPROVED".equals(oldStatus)) {
            try {
                createProjectFromFiling(filing);
            } catch (Exception e) {
                log.error("[updateFilingStatus] 自动立项失败: filingId={}", id, e);
                // 不影响主流程，立项失败可手动处理
            }
        }
    }

    /**
     * 从备案自动创建项目
     * 当备案国家局审核通过时，自动创建一个项目记录
     *
     * @param filing 备案信息
     */
    private void createProjectFromFiling(FilingDO filing) {
        // 1. 检查是否已存在关联项目
        if (filing.getProjectId() != null) {
            log.info("[createProjectFromFiling] 备案已有关联项目，跳过: filingId={}, projectId={}",
                filing.getId(), filing.getProjectId());
            return;
        }

        // 2. 创建项目基本信息
        ProjectSaveReqVO projectReq = new ProjectSaveReqVO();
        projectReq.setFilingId(filing.getId());
        // 项目名称：机构名称 + 试点项目
        projectReq.setProjectName(filing.getOrgName() + "试点项目");
        // 项目状态：立项中（待审批）
        projectReq.setProjectStatus(ProjectStatus.INITIATION.getStatus());
        // 立项时间为当前时间
        projectReq.setStartTime(java.time.LocalDateTime.now());
        // 初始进度为0
        projectReq.setActualProgress(0);
        // 继承备案的项目类型
        projectReq.setProjectType(filing.getProjectType());
        // 继承备案的建设内容作为项目描述（如果有相关字段）
        // projectReq.setConstructionContent(filing.getConstructionContent());

        // 3. 设置项目负责人信息（备案的创建人）
        if (filing.getCreator() != null && !filing.getCreator().isEmpty()) {
            try {
                Long creatorId = Long.parseLong(filing.getCreator());
                AdminUserRespDTO creator = adminUserApi.getUser(creatorId);
                if (creator != null) {
                    projectReq.setLeaderUserId(creatorId);
                    projectReq.setLeaderMobile(creator.getMobile());
                    projectReq.setLeaderName(creator.getNickname());
                    log.info("[createProjectFromFiling] 设置项目负责人: filingId={}, leaderUserId={}, leaderName={}",
                        filing.getId(), creatorId, creator.getNickname());
                }
            } catch (NumberFormatException e) {
                log.warn("[createProjectFromFiling] 解析创建人ID失败: filingId={}, creator={}",
                    filing.getId(), filing.getCreator());
            }
        }

        // 4. 设置项目计划完成时间（继承备案的计划完成时间）
        projectReq.setPlanEndTime(filing.getPlanEndTime());

        // 5. 继承备案的部门ID（用于数据权限控制）
        projectReq.setDeptId(filing.getDeptId());

        // 6. 保存项目
        Long projectId = projectService.createProject(projectReq);

        // 4. 更新备案的 projectId，建立关联
        filing.setProjectId(projectId);
        filingMapper.updateById(filing);

        // 5. 复制备案的指标数据到项目（避免误删备案导致指标数据丢失）
        try {
            copyIndicatorValuesFromFilingToProject(filing.getId(), projectId);
        } catch (Exception e) {
            log.warn("[createProjectFromFiling] 复制指标数据失败: filingId={}, projectId={}, error={}",
                filing.getId(), projectId, e.getMessage());
        }

        log.info("[createProjectFromFiling] 自动立项成功: filingId={}, projectId={}, projectName={}",
            filing.getId(), projectId, projectReq.getProjectName());
    }

    /**
     * 复制备案的指标数据到项目
     * 备案的指标业务类型为 1，项目的指标业务类型为 2
     * 复制后项目拥有独立的指标数据，避免误删备案导致数据丢失
     *
     * @param filingId 备案ID
     * @param projectId 项目ID
     */
    private void copyIndicatorValuesFromFilingToProject(Long filingId, Long projectId) {
        // 业务类型：1=备案，2=项目
        final Integer FILING_BUSINESS_TYPE = 1;
        final Integer PROJECT_BUSINESS_TYPE = 2;

        // 1. 获取备案的指标值
        List<DeclareIndicatorValueDO> filingValues = indicatorValueMapper.selectList(
            new QueryWrapper<DeclareIndicatorValueDO>()
                .eq("business_type", FILING_BUSINESS_TYPE)
                .eq("business_id", filingId)
                .eq("deleted", false)
        );

        if (filingValues == null || filingValues.isEmpty()) {
            log.info("[copyIndicatorValuesFromFilingToProject] 备案无指标数据: filingId={}", filingId);
            return;
        }

        // 2. 转换为项目的指标值
        List<DeclareIndicatorValueDO> projectValues = filingValues.stream()
            .map(v -> {
                DeclareIndicatorValueDO newValue = new DeclareIndicatorValueDO();
                newValue.setBusinessType(PROJECT_BUSINESS_TYPE);
                newValue.setBusinessId(projectId);
                newValue.setIndicatorId(v.getIndicatorId());
                newValue.setIndicatorCode(v.getIndicatorCode());
                newValue.setValueType(v.getValueType());
                newValue.setValueNum(v.getValueNum());
                newValue.setValueStr(v.getValueStr());
                newValue.setValueBool(v.getValueBool());
                newValue.setValueDate(v.getValueDate());
                newValue.setValueText(v.getValueText());
                newValue.setValueDateStart(v.getValueDateStart());
                newValue.setValueDateEnd(v.getValueDateEnd());
                newValue.setFillTime(v.getFillTime());
                newValue.setFillerId(v.getFillerId());
                newValue.setIsValid(v.getIsValid());
                newValue.setValidationMsg(v.getValidationMsg());
                return newValue;
            })
            .collect(Collectors.toList());

        // 3. 批量保存
        for (DeclareIndicatorValueDO value : projectValues) {
            indicatorValueMapper.insert(value);
        }

        log.info("[copyIndicatorValuesFromFilingToProject] 复制指标数据成功: filingId={}, projectId={}, count={}",
            filingId, projectId, projectValues.size());
    }

    @Override
    public String startProcess(Long id, String processDefinitionKey) {
        // 1. 校验备案是否存在
        FilingDO filing = validateFilingExists(id);

        // 2. 校验备案状态必须是 DRAFT（草稿）才能提交
        if (!"DRAFT".equals(filing.getFilingStatus())) {
            throw new RuntimeException("只有草稿状态的备案才能提交审核，当前状态: " + filing.getFilingStatus());
        }

        // 3. 如果已有流程实例ID，说明已发起流程
        if (filing.getProcessInstanceId() != null && !filing.getProcessInstanceId().isEmpty()) {
            log.warn("[startProcess] 备案已存在流程实例: filingId={}, processInstanceId={}",
                id, filing.getProcessInstanceId());
            return filing.getProcessInstanceId();
        }

        // 3. 获取流程定义ID
        ProcessDefinition processDefinition = bpmProcessDefinitionService.getActiveProcessDefinition(processDefinitionKey);
        if (processDefinition == null) {
            throw new RuntimeException("流程定义不存在: " + processDefinitionKey);
        }

        // 4. 构建流程变量
        // businessKey 格式: declare:filing:create:{filingId} - 用于监听器解析
        String businessKey = String.format("declare:filing:create:%d", id);
        Map<String, Object> variables = new HashMap<>();
        variables.put("businessKey", businessKey);
        variables.put("businessType", "filing");     // 业务类型
        variables.put("title", "医院备案申请 - " + filing.getOrgName());  // 流程标题

        // 5. 构建创建请求
        BpmProcessInstanceCreateReqVO createReqVO = new BpmProcessInstanceCreateReqVO();
        createReqVO.setProcessDefinitionId(processDefinition.getId());
        createReqVO.setVariables(variables);

        // 6. 发起流程（需要获取当前用户ID）
        Long userId = getCurrentUserId();
        String processInstanceId = bpmProcessInstanceService.createProcessInstance(userId, createReqVO);

        // 7. 更新备案的流程实例ID
        filing.setProcessInstanceId(processInstanceId);
        filing.setFilingStatus("SUBMITTED");  // 提交状态
        filingMapper.updateById(filing);

        log.info("[startProcess] 发起流程成功: filingId={}, processInstanceId={}, processDefinitionKey={}",
            id, processInstanceId, processDefinitionKey);

        return processInstanceId;
    }

    @Override
    public void updateFilingProcessInstance(Long id, String processInstanceId, String filingStatus) {
        FilingDO filing = validateFilingExists(id);
        filing.setProcessInstanceId(processInstanceId);
        filing.setFilingStatus(filingStatus);
        filingMapper.updateById(filing);

        log.info("[updateFilingProcessInstance] 更新备案流程实例: filingId={}, processInstanceId={}, filingStatus={}",
            id, processInstanceId, filingStatus);
    }

    /**
     * 生成备案编号
     * 格式：PRNT + 年月日 + 序号（如 PRNT202603140001）
     */
    private String generateFilingCode() {
        // 前缀
        String prefix = "PRNT";

        // 日期：YYYYMMDD
        java.time.LocalDate today = java.time.LocalDate.now();
        String dateStr = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 获取当天的序号：查询当天最大的备案编号
        String likePattern = prefix + dateStr + "%";
        String maxCode = filingMapper.selectMaxFilingCode(likePattern);

        int sequence = 1;
        if (maxCode != null && maxCode.length() > prefix.length() + dateStr.length()) {
            try {
                String seqStr = maxCode.substring(prefix.length() + dateStr.length());
                sequence = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                log.warn("[generateFilingCode] 解析序号失败: {}", maxCode);
            }
        }

        // 序号补零：4位（如 0001）
        String sequenceStr = String.format("%04d", sequence);

        return prefix + dateStr + sequenceStr;
    }

}