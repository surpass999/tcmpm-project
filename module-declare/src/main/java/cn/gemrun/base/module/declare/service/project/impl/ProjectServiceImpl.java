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
import cn.gemrun.base.module.declare.dal.mysql.project.ProjectMapper;
import cn.gemrun.base.module.declare.enums.ProjectStatus;
import cn.gemrun.base.module.declare.service.project.ProjectService;
import cn.gemrun.base.module.system.api.user.AdminUserApi;
import cn.gemrun.base.module.system.api.user.dto.AdminUserRespDTO;
import cn.gemrun.base.framework.web.core.util.WebFrameworkUtils;

import static cn.gemrun.base.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.gemrun.base.module.declare.enums.ErrorCodeConstants.*;

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
    public void updateProject(ProjectSaveReqVO updateReqVO) {
        validateProjectExists(updateReqVO.getId());
        ProjectDO updateObj = BeanUtils.toBean(updateReqVO, ProjectDO.class);
        projectMapper.updateById(updateObj);
    }

    @Override
    public void deleteProject(Long id) {
        validateProjectExists(id);
        projectMapper.deleteById(id);
    }

    @Override
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
