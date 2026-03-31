package cn.gemrun.base.module.declare.service.project.impl;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.declare.controller.admin.project.vo.ProjectTypeCreateReqVO;
import cn.gemrun.base.module.declare.controller.admin.project.vo.ProjectTypePageReqVO;
import cn.gemrun.base.module.declare.controller.admin.project.vo.ProjectTypeUpdateReqVO;
import cn.gemrun.base.module.declare.controller.admin.project.vo.ProjectTypeVO;
import cn.gemrun.base.module.declare.dal.dataobject.project.ProjectTypeDO;
import cn.gemrun.base.module.declare.dal.mysql.ProjectTypeMapper;
import cn.gemrun.base.module.declare.service.project.ProjectTypeService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目类型 Service 实现
 *
 * @author Gemini
 */
@Service
@Validated
@RequiredArgsConstructor
public class ProjectTypeServiceImpl implements ProjectTypeService {

    private final ProjectTypeMapper projectTypeMapper;

    @Override
    public Long createProjectType(ProjectTypeCreateReqVO reqVO) {
        // 校验 typeCode 唯一性
        Long count = projectTypeMapper.selectCount(
                new LambdaQueryWrapper<ProjectTypeDO>()
                        .eq(ProjectTypeDO::getTypeCode, reqVO.getTypeCode()));
        if (count > 0) {
            throw new RuntimeException("类型编码已存在");
        }
        // 校验 typeValue 唯一性
        count = projectTypeMapper.selectCount(
                new LambdaQueryWrapper<ProjectTypeDO>()
                        .eq(ProjectTypeDO::getTypeValue, reqVO.getTypeValue()));
        if (count > 0) {
            throw new RuntimeException("类型值已存在");
        }

        ProjectTypeDO projectType = ProjectTypeDO.builder()
                .typeCode(reqVO.getTypeCode())
                .typeValue(reqVO.getTypeValue())
                .name(reqVO.getName())
                .title(reqVO.getTitle())
                .description(reqVO.getDescription())
                .icon(reqVO.getIcon())
                .color(reqVO.getColor())
                .sort(reqVO.getSort() != null ? reqVO.getSort() : 0)
                .status(1)
                .build();
        projectTypeMapper.insert(projectType);
        return projectType.getId();
    }

    @Override
    public void updateProjectType(ProjectTypeUpdateReqVO reqVO) {
        ProjectTypeDO projectType = ProjectTypeDO.builder()
                .id(reqVO.getId())
                .name(reqVO.getName())
                .title(reqVO.getTitle())
                .description(reqVO.getDescription())
                .icon(reqVO.getIcon())
                .color(reqVO.getColor())
                .sort(reqVO.getSort())
                .status(reqVO.getStatus())
                .build();
        projectTypeMapper.updateById(projectType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProjectType(Long id) {
        projectTypeMapper.deleteById(id);
    }

    @Override
    public ProjectTypeVO getProjectType(Long id) {
        ProjectTypeDO projectType = projectTypeMapper.selectById(id);
        if (projectType == null) {
            return null;
        }
        return convertToVO(projectType);
    }

    @Override
    public ProjectTypeDO getProjectTypeByTypeValue(Integer typeValue) {
        return projectTypeMapper.selectOne(
                new LambdaQueryWrapper<ProjectTypeDO>()
                        .eq(ProjectTypeDO::getTypeValue, typeValue)
                        .eq(ProjectTypeDO::getStatus, 1));
    }

    @Override
    public PageResult<ProjectTypeVO> getProjectTypePage(ProjectTypePageReqVO reqVO) {
        LambdaQueryWrapper<ProjectTypeDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotEmpty(reqVO.getTypeCode()), ProjectTypeDO::getTypeCode, reqVO.getTypeCode())
                .like(StrUtil.isNotEmpty(reqVO.getName()), ProjectTypeDO::getName, reqVO.getName())
                .eq(reqVO.getStatus() != null, ProjectTypeDO::getStatus, reqVO.getStatus())
                .orderByAsc(ProjectTypeDO::getSort)
                .orderByDesc(ProjectTypeDO::getCreateTime);

        IPage<ProjectTypeDO> page = projectTypeMapper.selectPage(new Page<>(reqVO.getPageNo(), reqVO.getPageSize()), wrapper);
        List<ProjectTypeVO> voList = page.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return new PageResult<>(voList, page.getTotal());
    }

    @Override
    public List<ProjectTypeVO> getProjectTypeList() {
        List<ProjectTypeDO> list = projectTypeMapper.selectList(
                new LambdaQueryWrapper<ProjectTypeDO>()
                        .eq(ProjectTypeDO::getStatus, 1)
                        .orderByAsc(ProjectTypeDO::getSort));
        return list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProjectTypeVO> getProjectTypeSimpleList() {
        // 获取所有启用的项目类型
        List<ProjectTypeDO> list = projectTypeMapper.selectList(
                new LambdaQueryWrapper<ProjectTypeDO>()
                        .eq(ProjectTypeDO::getStatus, 1)
                        .orderByAsc(ProjectTypeDO::getSort));
        return list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public String getProjectTypeName(Integer typeValue) {
        if (typeValue == null) {
            return null;
        }
        ProjectTypeDO projectType = projectTypeMapper.selectOne(
                new LambdaQueryWrapper<ProjectTypeDO>()
                        .eq(ProjectTypeDO::getTypeValue, typeValue)
                        .eq(ProjectTypeDO::getStatus, 1));
        return projectType != null ? projectType.getName() : null;
    }

    @Override
    public String getProjectTypeTitle(Integer typeValue) {
        if (typeValue == null) {
            return null;
        }
        ProjectTypeDO projectType = projectTypeMapper.selectOne(
                new LambdaQueryWrapper<ProjectTypeDO>()
                        .eq(ProjectTypeDO::getTypeValue, typeValue)
                        .eq(ProjectTypeDO::getStatus, 1));
        return projectType != null ? projectType.getTitle() : null;
    }

    /**
     * DO 转 VO
     */
    private ProjectTypeVO convertToVO(ProjectTypeDO projectType) {
        return ProjectTypeVO.builder()
                .id(projectType.getId())
                .typeCode(projectType.getTypeCode())
                .typeValue(projectType.getTypeValue())
                .name(projectType.getName())
                .title(projectType.getTitle())
                .description(projectType.getDescription())
                .icon(projectType.getIcon())
                .color(projectType.getColor())
                .sort(projectType.getSort())
                .status(projectType.getStatus())
                .build();
    }

}
