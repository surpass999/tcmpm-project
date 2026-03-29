package cn.gemrun.base.module.trade.service.price.calculator;

import cn.gemrun.base.module.trade.service.price.bo.TradePriceCalculateReqBO;
import cn.gemrun.base.module.trade.service.price.bo.TradePriceCalculateRespBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

import static cn.gemrun.base.framework.common.util.collection.CollectionUtils.filterList;

/**
 * 赠送积分的 {@link TradePriceCalculator} 实现类
 *
 * @author owen
 */
@Component
@Order(TradePriceCalculator.ORDER_POINT_GIVE)
@Slf4j
public class TradePointGiveCalculator implements TradePriceCalculator {

    @Override
    public void calculate(TradePriceCalculateReqBO param, TradePriceCalculateRespBO result) {
        // 会员模块已移除，积分赠送功能不可用
        // 1.1 校验支付金额
        if (result.getPrice().getPayPrice() <= 0) {
            return;
        }

        // 1.2 计算分摊的赠送积分（设置为 0）
        List<TradePriceCalculateRespBO.OrderItem> orderItems = filterList(result.getItems(), TradePriceCalculateRespBO.OrderItem::getSelected);
        // 3.3 更新订单赠送积分
        TradePriceCalculatorHelper.recountAllGivePoint(result);
    }

}
