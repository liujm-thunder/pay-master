package com.appchina.pay.center.controller;

import com.appchina.pay.center.service.INotifyPayService;
import com.appchina.pay.common.util.MyLog;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @Description: 接收处理微信通知
 */
@RestController
public class NotifyWxPayController {

	private static final MyLog log = MyLog.getLog(NotifyWxPayController.class);

	@Autowired
	private INotifyPayService notifyPayService;

	/**
	 * 微信支付(统一下单接口)后台通知响应
	 * @param request
	 * @return
	 * @throws ServletException
	 * @throws IOException
     */
	@RequestMapping("/wechat/notify")
	@ResponseBody
	public String wxPayNotifyRes(HttpServletRequest request) throws ServletException, IOException {
		log.info("====== 开始接收微信支付回调通知 ======");
		String notifyRes = doWxPayRes(request);
		log.info("响应给微信:{}", notifyRes);
		log.info("====== 完成接收微信支付回调通知 ======");
		return notifyRes;
	}

	public String doWxPayRes(HttpServletRequest request) throws ServletException, IOException {
		String logPrefix = "【微信支付回调通知】";
		String xmlResult = IOUtils.toString(request.getInputStream(), request.getCharacterEncoding());
		log.info("{}通知请求数据:reqStr={}", logPrefix, xmlResult);
		return notifyPayService.handleWxPayNotify(xmlResult);
	}

}
