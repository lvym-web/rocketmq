package com.lvym.api.user;

import com.lvym.shop.entity.Result;
import com.lvym.shop.pojo.TradeUser;
import com.lvym.shop.pojo.TradeUserMoneyLog;

public interface IUserService {
    //校验下单用户是否存在
    TradeUser findOne(Long userId);
    //更新余额
    Result updateMoneyPaid(TradeUserMoneyLog tradeUserMoneyLog);
}
