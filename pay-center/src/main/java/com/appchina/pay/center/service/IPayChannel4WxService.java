package com.appchina.pay.center.service;

import com.appchina.pay.dao.model.PayOrder;

import java.util.Map;

public interface IPayChannel4WxService {

    Map doWxPayReq(String jsonParam);

    Map queryWxPayOrderRequest(PayOrder payOrder);
}
