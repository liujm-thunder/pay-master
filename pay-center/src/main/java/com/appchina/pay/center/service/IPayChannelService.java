package com.appchina.pay.center.service;

import com.alibaba.fastjson.JSONObject;

import java.util.Map;


public interface IPayChannelService {

    Map selectPayChannel(String jsonParam);

    Map selectPayChannels(String jsonParam);

    JSONObject getByMchIdAndChannelId(String mchId, String channelId);
}
