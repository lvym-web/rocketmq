package com.lvym.shop.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.lvym.api.coupon.ICouponService;
import com.lvym.api.goods.IGoodsService;
import com.lvym.api.order.IOrderService;
import com.lvym.api.user.IUserService;
import com.lvym.common.constant.ShopCode;
import com.lvym.common.exception.CastException;
import com.lvym.common.utils.IDWorker;
import com.lvym.shop.entity.MQEntity;
import com.lvym.shop.entity.Result;
import com.lvym.shop.order.mapper.TradeOrderMapper;
import com.lvym.shop.pojo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.annotation.Service;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;


import java.math.BigDecimal;
import java.util.Date;

@Service
@Slf4j
public class OrderServiceImpl implements IOrderService {

    @Reference
    private IGoodsService iGoodsService;

    @Reference
    private IUserService iUserService;

    @Reference
    private ICouponService iCouponService;
//
    @Autowired
    private TradeOrderMapper tradeOrderMapper;

    @Autowired
    private IDWorker idWorker;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Value("${mq.order.topic}")
    private String topic;

    @Value("${mq.order.tag.cancel}")
    private String cancelTag;


    @Override
    public Result confirmOrder(TradeOrder tradeOrder) {
        //1.校验订单
        checkOrder(tradeOrder);
        //2.生成预订单
        Long orderId = savePreOrder(tradeOrder);
        try {
            //3.扣减库存
            reduceGoodsNum(tradeOrder);
            //4.扣减优惠券
            changeCoponStatus(tradeOrder);
            //5.使用余额
            reduceMoneyPaid(tradeOrder);

           // CastException.cast(ShopCode.SHOP_FAIL);
            //6.确认订单
           updateOrderStatus(tradeOrder);
            //7.返回成功状态
          return new Result(ShopCode.SHOP_SUCCESS.getSuccess(),ShopCode.SHOP_SUCCESS.getMessage());
        } catch (Exception e) {
            //1.确认订单失败,发送消息
            MQEntity cancelOrderMQ = new MQEntity();
            cancelOrderMQ.setOrderId(tradeOrder.getOrderId());
            cancelOrderMQ.setCouponId(tradeOrder.getCouponId());
            cancelOrderMQ.setGoodsId(tradeOrder.getGoodsId());
            cancelOrderMQ.setUserId(tradeOrder.getUserId());
            cancelOrderMQ.setUserMoney(tradeOrder.getMoneyPaid());
            cancelOrderMQ.setGoodsNum(tradeOrder.getGoodsNumber());
            try {
                sendMessage(topic,
                        cancelTag,
                        cancelOrderMQ.getOrderId().toString(),
                        JSON.toJSONString(cancelOrderMQ));
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            //2.返回失败状态
            return new Result(ShopCode.SHOP_FAIL.getSuccess(),ShopCode.SHOP_FAIL.getMessage());
        }
    }

    /**
     *           发送消息
     * @param topic
     * @param cancelTag
     * @param keys
     * @param mq
     */
    private void sendMessage(String topic, String cancelTag, String keys, String mq) throws Exception {
        if (StringUtils.isBlank(topic) || StringUtils.isBlank(cancelTag) || StringUtils.isBlank(keys) || StringUtils.isBlank(mq)){
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        Message msg = new Message(topic,cancelTag,keys,mq.getBytes());
        rocketMQTemplate.getProducer().send(msg);

    }

    private void updateOrderStatus(TradeOrder tradeOrder) {
        tradeOrder.setOrderStatus(ShopCode.SHOP_ORDER_CONFIRM.getCode());
        tradeOrder.setPayStatus(ShopCode.SHOP_ORDER_PAY_STATUS_NO_PAY.getCode());
        tradeOrder.setConfirmTime(new Date());
        int update = tradeOrderMapper.updateByPrimaryKey(tradeOrder);
        if (update<=0){
            CastException.cast(ShopCode.SHOP_ORDER_CONFIRM_FAIL);
        }
        log.info("订单："+tradeOrder.getOrderId()+"确认订单成功");
    }

    /**
     *   扣减余额
     * @param tradeOrder
     */
    private void reduceMoneyPaid(TradeOrder tradeOrder) {
        //判断订单中使用的余额是否合法
        if (tradeOrder.getMoneyPaid()!=null && tradeOrder.getMoneyPaid().compareTo(BigDecimal.ZERO)==1){
            TradeUserMoneyLog tradeUserMoneyLog = new TradeUserMoneyLog();
            tradeUserMoneyLog.setUseMoney(tradeOrder.getMoneyPaid());
            tradeUserMoneyLog.setOrderId(tradeOrder.getOrderId());
            tradeUserMoneyLog.setUserId(tradeOrder.getUserId());
            tradeUserMoneyLog.setMoneyLogType(ShopCode.SHOP_USER_MONEY_PAID.getCode());
          Result result=iUserService.updateMoneyPaid(tradeUserMoneyLog);
        }
    }

    /**
     * 扣减优惠券
     * @param tradeOrder
     */
    private void changeCoponStatus(TradeOrder tradeOrder) {
        if (tradeOrder.getCouponId()!=null){
            TradeCoupon tradeCoupon = iCouponService.findOne(tradeOrder.getCouponId());
            if (tradeCoupon!=null){
                tradeCoupon.setIsUsed(ShopCode.SHOP_COUPON_ISUSED.getCode());
                tradeCoupon.setUsedTime(new Date());
                tradeCoupon.setOrderId(tradeOrder.getOrderId());
                //更新优惠券
              Result result=iCouponService.updateCouponStatus(tradeCoupon);
              if(result.getSuccess().equals(ShopCode.SHOP_FAIL.getSuccess())){
                  CastException.cast(ShopCode.SHOP_COUPON_USE_FAIL);
              }
              log.info("更新成功");
            }
        }

    }

    /**
     *  减库存
     * @param order
     */
    private void reduceGoodsNum(TradeOrder order) {
        TradeGoodsNumberLog goodsNumberLog = new TradeGoodsNumberLog();
        goodsNumberLog.setGoodsId(order.getGoodsId());
        goodsNumberLog.setOrderId(order.getOrderId());
        goodsNumberLog.setGoodsNumber(order.getGoodsNumber());
        Result result = iGoodsService.reduceGoodsNum(goodsNumberLog);
        if (result.getSuccess().equals(ShopCode.SHOP_FAIL.getSuccess())) {
            CastException.cast(ShopCode.SHOP_REDUCE_GOODS_NUM_FAIL);
        }
        log.info("订单:["+order.getOrderId()+"]扣减库存["+order.getGoodsNumber()+"个]成功");
    }

    /**
     * 生成预订单
     *
     * @param order
     * @return
     */
    private Long savePreOrder(TradeOrder order) {
        //1.设置订单状态为不可见
        order.setOrderStatus(ShopCode.SHOP_ORDER_NO_CONFIRM.getCode());
        //2.订单ID
        order.setOrderId(idWorker.nextId());
        //3.核算运费是否正确
        BigDecimal shippingfree = checkShippingFree(order.getOrderAmount());
        if (order.getShippingFee().compareTo(shippingfree) != 0) {
            CastException.cast(ShopCode.SHOP_ORDER_SHIPPINGFEE_INVALID);
        }
        //4.计算订单总价格是否正确
        BigDecimal goodsAmount = order.getGoodsPrice().multiply(new BigDecimal(order.getGoodsNumber()));
        goodsAmount.add(shippingfree);
        if (order.getOrderAmount().compareTo(goodsAmount) != 0) {
            CastException.cast(ShopCode.SHOP_ORDERAMOUNT_INVALID);
        }

        //5.判断优惠券信息是否合法
        Long couponId = order.getCouponId();
        if ((couponId != null)) {
            TradeCoupon tradeCoupon=iCouponService.findOne(couponId);
            //优惠券不存在
            if (tradeCoupon==null){
                CastException.cast(ShopCode.SHOP_COUPON_NO_EXIST);
            }
            //优惠券已经使用
            if (tradeCoupon.getIsUsed().toString().equals(ShopCode.SHOP_COUPON_ISUSED.getCode().toString())){
                CastException.cast(ShopCode.SHOP_COUPON_INVALIED);
            }
            //存在，没有被使用， 设置优惠券金额
            order.setCouponPaid(tradeCoupon.getCouponPrice());
        }else {
            order.setCouponPaid(BigDecimal.ZERO);
        }
        //6.判断余额是否正确
        BigDecimal moneyPaid = order.getMoneyPaid();
        if (moneyPaid != null) {
            //比较余额是否大于0
            int balance = moneyPaid.compareTo(BigDecimal.ZERO);
            //余额小于0
            if (balance == -1) {
                CastException.cast(ShopCode.SHOP_MONEY_PAID_LESS_ZERO);
            }
            //余额大于0
            if (balance == 1) {
                //查询用户信息
                TradeUser user = iUserService.findOne(order.getUserId());
                //比较余额是否大于用户账户余额
                if (moneyPaid.compareTo(user.getUserMoney()) == 1) {
                    CastException.cast(ShopCode.SHOP_MONEY_PAID_INVALID);
                }
                order.setMoneyPaid(order.getMoneyPaid());
            }
        } else {
            order.setMoneyPaid(BigDecimal.ZERO);
        }
        //7.计算订单支付总价
        BigDecimal payAmount = order.getOrderAmount().subtract(order.getCouponPaid()).subtract(order.getMoneyPaid());
       order.setPayAmount(payAmount);
        //8.设置订单添加时间
          order.setAddTime(new Date());
        //9.保存预订单
        int insert = tradeOrderMapper.insert(order);
        if (ShopCode.SHOP_SUCCESS.getCode() != insert) {
            CastException.cast(ShopCode.SHOP_ORDER_SAVE_ERROR);
        }
        log.info("订单:["+order.getOrderId()+"]预订单生成成功");
        return order.getOrderId();
    }

    private BigDecimal checkShippingFree(BigDecimal orderAmount) {
        //总价大于99，  免运费
        if (orderAmount.compareTo(new BigDecimal(99)) == 1) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(10);
    }

    /**
     * 校验订单
     *
     * @param
     */
    private void checkOrder(TradeOrder order) {
        //1.校验订单是否存在
        if (order == null) {
            CastException.cast(ShopCode.SHOP_ORDER_INVALID);
        }
        //2.校验订单中的商品是否存在
        TradeGoods goods = iGoodsService.findOne(order.getGoodsId());
        if (goods == null) {
            CastException.cast(ShopCode.SHOP_GOODS_NO_EXIST);
        }
        //3.校验下单用户是否存在
        TradeUser user = iUserService.findOne(order.getUserId());
        if (user == null) {
            CastException.cast(ShopCode.SHOP_USER_NO_EXIST);
        }
        //4.校验商品单价是否合法
        if (order.getGoodsPrice().compareTo(goods.getGoodsPrice()) != 0) {
            CastException.cast(ShopCode.SHOP_GOODS_PRICE_INVALID);
        }
        //5.校验订单商品数量是否合法
        if (order.getGoodsNumber() >= goods.getGoodsNumber()) {
            CastException.cast(ShopCode.SHOP_GOODS_NUM_NOT_ENOUGH);
        }

        log.info("校验订单通过");

    }
}
