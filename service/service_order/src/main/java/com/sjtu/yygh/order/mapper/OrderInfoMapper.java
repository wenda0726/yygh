package com.sjtu.yygh.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sjtu.yygh.model.order.OrderInfo;
import com.sjtu.yygh.vo.order.OrderCountQueryVo;
import com.sjtu.yygh.vo.order.OrderCountVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    List<OrderCountVo> selectOrderCount(@Param("vo") OrderCountQueryVo orderCountQueryVo);
}
