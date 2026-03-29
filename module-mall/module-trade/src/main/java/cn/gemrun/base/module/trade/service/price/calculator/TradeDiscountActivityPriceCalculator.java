package cn.gemrun.base.module.trade.service.price.calculator;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.gemrun.base.module.promotion.api.discount.DiscountActivityApi;
import cn.gemrun.base.module.promotion.api.discount.dto.DiscountProductRespDTO;
import cn.gemrun.base.module.promotion.enums.common.PromotionDiscountTypeEnum;
import cn.gemrun.base.module.promotion.enums.common.PromotionTypeEnum;
import cn.gemrun.base.module.trade.enums.order.TradeOrderTypeEnum;
import cn.gemrun.base.module.trade.service.price.bo.TradePriceCalculateReqBO;
import cn.gemrun.base.module.trade.service.price.bo.TradePriceCalculateRespBO;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

import static cn.gemrun.base.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.gemrun.base.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.gemrun.base.framework.common.util.number.MoneyUtils.calculateRatePrice;
import static cn.gemrun.base.module.trade.service.price.calculator.TradePriceCalculatorHelper.formatPrice;

/**
 * 限时折扣的 {@link TradePriceCalculator} 实现类
 *
 * 由于“会员折扣”和“限时折扣”是冲突，需要选择优惠金额多的，所以也放在这里计算
 *
 * @author 芋道源码
 */
@Component
@Order(TradePriceCalculator.ORDER_DISCOUNT_ACTIVITY)
public class TradeDiscountActivityPriceCalculator implements TradePriceCalculator {

    @Resource
    private DiscountActivityApi discountActivityApi;

    @Override
    public void calculate(TradePriceCalculateReqBO param, TradePriceCalculateRespBO result) {
        // 0. 只有【普通】订单，才计算该优惠
        if (ObjectUtil.notEqual(result.getType(), TradeOrderTypeEnum.NORMAL.getType())) {
            return;
        }

        // 1.1 获得 SKU 对应的限时折扣活动
        List<DiscountProductRespDTO> discountProducts = discountActivityApi.getMatchDiscountProductListBySkuIds(
                convertSet(result.getItems(), TradePriceCalculateRespBO.OrderItem::getSkuId));
        Map<Long, DiscountProductRespDTO> discountProductMap = convertMap(discountProducts, DiscountProductRespDTO::getSkuId);

        // 2. 计算每个 SKU 的优惠金额（会员模块已移除，只计算限时折扣优惠）
        result.getItems().forEach(orderItem -> {
            if (!orderItem.getSelected()) {
                return;
            }
            // 计算限时折扣的优惠金额
            DiscountProductRespDTO discountProduct = discountProductMap.get(orderItem.getSkuId());
            Integer discountPrice = calculateActivityPrice(discountProduct, orderItem);
            if (discountPrice <= 0) {
                return;
            }

            // 记录优惠明细
            TradePriceCalculatorHelper.addPromotion(result, orderItem,
                    discountProduct.getActivityId(), discountProduct.getActivityName(), PromotionTypeEnum.DISCOUNT_ACTIVITY.getType(),
                    StrUtil.format("限时折扣：省 {} 元", formatPrice(discountPrice)),
                    discountPrice);
            // 更新 SKU 优惠金额
            orderItem.setDiscountPrice(orderItem.getDiscountPrice() + discountPrice);

            // 分摊优惠
            TradePriceCalculatorHelper.recountPayPrice(orderItem);
            TradePriceCalculatorHelper.recountAllPrice(result);
        });
    }

    /**
     * 获得用户的等级
     *
     * @param userId 用户编号
     * @return 用户等级（会员模块已移除，始终返回 null）
     */
    public Object getMemberLevel(Long userId) {
        // 会员模块已移除，无法获取会员等级
        return null;
    }

    /**
     * 计算优惠活动的价格
     *
     * @param discount 优惠活动
     * @param orderItem 交易项
     * @return 优惠价格
     */
    public Integer calculateActivityPrice(DiscountProductRespDTO discount,
                                           TradePriceCalculateRespBO.OrderItem orderItem) {
        if (discount == null) {
            return 0;
        }
        Integer newPrice = orderItem.getPayPrice();
        if (PromotionDiscountTypeEnum.PRICE.getType().equals(discount.getDiscountType())) { // 减价
            newPrice -= discount.getDiscountPrice() * orderItem.getCount();
        } else if (PromotionDiscountTypeEnum.PERCENT.getType().equals(discount.getDiscountType())) { // 打折
            newPrice = calculateRatePrice(orderItem.getPayPrice(), discount.getDiscountPercent() / 100.0);
        } else {
            throw new IllegalArgumentException(String.format("优惠活动的商品(%s) 的优惠类型不正确", discount));
        }
        return orderItem.getPayPrice() - newPrice;
    }

    /**
     * 计算会员 VIP 的优惠价格（会员模块已移除，始终返回 0）
     *
     * @param level 会员等级
     * @param orderItem 交易项
     * @return 优惠价格
     */
    public Integer calculateVipPrice(Object level,
                                      TradePriceCalculateRespBO.OrderItem orderItem) {
        // 会员模块已移除，无法计算 VIP 优惠
        return 0;
    }

}
