package cn.gemrun.base.module.declare.dal.mysql.policy;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.gemrun.base.module.declare.controller.admin.policy.vo.NoticeReceiptPageReqVO;
import cn.gemrun.base.module.declare.dal.dataobject.policy.NoticeReceiptDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 政策通知回执 Mapper
 *
 * @author Gemini
 */
@Mapper
public interface NoticeReceiptMapper extends BaseMapperX<NoticeReceiptDO> {

    default PageResult<NoticeReceiptDO> selectPage(NoticeReceiptPageReqVO reqVO) {
        LambdaQueryWrapperX<NoticeReceiptDO> wrapper = new LambdaQueryWrapperX<>();

        wrapper.eqIfPresent(NoticeReceiptDO::getPolicyId, reqVO.getPolicyId())
                .eqIfPresent(NoticeReceiptDO::getReaderId, reqVO.getReaderId())
                .eqIfPresent(NoticeReceiptDO::getOrgName, reqVO.getOrgName())
                .eqIfPresent(NoticeReceiptDO::getReadStatus, reqVO.getReadStatus())
                .eqIfPresent(NoticeReceiptDO::getReceiptStatus, reqVO.getReceiptStatus())
                .orderByDesc(NoticeReceiptDO::getCreateTime);

        return selectPage(reqVO, wrapper);
    }

    /**
     * 根据政策ID查询回执列表
     */
    default List<NoticeReceiptDO> selectByPolicyId(Long policyId) {
        return selectList(new LambdaQueryWrapperX<NoticeReceiptDO>()
                .eq(NoticeReceiptDO::getPolicyId, policyId)
                .orderByDesc(NoticeReceiptDO::getCreateTime));
    }

    /**
     * 根据用户ID查询其收到的通知回执
     */
    default List<NoticeReceiptDO> selectByReaderId(Long readerId) {
        return selectList(new LambdaQueryWrapperX<NoticeReceiptDO>()
                .eq(NoticeReceiptDO::getReaderId, readerId)
                .orderByDesc(NoticeReceiptDO::getCreateTime));
    }

    /**
     * 检查用户是否已阅读某政策
     */
    default NoticeReceiptDO selectByPolicyIdAndReaderId(Long policyId, Long readerId) {
        return selectOne(new LambdaQueryWrapperX<NoticeReceiptDO>()
                .eq(NoticeReceiptDO::getPolicyId, policyId)
                .eq(NoticeReceiptDO::getReaderId, readerId));
    }

}
