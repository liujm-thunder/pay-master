package com.appchina.pay.center.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.appchina.pay.center.service.BaseService;
import com.appchina.pay.center.service.IPayChannelService;
import com.appchina.pay.common.constant.RetEnum;
import com.appchina.pay.common.model.BaseParam;
import org.springframework.stereotype.Service;

import com.appchina.pay.common.util.JsonUtil;
import com.appchina.pay.common.util.MyLog;
import com.appchina.pay.common.util.ObjectValidUtil;
import com.appchina.pay.common.util.RpcUtil;
import com.appchina.pay.dao.model.PayChannel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class PayChannelServiceImpl extends BaseService implements IPayChannelService {

    private static final MyLog log = MyLog.getLog(PayChannelServiceImpl.class);

    @Override
    public Map selectPayChannel(String jsonParam) {
        BaseParam baseParam = JsonUtil.getObjectFromJson(jsonParam, BaseParam.class);
        Map<String, Object> bizParamMap = baseParam.getBizParamMap();
        if (ObjectValidUtil.isInvalid(bizParamMap)) {
            log.warn("查询支付渠道信息失败, {}. jsonParam={}", RetEnum.RET_PARAM_NOT_FOUND.getMessage(), jsonParam);
            return RpcUtil.createFailResult(baseParam, RetEnum.RET_PARAM_NOT_FOUND);
        }
        String mchId = baseParam.isNullValue("mchId") ? null : bizParamMap.get("mchId").toString();
        String channelId = baseParam.isNullValue("channelId") ? null : bizParamMap.get("channelId").toString();
        if (ObjectValidUtil.isInvalid(mchId, channelId)) {
            log.warn("查询支付渠道信息失败, {}. jsonParam={}", RetEnum.RET_PARAM_INVALID.getMessage(), jsonParam);
            return RpcUtil.createFailResult(baseParam, RetEnum.RET_PARAM_INVALID);
        }
        PayChannel payChannel = super.baseSelectPayChannel(mchId, channelId);
        if(payChannel == null) return RpcUtil.createFailResult(baseParam, RetEnum.RET_BIZ_DATA_NOT_EXISTS);
        String jsonResult = JsonUtil.object2Json(payChannel);
        return RpcUtil.createBizResult(baseParam, jsonResult);
    }

    public JSONObject getByMchIdAndChannelId(String mchId, String channelId) {
        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("mchId", mchId);
        paramMap.put("channelId", channelId);
        String jsonParam = RpcUtil.createBaseParam(paramMap);
        Map<String, Object> result = selectPayChannel(jsonParam);
        String s = RpcUtil.mkRet(result);
        if(s == null) return null;
        return JSONObject.parseObject(s);
    }


    @Override
    public List<String> selectPayChannelByMuchId(String mchId) {
        List<PayChannel> payChannels = super.baseSelectPayChannels(mchId);
        if(payChannels != null){
            List<String> payChannelList = new ArrayList<>();
            for (PayChannel item : payChannels){
                payChannelList.add(item.getChannelId());
            }
            return payChannelList;
        }
        return null;
    }

}
