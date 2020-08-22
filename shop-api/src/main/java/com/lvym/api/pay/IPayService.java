package com.lvym.api.pay;

import com.lvym.shop.entity.Result;
import com.lvym.shop.pojo.TradePay;

public interface IPayService {

    Result createPayment(TradePay tradePay);
    Result callBackPayment(TradePay tradePay);
}
