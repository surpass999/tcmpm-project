package cn.gemrun.base.module.member.convert.point;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.member.controller.admin.point.vo.recrod.MemberPointRecordRespVO;
import cn.gemrun.base.module.member.dal.dataobject.point.MemberPointRecordDO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-12T21:04:58+0800",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.45.0.v20260128-0750, environment: Java 21.0.9 (Eclipse Adoptium)"
)
public class MemberPointRecordConvertImpl implements MemberPointRecordConvert {

    @Override
    public PageResult<MemberPointRecordRespVO> convertPage(PageResult<MemberPointRecordDO> pageResult) {
        if ( pageResult == null ) {
            return null;
        }

        PageResult<MemberPointRecordRespVO> pageResult1 = new PageResult<MemberPointRecordRespVO>();

        pageResult1.setTotal( pageResult.getTotal() );
        pageResult1.setList( memberPointRecordDOListToMemberPointRecordRespVOList( pageResult.getList() ) );

        return pageResult1;
    }

    protected MemberPointRecordRespVO memberPointRecordDOToMemberPointRecordRespVO(MemberPointRecordDO memberPointRecordDO) {
        if ( memberPointRecordDO == null ) {
            return null;
        }

        MemberPointRecordRespVO memberPointRecordRespVO = new MemberPointRecordRespVO();

        memberPointRecordRespVO.setBizId( memberPointRecordDO.getBizId() );
        memberPointRecordRespVO.setBizType( memberPointRecordDO.getBizType() );
        memberPointRecordRespVO.setCreateTime( memberPointRecordDO.getCreateTime() );
        memberPointRecordRespVO.setDescription( memberPointRecordDO.getDescription() );
        memberPointRecordRespVO.setId( memberPointRecordDO.getId() );
        memberPointRecordRespVO.setPoint( memberPointRecordDO.getPoint() );
        memberPointRecordRespVO.setTitle( memberPointRecordDO.getTitle() );
        memberPointRecordRespVO.setTotalPoint( memberPointRecordDO.getTotalPoint() );
        memberPointRecordRespVO.setUserId( memberPointRecordDO.getUserId() );

        return memberPointRecordRespVO;
    }

    protected List<MemberPointRecordRespVO> memberPointRecordDOListToMemberPointRecordRespVOList(List<MemberPointRecordDO> list) {
        if ( list == null ) {
            return null;
        }

        List<MemberPointRecordRespVO> list1 = new ArrayList<MemberPointRecordRespVO>( list.size() );
        for ( MemberPointRecordDO memberPointRecordDO : list ) {
            list1.add( memberPointRecordDOToMemberPointRecordRespVO( memberPointRecordDO ) );
        }

        return list1;
    }
}
