package cn.gemrun.base.module.trade.controller.app.order.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.AssertTrue;

@Schema(description = "用户 App - 交易订单创建 Request VO")
@Data
public class AppTradeOrderCreateReqVO extends AppTradeOrderSettlementReqVO {

    @Schema(description = "备注", example = "这个是我的订单哟")
    private String remark;

    @Schema(description = "收件人地区编号（快递配送时必填）", example = "7310")
    private Integer receiverAreaId;

    @Schema(description = "收件人详细地址（快递配送时必填）", example = "昆明市五华区xxx小区xxx")
    private String receiverDetailAddress;

    @AssertTrue(message = "配送方式不能为空")
    @JsonIgnore
    public boolean isDeliveryTypeNotNull() {
        return getDeliveryType() != null;
    }

}
