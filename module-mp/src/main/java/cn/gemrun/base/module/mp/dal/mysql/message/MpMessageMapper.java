package cn.gemrun.base.module.mp.dal.mysql.message;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.gemrun.base.module.mp.controller.admin.message.vo.message.MpMessagePageReqVO;
import cn.gemrun.base.module.mp.dal.dataobject.message.MpMessageDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MpMessageMapper extends BaseMapperX<MpMessageDO> {

    default PageResult<MpMessageDO> selectPage(MpMessagePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MpMessageDO>()
                .eqIfPresent(MpMessageDO::getAccountId, reqVO.getAccountId())
                .eqIfPresent(MpMessageDO::getType, reqVO.getType())
                .eqIfPresent(MpMessageDO::getOpenid, reqVO.getOpenid())
                .eqIfPresent(MpMessageDO::getUserId, reqVO.getUserId())
                .betweenIfPresent(MpMessageDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MpMessageDO::getId));
    }

}
