package com.sjtu.yygh.hosp.controller;


import com.sjtu.yygh.common.result.Result;
import com.sjtu.yygh.hosp.service.ScheduleService;
import com.sjtu.yygh.model.hosp.Schedule;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
//@CrossOrigin
@RequestMapping("/admin/hosp/schedule")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;
    //根据医院编号 和 科室编号 ，查询排班规则数据
    @ApiOperation(value ="查询排班规则数据")
    @GetMapping("getScheduleRule/{page}/{limit}/{hoscode}/{depcode}")
    public Result getScheduleRule(@PathVariable("page") Integer page,
                                  @PathVariable("limit") Integer limit,
                                  @PathVariable("hoscode") String hoscode,
                                  @PathVariable("depcode") String depcode){
        Map<String, Object> result = scheduleService.getRuleSchedule(page,limit,hoscode,depcode);
        return Result.ok(result);
    }

    //根据医院编号、科室编号、日期，查询对应的排班信息
    @ApiOperation(value = "查询排班详细信息")
    @GetMapping("getScheduleDetail/{hoscode}/{depcode}/{workDate}")
    public Result getScheduleDetail(@PathVariable("hoscode")String hoscode,
                                    @PathVariable("depcode")String depcode,
                                    @PathVariable("workDate")String workDate){
        List<Schedule> result = scheduleService.getScheduleDetail(hoscode,depcode,workDate);
        return Result.ok(result);
    }

}
