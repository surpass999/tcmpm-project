package cn.gemrun.base.module.declare.dal.dataobject.indicator;

import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 指标分组 DO（支持两级树形结构）
 *
 * @author Gemini
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("declare_indicator_group")
public class DeclareIndicatorGroupDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 父分组ID（顶级为0）
     */
    private Long parentId;

    /**
     * 分组编码（唯一）
     */
    private String groupCode;

    /**
     * 分组名称
     */
    private String groupName;

    /**
     * 分组层级：1=一级 2=二级
     */
    private Integer groupLevel;

    /**
     * 关联项目类型（0-6），一级分组必须填写，用于区分分组属于哪个项目类型
     */
    private Integer projectType;

    /**
     * 分组前缀标识（如101、20101），用于描述分组特征
     */
    private String groupPrefix;

    /**
     * 分组描述
     */
    private String description;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态：0-禁用 1=启用
     */
    private Integer status;

}
