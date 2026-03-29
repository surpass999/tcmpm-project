package cn.gemrun.base.module.declare.dal.mysql;

import cn.gemrun.base.module.declare.dal.dataobject.hospital.HospitalTagRelationDO;
import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 医院标签关联Mapper
 *
 * @author Gemini
 */
@Mapper
public interface HospitalTagRelationMapper extends BaseMapperX<HospitalTagRelationDO> {

    /**
     * 根据医院编码查询标签ID列表
     */
    default List<Long> selectTagIdsByHospitalCode(@Param("hospitalCode") String hospitalCode) {
        return selectList(new LambdaQueryWrapper<HospitalTagRelationDO>()
                .eq(HospitalTagRelationDO::getHospitalCode, hospitalCode))
                .stream().map(HospitalTagRelationDO::getTagId).collect(Collectors.toList());
    }

    /**
     * 根据标签ID查询医院编码列表
     */
    default List<String> selectHospitalCodesByTagId(@Param("tagId") Long tagId) {
        return selectList(new LambdaQueryWrapper<HospitalTagRelationDO>()
                .eq(HospitalTagRelationDO::getTagId, tagId))
                .stream().map(HospitalTagRelationDO::getHospitalCode).collect(Collectors.toList());
    }

    /**
     * 根据医院编码删除关联关系
     */
    default int deleteByHospitalCode(@Param("hospitalCode") String hospitalCode) {
        return delete(new LambdaQueryWrapper<HospitalTagRelationDO>()
                .eq(HospitalTagRelationDO::getHospitalCode, hospitalCode));
    }

    /**
     * 根据标签ID删除关联关系
     */
    default int deleteByTagId(@Param("tagId") Long tagId) {
        return delete(new LambdaQueryWrapper<HospitalTagRelationDO>()
                .eq(HospitalTagRelationDO::getTagId, tagId));
    }

    /**
     * 批量插入医院标签关联
     */
    int batchInsert(@Param("relations") List<HospitalTagRelationDO> relations);

    /**
     * 批量根据医院编码列表查询关联记录
     */
    default List<HospitalTagRelationDO> selectListByHospitalCodes(@Param("hospitalCodes") List<String> hospitalCodes) {
        if (hospitalCodes == null || hospitalCodes.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapper<HospitalTagRelationDO>()
                .in(HospitalTagRelationDO::getHospitalCode, hospitalCodes));
    }

}
