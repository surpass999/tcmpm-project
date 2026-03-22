package cn.gemrun.base.module.declare.service.project;

import java.util.List;
import java.util.Set;

import javax.validation.*;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.declare.controller.admin.project.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.project.ProjectDO;
import cn.gemrun.base.module.declare.enums.ProjectStatus;

/**
 * 项目信息 Service 接口
 *
 * @author Gemini
 */
public interface ProjectService {

    /**
     * 创建项目信息
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createProject(@Valid ProjectSaveReqVO createReqVO);

    /**
     * 更新项目信息
     *
     * @param updateReqVO 更新信息
     */
    void updateProject(@Valid ProjectSaveReqVO updateReqVO);

    /**
     * 删除项目信息
     *
     * @param id 编号
     */
    void deleteProject(Long id);

    /**
     * 批量删除项目信息
     *
     * @param ids 编号集合
     */
    void deleteProjectListByIds(Set<Long> ids);

    /**
     * 获得项目信息
     *
     * @param id 编号
     * @return 项目信息
     */
    ProjectRespVO getProject(Long id);

    /**
     * 获得项目信息分页
     *
     * @param pageReqVO 分页查询
     * @return 项目信息分页
     */
    PageResult<ProjectRespVO> getProjectPage(ProjectPageReqVO pageReqVO);

    /**
     * 根据备案ID查询项目列表
     *
     * @param filingId 备案ID
     * @return 项目列表
     */
    java.util.List<ProjectRespVO> getProjectListByFilingId(Long filingId);

    /**
     * 更新项目状态
     *
     * @param id     项目ID
     * @param status 状态（字典值）
     */
    void updateProjectStatus(Long id, String status);

    /**
     * 根据项目状态查询项目列表
     *
     * @param status 项目状态
     * @return 项目列表
     */
    List<ProjectDO> getProjectsByStatus(ProjectStatus status);

    /**
     * 获取需要验收检查的项目列表
     * 查询"建设中"或"中期评估"状态的项目
     *
     * @return 项目列表
     */
    List<ProjectDO> getProjectsForAcceptanceCheck();

    /**
     * 获取项目列表（用于下拉选择）
     * 只返回id和projectName
     *
     * @return 项目列表
     */
    List<ProjectRespVO> getProjectSimpleList();

    /**
     * 标记项目已完成验收
     * 设置 actual_end_time 为当前时间
     *
     * @param id 项目ID
     */
    void markProjectCompleted(Long id);

    /**
     * 更新项目进度
     *
     * @param id       项目ID
     * @param progress 进度值（0-100）
     */
    void updateProjectProgress(Long id, Integer progress);

}
