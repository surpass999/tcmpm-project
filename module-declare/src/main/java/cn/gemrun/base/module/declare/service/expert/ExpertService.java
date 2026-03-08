package cn.gemrun.base.module.declare.service.expert;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.declare.controller.admin.expert.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.expert.ExpertDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 专家管理 Service 接口
 *
 * @author Gemini
 */
public interface ExpertService {

    /**
     * 创建专家
     *
     * @param createReqVO 创建信息
     * @return 专家编号
     */
    Long createExpert(@Valid ExpertSaveReqVO createReqVO);

    /**
     * 更新专家
     *
     * @param updateReqVO 更新信息
     */
    void updateExpert(@Valid ExpertSaveReqVO updateReqVO);

    /**
     * 更新专家状态
     *
     * @param id     专家编号
     * @param status 状态
     */
    void updateExpertStatus(Long id, Integer status);

    /**
     * 删除专家
     *
     * @param id 专家编号
     */
    void deleteExpert(Long id);

    /**
     * 获得专家
     *
     * @param id 专家编号
     * @return 专家
     */
    ExpertDO getExpert(Long id);

    /**
     * 根据用户ID获取专家
     *
     * @param userId 系统用户ID
     * @return 专家信息
     */
    ExpertDO getExpertByUserId(Long userId);

    /**
     * 获取专家列表
     *
     * @param ids 专家编号列表
     * @return 专家列表
     */
    List<ExpertDO> getExpertListByIds(List<Long> ids);

    /**
     * 获取专家分页
     *
     * @param pageReqVO 分页查询
     * @return 分页结果
     */
    PageResult<ExpertDO> getExpertPage(ExpertPageReqVO pageReqVO);

    /**
     * 获取专家列表（带筛选条件）
     *
     * @param reqVO 查询条件
     * @return 专家列表
     */
    List<ExpertDO> getExpertList(ExpertListReqVO reqVO);

    /**
     * 获取所有绑定用户的专家的userId列表
     *
     * @return userId列表
     */
    List<Long> getExpertUserIds();

}
