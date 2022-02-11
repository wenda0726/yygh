package com.sjtu.yygh.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sjtu.yygh.model.order.OrderInfo;
import com.sjtu.yygh.model.order.PaymentInfo;

import java.util.Map;

public interface PaymentInfoService extends IService<PaymentInfo> {
    void savePaymentInfo(OrderInfo orderInfo, Integer paymentType);

    void paySuccess(String outTradeNo, Integer paymentType, Map<String, String> resultMap);

    PaymentInfo getPaymentInfo(Long orderId,Integer paymentType);
}
