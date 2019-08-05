package com.appchina.pay.center.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.*;
import com.alipay.api.request.*;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.appchina.pay.center.service.BaseService;
import com.appchina.pay.center.service.IPayChannel4AliService;
import com.appchina.pay.center.service.channel.alipay.AlipayConfig;
import com.appchina.pay.common.constant.PayConstant;
import com.appchina.pay.common.constant.RetEnum;
import com.appchina.pay.common.model.BaseParam;
import com.appchina.pay.common.util.*;
import com.appchina.pay.dao.model.PayChannel;
import com.appchina.pay.dao.model.PayOrder;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.HashMap;
import java.util.Map;

/**
 * @author: dingzhiwei
 * @date: 17/9/10
 * @description:
 */
@Service
public class PayChannel4AliServiceImpl extends BaseService implements IPayChannel4AliService {

    private static final MyLog log = MyLog.getLog(PayChannel4AliServiceImpl.class);

    @Autowired
    private AlipayConfig alipayConfig;

    @Override
    public Map doAliPayWapReq(String jsonParam) {
        String logPrefix = "【支付宝WAP支付下单】";
        BaseParam baseParam = JsonUtil.getObjectFromJson(jsonParam, BaseParam.class);
        Map<String, Object> bizParamMap = baseParam.getBizParamMap();
        if (ObjectValidUtil.isInvalid(bizParamMap)) {
            log.warn("{}失败, {}. jsonParam={}", logPrefix, RetEnum.RET_PARAM_NOT_FOUND.getMessage(), jsonParam);
            return RpcUtil.createFailResult(baseParam, RetEnum.RET_PARAM_NOT_FOUND);
        }
        JSONObject payOrderObj = baseParam.isNullValue("payOrder") ? null : JSONObject.parseObject(bizParamMap.get("payOrder").toString());
        PayOrder payOrder = BeanConvertUtils.map2Bean(payOrderObj, PayOrder.class);
        if (ObjectValidUtil.isInvalid(payOrder)) {
            log.warn("{}失败, {}. jsonParam={}", logPrefix, RetEnum.RET_PARAM_INVALID.getMessage(), jsonParam);
            return RpcUtil.createFailResult(baseParam, RetEnum.RET_PARAM_INVALID);
        }
        String payOrderId = payOrder.getPayOrderId();
        String mchId = payOrder.getMchId();
        String channelId = payOrder.getChannelId();
        PayChannel payChannel = super.baseSelectPayChannel(mchId, channelId);
        alipayConfig.init(payChannel.getParam());
        AlipayClient client = new DefaultAlipayClient(alipayConfig.getUrl(), alipayConfig.getApp_id(), alipayConfig.getRsa_private_key(), AlipayConfig.FORMAT, AlipayConfig.CHARSET, alipayConfig.getAlipay_public_key(), AlipayConfig.SIGNTYPE);
        AlipayTradeWapPayRequest alipay_request = new AlipayTradeWapPayRequest();
        // 封装请求支付信息
        AlipayTradeWapPayModel model=new AlipayTradeWapPayModel();
        model.setOutTradeNo(payOrderId);
        model.setSubject(payOrder.getSubject());
        model.setTotalAmount(AmountUtil.convertCent2Dollar(payOrder.getAmount().toString()));
        model.setBody(payOrder.getBody());
        model.setProductCode("QUICK_WAP_PAY");
        // 获取objParams参数
        String objParams = payOrder.getExtra();
        if (StringUtils.isNotEmpty(objParams)) {
            try {
                JSONObject objParamsJson = JSON.parseObject(objParams);
                if(StringUtils.isNotBlank(objParamsJson.getString("quit_url"))) {
                    model.setQuitUrl(objParamsJson.getString("quit_url"));
                }
            } catch (Exception e) {
                log.error("{}objParams参数格式错误！", logPrefix);
            }
        }
        alipay_request.setBizModel(model);
        // 设置异步通知地址
        alipay_request.setNotifyUrl(alipayConfig.getNotify_url());
        // 设置同步地址
        alipay_request.setReturnUrl(alipayConfig.getReturn_url());
        String payUrl = null;
        try {
            payUrl = client.pageExecute(alipay_request).getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        log.info("{}生成跳转路径：payUrl={}", logPrefix, payUrl);
        super.baseUpdateStatus4Ing(payOrderId, null);
        log.info("{}生成请求支付宝数据,req={}", logPrefix, alipay_request.getBizModel());
        log.info("###### 商户统一下单处理完成 ######");
        Map<String, Object> map = PayUtil.makeRetMap(PayConstant.RETURN_VALUE_SUCCESS, "", PayConstant.RETURN_VALUE_SUCCESS, null);
        map.put("payOrderId", payOrderId);
        map.put("payParams", payUrl);
        return RpcUtil.createBizResult(baseParam, map);
    }

    @Override
    public Map doAliPayPcReq(String jsonParam) {
        String logPrefix = "【支付宝PC支付下单】";
        BaseParam baseParam = JsonUtil.getObjectFromJson(jsonParam, BaseParam.class);
        Map<String, Object> bizParamMap = baseParam.getBizParamMap();
        if (ObjectValidUtil.isInvalid(bizParamMap)) {
            log.warn("{}失败, {}. jsonParam={}", logPrefix, RetEnum.RET_PARAM_NOT_FOUND.getMessage(), jsonParam);
            return RpcUtil.createFailResult(baseParam, RetEnum.RET_PARAM_NOT_FOUND);
        }
        JSONObject payOrderObj = baseParam.isNullValue("payOrder") ? null : JSONObject.parseObject(bizParamMap.get("payOrder").toString());
        PayOrder payOrder = BeanConvertUtils.map2Bean(payOrderObj, PayOrder.class);
        if (ObjectValidUtil.isInvalid(payOrder)) {
            log.warn("{}失败, {}. jsonParam={}", logPrefix, RetEnum.RET_PARAM_INVALID.getMessage(), jsonParam);
            return RpcUtil.createFailResult(baseParam, RetEnum.RET_PARAM_INVALID);
        }
        String payOrderId = payOrder.getPayOrderId();
        String mchId = payOrder.getMchId();
        String channelId = payOrder.getChannelId();
        PayChannel payChannel = super.baseSelectPayChannel(mchId, channelId);
        alipayConfig.init(payChannel.getParam());
        AlipayClient client = new DefaultAlipayClient(alipayConfig.getUrl(), alipayConfig.getApp_id(), alipayConfig.getRsa_private_key(), AlipayConfig.FORMAT, AlipayConfig.CHARSET, alipayConfig.getAlipay_public_key(), AlipayConfig.SIGNTYPE);
        AlipayTradePagePayRequest alipay_request = new AlipayTradePagePayRequest();
        // 封装请求支付信息
        AlipayTradePagePayModel model=new AlipayTradePagePayModel();
        model.setOutTradeNo(payOrderId);
        model.setSubject(payOrder.getSubject());
        model.setTotalAmount(AmountUtil.convertCent2Dollar(payOrder.getAmount().toString()));
        model.setBody(payOrder.getBody());
        model.setProductCode("FAST_INSTANT_TRADE_PAY");
        // 获取objParams参数
        String objParams = payOrder.getExtra();
        String qr_pay_mode = "2";
        String qrcode_width = "200";
        if (StringUtils.isNotEmpty(objParams)) {
            try {
                JSONObject objParamsJson = JSON.parseObject(objParams);
                qr_pay_mode = ObjectUtils.toString(objParamsJson.getString("qr_pay_mode"), "2");
                qrcode_width = ObjectUtils.toString(objParamsJson.getString("qrcode_width"), "200");
            } catch (Exception e) {
                log.error("{}objParams参数格式错误！", logPrefix);
            }
        }
        model.setQrPayMode(qr_pay_mode);
        model.setQrcodeWidth(Long.parseLong(qrcode_width));
        alipay_request.setBizModel(model);
        // 设置异步通知地址
        alipay_request.setNotifyUrl(alipayConfig.getNotify_url());
        // 设置同步地址
        alipay_request.setReturnUrl(alipayConfig.getReturn_url());
        String payUrl = null;
        try {
            payUrl = client.pageExecute(alipay_request).getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        log.info("{}生成跳转路径：payUrl={}", logPrefix, payUrl);
        super.baseUpdateStatus4Ing(payOrderId, null);
        log.info("{}生成请求支付宝数据,req={}", logPrefix, alipay_request.getBizModel());
        log.info("###### 商户统一下单处理完成 ######");
        Map<String, Object> map = PayUtil.makeRetMap(PayConstant.RETURN_VALUE_SUCCESS, "", PayConstant.RETURN_VALUE_SUCCESS, null);
        map.put("payOrderId", payOrderId);
        map.put("payParams", payUrl);
        return RpcUtil.createBizResult(baseParam, map);
    }

    @Override
    public Map doAliPayMobileReq(String jsonParam) {
        String logPrefix = "【支付宝APP支付下单】";
        BaseParam baseParam = JsonUtil.getObjectFromJson(jsonParam, BaseParam.class);
        Map<String, Object> bizParamMap = baseParam.getBizParamMap();
        if (ObjectValidUtil.isInvalid(bizParamMap)) {
            log.warn("{}失败, {}. jsonParam={}", logPrefix, RetEnum.RET_PARAM_NOT_FOUND.getMessage(), jsonParam);
            return RpcUtil.createFailResult(baseParam, RetEnum.RET_PARAM_NOT_FOUND);
        }
        JSONObject payOrderObj = baseParam.isNullValue("payOrder") ? null : JSONObject.parseObject(bizParamMap.get("payOrder").toString());
        PayOrder payOrder = BeanConvertUtils.map2Bean(payOrderObj, PayOrder.class);
        if (ObjectValidUtil.isInvalid(payOrder)) {
            log.warn("{}失败, {}. jsonParam={}", logPrefix, RetEnum.RET_PARAM_INVALID.getMessage(), jsonParam);
            return RpcUtil.createFailResult(baseParam, RetEnum.RET_PARAM_INVALID);
        }
        String payOrderId = payOrder.getPayOrderId();
        String mchId = payOrder.getMchId();
        String channelId = payOrder.getChannelId();
        PayChannel payChannel = super.baseSelectPayChannel(mchId, channelId);
        alipayConfig.init(payChannel.getParam());
        AlipayClient client = new DefaultAlipayClient(alipayConfig.getUrl(), alipayConfig.getApp_id(), alipayConfig.getRsa_private_key(), AlipayConfig.FORMAT, AlipayConfig.CHARSET, alipayConfig.getAlipay_public_key(), AlipayConfig.SIGNTYPE);
        AlipayTradeAppPayRequest alipay_request = new AlipayTradeAppPayRequest();
        // 封装请求支付信息
        AlipayTradeAppPayModel model=new AlipayTradeAppPayModel();
        model.setOutTradeNo(payOrderId);
        model.setSubject(payOrder.getSubject());
        model.setTotalAmount(AmountUtil.convertCent2Dollar(payOrder.getAmount().toString()));
        model.setBody(payOrder.getBody());
        model.setProductCode("QUICK_MSECURITY_PAY");
        alipay_request.setBizModel(model);
        // 设置异步通知地址
        alipay_request.setNotifyUrl(alipayConfig.getNotify_url());
        // 设置同步地址
        alipay_request.setReturnUrl(alipayConfig.getReturn_url());
        String payParams = null;
        try {
            payParams = client.sdkExecute(alipay_request).getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        super.baseUpdateStatus4Ing(payOrderId, null);
        log.info("{}生成请求支付宝数据,payParams={}", logPrefix, payParams);
        log.info("###### 商户统一下单处理完成 ######");
        Map<String, Object> map = PayUtil.makeRetMap(PayConstant.RETURN_VALUE_SUCCESS, "", PayConstant.RETURN_VALUE_SUCCESS, null);
        map.put("payOrderId", payOrderId);
        map.put("payParams", payParams);
        return RpcUtil.createBizResult(baseParam, map);
    }

    @Override
    public Map doAliPayQrReq(String jsonParam) {
        String logPrefix = "【支付宝当面付之扫码支付下单】";
        BaseParam baseParam = JsonUtil.getObjectFromJson(jsonParam, BaseParam.class);
        Map<String, Object> bizParamMap = baseParam.getBizParamMap();
        if (ObjectValidUtil.isInvalid(bizParamMap)) {
            log.warn("{}失败, {}. jsonParam={}", logPrefix, RetEnum.RET_PARAM_NOT_FOUND.getMessage(), jsonParam);
            return RpcUtil.createFailResult(baseParam, RetEnum.RET_PARAM_NOT_FOUND);
        }
        JSONObject payOrderObj = baseParam.isNullValue("payOrder") ? null : JSONObject.parseObject(bizParamMap.get("payOrder").toString());
        PayOrder payOrder = BeanConvertUtils.map2Bean(payOrderObj, PayOrder.class);
        if (ObjectValidUtil.isInvalid(payOrder)) {
            log.warn("{}失败, {}. jsonParam={}", logPrefix, RetEnum.RET_PARAM_INVALID.getMessage(), jsonParam);
            return RpcUtil.createFailResult(baseParam, RetEnum.RET_PARAM_INVALID);
        }
        String payOrderId = payOrder.getPayOrderId();
        String mchId = payOrder.getMchId();
        String channelId = payOrder.getChannelId();
        PayChannel payChannel = super.baseSelectPayChannel(mchId, channelId);
        alipayConfig.init(payChannel.getParam());
        AlipayClient client = new DefaultAlipayClient(alipayConfig.getUrl(), alipayConfig.getApp_id(), alipayConfig.getRsa_private_key(), AlipayConfig.FORMAT, AlipayConfig.CHARSET, alipayConfig.getAlipay_public_key(), AlipayConfig.SIGNTYPE);
        AlipayTradePrecreateRequest alipay_request = new AlipayTradePrecreateRequest();
        // 封装请求支付信息
        AlipayTradePrecreateModel model=new AlipayTradePrecreateModel();
        model.setOutTradeNo(payOrderId);
        model.setSubject(payOrder.getSubject());
        model.setTotalAmount(AmountUtil.convertCent2Dollar(payOrder.getAmount().toString()));
        model.setBody(payOrder.getBody());
        // 获取objParams参数
        String objParams = payOrder.getExtra();
        if (StringUtils.isNotEmpty(objParams)) {
            try {
                JSONObject objParamsJson = JSON.parseObject(objParams);
                if(StringUtils.isNotBlank(objParamsJson.getString("discountable_amount"))) {
                    //可打折金额
                    model.setDiscountableAmount(objParamsJson.getString("discountable_amount"));
                }
                if(StringUtils.isNotBlank(objParamsJson.getString("undiscountable_amount"))) {
                    //不可打折金额
                    model.setUndiscountableAmount(objParamsJson.getString("undiscountable_amount"));
                }
            } catch (Exception e) {
                log.error("{}objParams参数格式错误！", logPrefix);
            }
        }
        alipay_request.setBizModel(model);
        // 设置异步通知地址
        alipay_request.setNotifyUrl(alipayConfig.getNotify_url());
        // 设置同步地址
        alipay_request.setReturnUrl(alipayConfig.getReturn_url());
        String payUrl = null;
        try {
            payUrl = client.execute(alipay_request).getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        log.info("{}生成跳转路径：payUrl={}", logPrefix, payUrl);
        super.baseUpdateStatus4Ing(payOrderId, null);
        log.info("{}生成请求支付宝数据,req={}", logPrefix, alipay_request.getBizModel());
        log.info("###### 商户统一下单处理完成 ######");
        Map<String, Object> map = PayUtil.makeRetMap(PayConstant.RETURN_VALUE_SUCCESS, "", PayConstant.RETURN_VALUE_SUCCESS, null);
        map.put("payOrderId", payOrderId);
        map.put("payParams", payUrl);
        return RpcUtil.createBizResult(baseParam, map);
    }



    @Override
    public Map doQueryAliPayOrder(PayOrder payOrder) {
        String logPrefix = "【支付宝查询订单】";
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", payOrder.getMchOrderNo());
        if (payOrder.getStatus().intValue()==PayConstant.PAY_STATUS_SUCCESS){
            map.put("tradeState",PayConstant.RETURN_PAY_STATUS_SUCCESS);
            map.put("tradeStateDesc","支付成功");
            return map;
        }
        String payOrderId = payOrder.getPayOrderId();
        String mchId = payOrder.getMchId();
        String channelId = payOrder.getChannelId();
        PayChannel payChannel = super.baseSelectPayChannel(mchId, channelId);
        alipayConfig.init(payChannel.getParam());
        AlipayClient client = new DefaultAlipayClient(alipayConfig.getUrl(), alipayConfig.getApp_id(), alipayConfig.getRsa_private_key(), AlipayConfig.FORMAT, AlipayConfig.CHARSET, alipayConfig.getAlipay_public_key(), AlipayConfig.SIGNTYPE);

        AlipayTradeQueryRequest alipayTradeQueryRequest = new AlipayTradeQueryRequest();
        // 封装请求支付信息
        AlipayTradeQueryModel alipayTradeQueryModel = new AlipayTradeQueryModel();
        alipayTradeQueryModel.setOutTradeNo(payOrderId);
        alipayTradeQueryModel.setTradeNo(payOrder.getChannelOrderNo());

        alipayTradeQueryRequest.setBizModel(alipayTradeQueryModel);
        AlipayTradeQueryResponse response = null;
        try {
            response = client.execute(alipayTradeQueryRequest);
            byte status;
            if (response != null && response.getTradeStatus() != null) {
                String orderStatus = response.getTradeStatus();
                if(orderStatus.equalsIgnoreCase(PayConstant.AlipayConstant.TRADE_STATUS_SUCCESS)){
                    //支付成功
                    map.put("tradeState", PayConstant.RETURN_PAY_STATUS_SUCCESS);
                    map.put("tradeStateDesc", "支付成功");
                    status = PayConstant.PAY_STATUS_SUCCESS;
                }else if (orderStatus.equalsIgnoreCase(PayConstant.AlipayConstant.TRADE_STATUS_FINISHED)){
                    //支付成功但不能退款
                    map.put("tradeState", PayConstant.RETURN_PAY_STATUS_SUCCESS);
                    map.put("tradeStateDesc", "支付成功");
                    status = PayConstant.PAY_STATUS_SUCCESS;
                }else if (orderStatus.equalsIgnoreCase(PayConstant.AlipayConstant.TRADE_STATUS_WAIT)){
                    map.put("tradeState", PayConstant.RETURN_PAY_STATUS_NOTPAY);
                    map.put("tradeStateDesc", "待支付");
                    status = PayConstant.PAY_STATUS_PAYING;
                }else if (orderStatus.equalsIgnoreCase(PayConstant.AlipayConstant.TRADE_STATUS_CLOSED)){
                    map.put("tradeState", PayConstant.RETURN_PAY_STATUS_FAIL);
                    map.put("tradeStateDesc", "支付失败");
                    status = PayConstant.PAY_STATUS_FAILED;
                }else {
                    map.put("tradeState", PayConstant.RETURN_PAY_STATUS_FAIL);
                    map.put("tradeStateDesc", "支付失败");
                    status = PayConstant.PAY_STATUS_FAILED;
                }
            }else if (response != null && response.getSubCode() != null && response.getSubCode().equalsIgnoreCase("ACQ.TRADE_NOT_EXIST")){
                map.put("tradeState", PayConstant.RETURN_PAY_STATUS_NOTPAY);
                map.put("tradeStateDesc", "待支付");
                status = PayConstant.PAY_STATUS_PAYING;
            }else {
                status = PayConstant.PAY_STATUS_FAILED;
                map.put("tradeState", PayConstant.RETURN_PAY_STATUS_FAIL);
                map.put("tradeStateDesc", "支付失败");
            }
            super.baseUpdateStatus(payOrder.getPayOrderId(), payOrder.getChannelOrderNo(),status,null,null,null);
        } catch (AlipayApiException e) {
            log.error(e, "支付宝查询订单异常 response {}",response.getBody());
            map.put("tradeState", PayConstant.RETURN_PAY_STATUS_FAIL);
            map.put("tradeStateDesc", "支付宝查询订单异常");
            return map;
        }
        log.info("{}查询订单响应：response={}", logPrefix, response.getBody());
        log.info("{}生成请求支付宝数据,req={}", logPrefix, map);
        log.info("###### 商户查询订单处理完成 ######");
        return map;
    }

}
