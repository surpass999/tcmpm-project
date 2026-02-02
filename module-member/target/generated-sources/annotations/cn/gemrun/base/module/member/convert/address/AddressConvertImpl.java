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
    date = "2026-02-02T15:54:16+0800",
    comments = "version: 1.6.3, compiler: javac, environment: Java 1.8.0_371 (Oracle Corporation)"
)
public class AddressConvertImpl implements AddressConvert {

    @Override
    public MemberAddressDO convert(AppAddressCreateReqVO bean) {
        if ( bean == null ) {
            return null;
        }

        MemberAddressDO.MemberAddressDOBuilder memberAddressDO = MemberAddressDO.builder();

        memberAddressDO.name( bean.getName() );
        memberAddressDO.mobile( bean.getMobile() );
        memberAddressDO.areaId( bean.getAreaId() );
        memberAddressDO.detailAddress( bean.getDetailAddress() );
        memberAddressDO.defaultStatus( bean.getDefaultStatus() );

        return memberAddressDO.build();
    }

    @Override
    public MemberAddressDO convert(AppAddressUpdateReqVO bean) {
        if ( bean == null ) {
            return null;
        }

        MemberAddressDO.MemberAddressDOBuilder memberAddressDO = MemberAddressDO.builder();

        memberAddressDO.id( bean.getId() );
        memberAddressDO.name( bean.getName() );
        memberAddressDO.mobile( bean.getMobile() );
        memberAddressDO.areaId( bean.getAreaId() );
        memberAddressDO.detailAddress( bean.getDetailAddress() );
        memberAddressDO.defaultStatus( bean.getDefaultStatus() );

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
        appAddressRespVO.setName( bean.getName() );
        appAddressRespVO.setMobile( bean.getMobile() );
        appAddressRespVO.setAreaId( bean.getAreaId() );
        appAddressRespVO.setDetailAddress( bean.getDetailAddress() );
        appAddressRespVO.setDefaultStatus( bean.getDefaultStatus() );
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

        memberAddressRespDTO.setId( bean.getId() );
        memberAddressRespDTO.setUserId( bean.getUserId() );
        memberAddressRespDTO.setName( bean.getName() );
        memberAddressRespDTO.setMobile( bean.getMobile() );
        if ( bean.getAreaId() != null ) {
            memberAddressRespDTO.setAreaId( bean.getAreaId().intValue() );
        }
        memberAddressRespDTO.setDetailAddress( bean.getDetailAddress() );
        memberAddressRespDTO.setDefaultStatus( bean.getDefaultStatus() );

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

        addressRespVO.setName( memberAddressDO.getName() );
        addressRespVO.setMobile( memberAddressDO.getMobile() );
        addressRespVO.setAreaId( memberAddressDO.getAreaId() );
        addressRespVO.setDetailAddress( memberAddressDO.getDetailAddress() );
        addressRespVO.setDefaultStatus( memberAddressDO.getDefaultStatus() );
        addressRespVO.setId( memberAddressDO.getId() );
        addressRespVO.setCreateTime( memberAddressDO.getCreateTime() );

        return addressRespVO;
    }
}
