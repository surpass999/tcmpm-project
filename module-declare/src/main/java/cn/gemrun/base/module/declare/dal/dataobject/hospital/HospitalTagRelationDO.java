package cn.gemrun.base.module.declare.dal.dataobject.hospital;

import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 医院标签关联表（多对多）
 * 用于给组织架构中的部门（医院）打标签
 *
 * 注意：该表只有 id, hospital_code, tag_id, creator, create_time 字段
 */
@TableName("declare_hospital_tag_relation")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalTagRelationDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;

    /**
     * 医院编码（对应 declare_hospital.hospital_code）
     */
    @TableField("hospital_code")
    private String hospitalCode;

    /**
     * 标签ID
     */
    private Long tagId;

    /**
     * BaseDO 中的字段，该表无此列
     */
    @TableField(exist = false)
    private String creator;

    @TableField(exist = false)
    private String updater;

    @TableField(exist = false)
    private Boolean deleted;

    /**
     * BaseDO 中的 updateTime，该表无此列
     */
    @TableField(exist = false)
    private LocalDateTime updateTime;

}
