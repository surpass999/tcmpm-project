package cn.gemrun.base.module.bpm.service.definition;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.bpm.controller.admin.businessType.vo.BpmBusinessTypePageReqVO;
import cn.gemrun.base.module.bpm.controller.admin.businessType.vo.BpmBusinessTypeSaveReqVO;
import cn.gemrun.base.module.bpm.dal.dataobject.BpmBusinessTypeDO;

import javax.validation.Valid;
import java.util.List;

/**
 * BPM 业务类型 Service 接口
 *
 * 用于建立业务类型与流程定义的映射关系
 *
 * @author Gemini
 */
public interface BpmBusinessTypeService {

    /**
     * 创建业务类型
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createBusinessType(@Valid BpmBusinessTypeSaveReqVO createReqVO);

    /**
     * 更新业务类型
     *
     * @param updateReqVO 更新信息
     */
    void updateBusinessType(@Valid BpmBusinessTypeSaveReqVO updateReqVO);

    /**
     * 删除业务类型
     *
     * @param id 编号
     */
    void deleteBusinessType(Long id);

    /**
     * 获得业务类型
     *
     * @param id 编号
     * @return 业务类型
     */
    BpmBusinessTypeDO getBusinessType(Long id);

    /**
     * 根据业务类型标识获取业务类型
     *
     * @param businessType 业务类型标识
     * @return 业务类型
     */
    BpmBusinessTypeDO getBusinessTypeByType(String businessType);

    /**
     * 根据流程定义Key获取业务类型列表
     *
     * @param processDefinitionKey 流程定义Key
     * @return 业务类型列表
     */
    List<BpmBusinessTypeDO> getBusinessTypeListByProcessKey(String processDefinitionKey);

    /**
     * 根据分类获取业务类型列表
     *
     * @param category 流程分类
     * @return 业务类型列表
     */
    List<BpmBusinessTypeDO> getBusinessTypeListByCategory(String category);

    /**
     * 获取所有已启用的业务类型列表
     *
     * @return 业务类型列表
     */
    List<BpmBusinessTypeDO> getAllEnabledBusinessTypeList();

    /**
     * 获取业务类型分页
     *
     * @param pageReqVO 分页查询
     * @return 分页结果
     */
    PageResult<BpmBusinessTypeDO> getBusinessTypePage(BpmBusinessTypePageReqVO pageReqVO);

    /**
     * 根据业务类型标识获取流程定义Key
     *
     * @param businessType 业务类型标识
     * @return 流程定义Key，如果不存在或未启用返回null
     */
    String getProcessDefinitionKey(String businessType);

}
