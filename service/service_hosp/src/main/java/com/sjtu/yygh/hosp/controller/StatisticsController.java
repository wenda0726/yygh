package com.sjtu.yygh.hosp.controller;

import com.sjtu.yygh.common.result.Result;
import com.sjtu.yygh.order.client.OrderInfoFeignClient;
import com.sjtu.yygh.vo.order.OrderCountQueryVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "统计管理接口")
@RestController
@RequestMapping("/admin/hosp/statistics")
public class StatisticsController {

    @Autowired
    private OrderInfoFeignClient orderInfoFeignClient;

    @ApiOperation(value = "获取订单统计数据")
    @GetMapping("getCountMap")
    public Result getCountMap(OrderCountQueryVo orderCountQueryVo) {
        return Result.ok(orderInfoFeignClient.getCountMap(orderCountQueryVo));
    }

}
