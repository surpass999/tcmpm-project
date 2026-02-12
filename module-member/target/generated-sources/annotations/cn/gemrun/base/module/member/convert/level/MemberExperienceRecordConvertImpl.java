package cn.gemrun.base.module.member.convert.level;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.member.controller.admin.level.vo.experience.MemberExperienceRecordRespVO;
import cn.gemrun.base.module.member.controller.app.level.vo.experience.AppMemberExperienceRecordRespVO;
import cn.gemrun.base.module.member.dal.dataobject.level.MemberExperienceRecordDO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-12T21:04:58+0800",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.45.0.v20260128-0750, environment: Java 21.0.9 (Eclipse Adoptium)"
)
public class MemberExperienceRecordConvertImpl implements MemberExperienceRecordConvert {

    @Override
    public MemberExperienceRecordRespVO convert(MemberExperienceRecordDO bean) {
        if ( bean == null ) {
            return null;
        }

        MemberExperienceRecordRespVO memberExperienceRecordRespVO = new MemberExperienceRecordRespVO();

        memberExperienceRecordRespVO.setBizId( bean.getBizId() );
        memberExperienceRecordRespVO.setBizType( bean.getBizType() );
        memberExperienceRecordRespVO.setDescription( bean.getDescription() );
        memberExperienceRecordRespVO.setExperience( bean.getExperience() );
        memberExperienceRecordRespVO.setTitle( bean.getTitle() );
        memberExperienceRecordRespVO.setTotalExperience( bean.getTotalExperience() );
        memberExperienceRecordRespVO.setUserId( bean.getUserId() );
        memberExperienceRecordRespVO.setCreateTime( bean.getCreateTime() );
        memberExperienceRecordRespVO.setId( bean.getId() );

        return memberExperienceRecordRespVO;
    }

    @Override
    public List<MemberExperienceRecordRespVO> convertList(List<MemberExperienceRecordDO> list) {
        if ( list == null ) {
            return null;
        }

        List<MemberExperienceRecordRespVO> list1 = new ArrayList<MemberExperienceRecordRespVO>( list.size() );
        for ( MemberExperienceRecordDO memberExperienceRecordDO : list ) {
            list1.add( convert( memberExperienceRecordDO ) );
        }

        return list1;
    }

    @Override
    public PageResult<MemberExperienceRecordRespVO> convertPage(PageResult<MemberExperienceRecordDO> page) {
        if ( page == null ) {
            return null;
        }

        PageResult<MemberExperienceRecordRespVO> pageResult = new PageResult<MemberExperienceRecordRespVO>();

        pageResult.setTotal( page.getTotal() );
        pageResult.setList( convertList( page.getList() ) );

        return pageResult;
    }

    @Override
    public MemberExperienceRecordDO convert(Long userId, Integer experience, Integer totalExperience, String bizId, Integer bizType, String title, String description) {
        if ( userId == null && experience == null && totalExperience == null && bizId == null && bizType == null && title == null && description == null ) {
            return null;
        }

        MemberExperienceRecordDO.MemberExperienceRecordDOBuilder memberExperienceRecordDO = MemberExperienceRecordDO.builder();

        memberExperienceRecordDO.userId( userId );
        memberExperienceRecordDO.experience( experience );
        memberExperienceRecordDO.totalExperience( totalExperience );
        memberExperienceRecordDO.bizId( bizId );
        memberExperienceRecordDO.bizType( bizType );
        memberExperienceRecordDO.title( title );
        memberExperienceRecordDO.description( description );

        return memberExperienceRecordDO.build();
    }

    @Override
    public PageResult<AppMemberExperienceRecordRespVO> convertPage02(PageResult<MemberExperienceRecordDO> page) {
        if ( page == null ) {
            return null;
        }

        PageResult<AppMemberExperienceRecordRespVO> pageResult = new PageResult<AppMemberExperienceRecordRespVO>();

        pageResult.setTotal( page.getTotal() );
        pageResult.setList( memberExperienceRecordDOListToAppMemberExperienceRecordRespVOList( page.getList() ) );

        return pageResult;
    }

    protected AppMemberExperienceRecordRespVO memberExperienceRecordDOToAppMemberExperienceRecordRespVO(MemberExperienceRecordDO memberExperienceRecordDO) {
        if ( memberExperienceRecordDO == null ) {
            return null;
        }

        AppMemberExperienceRecordRespVO appMemberExperienceRecordRespVO = new AppMemberExperienceRecordRespVO();

        appMemberExperienceRecordRespVO.setCreateTime( memberExperienceRecordDO.getCreateTime() );
        appMemberExperienceRecordRespVO.setDescription( memberExperienceRecordDO.getDescription() );
        appMemberExperienceRecordRespVO.setExperience( memberExperienceRecordDO.getExperience() );
        appMemberExperienceRecordRespVO.setTitle( memberExperienceRecordDO.getTitle() );

        return appMemberExperienceRecordRespVO;
    }

    protected List<AppMemberExperienceRecordRespVO> memberExperienceRecordDOListToAppMemberExperienceRecordRespVOList(List<MemberExperienceRecordDO> list) {
        if ( list == null ) {
            return null;
        }

        List<AppMemberExperienceRecordRespVO> list1 = new ArrayList<AppMemberExperienceRecordRespVO>( list.size() );
        for ( MemberExperienceRecordDO memberExperienceRecordDO : list ) {
            list1.add( memberExperienceRecordDOToAppMemberExperienceRecordRespVO( memberExperienceRecordDO ) );
        }

        return list1;
    }
}
