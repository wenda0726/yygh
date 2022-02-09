package com.sjtu.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sjtu.yygh.common.exception.YyghException;
import com.sjtu.yygh.common.result.ResultCodeEnum;
import com.sjtu.yygh.hosp.repository.ScheduleRepository;
import com.sjtu.yygh.hosp.service.DepartmentService;
import com.sjtu.yygh.hosp.service.HospitalService;
import com.sjtu.yygh.hosp.service.ScheduleService;
import com.sjtu.yygh.model.hosp.BookingRule;
import com.sjtu.yygh.model.hosp.Department;
import com.sjtu.yygh.model.hosp.Hospital;
import com.sjtu.yygh.model.hosp.Schedule;
import com.sjtu.yygh.vo.hosp.BookingScheduleRuleVo;
import com.sjtu.yygh.vo.hosp.ScheduleQueryVo;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

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

    //根据医院编号和部门信息获取对应的排班日期
    @Override
    public Map<String, Object> getBookingScheduleRule(Integer page, Integer limit, String hoscode, String depcode) {
        Map<String,Object> result = new HashMap<>();
        Hospital hospital = hospitalService.getByHoscode(hoscode);
        if(hospital == null){
            throw new YyghException(ResultCodeEnum.PARAM_ERROR);
        }
        BookingRule bookingRule = hospital.getBookingRule();
        //获取可预约日期的分页数据
        IPage<Date> iPage = this.getListDate(page,limit,bookingRule);
        List<Date> dateList = iPage.getRecords();
        //根据可预约的日期，查询剩余的可预约数
        Criteria criteria = Criteria.where("hoscode").is(hoscode).and("depcode").is(depcode).and("workDate").in(dateList);
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group("workDate")//分组字段
                        .first("workDate").as("workDate")
                        .count().as("docCount")
                        .sum("availableNumber").as("availableNumber")
                        .sum("reservedNumber").as("reservedNumber")
        );
        AggregationResults<BookingScheduleRuleVo> aggregationResults = mongoTemplate.aggregate(agg, Schedule.class, BookingScheduleRuleVo.class);
        List<BookingScheduleRuleVo> scheduleVoList = aggregationResults.getMappedResults();
        //合并数据 将统计数据ScheduleVo根据“安排日期”合并到BookingRuleVo
        //map集合key为日期对象date，value为对应的预约规则
        Map<Date, BookingScheduleRuleVo> scheduleVoMap = new HashMap<>();
        if(!CollectionUtils.isEmpty(scheduleVoList)) {
            scheduleVoMap = scheduleVoList.stream().collect(Collectors.toMap(BookingScheduleRuleVo::getWorkDate, BookingScheduleRuleVo -> BookingScheduleRuleVo));
        }
        //获取可预约排班规则
        List<BookingScheduleRuleVo> bookingScheduleRuleVoList = new ArrayList<>();
        for(int i=0, len=dateList.size(); i<len; i++) {
            Date date = dateList.get(i);

            BookingScheduleRuleVo bookingScheduleRuleVo = scheduleVoMap.get(date);
            if(null == bookingScheduleRuleVo) { // 说明当天没有排班医生
                bookingScheduleRuleVo = new BookingScheduleRuleVo();
                //就诊医生人数
                bookingScheduleRuleVo.setDocCount(0);
                //科室剩余预约数  -1表示无号
                bookingScheduleRuleVo.setAvailableNumber(-1);
            }
            bookingScheduleRuleVo.setWorkDate(date);
            bookingScheduleRuleVo.setWorkDateMd(date);
            //计算当前预约日期为周几
            String dayOfWeek = this.getDayOfWeek(new DateTime(date));
            bookingScheduleRuleVo.setDayOfWeek(dayOfWeek);

            //最后一页最后一条记录为即将预约   状态 0：正常 1：即将放号 -1：当天已停止挂号
            if(i == len-1 && page == iPage.getPages()) {
                bookingScheduleRuleVo.setStatus(1);
            } else {
                bookingScheduleRuleVo.setStatus(0);
            }
            //当天预约如果过了停号时间， 不能预约
            if(i == 0 && page == 1) {
                DateTime stopTime = this.getDateTime(new Date(), bookingRule.getStopTime());
                if(stopTime.isBeforeNow()) {
                    //停止预约
                    bookingScheduleRuleVo.setStatus(-1);
                }
            }
            bookingScheduleRuleVoList.add(bookingScheduleRuleVo);
        }
        //可预约日期规则数据
        result.put("bookingScheduleList", bookingScheduleRuleVoList);
        result.put("total", iPage.getTotal());

        //其他基础数据
        Map<String, String> baseMap = new HashMap<>();
        //医院名称
        baseMap.put("hosname", hospitalService.getHosname(hoscode));
        //科室
        Department department =departmentService.getDepartment(hoscode, depcode);
        //大科室名称
        baseMap.put("bigname", department.getBigname());
        //科室名称
        baseMap.put("depname", department.getDepname());
        //月
        baseMap.put("workDateString", new DateTime().toString("yyyy年MM月"));
        //放号时间
        baseMap.put("releaseTime", bookingRule.getReleaseTime());
        //停号时间
        baseMap.put("stopTime", bookingRule.getStopTime());
        result.put("baseMap", baseMap);
        return result;
    }

    //根据id查询Schedule对象
    @Override
    public Schedule getById(String scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId).get();
        this.packageSchedule(schedule);
        return schedule;
    }

    private IPage<Date> getListDate(Integer page, Integer limit, BookingRule bookingRule) {
        //预约周期
        Integer cycle = bookingRule.getCycle();
        //当天的放号时间
        DateTime dateTime = this.getDateTime(new Date(),bookingRule.getReleaseTime());
        //如果当天的放号时间已过，则预约周期需要加1
        if(dateTime.isBeforeNow()){
            cycle++;
        }
        //可预约所有日期，最后一天显示即将放号倒计时
        List<Date> dateList = new ArrayList<>();
        for(int i = 0; i < cycle; i++){
            DateTime curDateTime  = new DateTime().plusDays(i);
            String s = curDateTime.toString("yyyy-MM-dd");
            dateList.add(new DateTime(s).toDate());
        }
        //日期分页，由于预约周期不一样，页面一排最多显示7天数据，多了就要分页显示
        List<Date> pageDateList = new ArrayList<>();
        int start = (page - 1)*limit;
        int end = page*limit;
        if(end > dateList.size()){
            end = dateList.size();
        }
        for(int i = start; i < end; i++){
            pageDateList.add(dateList.get(i));
        }
        IPage<Date> iPage = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page,7,dateList.size());
        iPage.setRecords(pageDateList);
        return iPage;
    }

    /**
     * 将Date日期（yyyy-MM-dd HH:mm）转换为DateTime
     */
    private DateTime getDateTime(Date date, String timeString) {
        String dateTimeString = new DateTime(date).toString("yyyy-MM-dd") + " "+ timeString;
        DateTime dateTime = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm").parseDateTime(dateTimeString);
        return dateTime;
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
