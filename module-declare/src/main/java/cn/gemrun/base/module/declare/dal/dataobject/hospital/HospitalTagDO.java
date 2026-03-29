package cn.gemrun.base.module.declare.dal.dataobject.hospital;

import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 医院标签定义表
 */
@TableName("declare_hospital_tag")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalTagDO extends BaseDO {

    /**
     * 标签主键
     */
    @TableId
    private Long id;

    /**
     * 标签编码（唯一，如 REGION_EAST）
     */
    private String tagCode;

    /**
     * 标签名称（如 华东区）
     */
    private String tagName;

    /**
     * 标签分类：region=区域 level=等级 feature=特征 attribute=属性
     */
    private String tagCategory;

    /**
     * 标签类型：1=单选 2=多选
     */
    private Integer tagType;

    /**
     * 父标签ID（用于树形结构）
     */
    private Long parentId;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态：0=禁用 1=启用
     */
    private Integer status;

}
