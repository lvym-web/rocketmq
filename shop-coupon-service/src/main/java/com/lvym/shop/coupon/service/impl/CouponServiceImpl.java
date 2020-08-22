package com.lvym.shop.coupon.service.impl;

import com.lvym.api.coupon.ICouponService;
import com.lvym.common.constant.ShopCode;
import com.lvym.common.exception.CastException;
import com.lvym.shop.coupon.mapper.TradeCouponMapper;
import com.lvym.shop.entity.Result;
import com.lvym.shop.pojo.TradeCoupon;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class CouponServiceImpl implements ICouponService {

    @Autowired
    private TradeCouponMapper tradeCouponMapper;
    @Override
    public TradeCoupon findOne(Long couponId) {
        if (couponId==null){
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        return tradeCouponMapper.selectByPrimaryKey(couponId);
    }

    @Override
    public Result updateCouponStatus(TradeCoupon tradeCoupon) {
        if (tradeCoupon==null || tradeCoupon.getCouponId()==null){
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        //更新
        int update = tradeCouponMapper.updateByPrimaryKey(tradeCoupon);
        if (update<=0){
            CastException.cast(ShopCode.SHOP_COUPON_INVALIED);
        }
        return new Result(ShopCode.SHOP_SUCCESS.getSuccess(),ShopCode.SHOP_SUCCESS.getMessage());
    }
}
