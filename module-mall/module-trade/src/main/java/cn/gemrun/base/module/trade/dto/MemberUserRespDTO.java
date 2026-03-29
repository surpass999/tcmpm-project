package cn.gemrun.base.module.trade.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员用户信息 Stub DTO（会员模块已移除，仅用于保留订单查询等功能的编译兼容）
 */
@Data
public class MemberUserRespDTO {

    private Long id;

    private String nickname;

    private String avatar;

    private String mobile;

    private String email;

    private Long levelId;

    private LocalDateTime createTime;
}
