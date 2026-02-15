package cn.gemrun.base.module.bpm.dal.dataobject;

import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * BPM 业务类型 DO
 *
 * 用于建立业务类型与流程定义的映射关系
 *
 * 业务类型示例：
 * - declare:filing:create (备案申请)
 * - declare:filing:update (备案修改)
 * - project:establish (立项申请)
 * - project:rectification (整改申请)
 *
 * @author Gemini
 */
@TableName("bpm_business_type")
@KeySequence("bpm_business_type_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BpmBusinessTypeDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;

    /**
     * 业务类型标识
     * 格式：模块:业务:动作，如 declare:filing:create
     */
    private String businessType;

    /**
     * 业务类型名称
     * 如：备案申请、备案修改、立项申请等
     */
    private String businessName;

    /**
     * 流程定义Key（对应 Flowable 的 processDefinitionKey）
     */
    private String processDefinitionKey;

    /**
     * 流程分类
     * 用于分组管理，如：declare、project、achievement 等
     */
    private String processCategory;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否启用
     * 0 = 禁用
     * 1 = 启用
     */
    private Integer enabled;

    /**
     * 排序
     */
    private Integer sort;

}
