package cn.gemrun.base.module.declare.dal.dataobject.process;

import com.baomidou.mybatisplus.annotation.*;
import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;
import lombok.*;

import java.io.*;
import java.time.*;

/**
 * 业务类型与流程关联配置 DO
 *
 * @author Gemini
 */
@TableName("declare_business_type")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DeclareBusinessTypeDO extends BaseDO implements Serializable {

    /**
     * 业务类型
     * 格式：{模块名}:{业务名}:{动作}
     * 例如：declare:filing:submit
     */
    private String businessType;

    /**
     * 业务名称
     * 例如：备案提交
     */
    private String businessName;

    /**
     * 流程定义Key
     * 例如：proc_filing
     */
    private String processDefinitionKey;

    /**
     * 流程分类
     * 例如：declare_filing
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
