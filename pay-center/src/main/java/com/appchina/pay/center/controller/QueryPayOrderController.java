package com.appchina.pay.center.controller;

import com.alibaba.fastjson.JSONObject;
import com.appchina.pay.center.service.IMchInfoService;
import com.appchina.pay.center.service.IPayOrderService;
import com.appchina.pay.center.service.PreOrderService;
import com.appchina.pay.common.constant.PayConstant;
import com.appchina.pay.common.constant.PayEnum;
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
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
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
            return PayUtil.makeRetDataRsa(map, payContext.getString("resKey"));
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
        boolean verifyFlag = false;
        try {
            verifyFlag = PayUtil.verifyPaySignByRsa(params, reqKey);
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage = "Verify pay sign failed.";
            return errorMessage;
        }
        if(!verifyFlag) {
            errorMessage = "Verify pay sign failed.";
            return errorMessage;
        }

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
            JSONObject payContext = new JSONObject();
            // 验证参数有效性
            Object object = validateParamsForQueryOrderState(params,payContext);
            if (object instanceof String) {
                log.info("{}参数校验不通过:{}", logPrefix, object);
                return PayUtil.makeRetDataError(object.toString());
            }
            if (object instanceof  PayOrder) {
                payOrder = (PayOrder) object;
            }

            if(payOrder == null){
                return PayUtil.makeRetDataError(object.toString());
            }
            //todo 预留一个已过期状态
            Map<String, Object> map = new HashMap<>();
            map.put("orderId",payOrder.getPayOrderId());
            if (payOrder.getStatus()==PayConstant.PAY_STATUS_SUCCESS){
                map.put("tradeState", PayConstant.RETURN_PAY_STATUS_SUCCESS);
                map.put("tradeStateDesc", "支付成功");
                return PayUtil.makeRetDataRsa(Result.success(map), payContext.getString("resKey"));
            }else if (payOrder.getStatus()==PayConstant.PAY_STATUS_INIT||payOrder.getStatus()==PayConstant.PAY_STATUS_PAYING){
                if (payOrder.getChannelId().startsWith("ALIPAY")){
                    return payOrderService.queryAliPayOrderRequest(payOrder,payContext.getString("resKey"));
                }
                return payOrderService.queryWxPayOrderRequest(payOrder,payContext.getString("resKey"));
            }else{
                map.put("tradeState", PayConstant.RETURN_PAY_STATUS_FAIL);
                map.put("tradeStateDesc", "支付失败");
                return PayUtil.makeRetDataRsa(Result.success(map), payContext.getString("resKey"));
            }
        }catch (Exception e) {
            log.error(e, "");
            return PayUtil.makeRetDataError( "支付中心系统异常");
        }
    }

    private Object validateParamsForQueryOrderState(JSONObject params,JSONObject payContext){
        String errorMessage;
        String payOrderId = params.getString("payOrderId");
        String sign = params.getString("sign");

        if (StringUtils.isEmpty(payOrderId)) {
            errorMessage = "request params[payOrderId] error.";
            return errorMessage;
        }
        if (StringUtils.isEmpty(sign)) {
            errorMessage = "request params[sign] error.";
            return errorMessage;
        }

        //验证签名公钥
        String PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDsJGt7HfAOzfjCzE6iIgBazjvv" +
                "/hRwfnVwIUGIl39h+/FvBVIrIvLBUenCgFGf09vDClkigNHGfzKRo019V6Mqm1E1" +
                "D8B+m92t1lEnBnjVU6e+6P5zT6ml/ahHgWHw3XW6RVy+NabJF26VN2ogG1SBuqDa" +
                "JFQ4kL0xr7piJbtUbQIDAQAB";

        String PRIVATE_KEY = "MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAOwka3sd8A7N+MLM" +
                "TqIiAFrOO+/+FHB+dXAhQYiXf2H78W8FUisi8sFR6cKAUZ/T28MKWSKA0cZ/MpGj" +
                "TX1XoyqbUTUPwH6b3a3WUScGeNVTp77o/nNPqaX9qEeBYfDddbpFXL41pskXbpU3" +
                "aiAbVIG6oNokVDiQvTGvumIlu1RtAgMBAAECgYEAj7Ll+QjR0aCDtb7wRveb8aY4" +
                "kSWzuHUr7+083OscKDtRw3agdwGQahX3w1Wk1jbtL7Y3Yai0fy9eTYPrns/ayOJ1" +
                "rUKmnFjnD0YgitMu+JNWdg/B2lX+ucHvucVK0mkbnmlY3TQFSRU6o/dDjgIdwIl4" +
                "hdxaheA+fnwAMdUXWcECQQD3blLkss7nfVBXgDiKaXri0BsBvA8gHGNfLjTfEIdP" +
                "lZsxmXzrgtn5Fu30aWfkcQn8IzDBPER47Fjp6xhVEf2RAkEA9FIDPmHXOUMPl453" +
                "84o5BwRSLjDff2EZMuPCOM9YFPVxOqDqbv1AfXgBHeqVtRvZUcHlAIT6+c3dFu4O" +
                "X1ZrHQJBAM+/T9Y508NF0llFjTOZ0NXziVlxfvmlHEJkV3wbMqE9qeqBRwOvADlG" +
                "aVDX16VUy99p5Ju6cHtfZmAxRmLXEiECQCryP5+3kx19rD/3yx4ELgINwGReMusx" +
                "JjzLzFgwGkuU2VJ09sCLw8pKTef0VFyBiLHWY2qz9WnzxelB70TS7AECQQCH+Uo4" +
                "WpRumb1PNnLc5yrjxXnNBSTRzxdjqArayBN2hq9R7O1iTPY1vrqCW3pGiI2J4Gqc" +
                "aPf0pfK7EZtkk06Y";//私钥



        //生成签名私钥
        String makeSignPriKey = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAMSCw3NmZ9k+0U1a" +
                "cCqmzpMaKoQ3Av6Fb3+sqAFJAK7VgufBdm/gLMCBCtYXOgv9RwCkqc/HSH2wYdI/" +
                "KR9H3Fu51/nkKglS1e4TPafxPo/lGC7GG/BgdV1++xld86EqE1Dwm1l7nrjsoMGO" +
                "VAE+Ghd2VcvBxaq7y5ipEkTFKsIXAgMBAAECgYEAsowFPiL7lF4JGflkFLy+0NVj" +
                "cAHzzII8zop3k8NaxX/lkuEq1Xef8cDNsbwk16PnEWSLjegJq3nJR5hvqqZGRdGU" +
                "L6xGDBrprV3ngU0eh+T74TAU7SNyYubs697CZvBpDnkh6U8D7YbZIo7hH9GSasz1" +
                "D18C7W82Nim7/4hGzCECQQDmxY8WDnWbfzKbDY77XoZF7h1h666qGO8MeUzyACKu" +
                "gu3sfaTqq+L4tBJYgNOKUFgoaVwRKWhCS982KWek35avAkEA2f5edLG4din1khUe" +
                "gPR0VHQvc5Z15yrnNvbCUefK/wz5Ra+MpgJjTp4Y/wmNQNXKwpiF7M50hneBzN+6" +
                "Eb5lGQJAZ9IBaX7f0jELZ05WQShpaBSUC3WogsXs5cO8pjMBZ1loCLkN9LWXyyPY" +
                "DREIGnXC84tS7DWgvhK8PPWrtzUP6QJAeVosNOQWXtle1lKhZ4IuHDGNlNgGjIiK" +
                "rENTy4qwq6kKPyvJrUSZCdPi8F7d3mDlfcywiTIpFg4DGQzWpTgLSQJAdu6WXyoj" +
                "V7XGgrPkkGmCvM5yBbJPpXcjkZYQR+EYk9jk1ExEU1iFpUsh/bskO+VaCkmgTvBZ" +
                "OcV7ed0F/KvVxw==";
        String pubKey2Client = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDEgsNzZmfZPtFNWnAqps6TGiqE" +
                "NwL+hW9/rKgBSQCu1YLnwXZv4CzAgQrWFzoL/UcApKnPx0h9sGHSPykfR9xbudf5" +
                "5CoJUtXuEz2n8T6P5RguxhvwYHVdfvsZXfOhKhNQ8JtZe5647KDBjlQBPhoXdlXL" +
                "wcWqu8uYqRJExSrCFwIDAQAB";
        try {
            boolean verifyFlag = PayUtil.verifyPaySignByRsa(params,PUBLIC_KEY);
            if(!verifyFlag) {
                errorMessage = "sign verify failed.";
                return errorMessage;
            }
        }catch (Exception e){
            errorMessage = "sign verify failed.";
            return errorMessage;
        }
        payContext.put("resKey",makeSignPriKey);
        return payOrderService.queryPayOrderByPayOrderId(payOrderId);
    }

}
