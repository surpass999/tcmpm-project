package cn.gemrun.base.module.declare.service.project.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.module.declare.controller.admin.project.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.project.ProjectDO;
import cn.gemrun.base.module.declare.dal.mysql.project.ProjectMapper;
import cn.gemrun.base.module.declare.enums.ProjectStatus;
import cn.gemrun.base.module.declare.service.project.ProjectService;
import cn.gemrun.base.module.system.api.user.AdminUserApi;
import cn.gemrun.base.module.system.api.user.dto.AdminUserRespDTO;
import cn.gemrun.base.framework.web.core.util.WebFrameworkUtils;
import com.mzt.logapi.starter.annotation.LogRecord;

import static cn.gemrun.base.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.gemrun.base.module.declare.enums.ErrorCodeConstants.*;
import static cn.gemrun.base.module.declare.enums.DeclareLogRecordConstants.*;

/**
 * 项目信息 Service 实现类
 *
 * @author Gemini
 */
@Service
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private AdminUserApi adminUserApi;

    @Override
    @LogRecord(type = PROJECT_TYPE, subType = PROJECT_CREATE_SUB_TYPE,
            bizNo = "{{#_ret}}", success = PROJECT_CREATE_SUCCESS)
    public Long createProject(ProjectSaveReqVO createReqVO) {
        ProjectDO project = BeanUtils.toBean(createReqVO, ProjectDO.class);
        // 设置部门ID（如果没有设置，则从当前用户获取）
        if (project.getDeptId() == null) {
            try {
                Long userId = WebFrameworkUtils.getLoginUserId();
                if (userId != null) {
                    AdminUserRespDTO user = adminUserApi.getUser(userId);
                    if (user != null) {
                        project.setDeptId(user.getDeptId());
                    }
                }
            } catch (Exception e) {
                log.warn("[createProject] 获取用户部门ID失败: {}", e.getMessage());
            }
        }
        projectMapper.insert(project);
        return project.getId();
    }

    @Override
    @LogRecord(type = PROJECT_TYPE, subType = PROJECT_UPDATE_SUB_TYPE,
            bizNo = "{{#updateReqVO.id}}", success = PROJECT_UPDATE_SUCCESS)
    public void updateProject(ProjectSaveReqVO updateReqVO) {
        validateProjectExists(updateReqVO.getId());
        ProjectDO updateObj = BeanUtils.toBean(updateReqVO, ProjectDO.class);
        projectMapper.updateById(updateObj);
    }

    @Override
    @LogRecord(type = PROJECT_TYPE, subType = PROJECT_DELETE_SUB_TYPE,
            bizNo = "{{#id}}", success = PROJECT_DELETE_SUCCESS)
    public void deleteProject(Long id) {
        validateProjectExists(id);
        projectMapper.deleteById(id);
    }

    @Override
    @LogRecord(type = PROJECT_TYPE, subType = PROJECT_BATCH_DELETE_SUB_TYPE,
            bizNo = "{{#ids}}", success = PROJECT_BATCH_DELETE_SUCCESS)
    public void deleteProjectListByIds(Set<Long> ids) {
        projectMapper.deleteByIds(ids);
    }

    private void validateProjectExists(Long id) {
        if (projectMapper.selectById(id) == null) {
            throw exception(PROJECT_NOT_EXISTS);
        }
    }

    @Override
    public ProjectRespVO getProject(Long id) {
        ProjectDO project = projectMapper.selectById(id);
        return convertToRespVO(project);
    }

    @Override
    public PageResult<ProjectRespVO> getProjectPage(ProjectPageReqVO pageReqVO) {
        PageResult<ProjectDO> pageResult = projectMapper.selectPage(pageReqVO);
        return convertToPageResult(pageResult);
    }

    @Override
    public List<ProjectRespVO> getProjectListByFilingId(Long filingId) {
        List<ProjectDO> list = projectMapper.selectListByFilingId(filingId);
        return BeanUtils.toBean(list, ProjectRespVO.class);
    }

    @Override
    @LogRecord(type = PROJECT_TYPE, subType = PROJECT_STATUS_CHANGE_SUB_TYPE,
            bizNo = "{{#id}}", success = PROJECT_STATUS_CHANGE_SUCCESS)
    public void updateProjectStatus(Long id, String status) {
        validateProjectExists(id);
        ProjectDO updateObj = new ProjectDO();
        updateObj.setId(id);
        updateObj.setProjectStatus(status);
        projectMapper.updateById(updateObj);
    }

    @Override
    public List<ProjectDO> getProjectsByStatus(ProjectStatus status) {
        return projectMapper.selectListByStatus(status.getStatus());
    }

    @Override
    public List<ProjectDO> getProjectsForAcceptanceCheck() {
        return projectMapper.selectListForAcceptanceCheck();
    }

    @Override
    public List<ProjectRespVO> getProjectSimpleList() {
        List<ProjectDO> list = projectMapper.selectAll();
        return BeanUtils.toBean(list, ProjectRespVO.class);
    }

    @Override
    public void markProjectCompleted(Long id) {
        validateProjectExists(id);
        ProjectDO updateObj = new ProjectDO();
        updateObj.setId(id);
        updateObj.setActualEndTime(LocalDateTime.now());
        updateObj.setActualProgress(100); // 验收完成时进度设为100%
        projectMapper.updateById(updateObj);
        log.info("[markProjectCompleted] 项目已标记为完成: projectId={}", id);
    }

    @Override
    public void updateProjectProgress(Long id, Integer progress) {
        validateProjectExists(id);
        // 确保进度值在有效范围内
        if (progress == null) {
            progress = 0;
        }
        progress = Math.max(0, Math.min(100, progress));

        ProjectDO updateObj = new ProjectDO();
        updateObj.setId(id);
        updateObj.setActualProgress(progress);
        projectMapper.updateById(updateObj);
        log.info("[updateProjectProgress] 项目进度已更新: projectId={}, progress={}%", id, progress);
    }

    private ProjectRespVO convertToRespVO(ProjectDO project) {
        if (project == null) {
            return null;
        }
        return BeanUtils.toBean(project, ProjectRespVO.class);
    }

    private PageResult<ProjectRespVO> convertToPageResult(PageResult<ProjectDO> pageResult) {
        if (pageResult == null) {
            return null;
        }
        List<ProjectRespVO> list = BeanUtils.toBean(pageResult.getList(), ProjectRespVO.class);
        return new PageResult<>(list, pageResult.getTotal());
    }

}
