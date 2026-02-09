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
 * @author 芋道源码
 */
@Mapper
public interface FilingMapper extends BaseMapperX<FilingDO> {

    default PageResult<FilingDO> selectPage(FilingPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<FilingDO>()
                .eqIfPresent(FilingDO::getSocialCreditCode, reqVO.getSocialCreditCode())
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
                .orderByDesc(FilingDO::getId));
    }

}