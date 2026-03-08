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

    /**
     * 带数据权限过滤的查询
     * 过滤条件：(creator = currentUserId) OR (id IN businessIds)
     *
     * @param reqVO      查询参数
     * @param currentUserId 当前用户ID（creator字段值）
     * @param businessIds   参与的业务ID列表（从 bpm_business_process 查询得到）
     * @return 分页结果
     */
    default PageResult<FilingDO> selectPageWithDataPermission(FilingPageReqVO reqVO, String currentUserId, Set<Long> businessIds) {
        LambdaQueryWrapperX<FilingDO> wrapper = new LambdaQueryWrapperX<>();

        // 条件1：发起人自己创建的
        wrapper.eq(FilingDO::getCreator, currentUserId);

        // 条件2：参与过的业务（businessIds 不为空时）
        if (businessIds != null && !businessIds.isEmpty()) {
            wrapper.or();
            wrapper.in(FilingDO::getId, businessIds);
        }

        // 其他查询条件
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
     * 不带数据权限过滤的查询（管理员用）
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

}