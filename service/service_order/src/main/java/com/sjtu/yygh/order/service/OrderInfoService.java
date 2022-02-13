package com.sjtu.yygh.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sjtu.yygh.model.order.OrderInfo;
import com.sjtu.yygh.vo.order.OrderCountQueryVo;
import com.sjtu.yygh.vo.order.OrderQueryVo;

import java.util.Map;

public interface OrderInfoService extends IService<OrderInfo> {
    //生成挂号订单
    Long saveOrder(String scheduleId, Long patientId);

    //根据订单id查询订单详情
    OrderInfo getOrderById(Long orderId);

    //订单列表（条件查询带分页）
    IPage<OrderInfo> selectPage(Page<OrderInfo> pageParam, OrderQueryVo orderQueryVo);

    Map<String, Object> show(Long id);

    //取消预约
    Boolean cancelOrder(Long orderId);

    /**
     * 订单统计
     */
    Map<String, Object> getCountMap(OrderCountQueryVo orderCountQueryVo);

}
