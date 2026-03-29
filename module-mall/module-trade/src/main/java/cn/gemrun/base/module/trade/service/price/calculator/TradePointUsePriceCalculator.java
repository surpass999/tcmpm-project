package cn.gemrun.base.module.trade.service.price.calculator;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.gemrun.base.module.promotion.enums.common.PromotionTypeEnum;
import cn.gemrun.base.module.trade.enums.order.TradeOrderTypeEnum;
import cn.gemrun.base.module.trade.service.price.bo.TradePriceCalculateReqBO;
import cn.gemrun.base.module.trade.service.price.bo.TradePriceCalculateRespBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

import static cn.gemrun.base.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.gemrun.base.framework.common.util.collection.CollectionUtils.filterList;
import static cn.gemrun.base.module.trade.enums.ErrorCodeConstants.PRICE_CALCULATE_PAY_PRICE_ILLEGAL;

/**
 * 使用积分的 {@link TradePriceCalculator} 实现类
 *
 * @author owen
 */
@Component
@Order(TradePriceCalculator.ORDER_POINT_USE)
@Slf4j
public class TradePointUsePriceCalculator implements TradePriceCalculator {

    @Override
    public void calculate(TradePriceCalculateReqBO param, TradePriceCalculateRespBO result) {
        // 判断订单类型是否不为积分商城活动
        if (ObjectUtil.equal(result.getType(), TradeOrderTypeEnum.POINT.getType())) {
            return;
        }
        // 会员模块已移除，积分使用功能不可用
        result.setTotalPoint(0).setUsePoint(0);
        return;
    }

}
