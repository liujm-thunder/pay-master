package com.appchina.pay.center.service;

import com.alibaba.fastjson.JSONObject;

import java.util.Map;


public interface IMchInfoService {

    Map selectMchInfo(String jsonParam);

    JSONObject getByMchId(String mchId);

}
