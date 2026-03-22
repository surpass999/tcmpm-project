package cn.gemrun.base.module.declare.service.project;

import java.util.Map;

import javax.validation.Valid;

import cn.gemrun.base.module.declare.controller.admin.project.vo.ProcessFormConfigRespVO;
import cn.gemrun.base.module.declare.controller.admin.project.vo.ProcessFormDataRespVO;
import cn.gemrun.base.module.declare.controller.admin.project.vo.ProcessFormDataSaveReqVO;

/**
 * 过程填报 Service 接口
 *
 * @author Gemini
 */
public interface ProcessFormService {

    /**
     * 获取表单配置（包含项目信息、指标配置、文本字段配置、已填报数据）
     *
     * @param processId 过程记录ID
     * @return 表单配置
     */
    ProcessFormConfigRespVO getFormConfig(Long processId);

    /**
     * 获取填报数据
     *
     * @param processId 过程记录ID
     * @return 填报数据
     */
    ProcessFormDataRespVO getFormData(Long processId);

    /**
     * 保存/提交填报数据
     *
     * @param reqVO 保存请求
     * @return 过程记录ID
     */
    Long saveFormData(ProcessFormDataSaveReqVO reqVO);

    /**
     * 验证填报数据
     *
     * @param reqVO 验证请求
     * @return 验证结果（errorMessages为空表示验证通过）
     */
    Map<String, Object> validateFormData(ProcessFormDataSaveReqVO reqVO);

    /**
     * 撤回填报数据
     *
     * @param processId 过程记录ID
     */
    void withdrawFormData(Long processId);

    /**
     * 审批通过后同步指标到项目
     * （被流程监听器调用）
     *
     * @param processId 过程记录ID
     */
    void syncIndicatorToProject(Long processId);

    /**
     * 发送催办通知
     *
     * @param processId 过程记录ID
     */
    void sendReminderNotification(Long processId);

}
