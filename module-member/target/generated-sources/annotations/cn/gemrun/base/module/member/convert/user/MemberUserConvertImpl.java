package cn.gemrun.base.module.member.convert.user;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.member.api.user.dto.MemberUserRespDTO;
import cn.gemrun.base.module.member.controller.admin.user.vo.MemberUserRespVO;
import cn.gemrun.base.module.member.controller.admin.user.vo.MemberUserUpdateReqVO;
import cn.gemrun.base.module.member.controller.app.user.vo.AppMemberUserInfoRespVO;
import cn.gemrun.base.module.member.convert.address.AddressConvert;
import cn.gemrun.base.module.member.dal.dataobject.level.MemberLevelDO;
import cn.gemrun.base.module.member.dal.dataobject.user.MemberUserDO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-09T17:50:52+0800",
    comments = "version: 1.6.3, compiler: javac, environment: Java 1.8.0_371 (Oracle Corporation)"
)
public class MemberUserConvertImpl implements MemberUserConvert {

    private final AddressConvert addressConvert = AddressConvert.INSTANCE;

    @Override
    public AppMemberUserInfoRespVO convert(MemberUserDO bean) {
        if ( bean == null ) {
            return null;
        }

        AppMemberUserInfoRespVO appMemberUserInfoRespVO = new AppMemberUserInfoRespVO();

        appMemberUserInfoRespVO.setId( bean.getId() );
        appMemberUserInfoRespVO.setNickname( bean.getNickname() );
        appMemberUserInfoRespVO.setAvatar( bean.getAvatar() );
        appMemberUserInfoRespVO.setMobile( bean.getMobile() );
        appMemberUserInfoRespVO.setSex( bean.getSex() );
        appMemberUserInfoRespVO.setPoint( bean.getPoint() );
        appMemberUserInfoRespVO.setExperience( bean.getExperience() );

        return appMemberUserInfoRespVO;
    }

    @Override
    public AppMemberUserInfoRespVO convert(MemberUserDO bean, MemberLevelDO level) {
        if ( bean == null && level == null ) {
            return null;
        }

        AppMemberUserInfoRespVO appMemberUserInfoRespVO = new AppMemberUserInfoRespVO();

        if ( bean != null ) {
            appMemberUserInfoRespVO.setId( bean.getId() );
            appMemberUserInfoRespVO.setExperience( bean.getExperience() );
            appMemberUserInfoRespVO.setNickname( bean.getNickname() );
            appMemberUserInfoRespVO.setAvatar( bean.getAvatar() );
            appMemberUserInfoRespVO.setMobile( bean.getMobile() );
            appMemberUserInfoRespVO.setSex( bean.getSex() );
            appMemberUserInfoRespVO.setPoint( bean.getPoint() );
        }
        appMemberUserInfoRespVO.setLevel( memberLevelDOToLevel( level ) );

        return appMemberUserInfoRespVO;
    }

    @Override
    public MemberUserRespDTO convert2(MemberUserDO bean) {
        if ( bean == null ) {
            return null;
        }

        MemberUserRespDTO memberUserRespDTO = new MemberUserRespDTO();

        memberUserRespDTO.setId( bean.getId() );
        memberUserRespDTO.setNickname( bean.getNickname() );
        memberUserRespDTO.setStatus( bean.getStatus() );
        memberUserRespDTO.setAvatar( bean.getAvatar() );
        memberUserRespDTO.setMobile( bean.getMobile() );
        memberUserRespDTO.setCreateTime( bean.getCreateTime() );
        memberUserRespDTO.setLevelId( bean.getLevelId() );
        memberUserRespDTO.setPoint( bean.getPoint() );

        return memberUserRespDTO;
    }

    @Override
    public List<MemberUserRespDTO> convertList2(List<MemberUserDO> list) {
        if ( list == null ) {
            return null;
        }

        List<MemberUserRespDTO> list1 = new ArrayList<MemberUserRespDTO>( list.size() );
        for ( MemberUserDO memberUserDO : list ) {
            list1.add( convert2( memberUserDO ) );
        }

        return list1;
    }

    @Override
    public MemberUserDO convert(MemberUserUpdateReqVO bean) {
        if ( bean == null ) {
            return null;
        }

        MemberUserDO.MemberUserDOBuilder memberUserDO = MemberUserDO.builder();

        memberUserDO.id( bean.getId() );
        memberUserDO.mobile( bean.getMobile() );
        if ( bean.getStatus() != null ) {
            memberUserDO.status( bean.getStatus().intValue() );
        }
        memberUserDO.nickname( bean.getNickname() );
        memberUserDO.avatar( bean.getAvatar() );
        memberUserDO.name( bean.getName() );
        memberUserDO.sex( bean.getSex() );
        memberUserDO.birthday( bean.getBirthday() );
        if ( bean.getAreaId() != null ) {
            memberUserDO.areaId( bean.getAreaId().intValue() );
        }
        memberUserDO.mark( bean.getMark() );
        List<Long> list = bean.getTagIds();
        if ( list != null ) {
            memberUserDO.tagIds( new ArrayList<Long>( list ) );
        }
        memberUserDO.levelId( bean.getLevelId() );
        memberUserDO.groupId( bean.getGroupId() );

        return memberUserDO.build();
    }

    @Override
    public PageResult<MemberUserRespVO> convertPage(PageResult<MemberUserDO> page) {
        if ( page == null ) {
            return null;
        }

        PageResult<MemberUserRespVO> pageResult = new PageResult<MemberUserRespVO>();

        pageResult.setTotal( page.getTotal() );
        pageResult.setList( memberUserDOListToMemberUserRespVOList( page.getList() ) );

        return pageResult;
    }

    @Override
    public MemberUserRespVO convert03(MemberUserDO bean) {
        if ( bean == null ) {
            return null;
        }

        MemberUserRespVO memberUserRespVO = new MemberUserRespVO();

        memberUserRespVO.setAreaName( addressConvert.convertAreaIdToAreaName( bean.getAreaId() ) );
        memberUserRespVO.setMobile( bean.getMobile() );
        if ( bean.getStatus() != null ) {
            memberUserRespVO.setStatus( bean.getStatus().byteValue() );
        }
        memberUserRespVO.setNickname( bean.getNickname() );
        memberUserRespVO.setAvatar( bean.getAvatar() );
        memberUserRespVO.setName( bean.getName() );
        memberUserRespVO.setSex( bean.getSex() );
        if ( bean.getAreaId() != null ) {
            memberUserRespVO.setAreaId( bean.getAreaId().longValue() );
        }
        memberUserRespVO.setBirthday( bean.getBirthday() );
        memberUserRespVO.setMark( bean.getMark() );
        List<Long> list = bean.getTagIds();
        if ( list != null ) {
            memberUserRespVO.setTagIds( new ArrayList<Long>( list ) );
        }
        memberUserRespVO.setLevelId( bean.getLevelId() );
        memberUserRespVO.setGroupId( bean.getGroupId() );
        memberUserRespVO.setId( bean.getId() );
        memberUserRespVO.setRegisterIp( bean.getRegisterIp() );
        memberUserRespVO.setLoginIp( bean.getLoginIp() );
        memberUserRespVO.setLoginDate( bean.getLoginDate() );
        memberUserRespVO.setCreateTime( bean.getCreateTime() );
        memberUserRespVO.setPoint( bean.getPoint() );
        memberUserRespVO.setExperience( bean.getExperience() );

        return memberUserRespVO;
    }

    protected AppMemberUserInfoRespVO.Level memberLevelDOToLevel(MemberLevelDO memberLevelDO) {
        if ( memberLevelDO == null ) {
            return null;
        }

        AppMemberUserInfoRespVO.Level level = new AppMemberUserInfoRespVO.Level();

        level.setId( memberLevelDO.getId() );
        level.setName( memberLevelDO.getName() );
        level.setLevel( memberLevelDO.getLevel() );
        level.setIcon( memberLevelDO.getIcon() );

        return level;
    }

    protected List<MemberUserRespVO> memberUserDOListToMemberUserRespVOList(List<MemberUserDO> list) {
        if ( list == null ) {
            return null;
        }

        List<MemberUserRespVO> list1 = new ArrayList<MemberUserRespVO>( list.size() );
        for ( MemberUserDO memberUserDO : list ) {
            list1.add( convert03( memberUserDO ) );
        }

        return list1;
    }
}
