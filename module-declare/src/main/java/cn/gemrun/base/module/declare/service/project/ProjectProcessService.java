package cn.gemrun.base.module.declare.service.project;

import java.util.Set;

import javax.validation.*;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.declare.controller.admin.project.vo.*;

/**
 * 项目过程记录 Service 接口
 *
 * @author Gemini
 */
public interface ProjectProcessService {

    /**
     * 创建项目过程记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createProjectProcess(@Valid ProjectProcessSaveReqVO createReqVO);

    /**
     * 更新项目过程记录
     *
     * @param updateReqVO 更新信息
     */
    void updateProjectProcess(@Valid ProjectProcessSaveReqVO updateReqVO);

    /**
     * 删除项目过程记录
     *
     * @param id 编号
     */
    void deleteProjectProcess(Long id);

    /**
     * 批量删除项目过程记录
     *
     * @param ids 编号集合
     */
    void deleteProjectProcessListByIds(Set<Long> ids);

    /**
     * 获得项目过程记录
     *
     * @param id 编号
     * @return 项目过程记录
     */
    ProjectProcessRespVO getProjectProcess(Long id);

    /**
     * 获得项目过程记录分页
     *
     * @param pageReqVO 分页查询
     * @return 项目过程记录分页
     */
    PageResult<ProjectProcessRespVO> getProjectProcessPage(ProjectProcessPageReqVO pageReqVO);

    /**
     * 根据项目ID查询过程记录列表
     *
     * @param projectId 项目ID
     * @return 过程记录列表
     */
    java.util.List<ProjectProcessRespVO> getProjectProcessListByProjectId(Long projectId);

    /**
     * 更新项目过程记录状态
     *
     * @param id     过程记录ID
     * @param status 状态
     */
    void updateProjectProcessStatus(Long id, String status);

    /**
     * 检查是否存在指定类型和年份的过程记录
     *
     * @param projectId  项目ID
     * @param processType 过程类型
     * @param year      年份
     * @return 是否存在
     */
    boolean hasProcess(Long projectId, Integer processType, Integer year);

    /**
     * 检查是否存在验收流程
     *
     * @param projectId 项目ID
     * @return 是否存在
     */
    boolean hasAcceptanceProcess(Long projectId);

    /**
     * 为项目创建过程记录（定时任务用）
     *
     * @param projectId         项目ID
     * @param processType       过程类型
     * @param title             标题
     * @param reportPeriodStart 报告周期开始时间
     * @param reportPeriodEnd   报告周期结束时间
     * @return 过程记录ID
     */
    Long createProcessForProject(Long projectId, Integer processType, String title,
                                  java.time.LocalDateTime reportPeriodStart,
                                  java.time.LocalDateTime reportPeriodEnd);

    /**
     * 根据项目ID查询整改记录列表（包含过程记录和子流程）
     *
     * @param projectId 项目ID
     * @return 整改记录列表
     */
    java.util.List<RectificationRecordRespVO> getRectificationRecordList(Long projectId);

}
