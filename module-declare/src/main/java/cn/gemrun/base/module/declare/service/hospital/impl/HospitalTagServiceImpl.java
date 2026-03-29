package cn.gemrun.base.module.declare.service.hospital.impl;

import cn.gemrun.base.module.declare.dal.dataobject.hospital.HospitalTagDO;
import cn.gemrun.base.module.declare.dal.dataobject.hospital.HospitalTagRelationDO;
import cn.gemrun.base.module.declare.dal.mysql.HospitalTagMapper;
import cn.gemrun.base.module.declare.dal.mysql.HospitalTagRelationMapper;
import cn.gemrun.base.module.declare.service.hospital.HospitalTagService;
import cn.gemrun.base.module.declare.controller.admin.hospital.vo.HospitalTagCreateReqVO;
import cn.gemrun.base.module.declare.controller.admin.hospital.vo.HospitalTagVO;
import cn.gemrun.base.module.declare.controller.admin.hospital.vo.HospitalTagAssignReqVO;
import cn.gemrun.base.module.declare.controller.admin.hospital.vo.HospitalTagUpdateReqVO;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class HospitalTagServiceImpl implements HospitalTagService {

    private final HospitalTagMapper hospitalTagMapper;
    private final HospitalTagRelationMapper hospitalTagRelationMapper;

    @Override
    public Long createTag(HospitalTagCreateReqVO reqVO) {
        HospitalTagDO tag = HospitalTagDO.builder()
                .tagCode(reqVO.getTagCode())
                .tagName(reqVO.getTagName())
                .tagCategory(reqVO.getTagCategory())
                .tagType(reqVO.getTagType())
                .parentId(reqVO.getParentId())
                .sort(reqVO.getSort())
                .status(1)
                .build();
        hospitalTagMapper.insert(tag);
        return tag.getId();
    }

    @Override
    public void updateTag(HospitalTagUpdateReqVO reqVO) {
        HospitalTagDO tag = HospitalTagDO.builder()
                .id(reqVO.getId())
                .tagName(reqVO.getTagName())
                .tagType(reqVO.getTagType())
                .parentId(reqVO.getParentId())
                .sort(reqVO.getSort())
                .status(reqVO.getStatus())
                .build();
        hospitalTagMapper.updateById(tag);
    }

    @Override
    public void deleteTag(Long id) {
        hospitalTagMapper.deleteById(id);
    }

    @Override
    public HospitalTagVO getTag(Long id) {
        HospitalTagDO tag = hospitalTagMapper.selectById(id);
        if (tag == null) {
            return null;
        }
        return convertToVO(tag);
    }

    @Override
    public List<HospitalTagVO> getTagList() {
        LambdaQueryWrapper<HospitalTagDO> wrapper = new LambdaQueryWrapper<HospitalTagDO>()
                .eq(HospitalTagDO::getStatus, 1)
                .orderByAsc(HospitalTagDO::getSort);
        return hospitalTagMapper.selectList(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<HospitalTagVO> getTagListByCategory(String category) {
        LambdaQueryWrapper<HospitalTagDO> wrapper = new LambdaQueryWrapper<HospitalTagDO>()
                .eq(HospitalTagDO::getTagCategory, category)
                .eq(HospitalTagDO::getStatus, 1)
                .orderByAsc(HospitalTagDO::getSort);
        return hospitalTagMapper.selectList(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<HospitalTagVO> getTagStatistics() {
        List<HospitalTagVO> tags = getTagList();
        for (HospitalTagVO tag : tags) {
            long count = hospitalTagRelationMapper.selectCount(
                    new LambdaQueryWrapper<HospitalTagRelationDO>()
                            .eq(HospitalTagRelationDO::getTagId, tag.getId()));
            tag.setHospitalCount(count);
        }
        return tags;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignTags(HospitalTagAssignReqVO reqVO) {
        log.info("[assignTags] hospitalCode={}, tagIds={}", reqVO.getHospitalCode(), reqVO.getTagIds());
        // 删除原有标签关联
        hospitalTagRelationMapper.deleteByHospitalCode(reqVO.getHospitalCode());

        // 批量插入新标签
        if (CollUtil.isNotEmpty(reqVO.getTagIds())) {
            List<HospitalTagRelationDO> relations = reqVO.getTagIds().stream()
                    .map(tagId -> HospitalTagRelationDO.builder()
                            .hospitalCode(reqVO.getHospitalCode())
                            .tagId(tagId)
                            .build())
                    .collect(Collectors.toList());
            log.info("[assignTags] batchInsert relations size={}", relations.size());
            hospitalTagRelationMapper.batchInsert(relations);
        }
        log.info("[assignTags] done");
    }

    @Override
    public List<HospitalTagVO> getTagsByHospitalCode(String hospitalCode) {
        List<Long> tagIds = hospitalTagRelationMapper.selectTagIdsByHospitalCode(hospitalCode);
        if (CollUtil.isEmpty(tagIds)) {
            return new ArrayList<>();
        }
        return hospitalTagMapper.selectBatchIds(tagIds).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    private HospitalTagVO convertToVO(HospitalTagDO tag) {
        return HospitalTagVO.builder()
                .id(tag.getId())
                .tagCode(tag.getTagCode())
                .tagName(tag.getTagName())
                .tagCategory(tag.getTagCategory())
                .tagType(tag.getTagType())
                .parentId(tag.getParentId())
                .sort(tag.getSort())
                .status(tag.getStatus())
                .build();
    }
}
