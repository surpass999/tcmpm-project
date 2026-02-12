package cn.gemrun.base.module.member.convert.address;

import cn.gemrun.base.module.member.api.address.dto.MemberAddressRespDTO;
import cn.gemrun.base.module.member.controller.admin.address.vo.AddressRespVO;
import cn.gemrun.base.module.member.controller.app.address.vo.AppAddressCreateReqVO;
import cn.gemrun.base.module.member.controller.app.address.vo.AppAddressRespVO;
import cn.gemrun.base.module.member.controller.app.address.vo.AppAddressUpdateReqVO;
import cn.gemrun.base.module.member.dal.dataobject.address.MemberAddressDO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-12T21:04:58+0800",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.45.0.v20260128-0750, environment: Java 21.0.9 (Eclipse Adoptium)"
)
public class AddressConvertImpl implements AddressConvert {

    @Override
    public MemberAddressDO convert(AppAddressCreateReqVO bean) {
        if ( bean == null ) {
            return null;
        }

        MemberAddressDO.MemberAddressDOBuilder memberAddressDO = MemberAddressDO.builder();

        memberAddressDO.areaId( bean.getAreaId() );
        memberAddressDO.defaultStatus( bean.getDefaultStatus() );
        memberAddressDO.detailAddress( bean.getDetailAddress() );
        memberAddressDO.mobile( bean.getMobile() );
        memberAddressDO.name( bean.getName() );

        return memberAddressDO.build();
    }

    @Override
    public MemberAddressDO convert(AppAddressUpdateReqVO bean) {
        if ( bean == null ) {
            return null;
        }

        MemberAddressDO.MemberAddressDOBuilder memberAddressDO = MemberAddressDO.builder();

        memberAddressDO.areaId( bean.getAreaId() );
        memberAddressDO.defaultStatus( bean.getDefaultStatus() );
        memberAddressDO.detailAddress( bean.getDetailAddress() );
        memberAddressDO.id( bean.getId() );
        memberAddressDO.mobile( bean.getMobile() );
        memberAddressDO.name( bean.getName() );

        return memberAddressDO.build();
    }

    @Override
    public AppAddressRespVO convert(MemberAddressDO bean) {
        if ( bean == null ) {
            return null;
        }

        AppAddressRespVO appAddressRespVO = new AppAddressRespVO();

        if ( bean.getAreaId() != null ) {
            appAddressRespVO.setAreaName( convertAreaIdToAreaName( bean.getAreaId().intValue() ) );
        }
        appAddressRespVO.setAreaId( bean.getAreaId() );
        appAddressRespVO.setDefaultStatus( bean.getDefaultStatus() );
        appAddressRespVO.setDetailAddress( bean.getDetailAddress() );
        appAddressRespVO.setMobile( bean.getMobile() );
        appAddressRespVO.setName( bean.getName() );
        appAddressRespVO.setId( bean.getId() );

        return appAddressRespVO;
    }

    @Override
    public List<AppAddressRespVO> convertList(List<MemberAddressDO> list) {
        if ( list == null ) {
            return null;
        }

        List<AppAddressRespVO> list1 = new ArrayList<AppAddressRespVO>( list.size() );
        for ( MemberAddressDO memberAddressDO : list ) {
            list1.add( convert( memberAddressDO ) );
        }

        return list1;
    }

    @Override
    public MemberAddressRespDTO convert02(MemberAddressDO bean) {
        if ( bean == null ) {
            return null;
        }

        MemberAddressRespDTO memberAddressRespDTO = new MemberAddressRespDTO();

        if ( bean.getAreaId() != null ) {
            memberAddressRespDTO.setAreaId( bean.getAreaId().intValue() );
        }
        memberAddressRespDTO.setDefaultStatus( bean.getDefaultStatus() );
        memberAddressRespDTO.setDetailAddress( bean.getDetailAddress() );
        memberAddressRespDTO.setId( bean.getId() );
        memberAddressRespDTO.setMobile( bean.getMobile() );
        memberAddressRespDTO.setName( bean.getName() );
        memberAddressRespDTO.setUserId( bean.getUserId() );

        return memberAddressRespDTO;
    }

    @Override
    public List<AddressRespVO> convertList2(List<MemberAddressDO> list) {
        if ( list == null ) {
            return null;
        }

        List<AddressRespVO> list1 = new ArrayList<AddressRespVO>( list.size() );
        for ( MemberAddressDO memberAddressDO : list ) {
            list1.add( memberAddressDOToAddressRespVO( memberAddressDO ) );
        }

        return list1;
    }

    protected AddressRespVO memberAddressDOToAddressRespVO(MemberAddressDO memberAddressDO) {
        if ( memberAddressDO == null ) {
            return null;
        }

        AddressRespVO addressRespVO = new AddressRespVO();

        addressRespVO.setAreaId( memberAddressDO.getAreaId() );
        addressRespVO.setDefaultStatus( memberAddressDO.getDefaultStatus() );
        addressRespVO.setDetailAddress( memberAddressDO.getDetailAddress() );
        addressRespVO.setMobile( memberAddressDO.getMobile() );
        addressRespVO.setName( memberAddressDO.getName() );
        addressRespVO.setCreateTime( memberAddressDO.getCreateTime() );
        addressRespVO.setId( memberAddressDO.getId() );

        return addressRespVO;
    }
}
