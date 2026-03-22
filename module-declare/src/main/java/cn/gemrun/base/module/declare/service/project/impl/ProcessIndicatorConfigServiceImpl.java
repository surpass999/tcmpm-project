package cn.gemrun.base.module.declare.service.project.impl;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.exception.ServiceException;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.module.declare.controller.admin.project.vo.ProcessIndicatorConfigPageReqVO;
import cn.gemrun.base.module.declare.controller.admin.project.vo.ProcessIndicatorConfigSaveReqVO;
import cn.gemrun.base.module.declare.dal.dataobject.project.ProcessIndicatorConfigDO;
import cn.gemrun.base.module.declare.dal.mysql.project.ProcessIndicatorConfigMapper;
import cn.gemrun.base.module.declare.enums.ErrorCodeConstants;
import cn.gemrun.base.module.declare.service.project.ProcessIndicatorConfigService;
import cn.gemrun.base.module.system.dal.dataobject.dict.DictDataDO;
import cn.gemrun.base.module.system.service.dict.DictDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 过程类型指标配置 Service 实现
 *
 * @author Gemini
 */
@Slf4j
@Service
public class ProcessIndicatorConfigServiceImpl implements ProcessIndicatorConfigService {

    /**
     * 项目类型字典类型
     */
    private static final String DICT_TYPE_PROJECT_TYPE = "declare_project_type";

    @Resource
    private ProcessIndicatorConfigMapper processIndicatorConfigMapper;

    @Resource
    private DictDataService dictDataService;

    @Override
    public Long createConfig(ProcessIndicatorConfigSaveReqVO reqVO) {
        // 校验唯一性
        validateConfigUnique(reqVO.getProcessType(), reqVO.getProjectType(), reqVO.getIndicatorId(), null);

        ProcessIndicatorConfigDO config = BeanUtils.toBean(reqVO, ProcessIndicatorConfigDO.class);
        processIndicatorConfigMapper.insert(config);
        return config.getId();
    }

    @Override
    public void updateConfig(ProcessIndicatorConfigSaveReqVO reqVO) {
        // 校验存在
        validateConfigExists(reqVO.getId());
        // 校验唯一性
        validateConfigUnique(reqVO.getProcessType(), reqVO.getProjectType(), reqVO.getIndicatorId(), reqVO.getId());

        ProcessIndicatorConfigDO config = BeanUtils.toBean(reqVO, ProcessIndicatorConfigDO.class);
        processIndicatorConfigMapper.updateById(config);
    }

    @Override
    public void deleteConfig(Long id) {
        validateConfigExists(id);
        processIndicatorConfigMapper.deleteById(id);
    }

    @Override
    public ProcessIndicatorConfigDO getConfig(Long id) {
        return processIndicatorConfigMapper.selectById(id);
    }

    @Override
    public PageResult<ProcessIndicatorConfigDO> getConfigPage(ProcessIndicatorConfigPageReqVO pageReqVO) {
        return processIndicatorConfigMapper.selectPage(pageReqVO, new cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX<ProcessIndicatorConfigDO>()
                .eq(pageReqVO.getProcessType() != null, ProcessIndicatorConfigDO::getProcessType, pageReqVO.getProcessType())
                .eq(pageReqVO.getProjectType() != null, ProcessIndicatorConfigDO::getProjectType, pageReqVO.getProjectType())
                .eq(pageReqVO.getIndicatorId() != null, ProcessIndicatorConfigDO::getIndicatorId, pageReqVO.getIndicatorId())
                .orderByAsc(ProcessIndicatorConfigDO::getProcessType)
                .orderByAsc(ProcessIndicatorConfigDO::getProjectType)
                .orderByAsc(ProcessIndicatorConfigDO::getSort));
    }

    @Override
    public List<ProcessIndicatorConfigDO> getConfigListByProcessTypeAndProjectType(Integer processType, Integer projectType) {
        return processIndicatorConfigMapper.selectByProcessTypeAndProjectType(processType, projectType);
    }

    @Override
    public List<Long> getIndicatorIdsByProcessTypeAndProjectType(Integer processType, Integer projectType) {
        List<ProcessIndicatorConfigDO> configs = processIndicatorConfigMapper.selectByProcessTypeAndProjectType(processType, projectType);
        if (CollectionUtils.isEmpty(configs)) {
            return new ArrayList<>();
        }
        return configs.stream()
                .map(ProcessIndicatorConfigDO::getIndicatorId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveConfigs(Integer processType, Integer projectType, List<ProcessIndicatorConfigSaveReqVO> configs) {
        // 先删除原配置
        processIndicatorConfigMapper.deleteByProcessTypeAndProjectType(processType, projectType);

        // 再插入新配置
        if (!CollectionUtils.isEmpty(configs)) {
            for (int i = 0; i < configs.size(); i++) {
                ProcessIndicatorConfigSaveReqVO reqVO = configs.get(i);
                ProcessIndicatorConfigDO config = new ProcessIndicatorConfigDO();
                config.setProcessType(processType);
                config.setProjectType(projectType);
                config.setIndicatorId(reqVO.getIndicatorId());
                config.setIsRequired(reqVO.getIsRequired() != null ? reqVO.getIsRequired() : false);
                config.setSort(reqVO.getSort() != null ? reqVO.getSort() : i);
                processIndicatorConfigMapper.insert(config);
            }
        }
    }

    /**
     * 校验配置是否存在
     */
    private void validateConfigExists(Long id) {
        if (processIndicatorConfigMapper.selectById(id) == null) {
            throw new ServiceException(ErrorCodeConstants.PROCESS_INDICATOR_CONFIG_NOT_EXISTS);
        }
    }

    /**
     * 校验配置唯一性
     */
    private void validateConfigUnique(Integer processType, Integer projectType, Long indicatorId, Long excludeId) {
        List<ProcessIndicatorConfigDO> configs = processIndicatorConfigMapper.selectList(new cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX<ProcessIndicatorConfigDO>()
                .eq(ProcessIndicatorConfigDO::getProcessType, processType)
                .eq(ProcessIndicatorConfigDO::getProjectType, projectType)
                .eq(ProcessIndicatorConfigDO::getIndicatorId, indicatorId)
                .ne(excludeId != null, ProcessIndicatorConfigDO::getId, excludeId));
        if (!CollectionUtils.isEmpty(configs)) {
            throw new ServiceException(ErrorCodeConstants.PROCESS_INDICATOR_CONFIG_EXISTS);
        }
    }

    @Override
    public String getProjectTypeName(Integer projectType) {
        if (projectType == null) {
            return "未知";
        }
        // projectType=0 表示通用类型
        if (projectType == 0) {
            return "通用类型";
        }
        try {
            DictDataDO dictData = dictDataService.getDictData(DICT_TYPE_PROJECT_TYPE, String.valueOf(projectType));
            return dictData != null ? dictData.getLabel() : "未知";
        } catch (Exception e) {
            log.warn("获取项目类型名称失败, projectType: {}, error: {}", projectType, e.getMessage());
            return "未知";
        }
    }

}
