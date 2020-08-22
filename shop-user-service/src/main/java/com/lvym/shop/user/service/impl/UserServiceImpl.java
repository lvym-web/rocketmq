package com.lvym.shop.user.service.impl;

import com.lvym.api.user.IUserService;
import com.lvym.common.constant.ShopCode;
import com.lvym.common.exception.CastException;
import com.lvym.shop.entity.Result;
import com.lvym.shop.pojo.TradeUser;
import com.lvym.shop.pojo.TradeUserMoneyLog;
import com.lvym.shop.pojo.TradeUserMoneyLogExample;
import com.lvym.shop.user.mapper.TradeUserMapper;
import com.lvym.shop.user.mapper.TradeUserMoneyLogMapper;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Date;

@Service
public class UserServiceImpl implements IUserService {

    @Autowired
    private TradeUserMapper tradeUserMapper;
    @Autowired
    private TradeUserMoneyLogMapper tradeUserMoneyLogMapper;
    @Override
    public TradeUser findOne(Long userId) {
        if (userId==null){
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        return tradeUserMapper.selectByPrimaryKey(userId);
    }

    @Override
    public Result updateMoneyPaid(TradeUserMoneyLog tradeUserMoneyLog) {
        if (tradeUserMoneyLog==null ||tradeUserMoneyLog.getOrderId()==null || tradeUserMoneyLog.getUseMoney()==null || tradeUserMoneyLog.getUseMoney().compareTo(BigDecimal.ZERO)<=0 ||tradeUserMoneyLog.getUserId()==null){
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        //查询该订单是否存在付款记录
        TradeUserMoneyLogExample example = new TradeUserMoneyLogExample();
        TradeUserMoneyLogExample.Criteria criteria = example.createCriteria();
        criteria.andOrderIdEqualTo(tradeUserMoneyLog.getOrderId());
        criteria.andUserIdEqualTo(tradeUserMoneyLog.getUserId());
        int count = tradeUserMoneyLogMapper.countByExample(example);
        //判断余额操作行为
        TradeUser tradeUser = tradeUserMapper.selectByPrimaryKey(tradeUserMoneyLog.getUserId());
        //【付款操作】
        if (tradeUserMoneyLog.getMoneyLogType().intValue()==ShopCode.SHOP_USER_MONEY_PAID.getCode()){
            if (count>0){
                //已付款
                CastException.cast(ShopCode.SHOP_ORDER_PAY_STATUS_IS_PAY);
            }
            //用户账户扣减余额

            tradeUser.setUserMoney(tradeUser.getUserMoney().subtract(tradeUserMoneyLog.getUseMoney()));
            //更新余额
        }
        //回退
        if (tradeUserMoneyLog.getMoneyLogType().intValue()==ShopCode.SHOP_USER_MONEY_REFUND.getCode()){
            criteria.andMoneyLogTypeEqualTo(tradeUserMoneyLog.getMoneyLogType());
            int count1 = tradeUserMoneyLogMapper.countByExample(example);
            if (count1>0){
                CastException.cast(ShopCode.SHOP_USER_MONEY_REFUND_ALREADY);
            }
            //回退
            tradeUser.setUserMoney(tradeUser.getUserMoney().add(tradeUserMoneyLog.getUseMoney()));
        }
        tradeUserMapper.updateByPrimaryKey(tradeUser);
        //记录日志
        tradeUserMoneyLog.setCreateTime(new Date());
        tradeUserMoneyLogMapper.insert(tradeUserMoneyLog);

        return new Result(ShopCode.SHOP_SUCCESS.getSuccess(),ShopCode.SHOP_SUCCESS.getMessage());
    }
}
