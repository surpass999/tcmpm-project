package cn.gemrun.base.module.bpm.service.definition;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.json.JsonUtils;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.module.bpm.controller.admin.dsl.vo.BpmProcessNodeDslPageReqVO;
import cn.gemrun.base.module.bpm.controller.admin.dsl.vo.BpmProcessNodeDslSaveReqVO;
import cn.gemrun.base.module.bpm.dal.dataobject.dsl.BpmProcessNodeDslDO;
import cn.gemrun.base.module.bpm.dal.mysql.dsl.BpmProcessNodeDslMapper;
import cn.gemrun.base.module.bpm.enums.ErrorCodeConstants;
import cn.gemrun.base.module.bpm.framework.dsl.config.DslConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.gemrun.base.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * BPM 流程节点 DSL 配置 Service 实现类
 *
 * @author Gemini
 */
@Slf4j
@Service
@Validated
public class BpmProcessNodeDslServiceImpl implements BpmProcessNodeDslService {

    @Resource
    private BpmProcessNodeDslMapper dslMapper;

    @Override
    public Long createDsl(BpmProcessNodeDslSaveReqVO createReqVO) {
        // 校验 DSL 配置
        validateDslConfig(createReqVO.getDslConfig());
        
        // 校验流程定义Key + 节点Key 唯一
        validateDslUnique(createReqVO.getProcessDefinitionKey(), createReqVO.getNodeKey(), null);
        
        // 插入
        BpmProcessNodeDslDO dsl = BeanUtils.toBean(createReqVO, BpmProcessNodeDslDO.class);
        dslMapper.insert(dsl);
        
        log.info("创建 DSL 配置成功, id={}, processKey={}, nodeKey={}", 
                dsl.getId(), dsl.getProcessDefinitionKey(), dsl.getNodeKey());
        
        return dsl.getId();
    }

    @Override
    public void updateDsl(BpmProcessNodeDslSaveReqVO updateReqVO) {
        // 校验存在
        validateDslExists(updateReqVO.getId());
        
        // 校验 DSL 配置
        validateDslConfig(updateReqVO.getDslConfig());
        
        // 校验流程定义Key + 节点Key 唯一
        validateDslUnique(updateReqVO.getProcessDefinitionKey(), updateReqVO.getNodeKey(), updateReqVO.getId());
        
        // 更新
        BpmProcessNodeDslDO updateObj = BeanUtils.toBean(updateReqVO, BpmProcessNodeDslDO.class);
        dslMapper.updateById(updateObj);
        
        log.info("更新 DSL 配置成功, id={}", updateReqVO.getId());
    }

    @Override
    public void deleteDsl(Long id) {
        // 校验存在
        validateDslExists(id);
        
        // 删除
        dslMapper.deleteById(id);
        
        log.info("删除 DSL 配置成功, id={}", id);
    }

    @Override
    public BpmProcessNodeDslDO getDsl(Long id) {
        return dslMapper.selectById(id);
    }

    @Override
    public BpmProcessNodeDslDO getDslByNodeKey(String processDefinitionKey, String nodeKey) {
        return dslMapper.selectByProcessDefinitionKeyAndNodeKey(processDefinitionKey, nodeKey);
    }

    @Override
    public List<BpmProcessNodeDslDO> getDslListByProcessKey(String processDefinitionKey) {
        return dslMapper.selectByProcessDefinitionKey(processDefinitionKey);
    }

    @Override
    public List<BpmProcessNodeDslDO> getAllEnabledDslList() {
        return dslMapper.selectAllEnabled();
    }

    @Override
    public PageResult<BpmProcessNodeDslDO> getDslPage(BpmProcessNodeDslPageReqVO pageReqVO) {
        return dslMapper.selectPage(pageReqVO);
    }

    @Override
    public void validateDslConfig(String dslConfigJson) {
        if (dslConfigJson == null || dslConfigJson.trim().isEmpty()) {
            throw exception(ErrorCodeConstants.DSL_CONFIG_IS_EMPTY);
        }
        
        try {
            // 尝试解析 JSON
            DslConfig config = JsonUtils.parseObject(dslConfigJson, DslConfig.class);
            
            // 校验必需字段
            if (config == null) {
                throw exception(ErrorCodeConstants.DSL_CONFIG_IS_EMPTY);
            }
            
            if (config.getNodeKey() == null || config.getNodeKey().trim().isEmpty()) {
                throw exception(ErrorCodeConstants.DSL_CONFIG_NODE_KEY_EMPTY);
            }
            
            if (config.getCap() == null || config.getCap().trim().isEmpty()) {
                throw exception(ErrorCodeConstants.DSL_CONFIG_CAP_EMPTY);
            }
            
        } catch (Exception e) {
            if (e instanceof cn.gemrun.base.framework.common.exception.ServiceException) {
                throw (cn.gemrun.base.framework.common.exception.ServiceException) e;
            }
            log.error("DSL 配置 JSON 解析失败: {}", dslConfigJson, e);
            throw exception(ErrorCodeConstants.DSL_CONFIG_INVALID);
        }
    }

    /**
     * 校验 DSL 配置是否存在
     */
    private void validateDslExists(Long id) {
        if (dslMapper.selectById(id) == null) {
            throw exception(ErrorCodeConstants.DSL_NOT_EXISTS);
        }
    }

    /**
     * 校验流程定义Key + 节点Key 唯一性
     */
    private void validateDslUnique(String processDefinitionKey, String nodeKey, Long excludeId) {
        BpmProcessNodeDslDO existing = dslMapper.selectByProcessDefinitionKeyAndNodeKey(processDefinitionKey, nodeKey);
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw exception(ErrorCodeConstants.DSL_NODE_KEY_EXISTS);
        }
    }

}
