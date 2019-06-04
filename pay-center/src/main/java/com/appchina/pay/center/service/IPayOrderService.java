package com.appchina.pay.center.service;

import com.alibaba.fastjson.JSONObject;
import com.appchina.pay.dao.model.PayOrder;

import java.util.Map;


public interface IPayOrderService {

    Map createPayOrder(String jsonParam);

    Map selectPayOrder(String jsonParam);

    Map selectPayOrderByMchIdAndPayOrderId(String jsonParam);

    Map selectPayOrderByMchIdAndMchOrderNo(String jsonParam);

    Map updateStatus4Ing(String jsonParam);

    Map updateStatus4Success(String jsonParam);

    Map updateStatus4Complete(String jsonParam);

    Map updateNotify(String jsonParam);

    int createPayOrder(JSONObject payOrder);

    JSONObject queryPayOrder(String mchId, String payOrderId, String mchOrderNo, String executeNotify);

    String doWxPayReq(String tradeType, JSONObject payOrder, String resKey);

    String doWxPayRequest(String tradeType, JSONObject payOrder, String resKey);

    String doAliPayReq(String channelId, JSONObject payOrder, String resKey);

    String queryWxPayOrderRequest(PayOrder payOrder,String resKey);

    PayOrder queryPayOrderByPayOrderId(String payOrderId);
}
