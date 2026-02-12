package cn.gemrun.base.module.member.convert.signin;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.member.controller.admin.signin.vo.record.MemberSignInRecordRespVO;
import cn.gemrun.base.module.member.controller.app.signin.vo.record.AppMemberSignInRecordRespVO;
import cn.gemrun.base.module.member.dal.dataobject.signin.MemberSignInRecordDO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-12T21:04:58+0800",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.45.0.v20260128-0750, environment: Java 21.0.9 (Eclipse Adoptium)"
)
public class MemberSignInRecordConvertImpl implements MemberSignInRecordConvert {

    @Override
    public PageResult<MemberSignInRecordRespVO> convertPage(PageResult<MemberSignInRecordDO> pageResult) {
        if ( pageResult == null ) {
            return null;
        }

        PageResult<MemberSignInRecordRespVO> pageResult1 = new PageResult<MemberSignInRecordRespVO>();

        pageResult1.setTotal( pageResult.getTotal() );
        pageResult1.setList( memberSignInRecordDOListToMemberSignInRecordRespVOList( pageResult.getList() ) );

        return pageResult1;
    }

    @Override
    public PageResult<AppMemberSignInRecordRespVO> convertPage02(PageResult<MemberSignInRecordDO> pageResult) {
        if ( pageResult == null ) {
            return null;
        }

        PageResult<AppMemberSignInRecordRespVO> pageResult1 = new PageResult<AppMemberSignInRecordRespVO>();

        pageResult1.setTotal( pageResult.getTotal() );
        pageResult1.setList( memberSignInRecordDOListToAppMemberSignInRecordRespVOList( pageResult.getList() ) );

        return pageResult1;
    }

    @Override
    public AppMemberSignInRecordRespVO coverRecordToAppRecordVo(MemberSignInRecordDO memberSignInRecordDO) {
        if ( memberSignInRecordDO == null ) {
            return null;
        }

        AppMemberSignInRecordRespVO appMemberSignInRecordRespVO = new AppMemberSignInRecordRespVO();

        appMemberSignInRecordRespVO.setCreateTime( memberSignInRecordDO.getCreateTime() );
        appMemberSignInRecordRespVO.setDay( memberSignInRecordDO.getDay() );
        appMemberSignInRecordRespVO.setExperience( memberSignInRecordDO.getExperience() );
        appMemberSignInRecordRespVO.setPoint( memberSignInRecordDO.getPoint() );

        return appMemberSignInRecordRespVO;
    }

    protected MemberSignInRecordRespVO memberSignInRecordDOToMemberSignInRecordRespVO(MemberSignInRecordDO memberSignInRecordDO) {
        if ( memberSignInRecordDO == null ) {
            return null;
        }

        MemberSignInRecordRespVO memberSignInRecordRespVO = new MemberSignInRecordRespVO();

        memberSignInRecordRespVO.setCreateTime( memberSignInRecordDO.getCreateTime() );
        memberSignInRecordRespVO.setDay( memberSignInRecordDO.getDay() );
        memberSignInRecordRespVO.setId( memberSignInRecordDO.getId() );
        memberSignInRecordRespVO.setPoint( memberSignInRecordDO.getPoint() );
        memberSignInRecordRespVO.setUserId( memberSignInRecordDO.getUserId() );

        return memberSignInRecordRespVO;
    }

    protected List<MemberSignInRecordRespVO> memberSignInRecordDOListToMemberSignInRecordRespVOList(List<MemberSignInRecordDO> list) {
        if ( list == null ) {
            return null;
        }

        List<MemberSignInRecordRespVO> list1 = new ArrayList<MemberSignInRecordRespVO>( list.size() );
        for ( MemberSignInRecordDO memberSignInRecordDO : list ) {
            list1.add( memberSignInRecordDOToMemberSignInRecordRespVO( memberSignInRecordDO ) );
        }

        return list1;
    }

    protected List<AppMemberSignInRecordRespVO> memberSignInRecordDOListToAppMemberSignInRecordRespVOList(List<MemberSignInRecordDO> list) {
        if ( list == null ) {
            return null;
        }

        List<AppMemberSignInRecordRespVO> list1 = new ArrayList<AppMemberSignInRecordRespVO>( list.size() );
        for ( MemberSignInRecordDO memberSignInRecordDO : list ) {
            list1.add( coverRecordToAppRecordVo( memberSignInRecordDO ) );
        }

        return list1;
    }
}
