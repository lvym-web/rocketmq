package com.lvym.api.coupon;

import com.lvym.shop.entity.Result;
import com.lvym.shop.pojo.TradeCoupon;

public interface ICouponService {
       //查询优惠券
    TradeCoupon findOne(Long couponId);
      //更新优惠券
    Result updateCouponStatus(TradeCoupon tradeCoupon);
}
