package cn.gemrun.base.module.declare.api.dto;

import lombok.Data;

/**
 * 专家信息 Response DTO
 *
 * 用于其他模块获取专家信息
 *
 * @author Gemini
 */
@Data
public class ExpertRespDTO {

    /**
     * 主键（自增）
     */
    private Long id;

    /**
     * 关联系统用户ID
     */
    private Long userId;

    /**
     * 专家姓名
     */
    private String expertName;

    /**
     * 专家类型：1=技术专家，2=财务专家，3=管理专家，4=行业专家
     */
    private Integer expertType;

    /**
     * 状态：1=在册，2=暂停，3=注销
     */
    private Integer status;

}
