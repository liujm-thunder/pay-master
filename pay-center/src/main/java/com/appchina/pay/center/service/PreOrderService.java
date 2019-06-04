package com.appchina.pay.center.service;

import com.alibaba.fastjson.JSONObject;
import com.appchina.pay.dao.model.GoodsOrder;

import java.util.Map;

public interface PreOrderService {

    String createPreOrderInfo(GoodsOrder goodsOrder,JSONObject params);

    GoodsOrder getPayOrder(String goodsOrderId);

    int updatePayOrder(GoodsOrder goodsOrder);

}
