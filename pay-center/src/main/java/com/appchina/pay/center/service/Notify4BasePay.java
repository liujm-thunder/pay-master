package com.appchina.pay.center.service;

import com.alibaba.fastjson.JSONObject;
import com.appchina.pay.common.constant.PayConstant;
import com.appchina.pay.common.util.HttpClient;
import com.appchina.pay.common.util.MyLog;
import com.appchina.pay.common.util.PayDigestUtil;
import com.appchina.pay.common.util.PayUtil;
import com.appchina.pay.dao.model.MchInfo;
import com.appchina.pay.dao.model.PayOrder;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;
import org.apache.http.impl.client.HttpClients;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import com.appchina.pay.center.service.mq.Mq4PayNotify;

/**
 * @Description: 支付通知处理基类
 */
@Component
public class Notify4BasePay extends BaseService {

	private static final MyLog log = MyLog.getLog(Notify4BasePay.class);

//	@Autowired
//	private Mq4PayNotify mq4PayNotify;

	/**
	 * 创建响应URL
	 * @param payOrder
	 * @param backType 1：前台页面；2：后台接口
	 * @return
	 */
	public String createNotifyUrl(PayOrder payOrder, String backType) {
		String mchId = payOrder.getMchId();
		MchInfo mchInfo = super.baseSelectMchInfo(mchId);
		String resKey = mchInfo.getResKey();
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put("payOrderId", payOrder.getPayOrderId() == null ? "" : payOrder.getPayOrderId());           // 支付订单号
		paramMap.put("mchId", payOrder.getMchId() == null ? "" : payOrder.getMchId());                      	// 商户ID
		paramMap.put("mchOrderNo", payOrder.getMchOrderNo() == null ? "" : payOrder.getMchOrderNo());       	// 商户订单号
		paramMap.put("channelId", payOrder.getChannelId() == null ? "" : payOrder.getChannelId());              // 渠道ID
		paramMap.put("amount", payOrder.getAmount() == null ? "" : payOrder.getAmount());                      	// 支付金额
		paramMap.put("currency", payOrder.getCurrency() == null ? "" : payOrder.getCurrency());                 // 货币类型
		paramMap.put("status", payOrder.getStatus() == null ? "" : payOrder.getStatus());               		// 支付状态
		paramMap.put("clientIp", payOrder.getClientIp()==null ? "" : payOrder.getClientIp());   				// 客户端IP
		paramMap.put("device", payOrder.getDevice()==null ? "" : payOrder.getDevice());               			// 设备
		paramMap.put("subject", payOrder.getSubject()==null ? "" : payOrder.getSubject());     	   				// 商品标题
		paramMap.put("channelOrderNo", payOrder.getChannelOrderNo()==null ? "" : payOrder.getChannelOrderNo()); // 渠道订单号
		paramMap.put("param1", payOrder.getParam1()==null ? "" : payOrder.getParam1());               		   	// 扩展参数1
		paramMap.put("param2", payOrder.getParam2()==null ? "" : payOrder.getParam2());               		   	// 扩展参数2
		paramMap.put("paySuccTime", payOrder.getPaySuccTime()==null ? "" : payOrder.getPaySuccTime());			// 支付成功时间
		paramMap.put("backType", backType==null ? "" : backType);
		// 先对原文签名
		String reqSign = PayDigestUtil.getSign(paramMap, resKey);
		paramMap.put("sign", reqSign);   // 签名
		// 签名后再对有中文参数编码
		try {
			paramMap.put("device", URLEncoder.encode(payOrder.getDevice()==null ? "" : payOrder.getDevice(), PayConstant.RESP_UTF8));
			paramMap.put("subject", URLEncoder.encode(payOrder.getSubject()==null ? "" : payOrder.getSubject(), PayConstant.RESP_UTF8));
			paramMap.put("param1", URLEncoder.encode(payOrder.getParam1()==null ? "" : payOrder.getParam1(), PayConstant.RESP_UTF8));
			paramMap.put("param2", URLEncoder.encode(payOrder.getParam2()==null ? "" : payOrder.getParam2(), PayConstant.RESP_UTF8));
		}catch (UnsupportedEncodingException e) {
			log.error("URL Encode exception.", e);
			return null;
		}
		String param = PayUtil.genUrlParams(paramMap);
		StringBuffer sb = new StringBuffer();
		sb.append(payOrder.getNotifyUrl()).append("?").append(param);
		return sb.toString();
	}

	/**
	 * 处理支付结果前台页面跳转
	 */
	public boolean doPage(PayOrder payOrder) {
		String redirectUrl = createNotifyUrl(payOrder, "1");
		log.info("redirect to respUrl:"+redirectUrl);
		// 前台跳转业务系统
		/*try {
			response.sendRedirect(redirectUrl);
		} catch (IOException e) {
			log.error("XxPay sendRedirect exception. respUrl="+redirectUrl, e);
			return false;
		}*/
		return true;
	}

	/**
	 * 处理支付结果后台服务器通知
	 */
	public void doNotify(PayOrder payOrder) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				log.info(">>>>>> PAY开始回调通知业务系统 <<<<<<");
				// 发起后台通知业务系统
//				JSONObject object = createNotifyInfo(payOrder);
				try {
//			mq4PayNotify.send(object.toJSONString());
				String result = send(payOrder);
				log.info(">>>>>> PAY通知业务系统，结果：<<<<<< result ={} ",result);
				} catch (Exception e) {
					log.error("payOrderId={},sendMessage error.", payOrder != null ? payOrder.getPayOrderId() : "", e);
				}
				log.info(">>>>>> PAY回调通知业务系统完成 <<<<<<");
			}
		}).start();
	}

	public JSONObject createNotifyInfo(PayOrder payOrder) {
		JSONObject object = new JSONObject();
		object.put("method", "GET");
		object.put("url", createNotifyUrl(payOrder, "2"));
		object.put("orderId", payOrder.getPayOrderId());
		object.put("count", payOrder.getNotifyCount());
		object.put("createTime", System.currentTimeMillis());
		return object;
	}

	private String send(PayOrder payOrder) throws Exception{
		String url = payOrder.getNotifyUrl();
		String mchId = payOrder.getMchId();
		MchInfo mchInfo = super.baseSelectMchInfo(mchId);
		String resKey = mchInfo.getResKey();
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put("payOrderId", payOrder.getPayOrderId() == null ? "" : payOrder.getPayOrderId());           // 支付订单号
		paramMap.put("mchId", payOrder.getMchId() == null ? "" : payOrder.getMchId());                      	// 商户ID
		paramMap.put("mchOrderNo", payOrder.getMchOrderNo() == null ? "" : payOrder.getMchOrderNo());       	// 商户订单号
		paramMap.put("channelId", payOrder.getChannelId() == null ? "" : payOrder.getChannelId());              // 渠道ID
		paramMap.put("amount", payOrder.getAmount() == null ? "" : payOrder.getAmount());                      	// 支付金额
		paramMap.put("currency", payOrder.getCurrency() == null ? "" : payOrder.getCurrency());                 // 货币类型
		Byte status = payOrder.getStatus();
		String pay_status;
		if (status==2){
			pay_status = "SUCCESS";
		}else if (status==PayConstant.PAY_STATUS_PAYING){
			pay_status = "PAYING";
		}else if (status==PayConstant.PAY_STATUS_INIT){
			pay_status = "NOTPAY";
		}else {
			pay_status = "FAIL";
		}
		paramMap.put("status", pay_status);               		// 支付状态
		paramMap.put("clientIp", payOrder.getClientIp()==null ? "" : payOrder.getClientIp());   				// 客户端IP
		paramMap.put("device", payOrder.getDevice()==null ? "" : payOrder.getDevice());               			// 设备
		paramMap.put("subject", payOrder.getSubject()==null ? "" : payOrder.getSubject());     	   				// 商品标题
		paramMap.put("param1", payOrder.getParam1()==null ? "" : payOrder.getParam1());               		   	// 扩展参数1
		paramMap.put("param2", payOrder.getParam2()==null ? "" : payOrder.getParam2());               		   	// 扩展参数2
		// 先对原文签名
		String reqSign = PayDigestUtil.getSignRsaByKey(paramMap, resKey);
		paramMap.put("sign", reqSign);   // 签名
		String result  = sendPost(url,paramMap);
		return result;
	}

	private static CloseableHttpClient client =  HttpClients.createDefault();

	public static String sendPost(String reqUrl, Map<String, Object> params) throws Exception {
		HttpPost httpPost = new HttpPost(reqUrl);
		HttpResponse response = null;
		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			if (params != null) {
				for (Map.Entry<String, Object> entry : params.entrySet()) {
					String key = entry.getKey();
					Object value = entry.getValue();
					nameValuePairs.add(new BasicNameValuePair(key, String.valueOf(value)));
				}
			}
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));
			response = client.execute(httpPost);
			int statusCode = response.getStatusLine().getStatusCode();
			HttpEntity entity = response.getEntity();
			if (statusCode != HttpStatus.SC_OK) {
				throw new Exception("Server excepiton, the HTTP status code is " + statusCode + ", URL = " + reqUrl);
			}
			return EntityUtils.toString(entity);
		} catch (Exception e) {
			log.error("HTTP error. url = " + reqUrl + ", " + e.getMessage(), e);
			throw e;
		} finally {
			HttpClientUtils.closeQuietly(response);
			if (httpPost != null) {
				httpPost.abort();
			}
		}
	}

	public static void main(String[] args) {
		String pub = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDKBKBr0Tj61ouSGInnXFWNQZow" +
				"Fv3O97s4L015WXgEHb+72i3RGA0t2Q7cUcGLT2BYZxEraB0VmFhOdI4MR9PnvQ6D" +
				"21sObFY8er8m2ndLrQZEkRQsW97LKdMIfsQOr8bYE13vsZ/y8acv8jNxEiHm67y6" +
				"WjVG9BSkYaeoPFCMnQIDAQAB";
		String pri = "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAMoEoGvROPrWi5IY" +
				"iedcVY1BmjAW/c73uzgvTXlZeAQdv7vaLdEYDS3ZDtxRwYtPYFhnEStoHRWYWE50" +
				"jgxH0+e9DoPbWw5sVjx6vybad0utBkSRFCxb3ssp0wh+xA6vxtgTXe+xn/Lxpy/y" +
				"M3ESIebrvLpaNUb0FKRhp6g8UIydAgMBAAECgYABnX/a3NVjzAtZo7CWlPpqIrgk" +
				"4kotOXXZwScRbVG0VriNu/TZ0yNn1nBz+oNdpcjTbB+LBU4WOh2aovvvxTNAh3Gd" +
				"zWm0ZcqpMd+rULJH/RuoX4Pti+HlK2o7i2FoD4aZTndfO8mxnHFUbA8Py457KOcy" +
				"301VUR51i2ye5ydiAQJBAPI+PVRMMvAMz55/6NvYvRxAqFlRwbyiiNVvFsBgYd5T" +
				"V5UQKzIm4auJIUKqpOBjdhOJeK5SpzbHZZTq4PPbUn0CQQDVfZcfufE6TdQr8nKU" +
				"UA3jBWAKqnKIDsxwCo8+5uEgRiYUchEfQ57i+IFOE5339ckVlROAyt8Hq7Mhqg2+" +
				"jByhAkEAwAwx6QPkGkW90AXOIYVKH/zuuqlDc/5ThwpkOi3vSSg/tjC0XjVPEgRM" +
				"dyL8Rdz0fnatU165rIcWdKJlp07IrQJAfFJKcvtA8oboCz+AYcXMkGtM5mkjkP+t" +
				"JYHAsQyaBMVU34sdVWt3Vw0Hn4Pk9cR3eM37MYDyJ/FguzXgExpcgQJBAPBVDcSq" +
				"m6s35J4zrY7cgeozasOy9f5nhe9bEXmAIV9i4hVFAbMwV5uvtevpk5O0AAf251sa" +
				"9pq1DdKS2+xS1sY=";




		String s = "";
//		System.out.println(pub);
//		System.out.println(pri);


		String pubK = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDLGzmpTEVDFk+k+RUIb3u8bAPe" +
                "2ZMgEKPiLq3rhajmKV4jteVj3bIGW4enQXLn8H/0k45p4HmZewfT26jgem3F9XVm" +
                "c54vLgLeyfbs+OefngQIHMg1Tg06bHcvIme0XiOYtMFoNNHVMnnBjLfGTWO8O1cC" +
                "liiJNzwsezVe4XDsKQIDAQAB";
		String priK = "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAMsbOalMRUMWT6T5" +
                "FQhve7xsA97ZkyAQo+IureuFqOYpXiO15WPdsgZbh6dBcufwf/STjmngeZl7B9Pb" +
                "qOB6bcX1dWZzni8uAt7J9uz455+eBAgcyDVODTpsdy8iZ7ReI5i0wWg00dUyecGM" +
                "t8ZNY7w7VwKWKIk3PCx7NV7hcOwpAgMBAAECgYA2IdR1bdGL9tdVVdmoPOZSqstB" +
                "SuXuhuDW+K/79My2Q1JG3ET+H+lBzoVSK5xveubvjaBIUb63DFZivcm9woOc5EPG" +
                "z23M1QFSryz44TcmjWep8lUdOOja0gwtyAU4jAcLkOzgYPpXzW716J1zAweBWxEY" +
                "3QidlZeL2oR+pr+qXQJBAPc22aKgmvZjFNeacBoPQkgMR4eRz8azU+22NtV+25wP" +
                "xUCciSW0K9eyCumffmY+/ojPIh/yIQniXtKOsvkmpp8CQQDSUxUSdy1+qRmgDpqX" +
                "LQWyvQjLR0H8sAnzLsbY8xkFnoYN/5ib2U7mVwvPf3dPCKdw3UPQdVxnuXy57PXQ" +
                "qOA3AkAgVLkTzsNVc9HW/Kiqj9JQT+LO9R/iUbOpRApZ05RvDZTzhUVee/i75doN" +
                "gcFrJ9PsGoLRAL6XZ1aVXPpFIWvrAkA/NMadYQFkEg9oYVsl2VrgBx0Qcd6rwH+M" +
                "/F63rf60CJrCtDA5jcm/QSOEfQruzmv7aBNMHyjg5wZLnaGVzlprAkA3runwwsWE" +
                "6VEfIrBxEvQMknW6QspPv1uUC7zVJx2+pA5xLEMo34Dw+IsRVEA2elVnfZSYpv5E" +
                "46uCU4EMxk4H";

		System.out.println(pubK);
		System.out.println(priK);
	}
}
