package cn.gemrun.base.module.declare.service.project;

import cn.gemrun.base.module.declare.controller.admin.project.vo.ProjectTypeCreateReqVO;
import cn.gemrun.base.module.declare.controller.admin.project.vo.ProjectTypePageReqVO;
import cn.gemrun.base.module.declare.controller.admin.project.vo.ProjectTypeUpdateReqVO;
import cn.gemrun.base.module.declare.controller.admin.project.vo.ProjectTypeVO;
import cn.gemrun.base.module.declare.dal.dataobject.project.ProjectTypeDO;
import cn.gemrun.base.framework.common.pojo.PageResult;

import javax.validation.Valid;
import java.util.List;

/**
 * 项目类型 Service 接口
 *
 * @author Gemini
 */
public interface ProjectTypeService {

    /**
     * 创建项目类型
     */
    Long createProjectType(@Valid ProjectTypeCreateReqVO reqVO);

    /**
     * 更新项目类型
     */
    void updateProjectType(@Valid ProjectTypeUpdateReqVO reqVO);

    /**
     * 删除项目类型
     */
    void deleteProjectType(Long id);

    /**
     * 获取项目类型详情
     */
    ProjectTypeVO getProjectType(Long id);

    /**
     * 获取项目类型详情（通过typeValue）
     */
    ProjectTypeDO getProjectTypeByTypeValue(Integer typeValue);

    /**
     * 获取项目类型分页列表
     */
    PageResult<ProjectTypeVO> getProjectTypePage(ProjectTypePageReqVO reqVO);

    /**
     * 获取项目类型列表（启用状态）
     */
    List<ProjectTypeVO> getProjectTypeList();

    /**
     * 获取项目类型列表（简化格式：value/label）
     */
    List<ProjectTypeVO> getProjectTypeSimpleList();

    /**
     * 根据typeValue获取类型名称
     */
    String getProjectTypeName(Integer typeValue);

    /**
     * 根据typeValue获取类型显示标题（title字段，如"综合型医院"）
     */
    String getProjectTypeTitle(Integer typeValue);

}
