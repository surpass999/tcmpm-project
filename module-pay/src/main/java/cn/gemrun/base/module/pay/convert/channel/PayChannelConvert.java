package cn.gemrun.base.module.pay.convert.channel;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.pay.controller.admin.channel.vo.PayChannelCreateReqVO;
import cn.gemrun.base.module.pay.controller.admin.channel.vo.PayChannelRespVO;
import cn.gemrun.base.module.pay.controller.admin.channel.vo.PayChannelUpdateReqVO;
import cn.gemrun.base.module.pay.dal.dataobject.channel.PayChannelDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PayChannelConvert {

    PayChannelConvert INSTANCE = Mappers.getMapper(PayChannelConvert.class);

    @Mapping(target = "config",ignore = true)
    PayChannelDO convert(PayChannelCreateReqVO bean);

    @Mapping(target = "config",ignore = true)
    PayChannelDO convert(PayChannelUpdateReqVO bean);

    @Mapping(target = "config",expression = "java(cn.gemrun.base.framework.common.util.json.JsonUtils.toJsonString(bean.getConfig()))")
    PayChannelRespVO convert(PayChannelDO bean);

    PageResult<PayChannelRespVO> convertPage(PageResult<PayChannelDO> page);

}
