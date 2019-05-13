package com.appchina.pay.center.service;

import java.util.Map;


public interface IPayChannel4AliService {

    Map doAliPayWapReq(String jsonParam);

    Map doAliPayPcReq(String jsonParam);

    Map doAliPayMobileReq(String jsonParam);

    Map doAliPayQrReq(String jsonParam);

}
