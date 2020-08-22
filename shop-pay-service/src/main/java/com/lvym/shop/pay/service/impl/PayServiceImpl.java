package com.lvym.shop.pay.service.impl;

import com.alibaba.fastjson.JSON;
import com.lvym.api.pay.IPayService;
import com.lvym.common.constant.ShopCode;
import com.lvym.common.exception.CastException;
import com.lvym.common.utils.IDWorker;
import com.lvym.shop.entity.Result;
import com.lvym.shop.pay.mapper.TradeMqProducerTempMapper;
import com.lvym.shop.pay.mapper.TradePayMapper;
import com.lvym.shop.pojo.TradeMqProducerTemp;
import com.lvym.shop.pojo.TradePay;
import com.lvym.shop.pojo.TradePayExample;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.Service;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Date;


@Service
public class PayServiceImpl implements IPayService {


    @Autowired
    private TradePayMapper tradePayMapper;
    @Autowired
    private TradeMqProducerTempMapper tradeMqProducerTempMapper;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Value("${mq.topic}")
    private String topic;
    @Value("${mq.pay.tag}")
    private String tag;
    @Value("${rocketmq.producer.group}")
    private String groupName;
    @Autowired
    private IDWorker idWorker;
    @Autowired
    private ThreadPoolTaskExecutor executorService;
    @Override
    public Result createPayment(TradePay tradePay) {
        if (tradePay==null || tradePay.getOrderId()==null){
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        //查询订单状态
        TradePayExample example=new TradePayExample();
        TradePayExample.Criteria criteria = example.createCriteria();
        criteria.andOrderIdEqualTo(tradePay.getOrderId());
        criteria.andIsPaidEqualTo(ShopCode.SHOP_ORDER_PAY_STATUS_IS_PAY.getCode());
        int count = tradePayMapper.countByExample(example);
        if (count>0){
            //已支付
            CastException.cast(ShopCode.SHOP_PAYMENT_IS_PAID);
        }
        tradePay.setIsPaid(ShopCode.SHOP_ORDER_PAY_STATUS_NO_PAY.getCode());
        tradePay.setOrderId(tradePay.getOrderId());
        tradePay.setPayId(idWorker.nextId());
     //  tradePay.setPayAmount(new BigDecimal(0));
        tradePayMapper.insert(tradePay);
        return new Result(ShopCode.SHOP_SUCCESS.getSuccess(),ShopCode.SHOP_SUCCESS.getMessage());
    }

    @Override
    public Result callBackPayment(TradePay tradePay) {

        if (tradePay==null){
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        //判断订单状态
        if (tradePay.getIsPaid().equals(ShopCode.SHOP_ORDER_PAY_STATUS_IS_PAY.getCode())){
            //已付款
            tradePay = tradePayMapper.selectByPrimaryKey(tradePay.getPayId());
            if (tradePay == null) {
                CastException.cast(ShopCode.SHOP_PAYMENT_NOT_FOUND);
            }
            tradePay.setIsPaid(ShopCode.SHOP_PAYMENT_IS_PAID.getCode());
             //更新状态为已支付
            int update = tradePayMapper.updateByPrimaryKeySelective(tradePay);
            if (update==1){
                //更新成功
                TradeMqProducerTemp mqProducerTemp = new TradeMqProducerTemp();
                mqProducerTemp.setId(String.valueOf(idWorker.nextId()));
                mqProducerTemp.setGroupName(groupName);
                mqProducerTemp.setMsgKey(String.valueOf(tradePay.getPayId()));
                mqProducerTemp.setMsgTag(tag);
                mqProducerTemp.setMsgTopic(topic);
                mqProducerTemp.setMsgBody(JSON.toJSONString(tradePay));
                mqProducerTemp.setCreateTime(new Date());
                //持久化DB
                tradeMqProducerTempMapper.insert(mqProducerTemp);
                //发送消息
                TradePay finalTradePay = tradePay;
                //线程池优化
               executorService.submit(()->{
                   try {
                       SendResult sendResult = sendMessage(topic, tag, String.valueOf(finalTradePay.getPayId()), JSON.toJSONString(finalTradePay));
                       if (sendResult.getSendStatus().equals(SendStatus.SEND_OK)){
                           //发送成功 删除数据库
                           tradeMqProducerTempMapper.deleteByPrimaryKey(mqProducerTemp.getId());
                       }
                   } catch (Exception e) {
                       e.printStackTrace();
                       CastException.cast(ShopCode.SHOP_PAYMENT_IS_PAID);
                   }
               });
            }
        }
        return new Result(ShopCode.SHOP_SUCCESS.getSuccess(), ShopCode.SHOP_SUCCESS.getMessage());
    }

    private SendResult sendMessage(String topic, String tag, String payId, String body) throws InterruptedException, RemotingException, MQClientException, MQBrokerException {
        if (StringUtils.isBlank(topic) || StringUtils.isBlank(tag) || StringUtils.isBlank(payId) || StringUtils.isBlank(body)){
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        Message message = new Message(topic, tag, payId, body.getBytes());
        SendResult sendResult = rocketMQTemplate.getProducer().send(message);
        return sendResult;
    }
}
