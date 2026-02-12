package cn.gemrun.base.module.member.convert.config;

import cn.gemrun.base.module.member.api.config.dto.MemberConfigRespDTO;
import cn.gemrun.base.module.member.controller.admin.config.vo.MemberConfigRespVO;
import cn.gemrun.base.module.member.controller.admin.config.vo.MemberConfigSaveReqVO;
import cn.gemrun.base.module.member.dal.dataobject.config.MemberConfigDO;
import javax.annotation.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-12T21:04:58+0800",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.45.0.v20260128-0750, environment: Java 21.0.9 (Eclipse Adoptium)"
)
public class MemberConfigConvertImpl implements MemberConfigConvert {

    @Override
    public MemberConfigRespVO convert(MemberConfigDO bean) {
        if ( bean == null ) {
            return null;
        }

        MemberConfigRespVO memberConfigRespVO = new MemberConfigRespVO();

        memberConfigRespVO.setPointTradeDeductEnable( bean.getPointTradeDeductEnable() );
        memberConfigRespVO.setPointTradeDeductMaxPrice( bean.getPointTradeDeductMaxPrice() );
        memberConfigRespVO.setPointTradeDeductUnitPrice( bean.getPointTradeDeductUnitPrice() );
        memberConfigRespVO.setPointTradeGivePoint( bean.getPointTradeGivePoint() );
        memberConfigRespVO.setId( bean.getId() );

        return memberConfigRespVO;
    }

    @Override
    public MemberConfigDO convert(MemberConfigSaveReqVO bean) {
        if ( bean == null ) {
            return null;
        }

        MemberConfigDO.MemberConfigDOBuilder memberConfigDO = MemberConfigDO.builder();

        memberConfigDO.pointTradeDeductEnable( bean.getPointTradeDeductEnable() );
        memberConfigDO.pointTradeDeductMaxPrice( bean.getPointTradeDeductMaxPrice() );
        memberConfigDO.pointTradeDeductUnitPrice( bean.getPointTradeDeductUnitPrice() );
        memberConfigDO.pointTradeGivePoint( bean.getPointTradeGivePoint() );

        return memberConfigDO.build();
    }

    @Override
    public MemberConfigRespDTO convert01(MemberConfigDO config) {
        if ( config == null ) {
            return null;
        }

        MemberConfigRespDTO memberConfigRespDTO = new MemberConfigRespDTO();

        memberConfigRespDTO.setPointTradeDeductEnable( config.getPointTradeDeductEnable() );
        memberConfigRespDTO.setPointTradeDeductMaxPrice( config.getPointTradeDeductMaxPrice() );
        memberConfigRespDTO.setPointTradeDeductUnitPrice( config.getPointTradeDeductUnitPrice() );
        memberConfigRespDTO.setPointTradeGivePoint( config.getPointTradeGivePoint() );

        return memberConfigRespDTO;
    }
}
