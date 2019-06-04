package com.appchina.pay.center.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.appchina.pay.center.service.BaseService;
import com.appchina.pay.center.service.IMchInfoService;
import com.appchina.pay.common.constant.RetEnum;
import com.appchina.pay.common.model.BaseParam;
import com.appchina.pay.common.util.JsonUtil;
import com.appchina.pay.common.util.MyLog;
import com.appchina.pay.common.util.ObjectValidUtil;
import com.appchina.pay.common.util.RpcUtil;
import com.appchina.pay.dao.model.MchInfo;
import org.springframework.stereotype.Service;


import java.util.HashMap;
import java.util.Map;


@Service
public class MchInfoServiceImpl extends BaseService implements IMchInfoService {

    private static final MyLog log = MyLog.getLog(MchInfoServiceImpl.class);

    @Override
    public Map selectMchInfo(String jsonParam) {
        BaseParam baseParam = JsonUtil.getObjectFromJson(jsonParam, BaseParam.class);
        Map<String, Object> bizParamMap = baseParam.getBizParamMap();
        if (ObjectValidUtil.isInvalid(bizParamMap)) {
            log.warn("查询商户信息失败, {}. jsonParam={}", RetEnum.RET_PARAM_NOT_FOUND.getMessage(), jsonParam);
            return RpcUtil.createFailResult(baseParam, RetEnum.RET_PARAM_NOT_FOUND);
        }
        String mchId = baseParam.isNullValue("mchId") ? null : bizParamMap.get("mchId").toString();
        if (ObjectValidUtil.isInvalid(mchId)) {
            log.warn("查询商户信息失败, {}. jsonParam={}", RetEnum.RET_PARAM_INVALID.getMessage(), jsonParam);
            return RpcUtil.createFailResult(baseParam, RetEnum.RET_PARAM_INVALID);
        }
        MchInfo mchInfo = super.baseSelectMchInfo(mchId);
        if(mchInfo == null) return RpcUtil.createFailResult(baseParam, RetEnum.RET_BIZ_DATA_NOT_EXISTS);
        String jsonResult = JsonUtil.object2Json(mchInfo);
        return RpcUtil.createBizResult(baseParam, jsonResult);
    }

    public JSONObject getByMchId(String mchId) {
        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("mchId", mchId);
        String jsonParam = RpcUtil.createBaseParam(paramMap);
        Map<String, Object> result = selectMchInfo(jsonParam);
        String s = RpcUtil.mkRet(result);
        if(s==null) return null;
        return JSONObject.parseObject(s);
    }
}
