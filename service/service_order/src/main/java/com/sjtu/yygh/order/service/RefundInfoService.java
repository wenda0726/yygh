package com.sjtu.yygh.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sjtu.yygh.model.order.PaymentInfo;
import com.sjtu.yygh.model.order.RefundInfo;

public interface RefundInfoService extends IService<RefundInfo> {
    /**
     * 保存退款记录
     * @param paymentInfo
     */
    RefundInfo saveRefundInfo(PaymentInfo paymentInfo);

}
