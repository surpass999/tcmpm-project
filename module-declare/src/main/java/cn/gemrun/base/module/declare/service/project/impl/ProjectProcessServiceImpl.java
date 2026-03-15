package cn.gemrun.base.module.declare.service.project.impl;

import java.util.List;
import java.util.Set;

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

import static cn.gemrun.base.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.gemrun.base.module.declare.enums.ErrorCodeConstants.*;

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

    @Override
    public Long createProjectProcess(ProjectProcessSaveReqVO createReqVO) {
        ProjectProcessDO projectProcess = BeanUtils.toBean(createReqVO, ProjectProcessDO.class);
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
                Long userId = WebFrameworkUtils.getLoginUserId();
                if (userId != null) {
                    AdminUserRespDTO user = adminUserApi.getUser(userId);
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
    public void updateProjectProcess(ProjectProcessSaveReqVO updateReqVO) {
        validateProjectProcessExists(updateReqVO.getId());
        ProjectProcessDO updateObj = BeanUtils.toBean(updateReqVO, ProjectProcessDO.class);
        projectProcessMapper.updateById(updateObj);
    }

    @Override
    public void deleteProjectProcess(Long id) {
        validateProjectProcessExists(id);
        projectProcessMapper.deleteById(id);
    }

    @Override
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
        List<ProjectProcessDO> list = projectProcessMapper.selectListByProjectId(projectId);
        return BeanUtils.toBean(list, ProjectProcessRespVO.class);
    }

    @Override
    public void updateProjectProcessStatus(Long id, Integer status) {
        validateProjectProcessExists(id);
        ProjectProcessDO updateObj = new ProjectProcessDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
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
        projectProcess.setStatus(0); // 草稿状态
        projectProcess.setReportPeriodStart(reportPeriodStart);
        projectProcess.setReportPeriodEnd(reportPeriodEnd);
        projectProcessMapper.insert(projectProcess);
        return projectProcess.getId();
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

}
