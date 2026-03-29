package cn.gemrun.base.module.trade.service.order.handler;

import cn.hutool.core.collection.CollUtil;
import cn.gemrun.base.module.trade.dal.dataobject.order.TradeOrderDO;
import cn.gemrun.base.module.trade.dal.dataobject.order.TradeOrderItemDO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 会员积分、等级的 {@link TradeOrderHandler} 实现类
 *
 * @author owen
 */
@Component
public class TradeMemberPointOrderHandler implements TradeOrderHandler {

    @Override
    public void afterOrderCreate(TradeOrderDO order, List<TradeOrderItemDO> orderItems) {
        // 会员模块已移除，积分扣减功能不可用
    }

    @Override
    public void afterPayOrder(TradeOrderDO order, List<TradeOrderItemDO> orderItems) {
        // 会员模块已移除，积分和经验增加功能不可用
    }

    @Override
    public void afterCancelOrder(TradeOrderDO order, List<TradeOrderItemDO> orderItems) {
        // 售后的订单项，已经在 afterCancelOrderItem 回滚库存，所以这里不需要重复回滚
        orderItems = filterOrderItemListByNoneAfterSale(orderItems);
        if (CollUtil.isEmpty(orderItems)) {
            return;
        }

        // 会员模块已移除，积分回滚功能不可用

        // 如下的返还，需要经过支持，也就是经历 afterPayOrder 流程
        if (!order.getPayStatus()) {
            return;
        }
        // 会员模块已移除，积分和经验回滚功能不可用
    }

    @Override
    public void afterCancelOrderItem(TradeOrderDO order, TradeOrderItemDO orderItem) {
        // 会员模块已移除，积分和经验回滚功能不可用
    }

}
