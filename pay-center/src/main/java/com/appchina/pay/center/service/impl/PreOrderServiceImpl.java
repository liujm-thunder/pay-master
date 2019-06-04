package com.appchina.pay.center.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.appchina.pay.center.service.BaseService;
import com.appchina.pay.center.service.PreOrderService;
import com.appchina.pay.common.constant.PayConstant;
import com.appchina.pay.common.util.MyLog;
import com.appchina.pay.common.util.PayUtil;
import com.appchina.pay.dao.model.GoodsOrder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PreOrderServiceImpl extends BaseService implements PreOrderService{

    private final MyLog log = MyLog.getLog(PreOrderServiceImpl.class);

    @Override
    public String createPreOrderInfo(GoodsOrder goodsOrder,JSONObject params) {
        int result = this.createPreOrder(goodsOrder);
        if(result != 1) {
            log.info("创建预订单失败:{}", params.toJSONString());
            return JSON.toJSONString(PayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, "创建预订单失败", null, null));
        }
        log.info("成功创建预订单:{}", params.toJSONString());
        Map<String, Object> resultMap = PayUtil.makeRetMap(PayConstant.RETURN_VALUE_SUCCESS, "", PayConstant.RETURN_VALUE_SUCCESS, null);
        resultMap.put("preOrderId",goodsOrder.getGoodsOrderId());
        return JSON.toJSONString(resultMap);
    }

    private int createPreOrder(GoodsOrder goodsOrder) {
        return super.baseCreatePreOrder(goodsOrder);
    }

    @Override
    public GoodsOrder getPayOrder(String goodsOrderId) {
        return super.baseSelectPreOrder(goodsOrderId);
    }

    @Override
    public int updatePayOrder(GoodsOrder goodsOrder) {
        return super.baseUpdatePreOrder(goodsOrder);
    }
}
