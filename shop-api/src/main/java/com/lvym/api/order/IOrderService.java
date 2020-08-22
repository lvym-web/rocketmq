package com.lvym.api.order;

import com.lvym.shop.entity.Result;
import com.lvym.shop.pojo.TradeOrder;

public interface IOrderService {
    /**
     *   提交订单
     * @param tradeOrder
     * @return
     */
    Result confirmOrder(TradeOrder tradeOrder);

}
