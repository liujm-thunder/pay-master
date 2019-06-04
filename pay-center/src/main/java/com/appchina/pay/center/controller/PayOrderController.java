package com.appchina.pay.center.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.appchina.pay.center.service.IMchInfoService;
import com.appchina.pay.center.service.IPayChannelService;
import com.appchina.pay.center.service.IPayOrderService;
import com.appchina.pay.center.service.PreOrderService;
import com.appchina.pay.common.constant.PayConstant;
import com.appchina.pay.common.model.Result;
import com.appchina.pay.common.util.*;
import com.appchina.pay.dao.model.GoodsOrder;
import com.appchina.pay.dao.model.PayOrder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * @Description: 支付订单,包括:统一下单,订单查询,补单等接口
 */
@RestController
public class PayOrderController {

    private final MyLog log = MyLog.getLog(PayOrderController.class);

    @Autowired
    private IPayOrderService payOrderService;

    @Autowired
    private IPayChannelService payChannelService;

    @Autowired
    private IMchInfoService mchInfoService;

    @Autowired
    private PreOrderService preOrderService;

    /**
     * 统一下单接口:
     * 1)先验证接口参数以及签名信息
     * 2)验证通过创建支付订单
     * 3)根据商户选择渠道,调用支付服务进行下单
     * 4)返回下单数据
     * @param params
     * @return
     */
    @RequestMapping(value = "/api/pay/create_order")
    public String payOrder(@RequestParam String params) {
    	JSONObject po = JSONObject.parseObject(params);
        return payOrder(po);
    }
    
    @RequestMapping(value = "/api/pay/create_order", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public String payOrder(@RequestBody JSONObject params) {
    	log.info("###### 开始接收商户统一下单请求 ######");
        String logPrefix = "【商户统一下单】";
        try {
            JSONObject payContext = new JSONObject();
            JSONObject payOrder = null;
            // 验证参数有效性
            Object object = validateParams(params, payContext);
            if (object instanceof String) {
                log.info("{}参数校验不通过:{}", logPrefix, object);
                return PayUtil.makeRetFail(PayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, object.toString(), null, null));
            }
            if (object instanceof JSONObject) payOrder = (JSONObject) object;
            if(payOrder == null) return PayUtil.makeRetFail(PayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, "支付中心下单失败", null, null));
            //自主生成 订单号
            SnowflakeIdWorker idWorker = new SnowflakeIdWorker(0, 0);// 支付订单号
            String mchOrderNo = String.valueOf(idWorker.nextId());
            payOrder.put("mchOrderNo",mchOrderNo);
            int result = payOrderService.createPayOrder(payOrder);
            log.info("{}创建支付订单,结果:{}", logPrefix, result);
            if(result != 1) {
                return PayUtil.makeRetFail(PayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, "创建支付订单失败", null, null));
            }
            String channelId = payOrder.getString("channelId");
            switch (channelId) {
                case PayConstant.PAY_CHANNEL_WX_APP :
                    return payOrderService.doWxPayReq(PayConstant.WxConstant.TRADE_TYPE_APP, payOrder, payContext.getString("resKey"));
                case PayConstant.PAY_CHANNEL_WX_JSAPI :
                    return payOrderService.doWxPayReq(PayConstant.WxConstant.TRADE_TYPE_JSPAI, payOrder, payContext.getString("resKey"));
                case PayConstant.PAY_CHANNEL_WX_NATIVE :
                    return payOrderService.doWxPayReq(PayConstant.WxConstant.TRADE_TYPE_NATIVE, payOrder, payContext.getString("resKey"));
                case PayConstant.PAY_CHANNEL_WX_MWEB :
                    return payOrderService.doWxPayReq(PayConstant.WxConstant.TRADE_TYPE_MWEB, payOrder, payContext.getString("resKey"));
                case PayConstant.PAY_CHANNEL_ALIPAY_MOBILE :
                    return payOrderService.doAliPayReq(channelId, payOrder, payContext.getString("resKey"));
                case PayConstant.PAY_CHANNEL_ALIPAY_PC :
                    return payOrderService.doAliPayReq(channelId, payOrder, payContext.getString("resKey"));
                case PayConstant.PAY_CHANNEL_ALIPAY_WAP :
                    return payOrderService.doAliPayReq(channelId, payOrder, payContext.getString("resKey"));
                case PayConstant.PAY_CHANNEL_ALIPAY_QR :
                    return payOrderService.doAliPayReq(channelId, payOrder, payContext.getString("resKey"));
                default:
                    return PayUtil.makeRetFail(PayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, "不支持的支付渠道类型[channelId="+channelId+"]", null, null));
            }
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
    private Object validateParams(JSONObject params, JSONObject payContext) {
        // 验证请求参数,参数有问题返回错误提示
        String errorMessage;
        // 支付参数
        String mchId = params.getString("mchId"); 			    // 商户ID
        String mchOrderNo = params.getString("mchOrderNo"); 	// 商户订单号
        String channelId = params.getString("channelId"); 	    // 渠道ID
        String amount = params.getString("amount"); 		    // 支付金额（单位分）
        String currency = params.getString("currency");         // 币种
        String clientIp = params.getString("clientIp");	        // 客户端IP
        String device = params.getString("device"); 	        // 设备
        String extra = params.getString("extra");		        // 特定渠道发起时额外参数
        String param1 = params.getString("param1"); 		    // 扩展参数1
        String param2 = params.getString("param2"); 		    // 扩展参数2
        String notifyUrl = params.getString("notifyUrl"); 		// 支付结果回调URL
        String sign = params.getString("sign"); 				// 签名
        String subject = params.getString("subject");	        // 商品主题
        String body = params.getString("body");	                // 商品描述信息
        // 验证请求参数有效性（必选项）
        if(StringUtils.isBlank(mchId)) {
            errorMessage = "request params[mchId] error.";
            return errorMessage;
        }
        if(StringUtils.isBlank(mchOrderNo)) {
            errorMessage = "request params[mchOrderNo] error.";
            return errorMessage;
        }
        if(StringUtils.isBlank(channelId)) {
            errorMessage = "request params[channelId] error.";
            return errorMessage;
        }
        if(!NumberUtils.isNumber(amount)) {
            errorMessage = "request params[amount] error.";
            return errorMessage;
        }
        if(StringUtils.isBlank(currency)) {
            errorMessage = "request params[currency] error.";
            return errorMessage;
        }
        if(StringUtils.isBlank(notifyUrl)) {
            errorMessage = "request params[notifyUrl] error.";
            return errorMessage;
        }
        if(StringUtils.isBlank(subject)) {
            errorMessage = "request params[subject] error.";
            return errorMessage;
        }
        if(StringUtils.isBlank(body)) {
            errorMessage = "request params[body] error.";
            return errorMessage;
        }
        // 根据不同渠道,判断extra参数
        if(PayConstant.PAY_CHANNEL_WX_JSAPI.equalsIgnoreCase(channelId)) {
            if(StringUtils.isEmpty(extra)) {
                errorMessage = "request params[extra] error.";
                return errorMessage;
            }
            JSONObject extraObject = JSON.parseObject(extra);
            String openId = extraObject.getString("openId");
            if(StringUtils.isBlank(openId)) {
                errorMessage = "request params[extra.openId] error.";
                return errorMessage;
            }
        }else if(PayConstant.PAY_CHANNEL_WX_NATIVE.equalsIgnoreCase(channelId)) {
            if(StringUtils.isEmpty(extra)) {
                errorMessage = "request params[extra] error.";
                return errorMessage;
            }
            JSONObject extraObject = JSON.parseObject(extra);
            String productId = extraObject.getString("productId");
            if(StringUtils.isBlank(productId)) {
                errorMessage = "request params[extra.productId] error.";
                return errorMessage;
            }
        }else if(PayConstant.PAY_CHANNEL_WX_MWEB.equalsIgnoreCase(channelId)) {
            if(StringUtils.isEmpty(extra)) {
                errorMessage = "request params[extra] error.";
                return errorMessage;
            }
            JSONObject extraObject = JSON.parseObject(extra);
            String sceneInfo = extraObject.getString("sceneInfo");
            if(StringUtils.isBlank(sceneInfo)) {
                errorMessage = "request params[extra.sceneInfo] error.";
                return errorMessage;
            }
            if(StringUtils.isBlank(clientIp)) {
                errorMessage = "request params[clientIp] error.";
                return errorMessage;
            }
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

        // 查询商户对应的支付渠道
        JSONObject payChannel = payChannelService.getByMchIdAndChannelId(mchId, channelId);
        if(payChannel == null) {
            errorMessage = "Can't found payChannel[channelId="+channelId+",mchId="+mchId+"] record in db.";
            return errorMessage;
        }
        if(payChannel.getByte("state") != 1) {
            errorMessage = "channel not available [channelId="+channelId+",mchId="+mchId+"]";
            return errorMessage;
        }

        // TODO 验证签名数据
        boolean verifyFlag = PayUtil.verifyPaySign(params, reqKey);
//        if(!verifyFlag) {
//            errorMessage = "Verify pay sign failed.";
//            return errorMessage;
//        }
        // 验证参数通过,返回JSONObject对象
        JSONObject payOrder = new JSONObject();
        payOrder.put("payOrderId", MySeq.getPay());
        payOrder.put("mchId", mchId);
        payOrder.put("mchOrderNo", mchOrderNo);
        payOrder.put("channelId", channelId);
        payOrder.put("amount", Long.parseLong(amount));
        payOrder.put("currency", currency);
        payOrder.put("clientIp", clientIp);
        payOrder.put("device", device);
        payOrder.put("subject", subject);
        payOrder.put("body", body);
        payOrder.put("extra", extra);
        payOrder.put("channelMchId", payChannel.getString("channelMchId"));
        payOrder.put("param1", param1);
        payOrder.put("param2", param2);
        payOrder.put("notifyUrl", notifyUrl);
        return payOrder;
    }


    @RequestMapping(value = "/api/pay/pre/create_order")
    public String preOrder(@RequestParam String params) {
        JSONObject po = JSONObject.parseObject(params);
        return preOrder(po);
    }

    @RequestMapping(value = "/api/pay/pre/create_order",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public String preOrder(@RequestBody JSONObject params){
        log.info("###### 开始接收商户统一预下单请求 ######");
        String logPrefix = "【商户统一预下单】";
        GoodsOrder preOrder = null;
        // 验证参数有效性
        Object object = validateParams(params);
        if (object != null && object instanceof String) {
            log.info("{}参数校验不通过:{}", logPrefix, object);
            PayUtil.makeRetFail(PayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, object.toString(), null, null));
        }
        if (object instanceof GoodsOrder) preOrder = (GoodsOrder) object;
        if (preOrder == null){
            return PayUtil.makeRetFail(PayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, "订单生成失败，请查看参数信息", null, null));
        }
        //自主生成 订单号
        SnowflakeIdWorker idWorker = new SnowflakeIdWorker(0, 0);// 预订单号
        String goodsOrderId = String.valueOf(idWorker.nextId());
        preOrder.setGoodsOrderId(goodsOrderId);
        return preOrderService.createPreOrderInfo(preOrder,params);
    }


    private Object validateParams(JSONObject params){
        String mchId = params.getString("mchId"); 			    // 商户ID
        String goodsId = params.getString("goodsId"); 	// 商品ID
        String goodsName = params.getString("goodsName"); 	    // 商品名称
        String amount = params.getString("amount"); 		    // 支付金额（单位分）
        String sign = params.getString("sign"); 				// 签名
//        String subject = params.getString("subject");	        // 商品主题
//        String body = params.getString("body");// 商品描述

        String errorMessage;
        if(StringUtils.isBlank(mchId)) {
            errorMessage = "request params[mchId] error.";
            return errorMessage;
        }

        if(StringUtils.isBlank(goodsId)) {
            errorMessage = "request params[goodsId] error.";
            return errorMessage;
        }

        if(StringUtils.isBlank(goodsName)) {
            errorMessage = "request params[goodsName] error.";
            return errorMessage;
        }

        if(StringUtils.isBlank(amount)) {
            errorMessage = "request params[amount] error.";
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
        // TODO 验证签名数据
        boolean verifyFlag = PayUtil.verifyPaySign(params, reqKey);
//        if(!verifyFlag) {
//            errorMessage = "Verify pay sign failed.";
//            return errorMessage;
//        }

        GoodsOrder goodsOrder = new GoodsOrder();
        goodsOrder.setGoodsId(goodsId);
        goodsOrder.setGoodsName(goodsName);
        goodsOrder.setAmount(Long.parseLong(amount));
        goodsOrder.setUserId(mchId);
        goodsOrder.setStatus(PayConstant.PAY_STATUS_INIT);
        Date date = new Date();
        goodsOrder.setCreateTime(date);
        goodsOrder.setUpdateTime(date);
        return goodsOrder;
    }


    @RequestMapping(value = "/api/pay/do/create_order")
    public String preOrder(@RequestParam String params, HttpServletRequest request) {
        JSONObject po = JSONObject.parseObject(params);
        return doCreatePayOrder(po,request);
    }


    @RequestMapping(value = "/api/pay/do/create_order",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public String doCreatePayOrder(@RequestParam JSONObject params, HttpServletRequest request){
        log.info("###### 开始接收商户统一下单请求 ######");
        String logPrefix = "【商户统一下单】";
        try {
            JSONObject payContext = new JSONObject();
            JSONObject payOrder = null;
            // 验证参数有效性
            Object object = validateParamsForDoCreateOrder(params,request,payContext);
            if (object instanceof String) {
                log.info("{}参数校验不通过:{}", logPrefix, object);
                PayUtil.makeRetFail(PayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, object.toString(), null, null));
            }
            if (object instanceof JSONObject) payOrder = (JSONObject) object;
            if(payOrder == null) return PayUtil.makeRetFail(PayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, "支付中心下单失败", null, null));
            GoodsOrder goodsOrder = preOrderService.getPayOrder(payOrder.getString("mchOrderNo"));
            if (goodsOrder!=null && goodsOrder.getStatus()==PayConstant.PAY_STATUS_SUCCESS){
                return PayUtil.makeRetFail(PayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, "该订单已支付，请勿重复支付", null, null));
            }
            int result;
            try {
                result = payOrderService.createPayOrder(payOrder);
            }catch (Exception e){
                log.error("", "{}创建订单失败{}",logPrefix,params.toJSONString());
                return PayUtil.makeRetFail(PayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, "创建订单失败，请重新创建订单", null, null));
            }
            log.info("{}创建支付订单,结果:{}", logPrefix, result);
            if(result != 1) {
                return PayUtil.makeRetFail(PayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, "创建支付订单失败", null, null));
            }
            String channelId = payOrder.getString("channelId");
            switch (channelId) {
                case PayConstant.PAY_CHANNEL_WX_APP :
                    return payOrderService.doWxPayRequest(PayConstant.WxConstant.TRADE_TYPE_APP, payOrder, payContext.getString("resKey"));
                case PayConstant.PAY_CHANNEL_WX_JSAPI :
                    return payOrderService.doWxPayRequest(PayConstant.WxConstant.TRADE_TYPE_JSPAI, payOrder, payContext.getString("resKey"));
                case PayConstant.PAY_CHANNEL_WX_NATIVE :
                    return payOrderService.doWxPayRequest(PayConstant.WxConstant.TRADE_TYPE_NATIVE, payOrder, payContext.getString("resKey"));
                case PayConstant.PAY_CHANNEL_WX_MWEB :
                    return payOrderService.doWxPayRequest(PayConstant.WxConstant.TRADE_TYPE_MWEB, payOrder, payContext.getString("resKey"));
                case PayConstant.PAY_CHANNEL_ALIPAY_MOBILE :
                    return payOrderService.doAliPayReq(channelId, payOrder, payContext.getString("resKey"));
                case PayConstant.PAY_CHANNEL_ALIPAY_PC :
                    return payOrderService.doAliPayReq(channelId, payOrder, payContext.getString("resKey"));
                case PayConstant.PAY_CHANNEL_ALIPAY_WAP :
                    return payOrderService.doAliPayReq(channelId, payOrder, payContext.getString("resKey"));
                case PayConstant.PAY_CHANNEL_ALIPAY_QR :
                    return payOrderService.doAliPayReq(channelId, payOrder, payContext.getString("resKey"));
                default:
                    return PayUtil.makeRetFail(PayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, "不支持的支付渠道类型[channelId="+channelId+"]", null, null));
            }
        }catch (Exception e) {
            log.error(e, "");
            return PayUtil.makeRetFail(PayUtil.makeRetMap(PayConstant.RETURN_VALUE_FAIL, "支付中心系统异常", null, null));
        }
    }

    private Object validateParamsForDoCreateOrder(JSONObject params, HttpServletRequest request, JSONObject payContext){
        String errorMessage;
        String preOrderId = params.getString("preOrderId");
        String extra = params.getString("extra");
        String channelId = params.getString("channelId");
        String sign = params.getString("sign");


        if(StringUtils.isBlank(preOrderId)) {
            errorMessage = "request params[preOrderId] error.";
            return errorMessage;
        }

        if(StringUtils.isBlank(channelId)) {
            errorMessage = "request params[goodsOrderId] error.";
            return errorMessage;
        }

        // 根据不同渠道,判断extra参数
        if(PayConstant.PAY_CHANNEL_WX_JSAPI.equalsIgnoreCase(channelId)) {
            if(StringUtils.isEmpty(extra)) {
                errorMessage = "request params[extra] error.";
                return errorMessage;
            }
            JSONObject extraObject = JSON.parseObject(extra);
            String openId = extraObject.getString("openId");
            if(StringUtils.isBlank(openId)) {
                errorMessage = "request params[extra.openId] error.";
                return errorMessage;
            }
        }else if(PayConstant.PAY_CHANNEL_WX_NATIVE.equalsIgnoreCase(channelId)) {
            if(StringUtils.isEmpty(extra)) {
                errorMessage = "request params[extra] error.";
                return errorMessage;
            }
            JSONObject extraObject = JSON.parseObject(extra);
            String productId = extraObject.getString("productId");
            if(StringUtils.isBlank(productId)) {
                errorMessage = "request params[extra.productId] error.";
                return errorMessage;
            }
        }else if(PayConstant.PAY_CHANNEL_WX_MWEB.equalsIgnoreCase(channelId)) {
            if(StringUtils.isEmpty(extra)) {
                errorMessage = "request params[extra] error.";
                return errorMessage;
            }
            //TODO 暂时还不确定谁传
//            JSONObject extraObject = JSON.parseObject(extra);
//            String sceneInfo = extraObject.getString("sceneInfo");
//            if(StringUtils.isBlank(sceneInfo)) {
//                errorMessage = "request params[extra.sceneInfo] error.";
//                return errorMessage;
//            }

        }

        String clientIp = IpHelper.getIpAddr(request);
        if(StringUtils.isBlank(clientIp)) {
            errorMessage = "request params[clientIp] error.";
            return errorMessage;
        }
        //TODO 查询预订单
        GoodsOrder goodsOrder =  preOrderService.getPayOrder(preOrderId);
        if (goodsOrder==null){
            errorMessage = "没有此预订单 [preOrderId="+preOrderId+"].";
            return errorMessage;
        }
        if(goodsOrder.getStatus() == 1) {
            errorMessage = "订单已已经支付完成 [preOrderId="+preOrderId+"].";
            return errorMessage;
        }

        if(goodsOrder.getStatus() != 0) {
            errorMessage = "订单处理失败 [preOrderId="+preOrderId+"].";
            return errorMessage;
        }

        // 查询商户信息
        JSONObject mchInfo = mchInfoService.getByMchId(goodsOrder.getUserId());
        if(mchInfo == null) {
            errorMessage = "没有该商户信息,mchInfo[mchId="+goodsOrder.getUserId()+"].";
            return errorMessage;
        }

        if(mchInfo.getByte("state") != 1) {
            errorMessage = "该商户信息已失效 [mchId="+goodsOrder.getUserId()+"].";
            return errorMessage;
        }

        String reqKey = mchInfo.getString("reqKey");
        if (StringUtils.isBlank(reqKey)) {
            errorMessage = "reqKey is null[mchId="+goodsOrder.getUserId()+"].";
            return errorMessage;
        }
        payContext.put("resKey", mchInfo.getString("resKey"));

        // 查询商户对应的支付渠道
        JSONObject payChannel = payChannelService.getByMchIdAndChannelId(goodsOrder.getUserId(), channelId);
        if(payChannel == null) {
            errorMessage = "Can't found payChannel[channelId="+channelId+",mchId="+goodsOrder.getUserId()+"] record in db.";
            return errorMessage;
        }

        if(payChannel.getByte("state") != 1) {
            errorMessage = "channel not available [channelId="+channelId+",mchId="+goodsOrder.getUserId()+"]";
            return errorMessage;
        }

        //TODO 暂时写死 需要再确认who传这个参数
        JSONObject jsonObject = new JSONObject();
        JSONObject jsonObject1 = new JSONObject();
        JSONObject jsonObject2 = new JSONObject();
        jsonObject2.put("type","Android");
        jsonObject2.put("app_name","王者荣耀");
        jsonObject2.put("package_name","com.tencent.tmgp.sgame");
        jsonObject1.put("h5_info",jsonObject2);
        jsonObject.put("sceneInfo",jsonObject1);

        // 验证参数通过,返回JSONObject对象
        JSONObject payOrder = new JSONObject();
        payOrder.put("payOrderId", MySeq.getPay());
        payOrder.put("mchId", mchInfo.getString("mchId"));
        payOrder.put("mchOrderNo", preOrderId);
        payOrder.put("channelId", channelId);
        payOrder.put("amount", goodsOrder.getAmount());
        payOrder.put("currency", "cny");
        payOrder.put("clientIp", clientIp);
        payOrder.put("device", "");
        payOrder.put("subject", goodsOrder.getGoodsName());
        payOrder.put("body", "测试商品描述");
        payOrder.put("extra", jsonObject);
        payOrder.put("channelMchId", payChannel.getString("channelMchId"));
        payOrder.put("param1", "");
        payOrder.put("param2", "");
        payOrder.put("notifyUrl", "");
        return payOrder;
    }

}
