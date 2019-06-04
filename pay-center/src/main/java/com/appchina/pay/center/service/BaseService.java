package com.appchina.pay.center.service;

import com.appchina.pay.common.constant.PayConstant;
import com.appchina.pay.dao.mapper.GoodsOrderMapper;
import com.appchina.pay.dao.mapper.MchInfoMapper;
import com.appchina.pay.dao.mapper.PayChannelMapper;
import com.appchina.pay.dao.mapper.PayOrderMapper;
import com.appchina.pay.dao.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;


import java.util.Date;
import java.util.List;


@Service
public class BaseService {

    @Autowired
    private PayOrderMapper payOrderMapper;

    @Autowired
    private MchInfoMapper mchInfoMapper;

    @Autowired
    private PayChannelMapper payChannelMapper;

    @Autowired
    private GoodsOrderMapper goodsOrderMapper;


    public MchInfo baseSelectMchInfo(String mchId) {
        return mchInfoMapper.selectByPrimaryKey(mchId);
    }


    public PayChannel baseSelectPayChannel(String mchId, String channelId) {
        PayChannelExample example = new PayChannelExample();
        PayChannelExample.Criteria criteria = example.createCriteria();
        criteria.andChannelIdEqualTo(channelId);
        criteria.andMchIdEqualTo(mchId);
        List<PayChannel> payChannelList = payChannelMapper.selectByExample(example);
        if(CollectionUtils.isEmpty(payChannelList)) return null;
        return payChannelList.get(0);
    }

    public List<PayChannel> baseSelectPayChannels(String mchId) {
        PayChannelExample example = new PayChannelExample();
        PayChannelExample.Criteria criteria = example.createCriteria();
        criteria.andMchIdEqualTo(mchId);
        List<PayChannel> payChannelList = payChannelMapper.selectByExample(example);
        if(CollectionUtils.isEmpty(payChannelList)) return null;
        return payChannelList;
    }

    public int baseCreatePayOrder(PayOrder payOrder) {
        payOrder.setCreateTime(new Date());
        return payOrderMapper.insertSelective(payOrder);
    }

    public PayOrder baseSelectPayOrder(String payOrderId) {
        return payOrderMapper.selectByPrimaryKey(payOrderId);
    }

    public PayOrder baseSelectPayOrderByMchIdAndPayOrderId(String mchId, String payOrderId) {
        PayOrderExample example = new PayOrderExample();
        PayOrderExample.Criteria criteria = example.createCriteria();
        criteria.andMchIdEqualTo(mchId);
        criteria.andPayOrderIdEqualTo(payOrderId);
        List<PayOrder> payOrderList = payOrderMapper.selectByExample(example);
        return CollectionUtils.isEmpty(payOrderList) ? null : payOrderList.get(0);
    }

    public PayOrder baseSelectPayOrderByMchIdAndMchOrderNo(String mchId, String mchOrderNo) {
        PayOrderExample example = new PayOrderExample();
        PayOrderExample.Criteria criteria = example.createCriteria();
        criteria.andMchIdEqualTo(mchId);
        criteria.andMchOrderNoEqualTo(mchOrderNo);
        example.setOrderByClause("createTime DESC");
        List<PayOrder> payOrderList = payOrderMapper.selectByExample(example);
        return CollectionUtils.isEmpty(payOrderList) ? null : payOrderList.get(0);
    }

    public int baseUpdateStatus(String payOrderId, String channelOrderNo,byte status,String param2,String errCode,String errMsg) {
        PayOrder payOrder = new PayOrder();
        payOrder.setPayOrderId(payOrderId);
        payOrder.setStatus(status);
        if(channelOrderNo != null) payOrder.setChannelOrderNo(channelOrderNo);
        if(!org.apache.commons.lang3.StringUtils.isBlank(param2))payOrder.setParam2(param2);
        if(!org.apache.commons.lang3.StringUtils.isBlank(errCode))payOrder.setErrCode(errCode);
        if(!org.apache.commons.lang3.StringUtils.isBlank(errMsg))payOrder.setErrMsg(errMsg);
        payOrder.setPaySuccTime(System.currentTimeMillis());
        PayOrderExample example = new PayOrderExample();
        PayOrderExample.Criteria criteria = example.createCriteria();
        criteria.andPayOrderIdEqualTo(payOrderId);
        return payOrderMapper.updateByExampleSelective(payOrder, example);
    }

    public int baseUpdateStatus4Failed(String payOrderId, String channelOrderNo) {
        PayOrder payOrder = new PayOrder();
        payOrder.setStatus(PayConstant.PAY_STATUS_FAILED);
        if(channelOrderNo != null) payOrder.setChannelOrderNo(channelOrderNo);
        payOrder.setPaySuccTime(System.currentTimeMillis());
        PayOrderExample example = new PayOrderExample();
        PayOrderExample.Criteria criteria = example.createCriteria();
        criteria.andPayOrderIdEqualTo(payOrderId);
        criteria.andStatusEqualTo(PayConstant.PAY_STATUS_INIT);
        return payOrderMapper.updateByExampleSelective(payOrder, example);
    }

    public int baseUpdateStatus4Ing(String payOrderId, String channelOrderNo) {
        PayOrder payOrder = new PayOrder();
        payOrder.setStatus(PayConstant.PAY_STATUS_PAYING);
        if(channelOrderNo != null) payOrder.setChannelOrderNo(channelOrderNo);
        payOrder.setPaySuccTime(System.currentTimeMillis());
        PayOrderExample example = new PayOrderExample();
        PayOrderExample.Criteria criteria = example.createCriteria();
        criteria.andPayOrderIdEqualTo(payOrderId);
        criteria.andStatusEqualTo(PayConstant.PAY_STATUS_INIT);
        return payOrderMapper.updateByExampleSelective(payOrder, example);
    }

    public int baseUpdateStatus4Success(String payOrderId, String channelOrderNo,String errCode,String errMsg,String param2) {
        PayOrder payOrder = new PayOrder();
        payOrder.setPayOrderId(payOrderId);
        payOrder.setStatus(PayConstant.PAY_STATUS_SUCCESS);
        payOrder.setParam2(param2);
        if(channelOrderNo != null) payOrder.setChannelOrderNo(channelOrderNo);
        payOrder.setPaySuccTime(System.currentTimeMillis());
        PayOrderExample example = new PayOrderExample();
        PayOrderExample.Criteria criteria = example.createCriteria();
        criteria.andPayOrderIdEqualTo(payOrderId);
        return payOrderMapper.updateByExampleSelective(payOrder, example);
    }

    public int baseUpdateStatus4Complete(String payOrderId) {
        PayOrder payOrder = new PayOrder();
        payOrder.setPayOrderId(payOrderId);
        payOrder.setStatus(PayConstant.PAY_STATUS_COMPLETE);
        PayOrderExample example = new PayOrderExample();
        PayOrderExample.Criteria criteria = example.createCriteria();
        criteria.andPayOrderIdEqualTo(payOrderId);
        criteria.andStatusEqualTo(PayConstant.PAY_STATUS_SUCCESS);
        return payOrderMapper.updateByExampleSelective(payOrder, example);
    }

    public int baseUpdateNotify(String payOrderId, byte count) {
        PayOrder newPayOrder = new PayOrder();
        newPayOrder.setNotifyCount(count);
        newPayOrder.setLastNotifyTime(System.currentTimeMillis());
        newPayOrder.setPayOrderId(payOrderId);
        return payOrderMapper.updateByPrimaryKeySelective(newPayOrder);
    }

    public int baseUpdateNotify(PayOrder payOrder) {
        return payOrderMapper.updateByPrimaryKeySelective(payOrder);
    }


    public int baseCreatePreOrder(GoodsOrder goodsOrder) {
        return goodsOrderMapper.insertSelective(goodsOrder);
    }

    public GoodsOrder baseSelectPreOrder(String goodsOrderId) {
        return goodsOrderMapper.selectByPrimaryKey(goodsOrderId);
    }

    public int baseUpdatePreOrder(GoodsOrder goodsOrder) {
        return goodsOrderMapper.updateByPrimaryKey(goodsOrder);
    }

}
