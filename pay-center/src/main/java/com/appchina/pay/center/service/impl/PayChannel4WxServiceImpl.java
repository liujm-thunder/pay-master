package com.appchina.pay.center.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.appchina.pay.center.service.BaseService;
import com.appchina.pay.center.service.IPayChannel4WxService;
import com.appchina.pay.center.service.PreOrderService;
import com.appchina.pay.center.service.channel.wechat.WxPayProperties;
import com.appchina.pay.center.service.channel.wechat.WxPayUtil;
import com.appchina.pay.common.constant.PayConstant;
import com.appchina.pay.common.constant.RetEnum;
import com.appchina.pay.common.model.BaseParam;
import com.appchina.pay.common.util.*;
import com.appchina.pay.dao.model.GoodsOrder;
import com.appchina.pay.dao.model.PayChannel;
import com.appchina.pay.dao.model.PayOrder;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderRequest;
import com.github.binarywang.wxpay.bean.result.WxPayOrderQueryResult;
import com.github.binarywang.wxpay.bean.result.WxPayUnifiedOrderResult;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.constant.WxPayConstants;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.github.binarywang.wxpay.service.impl.WxPayServiceImpl;
import com.github.binarywang.wxpay.util.SignUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description: 支付渠道接口:微信
 */
@Service
public class PayChannel4WxServiceImpl extends BaseService implements IPayChannel4WxService {

    private final MyLog log = MyLog.getLog(PayChannel4WxServiceImpl.class);

    @Resource
    private WxPayProperties wxPayProperties;

    public Map doWxPayReq(String jsonParam) {
        String logPrefix = "【微信支付统一下单】";
        BaseParam baseParam = JsonUtil.getObjectFromJson(jsonParam, BaseParam.class);
        Map<String, Object> bizParamMap = baseParam.getBizParamMap();
        try{
            if (ObjectValidUtil.isInvalid(bizParamMap)) {
                log.warn("{}失败, {}. jsonParam={}", logPrefix, RetEnum.RET_PARAM_NOT_FOUND.getMessage(), jsonParam);
                return RpcUtil.createFailResult(baseParam, RetEnum.RET_PARAM_NOT_FOUND);
            }
            JSONObject payOrderObj = baseParam.isNullValue("payOrder") ? null : JSONObject.parseObject(bizParamMap.get("payOrder").toString());
            String tradeType = baseParam.isNullValue("tradeType") ? null : bizParamMap.get("tradeType").toString();
            PayOrder payOrder = BeanConvertUtils.map2Bean(payOrderObj, PayOrder.class);
            if (ObjectValidUtil.isInvalid(payOrder, tradeType)) {
                log.warn("{}失败, {}. jsonParam={}", logPrefix, RetEnum.RET_PARAM_INVALID.getMessage(), jsonParam);
                return RpcUtil.createFailResult(baseParam, RetEnum.RET_PARAM_INVALID);
            }
            String mchId = payOrder.getMchId();
            String channelId = payOrder.getChannelId();
            PayChannel payChannel = super.baseSelectPayChannel(mchId, channelId);
//            WxPayConfig wxPayConfig = WxPayUtil.getWxPayConfig(payChannel.getParam(), tradeType, wxPayProperties.getCertRootPath(), wxPayProperties.getNotifyUrl());
            WxPayConfig wxPayConfig = WxPayUtil.getWxPayConfig(payChannel.getParam(),tradeType,wxPayProperties.getNotifyUrl());
            WxPayService wxPayService = new WxPayServiceImpl();
            wxPayService.setConfig(wxPayConfig);
            WxPayUnifiedOrderRequest wxPayUnifiedOrderRequest = buildUnifiedOrderRequest(payOrder, wxPayConfig);
            String payOrderId = payOrder.getPayOrderId();
            WxPayUnifiedOrderResult wxPayUnifiedOrderResult;
            try {
                wxPayUnifiedOrderResult = wxPayService.unifiedOrder(wxPayUnifiedOrderRequest);
                log.info("{} >>> 下单成功", logPrefix);
                Map<String, Object> map = new HashMap<>();
                map.put("payOrderId", payOrderId);
                map.put("prepayId", wxPayUnifiedOrderResult.getPrepayId());
                int result = super.baseUpdateStatus4Ing(payOrderId, wxPayUnifiedOrderResult.getPrepayId());
                log.info("更新第三方支付订单号:payOrderId={},prepayId={},result={}", payOrderId, wxPayUnifiedOrderResult.getPrepayId(), result);
                switch (tradeType) {
                    case PayConstant.WxConstant.TRADE_TYPE_NATIVE : {
                        map.put("codeUrl", wxPayUnifiedOrderResult.getCodeURL());   // 二维码支付链接
                        break;
                    }
                    case PayConstant.WxConstant.TRADE_TYPE_APP : {
                        Map<String, String> payInfo = new HashMap<>();
                        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
                        String nonceStr = String.valueOf(System.currentTimeMillis());
                        // APP支付绑定的是微信开放平台上的账号，APPID为开放平台上绑定APP后发放的参数
                        String appId = wxPayConfig.getAppId();
                        Map<String, String> configMap = new HashMap<>();
                        // 此map用于参与调起sdk支付的二次签名,格式全小写，timestamp只能是10位,格式固定，切勿修改
                        String partnerId = wxPayConfig.getMchId();
                        configMap.put("prepayid", wxPayUnifiedOrderResult.getPrepayId());
                        configMap.put("partnerid", partnerId);
                        String packageValue = "Sign=WXPay";
                        configMap.put("package", packageValue);
                        configMap.put("timestamp", timestamp);
                        configMap.put("noncestr", nonceStr);
                        configMap.put("appid", appId);
                        // 此map用于客户端与微信服务器交互
                        payInfo.put("sign", SignUtils.createSign(configMap, wxPayConfig.getMchKey(), null));
                        payInfo.put("prepayid", wxPayUnifiedOrderResult.getPrepayId());
                        payInfo.put("partnerid", partnerId);
                        payInfo.put("appid", appId);
                        payInfo.put("package", packageValue);
                        payInfo.put("timestamp", timestamp);
                        payInfo.put("noncestr", nonceStr);
                        map.put("payParams", payInfo);
                        break;
                    }
                    case PayConstant.WxConstant.TRADE_TYPE_JSPAI : {
                        Map<String, String> payInfo = new HashMap<>();
                        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
                        String nonceStr = String.valueOf(System.currentTimeMillis());
                        payInfo.put("appId", wxPayUnifiedOrderResult.getAppid());
                        // 支付签名时间戳，注意微信jssdk中的所有使用timestamp字段均为小写。但最新版的支付后台生成签名使用的timeStamp字段名需大写其中的S字符
                        payInfo.put("timeStamp", timestamp);
                        payInfo.put("nonceStr", nonceStr);
                        payInfo.put("package", "prepay_id=" + wxPayUnifiedOrderResult.getPrepayId());
                        payInfo.put("signType", WxPayConstants.SignType.MD5);
                        payInfo.put("paySign", SignUtils.createSign(payInfo, wxPayConfig.getMchKey(), null));
                        map.put("payParams", payInfo);
                        break;
                    }
                    case PayConstant.WxConstant.TRADE_TYPE_MWEB : {
                        map.put("payUrl", wxPayUnifiedOrderResult.getMwebUrl());    // h5支付链接地址
                        break;
                    }
                }
                return RpcUtil.createBizResult(baseParam, map);
            } catch (WxPayException e) {
                log.error(e, "下单失败");
                //出现业务错误
                log.info("{}下单返回失败", logPrefix);
                log.info("err_code:{}", e.getErrCode());
                log.info("err_code_des:{}", e.getErrCodeDes());
                return RpcUtil.createFailResult(baseParam, RetEnum.RET_BIZ_WX_PAY_CREATE_FAIL);

//                return PayUtil.makeRetData(PayUtil.makeRetMap(PayConstant.RETURN_VALUE_SUCCESS, "", PayConstant.RETURN_VALUE_FAIL, "0111", "调用微信支付失败," + e.getErrCode() + ":" + e.getErrCodeDes()), resKey);
            }
        }catch (Exception e) {
            log.error(e, "微信支付统一下单异常");

            return RpcUtil.createFailResult(baseParam, RetEnum.RET_BIZ_WX_PAY_CREATE_FAIL);

            //return XXPayUtil.makeRetFail(XXPayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, "", PayConstant.RETURN_VALUE_FAIL, PayEnum.ERR_0001));
        }
    }


    /**
     * 构建微信统一下单请求数据
     * @param payOrder
     * @param wxPayConfig
     * @return
     */
    WxPayUnifiedOrderRequest buildUnifiedOrderRequest(PayOrder payOrder, WxPayConfig wxPayConfig) throws IllegalAccessException {
        String tradeType = wxPayConfig.getTradeType();
        String appId = wxPayConfig.getAppId();
        String muchId = wxPayConfig.getMchId();
        String key = wxPayConfig.getMchKey();
        String payOrderId = payOrder.getPayOrderId();
        Integer totalFee = payOrder.getAmount().intValue();// 支付金额,单位分
        String deviceInfo = payOrder.getDevice();
        String body = payOrder.getBody();
        String detail = null;
        String attach = null;
        String outTradeNo = payOrderId;
        String feeType = "CNY";
        String spBillCreateIP = payOrder.getClientIp();
        String timeStart = null;
        String timeExpire = null;
        String goodsTag = null;
        String notifyUrl = wxPayConfig.getNotifyUrl();
        String productId = null;
        if(tradeType.equals(PayConstant.WxConstant.TRADE_TYPE_NATIVE)) productId = JSON.parseObject(payOrder.getExtra()).getString("productId");
        String limitPay = null;
        String openId = null;
        if(tradeType.equals(PayConstant.WxConstant.TRADE_TYPE_JSPAI)) openId = JSON.parseObject(payOrder.getExtra()).getString("openId");
        String sceneInfo = null;
        if(tradeType.equals(PayConstant.WxConstant.TRADE_TYPE_MWEB)) sceneInfo = JSON.parseObject(payOrder.getExtra()).getString("sceneInfo");
        // 微信统一下单请求对象
        WxPayUnifiedOrderRequest request = new WxPayUnifiedOrderRequest();
        //TODO appId
        request.setAppid(appId);
        request.setMchId(muchId);

        request.setDeviceInfo(deviceInfo);
        request.setBody(body);
        request.setDetail(detail);
        request.setAttach(attach);
        request.setOutTradeNo(outTradeNo);
        request.setFeeType(feeType);
        request.setTotalFee(totalFee);
        request.setSpbillCreateIp(spBillCreateIP);
        request.setTimeStart(timeStart);
        request.setTimeExpire(timeExpire);
        request.setGoodsTag(goodsTag);
        request.setNotifyURL(notifyUrl);
        request.setTradeType(tradeType);
        request.setProductId(productId);
        request.setLimitPay(limitPay);
        request.setOpenid(openId);
        request.setSceneInfo(sceneInfo);
        String sign = PayDigestUtil.getSign(request,key);
        request.setSign(sign);


        return request;
    }


    /***
     * 查询微信订单
     * @param payOrder
     * @return
     */
    public Map queryWxPayOrderRequest(PayOrder payOrder) {
        String logPrefix = "【查询微信支付订单】";
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", payOrder.getMchOrderNo());
        if (payOrder.getStatus().intValue()==PayConstant.PAY_STATUS_SUCCESS){
            map.put("tradeState",PayConstant.RETURN_PAY_STATUS_SUCCESS);
            map.put("tradeStateDesc","支付成功");
            return RpcUtil.createBizResult(null, map);
        }

        try{
            PayOrder order = new PayOrder();
            order.setPayOrderId(payOrder.getPayOrderId());
            String mchId = payOrder.getMchId();
            String channelId = payOrder.getChannelId();
            PayChannel payChannel = super.baseSelectPayChannel(mchId, channelId);
            WxPayConfig wxPayConfig = WxPayUtil.getWxPayConfig(payChannel.getParam());
            WxPayService wxPayService = new WxPayServiceImpl();
            wxPayService.setConfig(wxPayConfig);
            String transactionId = null;
            //TODO h5支付使用自己的支付id订单号查询
            String outTradeNo = payOrder.getPayOrderId();
            if (StringUtils.isBlank(outTradeNo)){
                transactionId = payOrder.getChannelOrderNo();
            }
            try {
                WxPayOrderQueryResult wxPayOrderQueryResult = wxPayService.queryOrder(transactionId,outTradeNo);
                String tradeState = wxPayOrderQueryResult.getTradeState();
                log.info("{} >>> 查询订单单成功", logPrefix);
                String trade_state_desc = wxPayOrderQueryResult.getTradeStateDesc();
                JSONObject param2 = new JSONObject();
                param2.put("trade_state",tradeState);
                param2.put("trade_state_desc",trade_state_desc);

                byte status;
                String returnTradeState;
                if (tradeState.equalsIgnoreCase(PayConstant.WX_TRADESTATE_SUCCESS)){
                    status = PayConstant.PAY_STATUS_SUCCESS;
                    returnTradeState=PayConstant.RETURN_PAY_STATUS_SUCCESS;
                    trade_state_desc = "支付成功";
                    super.baseUpdateStatus(payOrder.getPayOrderId(), payOrder.getChannelOrderNo(),status,null,null,null);
                }else if (tradeState.equalsIgnoreCase(PayConstant.RETURN_PAY_STATUS_NOTPAY)){
                    returnTradeState=PayConstant.RETURN_PAY_STATUS_CANCEL;
                    trade_state_desc = "待支付";
                }else {
                    returnTradeState=PayConstant.RETURN_PAY_STATUS_FAIL;
                    trade_state_desc = "支付失败";
                }
                map.put("tradeState", returnTradeState);
                map.put("tradeStateDesc", trade_state_desc);
                return RpcUtil.createBizResult(null, map);
            } catch (WxPayException e) {
                map.put("tradeState", PayConstant.RETURN_PAY_STATUS_FAIL);
                map.put("tradeStateDesc", "支付失败");
                return RpcUtil.createBizResult(null, map);
            }
        }catch (Exception e) {
            log.error(e, "微信支付查询订单异常");
            return RpcUtil.createFailResult(null, RetEnum.RET_BIZ_WX_PAY_CREATE_FAIL);

        }
    }


}
