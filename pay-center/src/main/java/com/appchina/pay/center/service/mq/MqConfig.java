package com.appchina.pay.center.service.mq;

import org.springframework.context.annotation.Configuration;


/**
 * @Copyright: www.xxpay.org
 */
@Configuration
public class MqConfig {

    public static final String PAY_NOTIFY_QUEUE_NAME = "pay.notify.queue";
    
    public static final String PAY_NOTIFY_EXCHANGE_NAME = "pay.notify.exchange";
    
    public static class Impl{
    	public static final String ACTIVE_MQ = "activeMQ";
    	public static final String RABBIT_MQ = "rabbitMQ";
    }
    
}
