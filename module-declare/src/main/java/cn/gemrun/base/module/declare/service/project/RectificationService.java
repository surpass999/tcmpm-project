package cn.gemrun.base.module.declare.service.project;

import java.util.Set;

import javax.validation.*;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.declare.controller.admin.project.vo.*;

/**
 * 整改记录 Service 接口
 *
 * @author Gemini
 */
public interface RectificationService {

    /**
     * 创建整改记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createRectification(@Valid RectificationSaveReqVO createReqVO);

    /**
     * 更新整改记录
     *
     * @param updateReqVO 更新信息
     */
    void updateRectification(@Valid RectificationSaveReqVO updateReqVO);

    /**
     * 删除整改记录
     *
     * @param id 编号
     */
    void deleteRectification(Long id);

    /**
     * 批量删除整改记录
     *
     * @param ids 编号集合
     */
    void deleteRectificationListByIds(Set<Long> ids);

    /**
     * 获得整改记录
     *
     * @param id 编号
     * @return 整改记录
     */
    RectificationRespVO getRectification(Long id);

    /**
     * 获得整改记录分页
     *
     * @param pageReqVO 分页查询
     * @return 整改记录分页
     */
    PageResult<RectificationRespVO> getRectificationPage(RectificationPageReqVO pageReqVO);

    /**
     * 根据项目ID查询整改记录列表
     *
     * @param projectId 项目ID
     * @return 整改记录列表
     */
    java.util.List<RectificationRespVO> getRectificationListByProjectId(Long projectId);

    /**
     * 根据过程记录ID查询整改记录列表
     *
     * @param processId 过程记录ID
     * @return 整改记录列表
     */
    java.util.List<RectificationRespVO> getRectificationListByProcessId(Long processId);

}
