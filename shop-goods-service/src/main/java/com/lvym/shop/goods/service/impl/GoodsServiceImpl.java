package com.lvym.shop.goods.service.impl;

import com.lvym.api.goods.IGoodsService;
import com.lvym.common.constant.ShopCode;
import com.lvym.common.exception.CastException;
import com.lvym.shop.entity.Result;
import com.lvym.shop.goods.mapper.TradeGoodsMapper;
import com.lvym.shop.goods.mapper.TradeGoodsNumberLogMapper;
import com.lvym.shop.pojo.TradeGoods;
import com.lvym.shop.pojo.TradeGoodsNumberLog;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

@Service
public class GoodsServiceImpl implements IGoodsService {

    @Autowired
    private TradeGoodsMapper tradeGoodsMapper;

    @Autowired
    private TradeGoodsNumberLogMapper tradeGoodsNumberLogMapper;

    @Override
    public TradeGoods findOne(Long goodsId) {
        if (goodsId==null){
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        return tradeGoodsMapper.selectByPrimaryKey(goodsId);

    }

    @Override
    public Result reduceGoodsNum(TradeGoodsNumberLog goodsNumberLog) {
        if (goodsNumberLog==null || goodsNumberLog.getGoodsNumber()==null || goodsNumberLog.getGoodsNumber().intValue()<=0 ||goodsNumberLog.getGoodsId()==null ||goodsNumberLog.getOrderId()==null){
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        TradeGoods goods = tradeGoodsMapper.selectByPrimaryKey(goodsNumberLog.getGoodsId());
        //验库存
        if (goods.getGoodsNumber()<goodsNumberLog.getGoodsNumber()){
            CastException.cast(ShopCode.SHOP_GOODS_NUM_NOT_ENOUGH);
        }
       goods.setGoodsNumber(goods.getGoodsNumber()-goodsNumberLog.getGoodsNumber());
        tradeGoodsMapper.updateByPrimaryKey(goods);
        //记录库存日志
        goodsNumberLog.setGoodsNumber(-(goodsNumberLog.getGoodsNumber()));
        goodsNumberLog.setLogTime(new Date());
        tradeGoodsNumberLogMapper.insert(goodsNumberLog);

        return new Result(ShopCode.SHOP_SUCCESS.getSuccess(),ShopCode.SHOP_SUCCESS.getMessage());
    }
}
