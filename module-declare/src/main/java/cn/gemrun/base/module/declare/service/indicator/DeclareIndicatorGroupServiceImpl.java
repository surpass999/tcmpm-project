package cn.gemrun.base.module.declare.service.indicator;

import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.declare.controller.admin.indicator.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorGroupDO;
import cn.gemrun.base.module.declare.dal.mysql.indicator.DeclareIndicatorGroupMapper;
import cn.gemrun.base.module.declare.enums.ErrorCodeConstants;
import cn.gemrun.base.module.declare.enums.ProjectTypeEnum;
import cn.gemrun.base.framework.common.exception.util.ServiceExceptionUtil;
import cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 指标分组 Service 实现
 *
 * @author Gemini
 */
@Slf4j
@Service
public class DeclareIndicatorGroupServiceImpl implements DeclareIndicatorGroupService {

    @Resource
    private DeclareIndicatorGroupMapper groupMapper;

    @Override
    public Long createGroup(DeclareIndicatorGroupSaveReqVO createReqVO) {
        // 校验分组编码唯一
        DeclareIndicatorGroupDO existing = groupMapper.selectByGroupCode(createReqVO.getGroupCode());
        if (existing != null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.INDICATOR_GROUP_CODE_EXISTS, createReqVO.getGroupCode());
        }

        // 一级分组必须选择项目类型
        if (createReqVO.getGroupLevel() == null || createReqVO.getGroupLevel() == 1) {
            if (createReqVO.getProjectType() == null) {
                throw ServiceExceptionUtil.exception(ErrorCodeConstants.INDICATOR_GROUP_PROJECT_TYPE_REQUIRED);
            }
        }

        // 插入
        DeclareIndicatorGroupDO group = BeanUtils.toBean(createReqVO, DeclareIndicatorGroupDO.class);
        // 如果是一级分组，parentId设为0
        if (group.getParentId() == null) {
            group.setParentId(0L);
        }
        groupMapper.insert(group);
        return group.getId();
    }

    @Override
    public void updateGroup(DeclareIndicatorGroupSaveReqVO updateReqVO) {
        // 校验存在
        DeclareIndicatorGroupDO existing = groupMapper.selectById(updateReqVO.getId());
        if (existing == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.INDICATOR_GROUP_NOT_EXISTS);
        }

        // 如果修改了分组编码，校验唯一
        if (!existing.getGroupCode().equals(updateReqVO.getGroupCode())) {
            DeclareIndicatorGroupDO sameCode = groupMapper.selectByGroupCode(updateReqVO.getGroupCode());
            if (sameCode != null) {
                throw ServiceExceptionUtil.exception(ErrorCodeConstants.INDICATOR_GROUP_CODE_EXISTS, updateReqVO.getGroupCode());
            }
        }

        // 更新
        DeclareIndicatorGroupDO group = BeanUtils.toBean(updateReqVO, DeclareIndicatorGroupDO.class);
        groupMapper.updateById(group);
    }

    @Override
    public void deleteGroup(Long id) {
        // 校验存在
        DeclareIndicatorGroupDO existing = groupMapper.selectById(id);
        if (existing == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.INDICATOR_GROUP_NOT_EXISTS);
        }

        // 校验是否有子分组
        List<DeclareIndicatorGroupDO> children = groupMapper.selectByParentId(id);
        if (!children.isEmpty()) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.INDICATOR_GROUP_HAS_CHILDREN);
        }

        // 删除
        groupMapper.deleteById(id);
    }

    @Override
    public DeclareIndicatorGroupDO getGroup(Long id) {
        return groupMapper.selectById(id);
    }

    @Override
    public DeclareIndicatorGroupRespVO getGroupResp(Long id) {
        DeclareIndicatorGroupDO group = groupMapper.selectById(id);
        if (group == null) {
            return null;
        }
        DeclareIndicatorGroupRespVO respVO = BeanUtils.toBean(group, DeclareIndicatorGroupRespVO.class);
        // 填充项目类型名称
        respVO.setProjectTypeName(getProjectTypeName(group.getProjectType()));
        return respVO;
    }

    @Override
    public PageResult<DeclareIndicatorGroupDO> getGroupPage(DeclareIndicatorGroupPageReqVO pageReqVO) {
        return groupMapper.selectPage(pageReqVO, new LambdaQueryWrapperX<DeclareIndicatorGroupDO>()
                .likeIfPresent(DeclareIndicatorGroupDO::getGroupCode, pageReqVO.getGroupCode())
                .likeIfPresent(DeclareIndicatorGroupDO::getGroupName, pageReqVO.getGroupName())
                .eqIfPresent(DeclareIndicatorGroupDO::getGroupLevel, pageReqVO.getGroupLevel())
                .eqIfPresent(DeclareIndicatorGroupDO::getProjectType, pageReqVO.getProjectType())
                .eqIfPresent(DeclareIndicatorGroupDO::getStatus, pageReqVO.getStatus())
                .orderByDesc(DeclareIndicatorGroupDO::getId));
    }

    @Override
    public List<DeclareIndicatorGroupRespVO> getGroupTree() {
        List<DeclareIndicatorGroupDO> all = groupMapper.selectAllEnabled();
        return buildTree(all, 0L);
    }

    private List<DeclareIndicatorGroupRespVO> buildTree(List<DeclareIndicatorGroupDO> all, Long parentId) {
        return all.stream()
                .filter(g -> parentId.equals(g.getParentId()))
                .map(g -> {
                    DeclareIndicatorGroupRespVO vo = BeanUtils.toBean(g, DeclareIndicatorGroupRespVO.class);
                    // 填充项目类型名称
                    vo.setProjectTypeName(getProjectTypeName(g.getProjectType()));
                    // 构建子节点
                    vo.setChildren(buildTree(all, g.getId()));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<DeclareIndicatorGroupDO> getAllEnabledList() {
        return groupMapper.selectAllEnabled();
    }

    @Override
    public List<DeclareIndicatorGroupDO> getLevelOneList(Integer projectType) {
        if (projectType == null) {
            return groupMapper.selectByGroupLevel(1);
        }
        // 按项目类型过滤一级分组
        return groupMapper.selectList(new LambdaQueryWrapperX<DeclareIndicatorGroupDO>()
                .eq(DeclareIndicatorGroupDO::getGroupLevel, 1)
                .eq(DeclareIndicatorGroupDO::getProjectType, projectType)
                .orderByAsc(DeclareIndicatorGroupDO::getSort));
    }

    @Override
    public List<DeclareIndicatorGroupDO> getLevelOneListByProjectType(Integer projectType) {
        return groupMapper.selectByProjectType(projectType);
    }

    @Override
    public List<DeclareIndicatorGroupDO> getLevelTwoListByParentId(Long parentId) {
        return groupMapper.selectByParentId(parentId);
    }

    @Override
    public List<DeclareIndicatorGroupRespVO> getGroupTreeByProjectType(Integer projectType) {
        List<DeclareIndicatorGroupDO> all = groupMapper.selectAllEnabled();
        // projectType 为 null 时返回全部
        if (projectType == null) {
            return buildTree(all, 0L);
        }
        // 只过滤一级分组（parentId=0 的记录），二级分组会通过 parentId 自动关联
        List<DeclareIndicatorGroupDO> roots = all.stream()
                .filter(g -> g.getParentId() == 0L && projectType.equals(g.getProjectType()))
                .collect(Collectors.toList());
        return roots.stream()
                .map(root -> buildTreeNode(all, root))
                .collect(Collectors.toList());
    }

    private DeclareIndicatorGroupRespVO buildTreeNode(List<DeclareIndicatorGroupDO> all, DeclareIndicatorGroupDO g) {
        DeclareIndicatorGroupRespVO vo = BeanUtils.toBean(g, DeclareIndicatorGroupRespVO.class);
        vo.setProjectTypeName(getProjectTypeName(g.getProjectType()));
        List<DeclareIndicatorGroupRespVO> children = all.stream()
                .filter(child -> g.getId().equals(child.getParentId()))
                .map(child -> buildTreeNode(all, child))
                .collect(Collectors.toList());
        vo.setChildren(children);
        return vo;
    }

    @Override
    public List<DeclareIndicatorGroupDO> getLevelTwoList() {
        // 查询 parentId != 0 的记录（所有二级分组）
        return groupMapper.selectList(new LambdaQueryWrapperX<DeclareIndicatorGroupDO>()
                .ne(DeclareIndicatorGroupDO::getParentId, 0L)
                .orderByAsc(DeclareIndicatorGroupDO::getSort));
    }

    /**
     * 获取项目类型名称
     */
    private String getProjectTypeName(Integer projectType) {
        if (projectType == null) {
            return null;
        }
        ProjectTypeEnum projectTypeEnum = ProjectTypeEnum.valueOf(projectType);
        return projectTypeEnum != null ? projectTypeEnum.getName() : null;
    }

}
