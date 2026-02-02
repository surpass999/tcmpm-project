package cn.gemrun.base.module.member.convert.point;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.member.controller.admin.point.vo.recrod.MemberPointRecordRespVO;
import cn.gemrun.base.module.member.dal.dataobject.point.MemberPointRecordDO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-02T15:54:15+0800",
    comments = "version: 1.6.3, compiler: javac, environment: Java 1.8.0_371 (Oracle Corporation)"
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

        memberPointRecordRespVO.setId( memberPointRecordDO.getId() );
        memberPointRecordRespVO.setUserId( memberPointRecordDO.getUserId() );
        memberPointRecordRespVO.setBizId( memberPointRecordDO.getBizId() );
        memberPointRecordRespVO.setBizType( memberPointRecordDO.getBizType() );
        memberPointRecordRespVO.setTitle( memberPointRecordDO.getTitle() );
        memberPointRecordRespVO.setDescription( memberPointRecordDO.getDescription() );
        memberPointRecordRespVO.setPoint( memberPointRecordDO.getPoint() );
        memberPointRecordRespVO.setTotalPoint( memberPointRecordDO.getTotalPoint() );
        memberPointRecordRespVO.setCreateTime( memberPointRecordDO.getCreateTime() );

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
