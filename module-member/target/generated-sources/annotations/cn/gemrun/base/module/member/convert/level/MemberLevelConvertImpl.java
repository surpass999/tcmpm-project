package cn.gemrun.base.module.member.convert.level;

import cn.gemrun.base.module.member.api.level.dto.MemberLevelRespDTO;
import cn.gemrun.base.module.member.controller.admin.level.vo.level.MemberLevelCreateReqVO;
import cn.gemrun.base.module.member.controller.admin.level.vo.level.MemberLevelRespVO;
import cn.gemrun.base.module.member.controller.admin.level.vo.level.MemberLevelSimpleRespVO;
import cn.gemrun.base.module.member.controller.admin.level.vo.level.MemberLevelUpdateReqVO;
import cn.gemrun.base.module.member.controller.app.level.vo.level.AppMemberLevelRespVO;
import cn.gemrun.base.module.member.dal.dataobject.level.MemberLevelDO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-12T21:04:58+0800",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.45.0.v20260128-0750, environment: Java 21.0.9 (Eclipse Adoptium)"
)
public class MemberLevelConvertImpl implements MemberLevelConvert {

    @Override
    public MemberLevelDO convert(MemberLevelCreateReqVO bean) {
        if ( bean == null ) {
            return null;
        }

        MemberLevelDO.MemberLevelDOBuilder memberLevelDO = MemberLevelDO.builder();

        memberLevelDO.backgroundUrl( bean.getBackgroundUrl() );
        memberLevelDO.discountPercent( bean.getDiscountPercent() );
        memberLevelDO.experience( bean.getExperience() );
        memberLevelDO.icon( bean.getIcon() );
        memberLevelDO.level( bean.getLevel() );
        memberLevelDO.name( bean.getName() );
        memberLevelDO.status( bean.getStatus() );

        return memberLevelDO.build();
    }

    @Override
    public MemberLevelDO convert(MemberLevelUpdateReqVO bean) {
        if ( bean == null ) {
            return null;
        }

        MemberLevelDO.MemberLevelDOBuilder memberLevelDO = MemberLevelDO.builder();

        memberLevelDO.backgroundUrl( bean.getBackgroundUrl() );
        memberLevelDO.discountPercent( bean.getDiscountPercent() );
        memberLevelDO.experience( bean.getExperience() );
        memberLevelDO.icon( bean.getIcon() );
        memberLevelDO.id( bean.getId() );
        memberLevelDO.level( bean.getLevel() );
        memberLevelDO.name( bean.getName() );
        memberLevelDO.status( bean.getStatus() );

        return memberLevelDO.build();
    }

    @Override
    public MemberLevelRespVO convert(MemberLevelDO bean) {
        if ( bean == null ) {
            return null;
        }

        MemberLevelRespVO memberLevelRespVO = new MemberLevelRespVO();

        memberLevelRespVO.setBackgroundUrl( bean.getBackgroundUrl() );
        memberLevelRespVO.setDiscountPercent( bean.getDiscountPercent() );
        memberLevelRespVO.setExperience( bean.getExperience() );
        memberLevelRespVO.setIcon( bean.getIcon() );
        memberLevelRespVO.setLevel( bean.getLevel() );
        memberLevelRespVO.setName( bean.getName() );
        memberLevelRespVO.setStatus( bean.getStatus() );
        memberLevelRespVO.setCreateTime( bean.getCreateTime() );
        memberLevelRespVO.setId( bean.getId() );

        return memberLevelRespVO;
    }

    @Override
    public List<MemberLevelRespVO> convertList(List<MemberLevelDO> list) {
        if ( list == null ) {
            return null;
        }

        List<MemberLevelRespVO> list1 = new ArrayList<MemberLevelRespVO>( list.size() );
        for ( MemberLevelDO memberLevelDO : list ) {
            list1.add( convert( memberLevelDO ) );
        }

        return list1;
    }

    @Override
    public List<MemberLevelSimpleRespVO> convertSimpleList(List<MemberLevelDO> list) {
        if ( list == null ) {
            return null;
        }

        List<MemberLevelSimpleRespVO> list1 = new ArrayList<MemberLevelSimpleRespVO>( list.size() );
        for ( MemberLevelDO memberLevelDO : list ) {
            list1.add( memberLevelDOToMemberLevelSimpleRespVO( memberLevelDO ) );
        }

        return list1;
    }

    @Override
    public List<AppMemberLevelRespVO> convertList02(List<MemberLevelDO> list) {
        if ( list == null ) {
            return null;
        }

        List<AppMemberLevelRespVO> list1 = new ArrayList<AppMemberLevelRespVO>( list.size() );
        for ( MemberLevelDO memberLevelDO : list ) {
            list1.add( memberLevelDOToAppMemberLevelRespVO( memberLevelDO ) );
        }

        return list1;
    }

    @Override
    public MemberLevelRespDTO convert02(MemberLevelDO bean) {
        if ( bean == null ) {
            return null;
        }

        MemberLevelRespDTO memberLevelRespDTO = new MemberLevelRespDTO();

        memberLevelRespDTO.setDiscountPercent( bean.getDiscountPercent() );
        memberLevelRespDTO.setExperience( bean.getExperience() );
        memberLevelRespDTO.setId( bean.getId() );
        memberLevelRespDTO.setLevel( bean.getLevel() );
        memberLevelRespDTO.setName( bean.getName() );
        memberLevelRespDTO.setStatus( bean.getStatus() );

        return memberLevelRespDTO;
    }

    protected MemberLevelSimpleRespVO memberLevelDOToMemberLevelSimpleRespVO(MemberLevelDO memberLevelDO) {
        if ( memberLevelDO == null ) {
            return null;
        }

        MemberLevelSimpleRespVO memberLevelSimpleRespVO = new MemberLevelSimpleRespVO();

        memberLevelSimpleRespVO.setIcon( memberLevelDO.getIcon() );
        memberLevelSimpleRespVO.setId( memberLevelDO.getId() );
        memberLevelSimpleRespVO.setName( memberLevelDO.getName() );

        return memberLevelSimpleRespVO;
    }

    protected AppMemberLevelRespVO memberLevelDOToAppMemberLevelRespVO(MemberLevelDO memberLevelDO) {
        if ( memberLevelDO == null ) {
            return null;
        }

        AppMemberLevelRespVO appMemberLevelRespVO = new AppMemberLevelRespVO();

        appMemberLevelRespVO.setBackgroundUrl( memberLevelDO.getBackgroundUrl() );
        appMemberLevelRespVO.setDiscountPercent( memberLevelDO.getDiscountPercent() );
        appMemberLevelRespVO.setExperience( memberLevelDO.getExperience() );
        appMemberLevelRespVO.setIcon( memberLevelDO.getIcon() );
        appMemberLevelRespVO.setLevel( memberLevelDO.getLevel() );
        appMemberLevelRespVO.setName( memberLevelDO.getName() );

        return appMemberLevelRespVO;
    }
}
