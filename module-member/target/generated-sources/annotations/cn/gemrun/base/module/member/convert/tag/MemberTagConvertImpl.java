package cn.gemrun.base.module.member.convert.tag;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.member.controller.admin.tag.vo.MemberTagCreateReqVO;
import cn.gemrun.base.module.member.controller.admin.tag.vo.MemberTagRespVO;
import cn.gemrun.base.module.member.controller.admin.tag.vo.MemberTagUpdateReqVO;
import cn.gemrun.base.module.member.dal.dataobject.tag.MemberTagDO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-02T15:54:15+0800",
    comments = "version: 1.6.3, compiler: javac, environment: Java 1.8.0_371 (Oracle Corporation)"
)
public class MemberTagConvertImpl implements MemberTagConvert {

    @Override
    public MemberTagDO convert(MemberTagCreateReqVO bean) {
        if ( bean == null ) {
            return null;
        }

        MemberTagDO.MemberTagDOBuilder memberTagDO = MemberTagDO.builder();

        memberTagDO.name( bean.getName() );

        return memberTagDO.build();
    }

    @Override
    public MemberTagDO convert(MemberTagUpdateReqVO bean) {
        if ( bean == null ) {
            return null;
        }

        MemberTagDO.MemberTagDOBuilder memberTagDO = MemberTagDO.builder();

        memberTagDO.id( bean.getId() );
        memberTagDO.name( bean.getName() );

        return memberTagDO.build();
    }

    @Override
    public MemberTagRespVO convert(MemberTagDO bean) {
        if ( bean == null ) {
            return null;
        }

        MemberTagRespVO memberTagRespVO = new MemberTagRespVO();

        memberTagRespVO.setName( bean.getName() );
        memberTagRespVO.setId( bean.getId() );
        memberTagRespVO.setCreateTime( bean.getCreateTime() );

        return memberTagRespVO;
    }

    @Override
    public List<MemberTagRespVO> convertList(List<MemberTagDO> list) {
        if ( list == null ) {
            return null;
        }

        List<MemberTagRespVO> list1 = new ArrayList<MemberTagRespVO>( list.size() );
        for ( MemberTagDO memberTagDO : list ) {
            list1.add( convert( memberTagDO ) );
        }

        return list1;
    }

    @Override
    public PageResult<MemberTagRespVO> convertPage(PageResult<MemberTagDO> page) {
        if ( page == null ) {
            return null;
        }

        PageResult<MemberTagRespVO> pageResult = new PageResult<MemberTagRespVO>();

        pageResult.setTotal( page.getTotal() );
        pageResult.setList( convertList( page.getList() ) );

        return pageResult;
    }
}
