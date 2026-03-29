package cn.gemrun.base.module.declare.dal.mysql;

import cn.gemrun.base.module.declare.dal.dataobject.hospital.DeclareHospitalDO;
import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collections;
import java.util.List;

/**
 * 医院信息Mapper
 *
 * @author Gemini
 */
@Mapper
public interface DeclareHospitalMapper extends BaseMapperX<DeclareHospitalDO> {

    default DeclareHospitalDO selectByDeptId(Long deptId) {
        LambdaQueryWrapper<DeclareHospitalDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeclareHospitalDO::getDeptId, deptId);
        return selectOne(wrapper);
    }

    default List<DeclareHospitalDO> selectByHospitalCodes(List<String> hospitalCodes) {
        if (hospitalCodes == null || hospitalCodes.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<DeclareHospitalDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(DeclareHospitalDO::getHospitalCode, hospitalCodes);
        return selectList(wrapper);
    }
}
