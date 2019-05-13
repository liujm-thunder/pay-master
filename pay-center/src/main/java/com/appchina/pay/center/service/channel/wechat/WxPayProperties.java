package com.appchina.pay.center.service.channel.wechat;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
@ConfigurationProperties(prefix="config.wx")
public class WxPayProperties {

	private String certRootPath;

	private String notifyUrl;

	public String getCertRootPath() {
		return certRootPath;
	}

	public void setCertRootPath(String certRootPath) {
		this.certRootPath = certRootPath;
	}

	public String getNotifyUrl() {
		return notifyUrl;
	}

	public void setNotifyUrl(String notifyUrl) {
		this.notifyUrl = notifyUrl;
	}
}
