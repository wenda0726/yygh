package com.sjtu.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.sjtu.yygh.common.exception.YyghException;
import com.sjtu.yygh.common.result.ResultCodeEnum;
import com.sjtu.yygh.hosp.repository.ScheduleRepository;
import com.sjtu.yygh.hosp.service.DepartmentService;
import com.sjtu.yygh.hosp.service.HospitalService;
import com.sjtu.yygh.hosp.service.ScheduleService;
import com.sjtu.yygh.model.hosp.Hospital;
import com.sjtu.yygh.model.hosp.Schedule;
import com.sjtu.yygh.vo.hosp.BookingScheduleRuleVo;
import com.sjtu.yygh.vo.hosp.ScheduleQueryVo;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScheduleServiceImpl implements ScheduleService {
    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private DepartmentService departmentService;



    @Override
    public void save(Map<String, Object> parameterMap) {
        String jsonString = JSONObject.toJSONString(parameterMap);
        Schedule schedule = JSONObject.parseObject(jsonString, Schedule.class);
        Schedule exits = scheduleRepository.
                getScheduleByHoscodeAndHosScheduleId(schedule.getHoscode(),schedule.getHosScheduleId());
        if(exits != null){
            exits.setIsDeleted(0);
            exits.setUpdateTime(new Date());
            scheduleRepository.save(exits);
        }else{
            schedule.setUpdateTime(new Date());
            schedule.setCreateTime(new Date());
            schedule.setIsDeleted(0);
            scheduleRepository.save(schedule);
        }
    }

    @Override
    public Page<Schedule> selectPage(int page, int limit, ScheduleQueryVo scheduleQueryVo) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        ExampleMatcher matcher = ExampleMatcher.matching().
                withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
                .withIgnoreCase(true);
        Schedule schedule = new Schedule();
        BeanUtils.copyProperties(scheduleQueryVo,schedule);
        schedule.setIsDeleted(0);
        schedule.setStatus(1);
        Example<Schedule> example = Example.of(schedule);
        return scheduleRepository.findAll(example,pageable);
    }

    @Override
    public void remove(String hoscode, String hosScheduleId) {
        if(StringUtils.isEmpty(hoscode) || StringUtils.isEmpty(hosScheduleId)){
            throw new YyghException(ResultCodeEnum.CODE_ERROR);
        }
        Schedule schedule = scheduleRepository.getScheduleByHoscodeAndHosScheduleId(hoscode, hosScheduleId);
        if(null != schedule){
            scheduleRepository.deleteById(schedule.getId());
        }
    }

    @Override
    public Map<String, Object> getRuleSchedule(Integer page, Integer limit, String hoscode, String depcode) {
        //根据hoscode 和 depcode 查询schedule的详细信息，并且按照工作日期workDate进行分组
        Criteria criteria = Criteria.where("hoscode").is(hoscode).and("depcode").is(depcode);
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group("workDate").first("workDate").as("workDate")
                .count().as("docCount")
                .sum("reservedNumber").as("reservedNumber")
                .sum("availableNumber").as("availableNumber"),
                //排序
                Aggregation.sort(Sort.Direction.DESC,"workDate"),
                Aggregation.skip((page - 1) * limit),
                Aggregation.limit(limit)
        );
        AggregationResults<BookingScheduleRuleVo> aggResults  = mongoTemplate.aggregate(agg, Schedule.class, BookingScheduleRuleVo.class);
        List<BookingScheduleRuleVo> listResults = aggResults.getMappedResults();
        //分组查询的总记录数
        Aggregation agg1 = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group("workDate")
        );
        AggregationResults<BookingScheduleRuleVo> bookingScheduleRuleVos = mongoTemplate.aggregate(agg1, Schedule.class, BookingScheduleRuleVo.class);
        int total = bookingScheduleRuleVos.getMappedResults().size();

        //获取对应的星期
        for(BookingScheduleRuleVo item : listResults){
            Date workDate = item.getWorkDate();
            String dayOfWeek = this.getDayOfWeek(new DateTime(workDate));
            item.setDayOfWeek(dayOfWeek);
        }

        //获取医院名称
        String hosName = hospitalService.getHosname(hoscode);

        //数据封装
        Map<String,Object> result = new HashMap<>();
        result.put("bookingScheduleRuleList",listResults);
        result.put("total",total);
        Map<String,String> baseMap = new HashMap<>();
        baseMap.put("hosname",hosName);
        result.put("baseMap",baseMap);
        return result;
    }

    //根据医院编号 、科室编号和工作日期，查询排班详细信息
    @Override
    public List<Schedule> getScheduleDetail(String hoscode, String depcode, String workDate) {
        Date date = new DateTime(workDate).toDate();
        List<Schedule> scheduleList = scheduleRepository.getScheduleByHoscodeAndDepcodeAndWorkDate(hoscode,depcode,date);
        scheduleList.stream().forEach(item ->{
            this.packageSchedule(item);
        });
        return scheduleList;
    }

    //对Schedule对象进行封装，加入医院名称，科室名称，星期几等信息
    private void packageSchedule(Schedule schedule){
        String hoscode = schedule.getHoscode();
        String depcode = schedule.getDepcode();
        Date workDate = schedule.getWorkDate();
        String hosname = hospitalService.getHosname(hoscode);
        String depname = departmentService.getDepname(hoscode,depcode);
        String dayOfWeek = this.getDayOfWeek(new DateTime(workDate));
        Map<String, Object> param = schedule.getParam();
        param.put("hosname",hosname);
        param.put("depname",depname);
        param.put("dayOfWeek",dayOfWeek);
    }

    /**
     * 根据日期获取周几数据
     * @param dateTime
     * @return
     */
    private String getDayOfWeek(DateTime dateTime) {
        String dayOfWeek = "";
        switch (dateTime.getDayOfWeek()) {
            case DateTimeConstants.SUNDAY:
                dayOfWeek = "周日";
                break;
            case DateTimeConstants.MONDAY:
                dayOfWeek = "周一";
                break;
            case DateTimeConstants.TUESDAY:
                dayOfWeek = "周二";
                break;
            case DateTimeConstants.WEDNESDAY:
                dayOfWeek = "周三";
                break;
            case DateTimeConstants.THURSDAY:
                dayOfWeek = "周四";
                break;
            case DateTimeConstants.FRIDAY:
                dayOfWeek = "周五";
                break;
            case DateTimeConstants.SATURDAY:
                dayOfWeek = "周六";
            default:
                break;
        }
        return dayOfWeek;
    }

}
