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
    date = "2026-02-09T17:50:52+0800",
    comments = "version: 1.6.3, compiler: javac, environment: Java 1.8.0_371 (Oracle Corporation)"
)
public class MemberLevelConvertImpl implements MemberLevelConvert {

    @Override
    public MemberLevelDO convert(MemberLevelCreateReqVO bean) {
        if ( bean == null ) {
            return null;
        }

        MemberLevelDO.MemberLevelDOBuilder memberLevelDO = MemberLevelDO.builder();

        memberLevelDO.name( bean.getName() );
        memberLevelDO.level( bean.getLevel() );
        memberLevelDO.experience( bean.getExperience() );
        memberLevelDO.discountPercent( bean.getDiscountPercent() );
        memberLevelDO.icon( bean.getIcon() );
        memberLevelDO.backgroundUrl( bean.getBackgroundUrl() );
        memberLevelDO.status( bean.getStatus() );

        return memberLevelDO.build();
    }

    @Override
    public MemberLevelDO convert(MemberLevelUpdateReqVO bean) {
        if ( bean == null ) {
            return null;
        }

        MemberLevelDO.MemberLevelDOBuilder memberLevelDO = MemberLevelDO.builder();

        memberLevelDO.id( bean.getId() );
        memberLevelDO.name( bean.getName() );
        memberLevelDO.level( bean.getLevel() );
        memberLevelDO.experience( bean.getExperience() );
        memberLevelDO.discountPercent( bean.getDiscountPercent() );
        memberLevelDO.icon( bean.getIcon() );
        memberLevelDO.backgroundUrl( bean.getBackgroundUrl() );
        memberLevelDO.status( bean.getStatus() );

        return memberLevelDO.build();
    }

    @Override
    public MemberLevelRespVO convert(MemberLevelDO bean) {
        if ( bean == null ) {
            return null;
        }

        MemberLevelRespVO memberLevelRespVO = new MemberLevelRespVO();

        memberLevelRespVO.setName( bean.getName() );
        memberLevelRespVO.setExperience( bean.getExperience() );
        memberLevelRespVO.setLevel( bean.getLevel() );
        memberLevelRespVO.setDiscountPercent( bean.getDiscountPercent() );
        memberLevelRespVO.setIcon( bean.getIcon() );
        memberLevelRespVO.setBackgroundUrl( bean.getBackgroundUrl() );
        memberLevelRespVO.setStatus( bean.getStatus() );
        memberLevelRespVO.setId( bean.getId() );
        memberLevelRespVO.setCreateTime( bean.getCreateTime() );

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

        memberLevelRespDTO.setId( bean.getId() );
        memberLevelRespDTO.setName( bean.getName() );
        memberLevelRespDTO.setLevel( bean.getLevel() );
        memberLevelRespDTO.setExperience( bean.getExperience() );
        memberLevelRespDTO.setDiscountPercent( bean.getDiscountPercent() );
        memberLevelRespDTO.setStatus( bean.getStatus() );

        return memberLevelRespDTO;
    }

    protected MemberLevelSimpleRespVO memberLevelDOToMemberLevelSimpleRespVO(MemberLevelDO memberLevelDO) {
        if ( memberLevelDO == null ) {
            return null;
        }

        MemberLevelSimpleRespVO memberLevelSimpleRespVO = new MemberLevelSimpleRespVO();

        memberLevelSimpleRespVO.setId( memberLevelDO.getId() );
        memberLevelSimpleRespVO.setName( memberLevelDO.getName() );
        memberLevelSimpleRespVO.setIcon( memberLevelDO.getIcon() );

        return memberLevelSimpleRespVO;
    }

    protected AppMemberLevelRespVO memberLevelDOToAppMemberLevelRespVO(MemberLevelDO memberLevelDO) {
        if ( memberLevelDO == null ) {
            return null;
        }

        AppMemberLevelRespVO appMemberLevelRespVO = new AppMemberLevelRespVO();

        appMemberLevelRespVO.setName( memberLevelDO.getName() );
        appMemberLevelRespVO.setLevel( memberLevelDO.getLevel() );
        appMemberLevelRespVO.setExperience( memberLevelDO.getExperience() );
        appMemberLevelRespVO.setDiscountPercent( memberLevelDO.getDiscountPercent() );
        appMemberLevelRespVO.setIcon( memberLevelDO.getIcon() );
        appMemberLevelRespVO.setBackgroundUrl( memberLevelDO.getBackgroundUrl() );

        return appMemberLevelRespVO;
    }
}
