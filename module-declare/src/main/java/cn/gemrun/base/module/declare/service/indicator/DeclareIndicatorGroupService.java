package cn.gemrun.base.module.declare.service.indicator;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.declare.controller.admin.indicator.vo.DeclareIndicatorGroupPageReqVO;
import cn.gemrun.base.module.declare.controller.admin.indicator.vo.DeclareIndicatorGroupRespVO;
import cn.gemrun.base.module.declare.controller.admin.indicator.vo.DeclareIndicatorGroupSaveReqVO;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorGroupDO;

import java.util.List;

/**
 * 指标分组 Service 接口
 *
 * @author Gemini
 */
public interface DeclareIndicatorGroupService {

    /**
     * 创建分组
     */
    Long createGroup(DeclareIndicatorGroupSaveReqVO createReqVO);

    /**
     * 更新分组
     */
    void updateGroup(DeclareIndicatorGroupSaveReqVO updateReqVO);

    /**
     * 删除分组
     */
    void deleteGroup(Long id);

    /**
     * 获取分组
     */
    DeclareIndicatorGroupDO getGroup(Long id);

    /**
     * 获取分组详情
     */
    DeclareIndicatorGroupRespVO getGroupResp(Long id);

    /**
     * 获取分组分页
     */
    PageResult<DeclareIndicatorGroupDO> getGroupPage(DeclareIndicatorGroupPageReqVO pageReqVO);

    /**
     * 获取树形分组列表
     */
    List<DeclareIndicatorGroupRespVO> getGroupTree();

    /**
     * 获取所有启用的分组列表
     */
    List<DeclareIndicatorGroupDO> getAllEnabledList();

    /**
     * 获取一级分组列表
     * @param projectType 项目类型，可选，null时返回全部
     */
    List<DeclareIndicatorGroupDO> getLevelOneList(Integer projectType);

    /**
     * 获取二级分组列表
     */
    List<DeclareIndicatorGroupDO> getLevelTwoList();

    /**
     * 根据项目类型获取一级分组
     */
    List<DeclareIndicatorGroupDO> getLevelOneListByProjectType(Integer projectType);

    /**
     * 根据一级分组ID获取二级分组
     */
    List<DeclareIndicatorGroupDO> getLevelTwoListByParentId(Long parentId);

    /**
     * 根据项目类型获取树形分组列表
     * @param projectType 项目类型，null或0表示全部项目
     */
    List<DeclareIndicatorGroupRespVO> getGroupTreeByProjectType(Integer projectType);

}
