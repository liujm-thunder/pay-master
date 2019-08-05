package com.appchina.pay.center.controller;

import com.alibaba.fastjson.JSONObject;
import com.appchina.pay.center.service.IMchInfoService;
import com.appchina.pay.center.service.IPayChannelService;
import com.appchina.pay.common.util.MyLog;
import com.appchina.pay.common.util.PayUtil;
import com.appchina.pay.common.util.Result;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*****
 * 支付渠道相关
 */
@RestController
public class PayChannelController {

    private final MyLog log = MyLog.getLog(PayChannelController.class);

    @Autowired
    private IMchInfoService mchInfoService;

    @Autowired
    private IPayChannelService payChannelService;

    @RequestMapping(value = "/api/pay/query_channel")
    public String queryOrderState(@RequestParam String params) {
        JSONObject po = JSONObject.parseObject(params);
        return doQueryOrderState(po);
    }


    @RequestMapping(value = "/api/pay/query_channel",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public String doQueryOrderState(@RequestParam JSONObject params){
        log.info("###### 开始接收支付渠道信息 ######");
        String logPrefix = "【查询订单】";
        try {
            JSONObject mchInfo = null;
            JSONObject payContext = new JSONObject();
            // 验证参数有效性
            Object object = validateParams(params,payContext);
            if (object instanceof String) {
                log.info("{}参数校验不通过:{}", logPrefix, object);
                return PayUtil.makeRetDataError(object.toString());
            }
            if (object instanceof  JSONObject) {
                mchInfo = (JSONObject) object;
            }
            if(mchInfo == null){
                return PayUtil.makeRetDataError(object.toString());
            }
            List<String> payChannels = payChannelService.selectPayChannelByMuchId(mchInfo.getString("mchId"));
            return PayUtil.makeRetDataRsa(Result.success(payChannels), payContext.getString("resKey"));
        }catch (Exception e) {
            log.error(e, "");
            return PayUtil.makeRetDataError( "支付中心系统异常");
        }
    }


    private Object validateParams(JSONObject params, JSONObject payContext){
        String errorMessage;
        String mchId = params.getString("mchId");
        String sign = params.getString("sign");

        if (StringUtils.isEmpty(mchId)) {
            errorMessage = "request params[mchId] error.";
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

        // 查询商户信息
        JSONObject mchInfo = mchInfoService.getByMchId(mchId);
        if(mchInfo == null) {
            errorMessage = "Can't found mchInfo[mchId="+mchId+",the mchInfo is not exist";
            return errorMessage;
        }
        if(mchInfo.getByte("state") != 1) {
            errorMessage = "the mchInfo is not available [mchId="+mchId+"],";
            return errorMessage;
        }
        payContext.put("resKey",makeSignPriKey);
        return mchInfo;
    }




}
