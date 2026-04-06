package cn.gemrun.base.module.declare.dal.mysql;

import cn.gemrun.base.module.declare.dal.dataobject.hospital.DeclareHospitalDO;
import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import cn.gemrun.base.module.declare.controller.admin.dashboard.vo.DashboardStatsVO;
import cn.gemrun.base.module.declare.controller.admin.dashboard.vo.NationalStatsVO;
import cn.gemrun.base.module.declare.enums.ProjectTypeEnum;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
     * 按部门统计启用的医院数量
     */
    default int selectCountActiveByDeptIds(Set<Long> deptIds) {
        if (CollUtil.isEmpty(deptIds)) {
            return 0;
        }
        LambdaQueryWrapper<DeclareHospitalDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(DeclareHospitalDO::getDeptId, deptIds);
        return selectCount(wrapper).intValue();
    }

    /**
     * 按项目类型（1-6）分组统计某部门集合的医院数量
     */
    default List<DashboardStatsVO.NationalStats.ProjectTypeItem> selectCountGroupByProjectTypeOfDeptIds(Set<Long> deptIds) {
        if (CollUtil.isEmpty(deptIds)) {
            return Collections.emptyList();
        }
        // 统计每个项目类型的医院数量
        java.util.Map<Integer, Long> typeCountMap = new java.util.HashMap<>();
        for (int type = 1; type <= 6; type++) {
            long cnt = selectCount(new LambdaQueryWrapper<DeclareHospitalDO>()
                    .in(DeclareHospitalDO::getDeptId, deptIds)
                    .eq(DeclareHospitalDO::getProjectType, type));
            typeCountMap.put(type, cnt);
        }
        long total = typeCountMap.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) {
            return Collections.emptyList();
        }
        return typeCountMap.entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .map(entry -> {
                    DashboardStatsVO.NationalStats.ProjectTypeItem item = new DashboardStatsVO.NationalStats.ProjectTypeItem();
                    item.setTypeValue(entry.getKey());
                    item.setProjectCount(entry.getValue().intValue());
                    item.setPercentage((int) Math.round(entry.getValue() * 1000.0 / total / 10));
                    ProjectTypeEnum e = ProjectTypeEnum.valueOf(entry.getKey());
                    item.setTypeName(e != null ? e.getName() : "未知");
                    return item;
                })
                .collect(Collectors.toList());
    }

}
