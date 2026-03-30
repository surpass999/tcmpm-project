package cn.gemrun.base.module.declare.dal.mysql;

import cn.gemrun.base.module.declare.dal.dataobject.hospital.DeclareHospitalDO;
import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import cn.gemrun.base.module.declare.controller.admin.dashboard.vo.DashboardStatsVO;
import cn.gemrun.base.module.declare.controller.admin.dashboard.vo.NationalStatsVO;
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

    default List<DeclareHospitalDO> selectByProvinceCode(String provinceCode) {
        LambdaQueryWrapper<DeclareHospitalDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeclareHospitalDO::getProvinceCode, provinceCode);
        return selectList(wrapper);
    }

    /**
     * 通过省份编码查一个医院（用于确定省局用户的归属省份）
     */
    default DeclareHospitalDO selectOneByProvinceCode(String provinceCode) {
        LambdaQueryWrapper<DeclareHospitalDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeclareHospitalDO::getProvinceCode, provinceCode).last("LIMIT 1");
        return selectOne(wrapper);
    }

    /**
     * 统计所有启用的医院数量
     */
    int selectCountActive();

    /**
     * 按项目类型（1-6）分组统计医院数量及占比
     */
    List<DashboardStatsVO.NationalStats.ProjectTypeItem> selectCountGroupByProjectType();

    /**
     * 按省份分组统计医院数量
     */
    List<NationalStatsVO.ProvinceItem> selectCountGroupByProvince();

    /**
     * 按省份统计启用的医院数量
     */
    int selectCountActiveByProvince(String provinceCode);

    /**
     * 按项目类型（1-6）分组统计某省份的医院数量
     */
    List<DashboardStatsVO.NationalStats.ProjectTypeItem> selectCountGroupByProjectTypeOfProvince(String provinceCode);
}
