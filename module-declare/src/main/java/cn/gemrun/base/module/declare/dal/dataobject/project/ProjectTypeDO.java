package cn.gemrun.base.module.declare.dal.dataobject.project;

import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 项目类型定义 DO
 */
@TableName("declare_project_type")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectTypeDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 类型编码（唯一标识，如 PROJECT_01）
     */
    private String typeCode;

    /**
     * 类型值（业务使用：1=综合型，2=示范型等）
     */
    private Integer typeValue;

    /**
     * 类型名称（如 综合型）
     */
    private String name;

    /**
     * 显示标题（如 综合型医院）
     */
    private String title;

    /**
     * 类型描述
     */
    private String description;

    /**
     * 图标名称或URL
     */
    private String icon;

    /**
     * 主题颜色（如 #1890ff）
     */
    private String color;

    /**
     * 排序（升序）
     */
    private Integer sort;

    /**
     * 状态：0=禁用，1=启用
     */
    private Integer status;

    // Lombok @Data provides toString()

}
