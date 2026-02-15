package cn.gemrun.base.module.bpm.service.definition;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.module.bpm.controller.admin.businessType.vo.BpmBusinessTypePageReqVO;
import cn.gemrun.base.module.bpm.controller.admin.businessType.vo.BpmBusinessTypeSaveReqVO;
import cn.gemrun.base.module.bpm.dal.dataobject.BpmBusinessTypeDO;
import cn.gemrun.base.module.bpm.dal.mysql.BpmBusinessTypeMapper;
import cn.gemrun.base.module.bpm.enums.ErrorCodeConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.gemrun.base.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * BPM 业务类型 Service 实现类
 *
 * @author Gemini
 */
@Slf4j
@Service
@Validated
public class BpmBusinessTypeServiceImpl implements BpmBusinessTypeService {

    @Resource
    private BpmBusinessTypeMapper businessTypeMapper;

    @Override
    public Long createBusinessType(BpmBusinessTypeSaveReqVO createReqVO) {
        // 校验业务类型标识唯一
        validateBusinessTypeUnique(createReqVO.getBusinessType(), null);

        // 校验流程定义存在（这里只是简单校验，实际应该调用流程定义Service）
        validateProcessDefinitionKey(createReqVO.getProcessDefinitionKey());

        // 插入
        BpmBusinessTypeDO businessType = BeanUtils.toBean(createReqVO, BpmBusinessTypeDO.class);
        businessTypeMapper.insert(businessType);

        log.info("创建业务类型成功, id={}, businessType={}",
                businessType.getId(), businessType.getBusinessType());

        return businessType.getId();
    }

    @Override
    public void updateBusinessType(BpmBusinessTypeSaveReqVO updateReqVO) {
        // 校验存在
        validateBusinessTypeExists(updateReqVO.getId());

        // 校验业务类型标识唯一
        validateBusinessTypeUnique(updateReqVO.getBusinessType(), updateReqVO.getId());

        // 校验流程定义存在
        validateProcessDefinitionKey(updateReqVO.getProcessDefinitionKey());

        // 更新
        BpmBusinessTypeDO updateObj = BeanUtils.toBean(updateReqVO, BpmBusinessTypeDO.class);
        businessTypeMapper.updateById(updateObj);

        log.info("更新业务类型成功, id={}", updateReqVO.getId());
    }

    @Override
    public void deleteBusinessType(Long id) {
        // 校验存在
        validateBusinessTypeExists(id);

        // 删除
        businessTypeMapper.deleteById(id);

        log.info("删除业务类型成功, id={}", id);
    }

    @Override
    public BpmBusinessTypeDO getBusinessType(Long id) {
        return businessTypeMapper.selectById(id);
    }

    @Override
    public BpmBusinessTypeDO getBusinessTypeByType(String businessType) {
        return businessTypeMapper.selectByBusinessType(businessType);
    }

    @Override
    public List<BpmBusinessTypeDO> getBusinessTypeListByProcessKey(String processDefinitionKey) {
        return businessTypeMapper.selectByProcessDefinitionKey(processDefinitionKey);
    }

    @Override
    public List<BpmBusinessTypeDO> getBusinessTypeListByCategory(String category) {
        return businessTypeMapper.selectByCategory(category);
    }

    @Override
    public List<BpmBusinessTypeDO> getAllEnabledBusinessTypeList() {
        return businessTypeMapper.selectAllEnabled();
    }

    @Override
    public PageResult<BpmBusinessTypeDO> getBusinessTypePage(BpmBusinessTypePageReqVO pageReqVO) {
        return businessTypeMapper.selectPage(pageReqVO);
    }

    @Override
    public String getProcessDefinitionKey(String businessType) {
        BpmBusinessTypeDO businessTypeDO = businessTypeMapper.selectByBusinessType(businessType);
        if (businessTypeDO == null || businessTypeDO.getEnabled() == 0) {
            return null;
        }
        return businessTypeDO.getProcessDefinitionKey();
    }

    /**
     * 校验业务类型是否存在
     */
    private void validateBusinessTypeExists(Long id) {
        if (businessTypeMapper.selectById(id) == null) {
            throw exception(ErrorCodeConstants.BUSINESS_TYPE_NOT_EXISTS);
        }
    }

    /**
     * 校验业务类型标识唯一性
     */
    private void validateBusinessTypeUnique(String businessType, Long excludeId) {
        BpmBusinessTypeDO existing = businessTypeMapper.selectByBusinessType(businessType);
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw exception(ErrorCodeConstants.BUSINESS_TYPE_EXISTS);
        }
    }

    /**
     * 校验流程定义Key是否存在
     * 这里可以调用流程定义Service进行更严格的校验
     * 目前仅做简单校验
     */
    private void validateProcessDefinitionKey(String processDefinitionKey) {
        if (processDefinitionKey == null || processDefinitionKey.trim().isEmpty()) {
            throw exception(ErrorCodeConstants.PROCESS_DEFINITION_KEY_EMPTY);
        }
    }

}
