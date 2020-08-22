package com.lvym.api.goods;

import com.lvym.shop.entity.Result;
import com.lvym.shop.pojo.TradeGoods;
import com.lvym.shop.pojo.TradeGoodsNumberLog;

public interface IGoodsService {
    //校验订单中的商品是否存在
    TradeGoods findOne(Long goodsId);
     //减库存
    Result reduceGoodsNum(TradeGoodsNumberLog goodsNumberLog);
}
