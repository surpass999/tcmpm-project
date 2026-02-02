package cn.gemrun.base.module.member.convert.signin;

import cn.gemrun.base.module.member.controller.admin.signin.vo.config.MemberSignInConfigCreateReqVO;
import cn.gemrun.base.module.member.controller.admin.signin.vo.config.MemberSignInConfigRespVO;
import cn.gemrun.base.module.member.controller.admin.signin.vo.config.MemberSignInConfigUpdateReqVO;
import cn.gemrun.base.module.member.controller.app.signin.vo.config.AppMemberSignInConfigRespVO;
import cn.gemrun.base.module.member.dal.dataobject.signin.MemberSignInConfigDO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-02T15:54:15+0800",
    comments = "version: 1.6.3, compiler: javac, environment: Java 1.8.0_371 (Oracle Corporation)"
)
public class MemberSignInConfigConvertImpl implements MemberSignInConfigConvert {

    @Override
    public MemberSignInConfigDO convert(MemberSignInConfigCreateReqVO bean) {
        if ( bean == null ) {
            return null;
        }

        MemberSignInConfigDO.MemberSignInConfigDOBuilder memberSignInConfigDO = MemberSignInConfigDO.builder();

        memberSignInConfigDO.day( bean.getDay() );
        memberSignInConfigDO.point( bean.getPoint() );
        memberSignInConfigDO.experience( bean.getExperience() );
        memberSignInConfigDO.status( bean.getStatus() );

        return memberSignInConfigDO.build();
    }

    @Override
    public MemberSignInConfigDO convert(MemberSignInConfigUpdateReqVO bean) {
        if ( bean == null ) {
            return null;
        }

        MemberSignInConfigDO.MemberSignInConfigDOBuilder memberSignInConfigDO = MemberSignInConfigDO.builder();

        memberSignInConfigDO.id( bean.getId() );
        memberSignInConfigDO.day( bean.getDay() );
        memberSignInConfigDO.point( bean.getPoint() );
        memberSignInConfigDO.experience( bean.getExperience() );
        memberSignInConfigDO.status( bean.getStatus() );

        return memberSignInConfigDO.build();
    }

    @Override
    public MemberSignInConfigRespVO convert(MemberSignInConfigDO bean) {
        if ( bean == null ) {
            return null;
        }

        MemberSignInConfigRespVO memberSignInConfigRespVO = new MemberSignInConfigRespVO();

        memberSignInConfigRespVO.setDay( bean.getDay() );
        memberSignInConfigRespVO.setPoint( bean.getPoint() );
        memberSignInConfigRespVO.setExperience( bean.getExperience() );
        memberSignInConfigRespVO.setStatus( bean.getStatus() );
        if ( bean.getId() != null ) {
            memberSignInConfigRespVO.setId( bean.getId().intValue() );
        }
        memberSignInConfigRespVO.setCreateTime( bean.getCreateTime() );

        return memberSignInConfigRespVO;
    }

    @Override
    public List<MemberSignInConfigRespVO> convertList(List<MemberSignInConfigDO> list) {
        if ( list == null ) {
            return null;
        }

        List<MemberSignInConfigRespVO> list1 = new ArrayList<MemberSignInConfigRespVO>( list.size() );
        for ( MemberSignInConfigDO memberSignInConfigDO : list ) {
            list1.add( convert( memberSignInConfigDO ) );
        }

        return list1;
    }

    @Override
    public List<AppMemberSignInConfigRespVO> convertList02(List<MemberSignInConfigDO> list) {
        if ( list == null ) {
            return null;
        }

        List<AppMemberSignInConfigRespVO> list1 = new ArrayList<AppMemberSignInConfigRespVO>( list.size() );
        for ( MemberSignInConfigDO memberSignInConfigDO : list ) {
            list1.add( memberSignInConfigDOToAppMemberSignInConfigRespVO( memberSignInConfigDO ) );
        }

        return list1;
    }

    protected AppMemberSignInConfigRespVO memberSignInConfigDOToAppMemberSignInConfigRespVO(MemberSignInConfigDO memberSignInConfigDO) {
        if ( memberSignInConfigDO == null ) {
            return null;
        }

        AppMemberSignInConfigRespVO appMemberSignInConfigRespVO = new AppMemberSignInConfigRespVO();

        appMemberSignInConfigRespVO.setDay( memberSignInConfigDO.getDay() );
        appMemberSignInConfigRespVO.setPoint( memberSignInConfigDO.getPoint() );

        return appMemberSignInConfigRespVO;
    }
}
