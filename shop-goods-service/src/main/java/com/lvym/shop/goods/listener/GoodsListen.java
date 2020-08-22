package com.lvym.shop.goods.listener;

import com.alibaba.fastjson.JSON;
import com.lvym.common.constant.ShopCode;
import com.lvym.shop.entity.MQEntity;
import com.lvym.shop.goods.mapper.TradeGoodsMapper;
import com.lvym.shop.goods.mapper.TradeGoodsNumberLogMapper;
import com.lvym.shop.goods.mapper.TradeMqConsumerLogMapper;
import com.lvym.shop.pojo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.Date;

@Slf4j
@Component
@RocketMQMessageListener(topic = "${mq.order.topic}",
        consumerGroup = "${mq.order.consumer.group.name}",
        messageModel = MessageModel.BROADCASTING)
public class GoodsListen  implements RocketMQListener<MessageExt> {

    @Autowired
    private TradeMqConsumerLogMapper tradeMqConsumerLogMapper;
    @Autowired
    private TradeGoodsMapper goodsMapper;

    @Autowired
    private TradeGoodsNumberLogMapper goodsNumberLogMapper;

    @Value("${mq.order.consumer.group.name}")
    private String groupName;
    @Override
    public void onMessage(MessageExt messageExt) {
        String msgId=null;
        String tags=null;
        String keys=null;
        String body=null;
        try {
            //解析数据
             msgId = messageExt.getMsgId();
             tags = messageExt.getTags();
             keys = messageExt.getKeys();
             body = new String(messageExt.getBody(), "UTF-8");
              //查询消费记录
            TradeMqConsumerLogKey consumerLogKey = new TradeMqConsumerLogKey();
            consumerLogKey.setGroupName(groupName);
            consumerLogKey.setMsgKey(keys);
            consumerLogKey.setMsgTag(tags);
            TradeMqConsumerLog tradeMqConsumerLog = tradeMqConsumerLogMapper.selectByPrimaryKey(consumerLogKey);
           if (tradeMqConsumerLog!=null){
               //消费过
               Integer status = tradeMqConsumerLog.getConsumerStatus();
               //处理过
               if (ShopCode.SHOP_MQ_MESSAGE_STATUS_SUCCESS.getCode().intValue()==status.intValue()){
                   log.info("处理过>>>>>>>");
                   return;
               }
               //正在处理
               if (ShopCode.SHOP_MQ_MESSAGE_STATUS_PROCESSING.getCode().intValue()==status.intValue()){
                   log.info("正在处理>>>>>>>");
                   return;
               }

               //处理失败
               if (ShopCode.SHOP_MQ_MESSAGE_STATUS_FAIL.getCode().intValue()==status.intValue()){
                   Integer consumerTimes = tradeMqConsumerLog.getConsumerTimes();
                   if (consumerTimes>3){
                       log.info("处理失败>>>>>>>");
                       return;
                   }
                   //处理失败，再次处理
                   tradeMqConsumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_PROCESSING.getCode());
                   //使用数据库乐观锁更新
                   TradeMqConsumerLogExample example=new TradeMqConsumerLogExample();
                   TradeMqConsumerLogExample.Criteria criteria = example.createCriteria();
                   criteria.andMsgTagEqualTo(tradeMqConsumerLog.getMsgId());
                   criteria.andMsgKeyEqualTo(tradeMqConsumerLog.getMsgKey());
                   criteria.andGroupNameEqualTo(groupName);
                   criteria.andConsumerTimesEqualTo(consumerTimes);
                   int update = tradeMqConsumerLogMapper.updateByExampleSelective(tradeMqConsumerLog, example);
                   if (update<=0){
                       log.info("并发修改，请稍后处理");
                   }
               }
           }else {
               //没有消费记录
               tradeMqConsumerLog = new TradeMqConsumerLog();
               tradeMqConsumerLog.setMsgTag(tags);
               tradeMqConsumerLog.setMsgKey(keys);
               tradeMqConsumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_PROCESSING.getCode());
               tradeMqConsumerLog.setMsgBody(body);
               tradeMqConsumerLog.setMsgId(msgId);
               tradeMqConsumerLog.setConsumerTimes(0);

               //将消息处理信息添加到数据库
               tradeMqConsumerLogMapper.insert(tradeMqConsumerLog);

           }
            //5. 回退库存
            MQEntity mqEntity = JSON.parseObject(body, MQEntity.class);
            Long goodsId = mqEntity.getGoodsId();
            TradeGoods goods = goodsMapper.selectByPrimaryKey(goodsId);
            goods.setGoodsNumber(goods.getGoodsNumber()+mqEntity.getGoodsNum());
            goodsMapper.updateByPrimaryKey(goods);

            //记录库存操作日志
            TradeGoodsNumberLog goodsNumberLog = new TradeGoodsNumberLog();
            goodsNumberLog.setOrderId(mqEntity.getOrderId());
            goodsNumberLog.setGoodsId(goodsId);
            goodsNumberLog.setGoodsNumber(mqEntity.getGoodsNum());
            goodsNumberLog.setLogTime(new Date());
            goodsNumberLogMapper.insert(goodsNumberLog);

            //6. 将消息的处理状态改为成功
            tradeMqConsumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_SUCCESS.getCode());
            tradeMqConsumerLog.setConsumerTimestamp(new Date());
            tradeMqConsumerLogMapper.updateByPrimaryKey(tradeMqConsumerLog);
            log.info("回退库存成功");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            TradeMqConsumerLogKey primaryKey = new TradeMqConsumerLogKey();
            primaryKey.setMsgTag(tags);
            primaryKey.setMsgKey(keys);
            primaryKey.setGroupName(groupName);
            TradeMqConsumerLog mqConsumerLog = tradeMqConsumerLogMapper.selectByPrimaryKey(primaryKey);
            if(mqConsumerLog==null){
                //数据库未有记录
                mqConsumerLog = new TradeMqConsumerLog();
                mqConsumerLog.setMsgTag(tags);
                mqConsumerLog.setMsgKey(keys);
                mqConsumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_FAIL.getCode());
                mqConsumerLog.setMsgBody(body);
                mqConsumerLog.setMsgId(msgId);
                mqConsumerLog.setConsumerTimes(1);
                tradeMqConsumerLogMapper.insert(mqConsumerLog);
            }else{
                mqConsumerLog.setConsumerTimes(mqConsumerLog.getConsumerTimes()+1);
                tradeMqConsumerLogMapper.updateByPrimaryKeySelective(mqConsumerLog);
            }
        }

    }
}
