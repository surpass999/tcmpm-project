package cn.gemrun.base.module.declare.controller.admin.training.vo;

import cn.gemrun.base.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 活动报名分页请求 VO
 *
 * @author Gemini
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RegistrationPageReqVO extends PageParam {

    /**
     * 活动ID
     */
    private Long trainingId;

    /**
     * 关键词搜索（姓名/单位）
     */
    private String keyword;

    /**
     * 状态：1=已报名，2=已签到，3=已取消，4=未出席
     */
    private Integer status;

    /**
     * 报名时间起
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime registerTimeStart;

    /**
     * 报名时间止
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime registerTimeEnd;

}
