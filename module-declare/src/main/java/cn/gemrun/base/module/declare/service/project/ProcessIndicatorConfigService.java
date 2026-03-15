package cn.gemrun.base.module.declare.service.project;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.declare.controller.admin.project.vo.ProcessIndicatorConfigPageReqVO;
import cn.gemrun.base.module.declare.controller.admin.project.vo.ProcessIndicatorConfigSaveReqVO;
import cn.gemrun.base.module.declare.dal.dataobject.project.ProcessIndicatorConfigDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 过程类型指标配置 Service 接口
 *
 * @author Gemini
 */
public interface ProcessIndicatorConfigService {

    /**
     * 创建过程指标配置
     */
    Long createConfig(@Valid ProcessIndicatorConfigSaveReqVO reqVO);

    /**
     * 更新过程指标配置
     */
    void updateConfig(@Valid ProcessIndicatorConfigSaveReqVO reqVO);

    /**
     * 删除过程指标配置
     */
    void deleteConfig(Long id);

    /**
     * 获取过程指标配置
     */
    ProcessIndicatorConfigDO getConfig(Long id);

    /**
     * 获取过程指标配置分页
     */
    PageResult<ProcessIndicatorConfigDO> getConfigPage(ProcessIndicatorConfigPageReqVO pageReqVO);

    /**
     * 根据过程类型和项目类型获取已配置的指标列表
     */
    List<ProcessIndicatorConfigDO> getConfigListByProcessTypeAndProjectType(Integer processType, Integer projectType);

    /**
     * 根据过程类型和项目类型获取已配置的指标ID列表
     */
    List<Long> getIndicatorIdsByProcessTypeAndProjectType(Integer processType, Integer projectType);

    /**
     * 保存过程指标配置（批量）
     * 先删除原配置，再插入新配置
     */
    void saveConfigs(Integer processType, Integer projectType, List<ProcessIndicatorConfigSaveReqVO> configs);

    /**
     * 获取项目类型名称
     *
     * @param projectType 项目类型
     * @return 项目类型名称
     */
    String getProjectTypeName(Integer projectType);

}
