package cn.gemrun.base.module.declare.service.hospital.impl;

import cn.gemrun.base.module.declare.dal.dataobject.hospital.DeclareHospitalDO;
import cn.gemrun.base.module.declare.dal.dataobject.hospital.HospitalTagDO;
import cn.gemrun.base.module.declare.dal.dataobject.hospital.HospitalTagRelationDO;
import cn.gemrun.base.module.declare.dal.dataobject.progress.DeclareProgressReportDO;
import cn.gemrun.base.module.declare.dal.mysql.DeclareHospitalMapper;
import cn.gemrun.base.module.declare.dal.mysql.HospitalTagMapper;
import cn.gemrun.base.module.declare.dal.mysql.HospitalTagRelationMapper;
import cn.gemrun.base.module.declare.dal.mysql.DeclareProgressReportMapper;
import cn.gemrun.base.module.declare.service.hospital.HospitalTagQueryService;
import cn.gemrun.base.module.declare.controller.admin.hospital.vo.DimensionStatVO;
import cn.gemrun.base.module.declare.vo.progress.DeclareProgressReportVO;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HospitalTagQueryServiceImpl implements HospitalTagQueryService {

    private final HospitalTagMapper hospitalTagMapper;
    private final HospitalTagRelationMapper hospitalTagRelationMapper;
    private final DeclareProgressReportMapper progressReportMapper;
    private final DeclareHospitalMapper hospitalMapper;

    @Override
    public List<String> queryHospitalCodesByTags(List<Long> tagIds) {
        if (CollUtil.isEmpty(tagIds)) {
            return new ArrayList<>();
        }

        // 获取每个标签对应的医院编码集合
        List<Set<String>> codeSets = tagIds.stream()
                .map(tagId -> new HashSet<>(hospitalTagRelationMapper.selectHospitalCodesByTagId(tagId)))
                .collect(Collectors.toList());

        // 取交集（AND条件）
        return codeSets.stream()
                .reduce((set1, set2) -> {
                    set1.retainAll(set2);
                    return set1;
                })
                .map(ArrayList::new)
                .orElse(new ArrayList<>());
    }

    @Override
    public Map<String, Long> statisticsByCategory(String category) {
        List<HospitalTagDO> tags = hospitalTagMapper.selectList(
                new LambdaQueryWrapper<HospitalTagDO>()
                        .eq(HospitalTagDO::getTagCategory, category)
                        .eq(HospitalTagDO::getStatus, 1));

        Map<String, Long> result = new HashMap<>();
        for (HospitalTagDO tag : tags) {
            long count = hospitalTagRelationMapper.selectCount(
                    new LambdaQueryWrapper<HospitalTagRelationDO>()
                            .eq(HospitalTagRelationDO::getTagId, tag.getId()));
            result.put(tag.getTagName(), count);
        }
        return result;
    }

    @Override
    public List<DimensionStatVO> crossStatistics(String category1, String category2) {
        List<HospitalTagDO> tags1 = hospitalTagMapper.selectList(
                new LambdaQueryWrapper<HospitalTagDO>()
                        .eq(HospitalTagDO::getTagCategory, category1)
                        .eq(HospitalTagDO::getStatus, 1));
        List<HospitalTagDO> tags2 = hospitalTagMapper.selectList(
                new LambdaQueryWrapper<HospitalTagDO>()
                        .eq(HospitalTagDO::getTagCategory, category2)
                        .eq(HospitalTagDO::getStatus, 1));

        List<DimensionStatVO> result = new ArrayList<>();

        for (HospitalTagDO tag1 : tags1) {
            for (HospitalTagDO tag2 : tags2) {
                Set<String> codes1 = new HashSet<>(hospitalTagRelationMapper.selectHospitalCodesByTagId(tag1.getId()));
                Set<String> codes2 = new HashSet<>(hospitalTagRelationMapper.selectHospitalCodesByTagId(tag2.getId()));
                codes1.retainAll(codes2);

                result.add(DimensionStatVO.builder()
                        .dimension1(tag1.getTagName())
                        .dimension2(tag2.getTagName())
                        .count((long) codes1.size())
                        .build());
            }
        }
        return result;
    }

    @Override
    public List<DeclareProgressReportVO> queryReportByTags(List<Long> tagIds, Integer reportYear, Integer reportBatch) {
        List<String> hospitalCodes = queryHospitalCodesByTags(tagIds);
        if (CollUtil.isEmpty(hospitalCodes)) {
            return new ArrayList<>();
        }

        // 医院编码 -> 医院ID
        List<DeclareHospitalDO> hospitals = hospitalMapper.selectByHospitalCodes(hospitalCodes);
        List<Long> hospitalIds = hospitals.stream()
                .map(DeclareHospitalDO::getId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<DeclareProgressReportDO> wrapper = new LambdaQueryWrapper<DeclareProgressReportDO>()
                .eq(reportYear != null, DeclareProgressReportDO::getReportYear, reportYear)
                .eq(reportBatch != null, DeclareProgressReportDO::getReportBatch, reportBatch)
                .in(CollUtil.isNotEmpty(hospitalIds), DeclareProgressReportDO::getHospitalId, hospitalIds);

        return progressReportMapper.selectList(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    private DeclareProgressReportVO convertToVO(DeclareProgressReportDO report) {
        return DeclareProgressReportVO.builder()
                .id(report.getId())
                .reportYear(report.getReportYear())
                .reportBatch(report.getReportBatch())
                .hospitalId(report.getHospitalId())
                .hospitalName(report.getHospitalName())
                .provinceCode(report.getProvinceCode())
                .provinceName(report.getProvinceName())
                .reportStatus(report.getReportStatus())
                .provinceStatus(report.getProvinceStatus())
                .nationalReportStatus(report.getNationalReportStatus())
                .nationalReportTime(report.getNationalReportTime())
                .createTime(report.getCreateTime())
                .updateTime(report.getUpdateTime())
                .build();
    }
}
