package cn.gemrun.base.module.bpm.api;

import cn.gemrun.base.module.bpm.api.dto.BpmBusinessTypeGetReqDTO;
import cn.gemrun.base.module.bpm.api.dto.BpmBusinessTypeRespDTO;
import cn.gemrun.base.module.bpm.dal.dataobject.BpmBusinessTypeDO;
import cn.gemrun.base.module.bpm.service.definition.BpmBusinessTypeService;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * BPM 业务类型 Api 实现类
 *
 * @author Gemini
 */
@Slf4j
@Service
@Validated
public class BpmBusinessTypeApiImpl implements BpmBusinessTypeApi {

    @Resource
    private BpmBusinessTypeService businessTypeService;

    @Override
    public String getProcessDefinitionKey(String businessType) {
        return businessTypeService.getProcessDefinitionKey(businessType);
    }

    @Override
    public Boolean hasProcessConfig(String businessType) {
        return businessTypeService.getBusinessTypeByType(businessType) != null;
    }

    @Override
    public BpmBusinessTypeRespDTO getProcessConfig(@Valid BpmBusinessTypeGetReqDTO reqDTO) {
        BpmBusinessTypeDO businessTypeDO = businessTypeService.getBusinessTypeByType(reqDTO.getBusinessType());
        if (businessTypeDO == null) {
            return null;
        }
        return BeanUtils.toBean(businessTypeDO, BpmBusinessTypeRespDTO.class);
    }

}
