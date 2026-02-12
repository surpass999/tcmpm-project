package cn.gemrun.base.module.member.convert.level;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.member.controller.admin.level.vo.record.MemberLevelRecordRespVO;
import cn.gemrun.base.module.member.dal.dataobject.level.MemberLevelRecordDO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-12T21:04:58+0800",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.45.0.v20260128-0750, environment: Java 21.0.9 (Eclipse Adoptium)"
)
public class MemberLevelRecordConvertImpl implements MemberLevelRecordConvert {

    @Override
    public MemberLevelRecordRespVO convert(MemberLevelRecordDO bean) {
        if ( bean == null ) {
            return null;
        }

        MemberLevelRecordRespVO memberLevelRecordRespVO = new MemberLevelRecordRespVO();

        memberLevelRecordRespVO.setDescription( bean.getDescription() );
        memberLevelRecordRespVO.setDiscountPercent( bean.getDiscountPercent() );
        memberLevelRecordRespVO.setExperience( bean.getExperience() );
        memberLevelRecordRespVO.setLevel( bean.getLevel() );
        memberLevelRecordRespVO.setLevelId( bean.getLevelId() );
        memberLevelRecordRespVO.setRemark( bean.getRemark() );
        memberLevelRecordRespVO.setUserExperience( bean.getUserExperience() );
        memberLevelRecordRespVO.setUserId( bean.getUserId() );
        memberLevelRecordRespVO.setCreateTime( bean.getCreateTime() );
        memberLevelRecordRespVO.setId( bean.getId() );

        return memberLevelRecordRespVO;
    }

    @Override
    public List<MemberLevelRecordRespVO> convertList(List<MemberLevelRecordDO> list) {
        if ( list == null ) {
            return null;
        }

        List<MemberLevelRecordRespVO> list1 = new ArrayList<MemberLevelRecordRespVO>( list.size() );
        for ( MemberLevelRecordDO memberLevelRecordDO : list ) {
            list1.add( convert( memberLevelRecordDO ) );
        }

        return list1;
    }

    @Override
    public PageResult<MemberLevelRecordRespVO> convertPage(PageResult<MemberLevelRecordDO> page) {
        if ( page == null ) {
            return null;
        }

        PageResult<MemberLevelRecordRespVO> pageResult = new PageResult<MemberLevelRecordRespVO>();

        pageResult.setTotal( page.getTotal() );
        pageResult.setList( convertList( page.getList() ) );

        return pageResult;
    }
}
