package cn.gemrun.base.module.trade.dto;

import lombok.Data;

/**
 * 会员收货地址 Stub DTO（会员模块已移除，仅用于保留订单结算等功能的编译兼容）
 */
@Data
public class MemberAddressRespDTO {

    private Long id;

    private Long userId;

    private String name;

    private String mobile;

    private Long areaId;

    private String detailAddress;
}
