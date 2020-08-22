package com.lvym.shop.order.listener;



import com.alibaba.fastjson.JSON;
import com.lvym.common.constant.ShopCode;
import com.lvym.common.exception.CastException;
import com.lvym.shop.order.mapper.TradeOrderMapper;
import com.lvym.shop.pojo.TradeOrder;
import com.lvym.shop.pojo.TradePay;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;

@Component
@RocketMQMessageListener(topic = "${mq.pay.topic}",consumerGroup = "${mq.pay.consumer.group.name}",messageModel = MessageModel.BROADCASTING)
public class PaymentListener implements RocketMQListener<MessageExt> {

    @Autowired
    private TradeOrderMapper tradeOrderMapper;
    @Override
    public void onMessage(MessageExt messageExt) {

        try {
            //解析消息内容
            String body = new String(messageExt.getBody(), "UTF-8");
            //反序列化
            TradePay tradePay = JSON.parseObject(body, TradePay.class);
            //查询订单
            TradeOrder tradeOrder = tradeOrderMapper.selectByPrimaryKey(tradePay.getOrderId());
            //更改状态
            tradeOrder.setPayStatus(ShopCode.SHOP_ORDER_PAY_STATUS_IS_PAY.getCode());
            //更新订单
            tradeOrderMapper.updateByPrimaryKey(tradeOrder);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            CastException.cast(ShopCode.SHOP_ORDER_STATUS_UPDATE_FAIL);
        }
    }
}
