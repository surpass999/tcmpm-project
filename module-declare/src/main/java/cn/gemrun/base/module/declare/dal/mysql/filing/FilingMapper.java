package cn.gemrun.base.module.declare.dal.mysql.filing;

import java.util.*;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import cn.gemrun.base.module.declare.dal.dataobject.filing.FilingDO;
import org.apache.ibatis.annotations.Mapper;
import cn.gemrun.base.module.declare.controller.admin.filing.vo.*;

/**
 * 项目备案核心信息 Mapper
 *
 * 数据权限：使用系统默认的部门数据权限（根据角色配置自动过滤）
 * - 全部数据权限：无过滤
 * - 本部门及以下：WHERE dept_id IN (本部门, 子部门)
 * - 仅本人：WHERE creator = 当前用户
 *
 * @author 芋道源码
 */
@Mapper
public interface FilingMapper extends BaseMapperX<FilingDO> {

    /**
     * 分页查询（使用系统数据权限自动过滤）
     */
    default PageResult<FilingDO> selectPage(FilingPageReqVO reqVO) {
        LambdaQueryWrapperX<FilingDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(FilingDO::getSocialCreditCode, reqVO.getSocialCreditCode())
                .eqIfPresent(FilingDO::getMedicalLicenseNo, reqVO.getMedicalLicenseNo())
                .likeIfPresent(FilingDO::getOrgName, reqVO.getOrgName())
                .eqIfPresent(FilingDO::getProjectType, reqVO.getProjectType())
                .betweenIfPresent(FilingDO::getValidStartTime, reqVO.getValidStartTime())
                .betweenIfPresent(FilingDO::getValidEndTime, reqVO.getValidEndTime())
                .eqIfPresent(FilingDO::getConstructionContent, reqVO.getConstructionContent())
                .eqIfPresent(FilingDO::getFilingStatus, reqVO.getFilingStatus())
                .eqIfPresent(FilingDO::getProvinceReviewOpinion, reqVO.getProvinceReviewOpinion())
                .betweenIfPresent(FilingDO::getProvinceReviewTime, reqVO.getProvinceReviewTime())
                .eqIfPresent(FilingDO::getProvinceReviewerId, reqVO.getProvinceReviewerId())
                .eqIfPresent(FilingDO::getExpertReviewOpinion, reqVO.getExpertReviewOpinion())
                .eqIfPresent(FilingDO::getExpertReviewerIds, reqVO.getExpertReviewerIds())
                .betweenIfPresent(FilingDO::getFilingArchiveTime, reqVO.getFilingArchiveTime())
                .betweenIfPresent(FilingDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(FilingDO::getId);
        return selectPage(reqVO, wrapper);
    }

    /**
     * 查询当天最大的备案编号
     * @param likePattern 模糊匹配模式，如 PRNT20260314%
     * @return 最大编号
     */
    String selectMaxFilingCode(String likePattern);

}