package cn.gemrun.base.module.declare.service.project.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.module.declare.controller.admin.project.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.project.ProjectDO;
import cn.gemrun.base.module.declare.dal.dataobject.project.ProjectProcessDO;
import cn.gemrun.base.module.declare.dal.mysql.project.ProjectMapper;
import cn.gemrun.base.module.declare.dal.mysql.project.ProjectProcessMapper;
import cn.gemrun.base.module.declare.service.project.ProjectProcessService;
import cn.gemrun.base.module.system.api.user.AdminUserApi;
import cn.gemrun.base.module.system.api.user.dto.AdminUserRespDTO;
import cn.gemrun.base.framework.web.core.util.WebFrameworkUtils;
import cn.gemrun.base.module.bpm.service.task.BpmProcessInstanceService;
import org.flowable.engine.history.HistoricProcessInstance;
import com.mzt.logapi.starter.annotation.LogRecord;

import static cn.gemrun.base.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.gemrun.base.module.declare.enums.ErrorCodeConstants.*;
import static cn.gemrun.base.module.declare.enums.DeclareLogRecordConstants.*;

/**
 * 项目过程记录 Service 实现类
 *
 * @author Gemini
 */
@Service
@Slf4j
public class ProjectProcessServiceImpl implements ProjectProcessService {

    @Resource
    private ProjectProcessMapper projectProcessMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private BpmProcessInstanceService processInstanceService;

    @Override
    @LogRecord(type = PROJECT_PROCESS_TYPE, subType = PROJECT_PROCESS_CREATE_SUB_TYPE,
            bizNo = "{{#_ret}}", success = PROJECT_PROCESS_CREATE_SUCCESS)
    public Long createProjectProcess(ProjectProcessSaveReqVO createReqVO) {
        log.info("[createProjectProcess] 接收参数: reportPeriodStart={}, reportPeriodEnd={}",
                createReqVO.getReportPeriodStart(), createReqVO.getReportPeriodEnd());

        ProjectProcessDO projectProcess = BeanUtils.toBean(createReqVO, ProjectProcessDO.class);

        // 手动转换日期字段（直接使用LocalDateTime.parse解析字符串格式）
        if (createReqVO.getReportPeriodStart() != null && createReqVO.getReportPeriodStart().getYear() < 1900) {
            // 如果解析失败导致年份为1970，需要手动解析
            projectProcess.setReportPeriodStart(null);
        }
        if (createReqVO.getReportPeriodEnd() != null && createReqVO.getReportPeriodEnd().getYear() < 1900) {
            projectProcess.setReportPeriodEnd(null);
        }

        log.info("[createProjectProcess] 转换后: reportPeriodStart={}, reportPeriodEnd={}",
                projectProcess.getReportPeriodStart(), projectProcess.getReportPeriodEnd());

        // 设置默认状态为 DRAFT
        projectProcess.setStatus("DRAFT");

        // 状态（前端直接传字符串）
        if (createReqVO.getStatus() != null) {
            projectProcess.setStatus(createReqVO.getStatus());
        }

        // 设置创建人ID和更新人（使用用户ID）
        Long userId = getLoginUserIdLong();
        projectProcess.setCreatorId(userId);
        projectProcess.setUpdater(String.valueOf(userId));

        // 设置部门ID（优先从项目继承，否则从当前用户获取）
        if (projectProcess.getDeptId() == null && projectProcess.getProjectId() != null) {
            ProjectDO project = projectMapper.selectById(projectProcess.getProjectId());
            if (project != null && project.getDeptId() != null) {
                projectProcess.setDeptId(project.getDeptId());
            }
        }
        // 如果仍然没有部门ID，则从当前用户获取
        if (projectProcess.getDeptId() == null) {
            try {
                Long loginUserId = WebFrameworkUtils.getLoginUserId();
                if (loginUserId != null) {
                    AdminUserRespDTO user = adminUserApi.getUser(loginUserId);
                    if (user != null) {
                        projectProcess.setDeptId(user.getDeptId());
                    }
                }
            } catch (Exception e) {
                log.warn("[createProjectProcess] 获取用户部门ID失败: {}", e.getMessage());
            }
        }
        projectProcessMapper.insert(projectProcess);
        return projectProcess.getId();
    }

    @Override
    @LogRecord(type = PROJECT_PROCESS_TYPE, subType = PROJECT_PROCESS_UPDATE_SUB_TYPE,
            bizNo = "{{#updateReqVO.id}}", success = PROJECT_PROCESS_UPDATE_SUCCESS)
    public void updateProjectProcess(ProjectProcessSaveReqVO updateReqVO) {
        validateProjectProcessExists(updateReqVO.getId());
        ProjectProcessDO updateObj = BeanUtils.toBean(updateReqVO, ProjectProcessDO.class);

        // 状态（前端直接传字符串）
        if (updateReqVO.getStatus() != null) {
            updateObj.setStatus(updateReqVO.getStatus());
        }

        // 设置更新人（使用用户ID）
        String userId = getLoginUserId();
        updateObj.setUpdater(userId);

        projectProcessMapper.updateById(updateObj);
    }

    @Override
    @LogRecord(type = PROJECT_PROCESS_TYPE, subType = PROJECT_PROCESS_DELETE_SUB_TYPE,
            bizNo = "{{#id}}", success = PROJECT_PROCESS_DELETE_SUCCESS)
    public void deleteProjectProcess(Long id) {
        validateProjectProcessExists(id);
        projectProcessMapper.deleteById(id);
    }

    @Override
    @LogRecord(type = PROJECT_PROCESS_TYPE, subType = PROJECT_PROCESS_BATCH_DELETE_SUB_TYPE,
            bizNo = "{{#ids}}", success = PROJECT_PROCESS_BATCH_DELETE_SUCCESS)
    public void deleteProjectProcessListByIds(Set<Long> ids) {
        projectProcessMapper.deleteByIds(ids);
    }

    private void validateProjectProcessExists(Long id) {
        if (projectProcessMapper.selectById(id) == null) {
            throw exception(PROJECT_PROCESS_NOT_EXISTS);
        }
    }

    @Override
    public ProjectProcessRespVO getProjectProcess(Long id) {
        ProjectProcessDO projectProcess = projectProcessMapper.selectById(id);
        return convertToRespVO(projectProcess);
    }

    @Override
    public PageResult<ProjectProcessRespVO> getProjectProcessPage(ProjectProcessPageReqVO pageReqVO) {
        PageResult<ProjectProcessDO> pageResult = projectProcessMapper.selectPageByProjectId(pageReqVO);
        return convertToPageResult(pageResult);
    }

    @Override
    public List<ProjectProcessRespVO> getProjectProcessListByProjectId(Long projectId) {
        log.info("[getProjectProcessListByProjectId] 查询项目ID: {}, 数据库记录数: {}", projectId, projectProcessMapper.selectCount(null));
        List<ProjectProcessDO> list = projectProcessMapper.selectListByProjectId(projectId);
        log.info("[getProjectProcessListByProjectId] 查询结果数量: {}, 数据: {}", list.size(), list);
        return BeanUtils.toBean(list, ProjectProcessRespVO.class);
    }

    @Override
    @LogRecord(type = PROJECT_PROCESS_TYPE, subType = PROJECT_PROCESS_STATUS_CHANGE_SUB_TYPE,
            bizNo = "{{#id}}", success = PROJECT_PROCESS_STATUS_CHANGE_SUCCESS)
    public void updateProjectProcessStatus(Long id, String bizStatus) {
        validateProjectProcessExists(id);
        ProjectProcessDO updateObj = new ProjectProcessDO();
        updateObj.setId(id);
        updateObj.setStatus(bizStatus);
        projectProcessMapper.updateById(updateObj);
    }

    @Override
    public boolean hasProcess(Long projectId, Integer processType, Integer year) {
        return projectProcessMapper.existsByProjectIdAndTypeAndYear(projectId, processType, year);
    }

    @Override
    public boolean hasAcceptanceProcess(Long projectId) {
        return projectProcessMapper.existsByProjectIdAndType(projectId, 6); // 6=验收申请
    }

    @Override
    public Long createProcessForProject(Long projectId, Integer processType, String title,
                                        java.time.LocalDateTime reportPeriodStart,
                                        java.time.LocalDateTime reportPeriodEnd) {
        ProjectProcessDO projectProcess = new ProjectProcessDO();
        projectProcess.setProjectId(projectId);
        projectProcess.setProcessType(processType);
        projectProcess.setProcessTitle(title);
        projectProcess.setStatus("DRAFT"); // 草稿状态
        projectProcess.setReportPeriodStart(reportPeriodStart);
        projectProcess.setReportPeriodEnd(reportPeriodEnd);
        projectProcessMapper.insert(projectProcess);
        return projectProcess.getId();
    }

    @Override
    public List<RectificationRecordRespVO> getRectificationRecordList(Long projectId) {
        List<RectificationRecordRespVO> result = new ArrayList<>();

        // 1. 获取项目信息
        ProjectDO project = projectMapper.selectById(projectId);
        String projectName = project != null ? project.getProjectName() : null;

        // 2. 查询项目下所有过程记录
        List<ProjectProcessDO> processList = projectProcessMapper.selectListByProjectId(projectId);

        if (processList == null || processList.isEmpty()) {
            return result;
        }

        // 3. 获取所有过程记录关联的流程实例ID
        List<String> processInstanceIds = processList.stream()
                .map(ProjectProcessDO::getProcessInstanceId)
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toList());

        // 4. 获取所有子流程（根据父流程实例ID查询）
        Map<String, List<HistoricProcessInstance>> childProcessMap = processInstanceIds.stream()
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toMap(
                        id -> id,
                        id -> processInstanceService.getChildProcessInstancesByParentId(id),
                        (v1, v2) -> v1
                ));

        // 5. 获取发起人信息
        Set<Long> userIds = processList.stream()
                .map(ProjectProcessDO::getCreatorId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        // 子流程发起人
        childProcessMap.values().stream()
                .flatMap(List::stream)
                .forEach(instance -> {
                    if (instance.getStartUserId() != null) {
                        try {
                            userIds.add(Long.parseLong(instance.getStartUserId()));
                        } catch (NumberFormatException ignored) {}
                    }
                });

        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);

        // 6. 构建过程记录列表（recordType=1）
        for (ProjectProcessDO process : processList) {
            RectificationRecordRespVO vo = new RectificationRecordRespVO();
            vo.setRecordType(1);
            vo.setProcessId(process.getId());
            vo.setProjectId(process.getProjectId());
            vo.setProjectName(projectName);
            vo.setTitle(process.getProcessTitle());
            vo.setStatus(process.getStatus());
            vo.setReportPeriodStart(process.getReportPeriodStart());
            vo.setReportPeriodEnd(process.getReportPeriodEnd());
            vo.setCreateTime(process.getCreateTime());
            vo.setParentProcessInstanceId(process.getProcessInstanceId());

            // 设置发起人信息
            if (process.getCreatorId() != null) {
                AdminUserRespDTO user = userMap.get(process.getCreatorId());
                if (user != null) {
                    vo.setStartUserId(process.getCreatorId());
                    vo.setStartUserName(user.getNickname());
                }
            }

            result.add(vo);
        }

        // 7. 构建子流程列表（recordType=2）
        for (Map.Entry<String, List<HistoricProcessInstance>> entry : childProcessMap.entrySet()) {
            String parentProcessInstanceId = entry.getKey();
            List<HistoricProcessInstance> childProcesses = entry.getValue();

            // 找到对应的父过程记录
            ProjectProcessDO parentProcess = processList.stream()
                    .filter(p -> parentProcessInstanceId.equals(p.getProcessInstanceId()))
                    .findFirst()
                    .orElse(null);

            for (HistoricProcessInstance childProcess : childProcesses) {
                RectificationRecordRespVO vo = new RectificationRecordRespVO();
                vo.setRecordType(2);
                vo.setProjectId(projectId);
                vo.setProjectName(projectName);
                vo.setChildProcessInstanceId(childProcess.getId());
                vo.setParentProcessInstanceId(parentProcessInstanceId);
                vo.setTitle(childProcess.getName());
                vo.setCreateTime(toLocalDateTime(childProcess.getStartTime()));
                vo.setEndTime(toLocalDateTime(childProcess.getEndTime()));

                // 设置状态
                vo.setStatus(childProcess.getEndTime() != null ? "COMPLETED" : "RUNNING");

                // 设置发起人信息
                if (childProcess.getStartUserId() != null) {
                    try {
                        Long startUserId = Long.parseLong(childProcess.getStartUserId());
                        AdminUserRespDTO user = userMap.get(startUserId);
                        if (user != null) {
                            vo.setStartUserId(startUserId);
                            vo.setStartUserName(user.getNickname());
                        }
                    } catch (NumberFormatException ignored) {}
                }

                // 子流程的 processId 关联到父过程记录
                if (parentProcess != null) {
                    vo.setProcessId(parentProcess.getId());
                }

                result.add(vo);
            }
        }

        // 8. 按创建时间倒序排序
        result.sort((a, b) -> {
            if (a.getCreateTime() == null && b.getCreateTime() == null) return 0;
            if (a.getCreateTime() == null) return 1;
            if (b.getCreateTime() == null) return -1;
            return b.getCreateTime().compareTo(a.getCreateTime());
        });

        return result;
    }

    private ProjectProcessRespVO convertToRespVO(ProjectProcessDO projectProcess) {
        if (projectProcess == null) {
            return null;
        }
        return BeanUtils.toBean(projectProcess, ProjectProcessRespVO.class);
    }

    private PageResult<ProjectProcessRespVO> convertToPageResult(PageResult<ProjectProcessDO> pageResult) {
        if (pageResult == null) {
            return null;
        }
        List<ProjectProcessRespVO> list = BeanUtils.toBean(pageResult.getList(), ProjectProcessRespVO.class);
        return new PageResult<>(list, pageResult.getTotal());
    }

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
     * 获取当前登录用户的ID（Long类型）
     */
    private Long getLoginUserIdLong() {
        try {
            Long userId = WebFrameworkUtils.getLoginUserId();
            if (userId != null) {
                return userId;
            }
        } catch (Exception e) {
            log.warn("获取登录用户ID失败: {}", e.getMessage());
        }
        return 0L;
    }

    /**
     * 将 java.util.Date 转换为 java.time.LocalDateTime
     */
    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

}
