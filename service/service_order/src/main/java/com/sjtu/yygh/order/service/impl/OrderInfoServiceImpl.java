package com.sjtu.yygh.order.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Maps;
import com.sjtu.common.rabbit.constant.MqConst;
import com.sjtu.common.rabbit.service.RabbitService;

import com.sjtu.yygh.model.hosp.Schedule;
import com.sjtu.yygh.order.repository.ScheduleRepository;
import com.sjtu.yygh.order.service.WeixinService;
import com.sjtu.yygh.vo.msm.MsmVo;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sjtu.yygh.common.exception.YyghException;
import com.sjtu.yygh.common.helper.HttpRequestHelper;
import com.sjtu.yygh.common.result.ResultCodeEnum;
import com.sjtu.yygh.enums.OrderStatusEnum;
import com.sjtu.yygh.hosp.client.HospitalFeignClient;
import com.sjtu.yygh.model.order.OrderInfo;
import com.sjtu.yygh.model.user.Patient;
import com.sjtu.yygh.order.mapper.OrderInfoMapper;
import com.sjtu.yygh.order.service.OrderInfoService;
import com.sjtu.yygh.user.client.PatientFeignClient;
import com.sjtu.yygh.vo.hosp.ScheduleOrderVo;
import com.sjtu.yygh.vo.order.*;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo>
        implements OrderInfoService {

    @Autowired
    private PatientFeignClient patientFeignClient;

    @Autowired
    private HospitalFeignClient hospitalFeignClient;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private WeixinService weixinService;
    //生成挂号订单
    @Override
    public Long saveOrder(String scheduleId, Long patientId) {
        //获取就诊人信息
        Patient patient = patientFeignClient.getPatientOrder(patientId);
        //获取排班信息
        ScheduleOrderVo scheduleOrderVo = hospitalFeignClient.getScheduleOrderVo(scheduleId);
        //当前时间不可以预约
//        if(new DateTime(scheduleOrderVo.getStartTime()).isAfterNow()
//                || new DateTime(scheduleOrderVo.getEndTime()).isBeforeNow()) {
//            throw new YyghException(ResultCodeEnum.TIME_NO);
//        }
        //获取信息签名信息
        SignInfoVo signInfoVo = hospitalFeignClient.getSignInfoVo(scheduleOrderVo.getHoscode());
        //数据添加到订单表中
        OrderInfo orderInfo = new OrderInfo();
        //大部分数据是scheduleOrderVo中的
        BeanUtils.copyProperties(scheduleOrderVo,orderInfo);
        //设置一些其他的值
        String outTradeNo = System.currentTimeMillis() + ""+ new Random().nextInt(100);
        orderInfo.setOutTradeNo(outTradeNo);
        orderInfo.setScheduleId(scheduleId);
        orderInfo.setUserId(patient.getUserId());
        orderInfo.setPatientId(patientId);
        orderInfo.setPatientName(patient.getName());
        orderInfo.setPatientPhone(patient.getPhone());
        orderInfo.setOrderStatus(OrderStatusEnum.UNPAID.getStatus());
        baseMapper.insert(orderInfo);

        /**
         * 根据前端传过来的scheduleId查询hosScheduleId
         * 由于该项目数据存储设计不合理，这里存在mongodb中的主键id 与数据表中的id不一致
         */
        Schedule scheduleById = scheduleRepository.findScheduleById(scheduleId);
        String hosScheduleIdTemp = null;
        if(scheduleById!=null){
            hosScheduleIdTemp = scheduleById.getHosScheduleId();
        }
        Long hosScheduleId = null;
        try {
            hosScheduleId = Long.parseLong(hosScheduleIdTemp);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        //调用医院接口，实现预约下单功能
        //将调用医院接口需要的参数放到一个Map集合中
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("hoscode",orderInfo.getHoscode());
        paramMap.put("depcode",orderInfo.getDepcode());
        paramMap.put("hosScheduleId",hosScheduleId);
        paramMap.put("reserveDate",new DateTime(orderInfo.getReserveDate()).toString("yyyy-MM-dd"));
        paramMap.put("reserveTime", orderInfo.getReserveTime());
        paramMap.put("amount",orderInfo.getAmount());
        paramMap.put("name", patient.getName());
        paramMap.put("certificatesType",patient.getCertificatesType());
        paramMap.put("certificatesNo", patient.getCertificatesNo());
        paramMap.put("sex",patient.getSex());
        paramMap.put("birthdate", patient.getBirthdate());
        paramMap.put("phone",patient.getPhone());
        paramMap.put("isMarry", patient.getIsMarry());
        paramMap.put("provinceCode",patient.getProvinceCode());
        paramMap.put("cityCode", patient.getCityCode());
        paramMap.put("districtCode",patient.getDistrictCode());
        paramMap.put("address",patient.getAddress());
        //联系人
        paramMap.put("contactsName",patient.getContactsName());
        paramMap.put("contactsCertificatesType", patient.getContactsCertificatesType());
        paramMap.put("contactsCertificatesNo",patient.getContactsCertificatesNo());
        paramMap.put("contactsPhone",patient.getContactsPhone());
        paramMap.put("timestamp", HttpRequestHelper.getTimestamp());
        String sign = HttpRequestHelper.getSign(paramMap, signInfoVo.getSignKey());
        paramMap.put("sign", sign);

        //请求医院接口
        JSONObject result = HttpRequestHelper.sendRequest(paramMap, "http://localhost:9998/order/submitOrder");
        if(result.getInteger("code") == 200){
            JSONObject jsonObject = result.getJSONObject("data");
            //预约记录唯一标识（医院预约记录主键）
            String hosRecordId = jsonObject.getString("hosRecordId");
            //预约序号
            Integer number = jsonObject.getInteger("number");;
            //取号时间
            String fetchTime = jsonObject.getString("fetchTime");;
            //取号地址
            String fetchAddress = jsonObject.getString("fetchAddress");;
            //更新订单
            orderInfo.setHosRecordId(hosRecordId);
            orderInfo.setNumber(number);
            orderInfo.setFetchTime(fetchTime);
            orderInfo.setFetchAddress(fetchAddress);
            baseMapper.updateById(orderInfo);
            //排班可预约数
            Integer reservedNumber = jsonObject.getInteger("reservedNumber");
            //排班剩余预约数
            Integer availableNumber = jsonObject.getInteger("availableNumber");
            //消息队列进行数据库操作，发送mq信息,更新号源和短信通知

            //mq发送号源更新
            OrderMqVo orderMqVo = new OrderMqVo();
            orderMqVo.setReservedNumber(reservedNumber);
            orderMqVo.setAvailableNumber(availableNumber);
            orderMqVo.setScheduleId(scheduleId);

            MsmVo msmVo = new MsmVo();
            msmVo.setPhone(orderInfo.getPatientPhone());
            msmVo.setTemplateCode("SMS_194640721");
            String reserveDate =
                    new DateTime(orderInfo.getReserveDate()).toString("yyyy-MM-dd")
                            + (orderInfo.getReserveTime()==0 ? "上午": "下午");
            Map<String,Object> param = new HashMap<String,Object>(){{
                put("title", orderInfo.getHosname()+"|"+orderInfo.getDepname()+"|"+orderInfo.getTitle());
                put("amount", orderInfo.getAmount());
                put("reserveDate", reserveDate);
                put("name", orderInfo.getPatientName());
                put("quitTime", new DateTime(orderInfo.getQuitTime()).toString("yyyy-MM-dd HH:mm"));
            }};
            msmVo.setParam(param);
            orderMqVo.setMsmVo(new MsmVo());

            //发送
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_ORDER, MqConst.ROUTING_ORDER, orderMqVo);

        }else {
            throw new YyghException(result.getString("message"), ResultCodeEnum.FAIL.getCode());
        }
        return orderInfo.getId();
    }

    //根据订单id查询订单详情
    @Override
    public OrderInfo getOrderById(Long orderId) {
        OrderInfo orderInfo = baseMapper.selectById(orderId);
        this.packOrderInfo(orderInfo);
        return orderInfo;
    }

    //订单列表（条件查询带分页）
    @Override
    public IPage<OrderInfo> selectPage(Page<OrderInfo> pageParam, OrderQueryVo orderQueryVo) {
        //orderQueryVo获取条件值
        String keyword = orderQueryVo.getKeyword(); //医院名称
        Long patientId = orderQueryVo.getPatientId(); //就诊人名称
        String orderStatus = orderQueryVo.getOrderStatus(); //订单状态
        String reserveDate = orderQueryVo.getReserveDate();//安排时间
        String createTimeBegin = orderQueryVo.getCreateTimeBegin();
        String createTimeEnd = orderQueryVo.getCreateTimeEnd();
        Long userId = orderQueryVo.getUserId();
        //对条件值进行非空判断
        QueryWrapper<OrderInfo> wrapper = new QueryWrapper<>();
        if(userId != null){
            wrapper.eq("user_id",userId);
        }
        if(!StringUtils.isEmpty(keyword)) {
            wrapper.like("hosname",keyword);
        }
        if(!StringUtils.isEmpty(patientId)) {
            wrapper.eq("patient_id",patientId);
        }
        if(!StringUtils.isEmpty(orderStatus)) {
            wrapper.eq("order_status",orderStatus);
        }
        if(!StringUtils.isEmpty(reserveDate)) {
            wrapper.ge("reserve_date",reserveDate);
        }
        if(!StringUtils.isEmpty(createTimeBegin)) {
            wrapper.ge("create_time",createTimeBegin);
        }
        if(!StringUtils.isEmpty(createTimeEnd)) {
            wrapper.le("create_time",createTimeEnd);
        }
        Page<OrderInfo> orderInfoPage = baseMapper.selectPage(pageParam, wrapper);
        orderInfoPage.getRecords().stream().forEach(orderInfo -> {
            this.packOrderInfo(orderInfo);
        });

        return orderInfoPage;
    }

    //订单详情
    @Override
    public Map<String, Object> show(Long orderId) {
        Map<String, Object> map = new HashMap<>();
        OrderInfo orderInfo = this.getOrderById(orderId);
        this.packOrderInfo(orderInfo);
        map.put("orderInfo", orderInfo);
        Patient patient
                =  patientFeignClient.getPatientOrder(orderInfo.getPatientId());
        map.put("patient", patient);
        return map;
    }

    //取消预约
    @Override
    public Boolean cancelOrder(Long orderId) {
        OrderInfo orderInfo = this.getById(orderId);
        //当前时间大于退号时间，不能取消预约
//        DateTime quitTime = new DateTime(orderInfo.getQuitTime());
//        if(quitTime.isBeforeNow()) {
//            throw new YyghException(ResultCodeEnum.CANCEL_ORDER_NO);
//        }
        SignInfoVo signInfoVo = hospitalFeignClient.getSignInfoVo(orderInfo.getHoscode());
        if(null == signInfoVo) {
            throw new YyghException(ResultCodeEnum.PARAM_ERROR);
        }
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("hoscode",orderInfo.getHoscode());
        reqMap.put("hosRecordId",orderInfo.getHosRecordId());
        reqMap.put("timestamp", HttpRequestHelper.getTimestamp());
        String sign = HttpRequestHelper.getSign(reqMap, signInfoVo.getSignKey());
        reqMap.put("sign", sign);

        JSONObject result = HttpRequestHelper.sendRequest(reqMap, signInfoVo.getApiUrl()+"/order/updateCancelStatus");

        if(result.getInteger("code") != 200) {
            throw new YyghException(result.getString("message"), ResultCodeEnum.FAIL.getCode());
        } else {
            //是否支付 退款
            if(orderInfo.getOrderStatus().intValue() == OrderStatusEnum.PAID.getStatus().intValue()) {
            //已支付 退款
                boolean isRefund = weixinService.refund(orderId);
                if(!isRefund) {
                    throw new YyghException(ResultCodeEnum.CANCEL_ORDER_FAIL);
                }
            }
            //更改订单状态
            orderInfo.setOrderStatus(OrderStatusEnum.CANCLE.getStatus());
            this.updateById(orderInfo);
            //发送mq信息更新预约数 我们与下单成功更新预约数使用相同的mq信息，不设置可预约数与剩余预约数，接收端可预约数减1即可
            OrderMqVo orderMqVo = new OrderMqVo();
            orderMqVo.setScheduleId(orderInfo.getScheduleId());
            //短信提示
            MsmVo msmVo = new MsmVo();
            msmVo.setPhone(orderInfo.getPatientPhone());
            msmVo.setTemplateCode("SMS_194640722");
            String reserveDate = new DateTime(orderInfo.getReserveDate()).toString("yyyy-MM-dd") + (orderInfo.getReserveTime()==0 ? "上午": "下午");
            Map<String,Object> param = new HashMap<String,Object>(){{
                put("title", orderInfo.getHosname()+"|"+orderInfo.getDepname()+"|"+orderInfo.getTitle());
                put("reserveDate", reserveDate);
                put("name", orderInfo.getPatientName());
            }};
            msmVo.setParam(param);
            orderMqVo.setMsmVo(msmVo);
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_ORDER, MqConst.ROUTING_ORDER, orderMqVo);
        }
        return true;
    }

    @Override
    public Map<String, Object> getCountMap(OrderCountQueryVo orderCountQueryVo) {
        List<OrderCountVo> orderCountVoList = baseMapper.selectOrderCount(orderCountQueryVo);
        List<String> dateList = new ArrayList<>();
        List<Integer> countList = new ArrayList<>();
        for(OrderCountVo vo : orderCountVoList){
            dateList.add(vo.getReserveDate());
            countList.add(vo.getCount());
        }
        Map<String, Object> result = new HashMap<>();
        result.put("dateList",dateList);
        result.put("countList",countList);
        return result;
    }

    private void packOrderInfo(OrderInfo orderInfo){
        String orderStatusString = OrderStatusEnum.getStatusNameByStatus(orderInfo.getOrderStatus());
        orderInfo.getParam().put("orderStatusString",orderStatusString);
    }
}
