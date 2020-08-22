package com.lvym.shop.order.listener;

import com.alibaba.fastjson.JSON;
import com.lvym.common.constant.ShopCode;
import com.lvym.shop.entity.MQEntity;
import com.lvym.shop.order.mapper.TradeOrderMapper;
import com.lvym.shop.pojo.TradeOrder;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;


@Slf4j
@Component
@RocketMQMessageListener(topic = "${mq.order.topic}", consumerGroup = "${mq.order.consumer.group.name}", messageModel = MessageModel.BROADCASTING)
public class CancelMQListener implements RocketMQListener<MessageExt> {
    @Autowired
    private TradeOrderMapper tradeOrderMapper;

    @Override
    public void onMessage(MessageExt messageExt) {


        try {
           String body = new String(messageExt.getBody(), "UTF-8");

            log.info("CancelOrderProcessor receive message:" + messageExt);

            MQEntity cancelOrderMQ = JSON.parseObject(body, MQEntity.class);

            TradeOrder order = tradeOrderMapper.selectByPrimaryKey(cancelOrderMQ.getOrderId());

            order.setOrderStatus(ShopCode.SHOP_ORDER_CANCEL.getCode());
            tradeOrderMapper.updateByPrimaryKey(order);
            log.info("订单:[" + order.getOrderId() + "]状态设置为取消");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            log.info("订单取消失败");
        }


    }
}