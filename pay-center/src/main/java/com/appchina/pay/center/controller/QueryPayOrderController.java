package com.appchina.pay.center.controller;

import com.alibaba.fastjson.JSONObject;
import com.appchina.pay.center.service.IMchInfoService;
import com.appchina.pay.center.service.IPayOrderService;
import com.appchina.pay.center.service.PreOrderService;
import com.appchina.pay.common.constant.PayConstant;
import com.appchina.pay.common.util.*;
import com.appchina.pay.dao.model.GoodsOrder;
import com.appchina.pay.dao.model.PayOrder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description: 支付订单查询
 */
@RestController
public class QueryPayOrderController {

    private final MyLog log = MyLog.getLog(QueryPayOrderController.class);

    @Autowired
    private IPayOrderService payOrderService;

    @Autowired
    private IMchInfoService mchInfoService;

    @Autowired
    private PreOrderService preOrderService;

    /**
     * 查询支付订单接口:
     * 1)先验证接口参数以及签名信息
     * 2)根据参数查询订单
     * 3)返回订单数据
     * @param params
     * @return
     */
    @RequestMapping(value = "/api/pay/query_order")
    public String queryPayOrder(@RequestParam String params) {
    	JSONObject po = JSONObject.parseObject(params);
    	return queryPayOrder(po);
    }
    
    @RequestMapping(value = "/api/pay/query_order", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public String queryPayOrder(@RequestBody JSONObject params) {
        log.info("###### 开始接收商户查询支付订单请求 ######");
        String logPrefix = "【商户支付订单查询】";
        try {
            JSONObject payContext = new JSONObject();
            // 验证参数有效性
            String errorMessage = validateParams(params, payContext);
            if (!"success".equalsIgnoreCase(errorMessage)) {
                log.warn(errorMessage);
                return PayUtil.makeRetFail(PayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, errorMessage, null, null));
            }
            log.debug("请求参数及签名校验通过");
            String mchId = params.getString("mchId"); 			    // 商户ID
            String mchOrderNo = params.getString("mchOrderNo"); 	// 商户订单号
            String payOrderId = params.getString("payOrderId"); 	// 支付订单号
            String executeNotify = params.getString("executeNotify");   // 是否执行回调
            JSONObject payOrder = payOrderService.queryPayOrder(mchId, payOrderId, mchOrderNo, executeNotify);
            log.info("{}查询支付订单,结果:{}", logPrefix, payOrder);
            if (payOrder == null) {
                return PayUtil.makeRetFail(PayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, "支付订单不存在", null, null));
            }
            Map<String, Object> map = PayUtil.makeRetMap(PayConstant.RETURN_VALUE_SUCCESS, "", PayConstant.RETURN_VALUE_SUCCESS, null);
            map.put("result", payOrder);
            log.info("###### 商户查询订单处理完成 ######");
            return PayUtil.makeRetData(map, payContext.getString("resKey"));
        }catch (Exception e) {
            log.error(e, "");
            return PayUtil.makeRetFail(PayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, "支付中心系统异常", null, null));
        }
    }

    /**
     * 验证创建订单请求参数,参数通过返回JSONObject对象,否则返回错误文本信息
     * @param params
     * @return
     */
    private String validateParams(JSONObject params, JSONObject payContext) {
        // 验证请求参数,参数有问题返回错误提示
        String errorMessage;
        // 支付参数
        String mchId = params.getString("mchId"); 			    // 商户ID
        String mchOrderNo = params.getString("mchOrderNo"); 	// 商户订单号
        String payOrderId = params.getString("payOrderId"); 	// 支付订单号

        String sign = params.getString("sign"); 				// 签名

        // 验证请求参数有效性（必选项）
        if(StringUtils.isBlank(mchId)) {
            errorMessage = "request params[mchId] error.";
            return errorMessage;
        }
        if(StringUtils.isBlank(mchOrderNo) && StringUtils.isBlank(payOrderId)) {
            errorMessage = "request params[mchOrderNo or payOrderId] error.";
            return errorMessage;
        }

        // 签名信息
        if (StringUtils.isEmpty(sign)) {
            errorMessage = "request params[sign] error.";
            return errorMessage;
        }

        // 查询商户信息
        JSONObject mchInfo = mchInfoService.getByMchId(mchId);
        if(mchInfo == null) {
            errorMessage = "Can't found mchInfo[mchId="+mchId+"] record in db.";
            return errorMessage;
        }
        if(mchInfo.getByte("state") != 1) {
            errorMessage = "mchInfo not available [mchId="+mchId+"] record in db.";
            return errorMessage;
        }

        String reqKey = mchInfo.getString("reqKey");
        if (StringUtils.isBlank(reqKey)) {
            errorMessage = "reqKey is null[mchId="+mchId+"] record in db.";
            return errorMessage;
        }
        payContext.put("resKey", mchInfo.getString("resKey"));

        // TODO 验证签名数据
        boolean verifyFlag = PayUtil.verifyPaySign(params, reqKey);
//        if(!verifyFlag) {
//            errorMessage = "Verify XX pay sign failed.";
//            return errorMessage;
//        }

        return "success";
    }


    @RequestMapping(value = "/api/pay/query_order_state")
    public String queryOrderState(@RequestParam String params) {
        JSONObject po = JSONObject.parseObject(params);
        return doQueryOrderState(po);
    }


    @RequestMapping(value = "/api/pay/query_order_state",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public String doQueryOrderState(@RequestParam JSONObject params){
        log.info("###### 开始接收查询订单状态 ######");
        String logPrefix = "【查询订单】";
        try {
            PayOrder payOrder = null;
            // 验证参数有效性
            Object object = validateParamsForQueryOrderState(params);
            if (object instanceof String) {
                log.info("{}参数校验不通过:{}", logPrefix, object);
                return PayUtil.makeRetFail(PayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, object.toString(), null, null));
            }
            if (object instanceof  PayOrder) {
                payOrder = (PayOrder) object;
            }
            if(payOrder == null) return PayUtil.makeRetFail(PayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, "该预订单未发起支付", null, null));
            return payOrderService.queryWxPayOrderRequest(payOrder,"");
        }catch (Exception e) {
            log.error(e, "");
            return PayUtil.makeRetFail(PayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, "支付中心系统异常", null, null));
        }
    }

    private Object validateParamsForQueryOrderState(JSONObject params){
        String errorMessage;
        String payOrderId = params.getString("payOrderId");

        if(StringUtils.isBlank(payOrderId)) {
            errorMessage = "request params[payOrderId] error.";
            return errorMessage;
        }
        return payOrderService.queryPayOrderByPayOrderId(payOrderId);
    }

}
