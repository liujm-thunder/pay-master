package com.appchina.pay.center.service;

import com.alibaba.fastjson.JSONObject;

import java.util.List;
import java.util.Map;


public interface IPayChannelService {

    Map selectPayChannel(String jsonParam);

    List<String> selectPayChannelByMuchId(String mchId);

    JSONObject getByMchIdAndChannelId(String mchId, String channelId);
}
