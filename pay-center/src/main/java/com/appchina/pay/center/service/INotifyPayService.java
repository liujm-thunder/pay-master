package com.appchina.pay.center.service;

import java.util.Map;


public interface INotifyPayService {

    Map doAliPayNotify(String jsonParam);

    Map doWxPayNotify(String jsonParam);

    Map sendBizPayNotify(String jsonParam);

    String handleAliPayNotify(Map params);

    String handleWxPayNotify(String xmlResult);
}
