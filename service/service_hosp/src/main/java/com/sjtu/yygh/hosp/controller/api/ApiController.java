package com.sjtu.yygh.hosp.controller.api;


import com.sjtu.yygh.common.exception.YyghException;
import com.sjtu.yygh.common.helper.HttpRequestHelper;
import com.sjtu.yygh.common.result.Result;
import com.sjtu.yygh.common.result.ResultCodeEnum;
import com.sjtu.yygh.common.utils.MD5;
import com.sjtu.yygh.hosp.service.DepartmentService;
import com.sjtu.yygh.hosp.service.HospitalService;
import com.sjtu.yygh.hosp.service.HospitalSetService;
import com.sjtu.yygh.hosp.service.ScheduleService;
import com.sjtu.yygh.model.hosp.Department;
import com.sjtu.yygh.model.hosp.Hospital;
import com.sjtu.yygh.model.hosp.Schedule;
import com.sjtu.yygh.vo.hosp.DepartmentQueryVo;
import com.sjtu.yygh.vo.hosp.ScheduleOrderVo;
import com.sjtu.yygh.vo.hosp.ScheduleQueryVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Api(tags = "医院管理API接口")
@RestController
@RequestMapping("/api/hosp")
public class ApiController {

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private HospitalSetService hospitalSetService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ScheduleService scheduleService;

    @ApiOperation(value = "上传医院")
    @PostMapping("saveHospital")
    public Result saveHospital(HttpServletRequest request){
        Map<String, String[]> requestParameterMap = request.getParameterMap();
        Map<String, Object> parameterMap = HttpRequestHelper.switchMap(requestParameterMap);
        //进行签名的校验
        String signKey = (String)parameterMap.get("sign");
        String hoscode = (String)parameterMap.get("hoscode");
        //根据保存的hoscode查询数据库
        String signKeyInDB = hospitalSetService.getSignByHoscode(hoscode);
        String MD5signKey = MD5.encrypt(signKeyInDB);
        if(!MD5signKey.equals(signKey)){
            throw new YyghException(ResultCodeEnum.SIGN_ERROR);
        }
        //传输过程中“+”转换为了“ ”，因此我们要转换回来
        String logoDataString = (String)parameterMap.get("logoData");
        if(!StringUtils.isEmpty(logoDataString)) {
            String logoData = logoDataString.replaceAll(" ", "+");
            parameterMap.put("logoData", logoData);
        }
        hospitalService.save(parameterMap);
        return Result.ok();

    }

    //查询医院
    @ApiOperation(value = "获取医院信息")
    @PostMapping("hospital/show")
    public Result getHospital(HttpServletRequest request){
        Map<String, String[]> requestParameterMap = request.getParameterMap();
        Map<String, Object> parameterMap = HttpRequestHelper.switchMap(requestParameterMap);
        //进行签名的校验
        String signKey = (String)parameterMap.get("sign");
        String hoscode = (String)parameterMap.get("hoscode");
        //根据保存的hoscode查询数据库
        String signKeyInDB = hospitalSetService.getSignByHoscode(hoscode);
        String MD5signKey = MD5.encrypt(signKeyInDB);
        if(!MD5signKey.equals(signKey)){
            throw new YyghException(ResultCodeEnum.SIGN_ERROR);
        }
        Hospital hospital = hospitalService.getByHoscode(hoscode);
        return Result.ok(hospital);
    }

    //上传科室
    @ApiOperation(value = "上传科室")
    @PostMapping("saveDepartment")
    public Result saveDepartment(HttpServletRequest request){
        Map<String, String[]> requestParameterMap = request.getParameterMap();
        Map<String, Object> parameterMap = HttpRequestHelper.switchMap(requestParameterMap);
        //进行签名的校验
        String signKey = (String)parameterMap.get("sign");
        String hoscode = (String)parameterMap.get("hoscode");
        //根据保存的hoscode查询数据库
        String signKeyInDB = hospitalSetService.getSignByHoscode(hoscode);
        String MD5signKey = MD5.encrypt(signKeyInDB);
        if(!MD5signKey.equals(signKey)){
            throw new YyghException(ResultCodeEnum.SIGN_ERROR);
        }
        departmentService.save(parameterMap);
        return Result.ok();
    }

    //查询显示科室，分页查询
    @ApiOperation(value = "获取分页列表")
    @PostMapping("department/list")
    public Result department(HttpServletRequest request){
        Map<String, String[]> requestParameterMap = request.getParameterMap();
        Map<String, Object> parameterMap = HttpRequestHelper.switchMap(requestParameterMap);
        //进行签名的校验
        String signKey = (String)parameterMap.get("sign");
        String hoscode = (String)parameterMap.get("hoscode");
        //根据保存的hoscode查询数据库
        String signKeyInDB = hospitalSetService.getSignByHoscode(hoscode);
        String MD5signKey = MD5.encrypt(signKeyInDB);
        if(!MD5signKey.equals(signKey)){
            throw new YyghException(ResultCodeEnum.SIGN_ERROR);
        }


        String depcode = (String)parameterMap.get("depcode");
        int page = StringUtils.isEmpty((String)parameterMap.get("page")) ? 1 :
                Integer.parseInt((String)parameterMap.get("page"));
        int limit = StringUtils.isEmpty((String)parameterMap.get("limit")) ? 10 :
                Integer.parseInt((String)parameterMap.get("limit"));
        DepartmentQueryVo queryVo = new DepartmentQueryVo();
        queryVo.setHoscode(hoscode);
        queryVo.setDepcode(depcode);
        Page<Department> departmentPage = departmentService.selectPage(page,limit,queryVo);
        return Result.ok(departmentPage);
    }

    //删除科室接口
    @ApiOperation(value = "删除科室")
    @PostMapping("department/remove")
    public Result removeDepartment(HttpServletRequest request){
        Map<String, String[]> requestParameterMap = request.getParameterMap();
        Map<String, Object> parameterMap = HttpRequestHelper.switchMap(requestParameterMap);
        //进行签名的校验
        String signKey = (String)parameterMap.get("sign");
        String hoscode = (String)parameterMap.get("hoscode");
        String depcode = (String)parameterMap.get("depcode");
        if(StringUtils.isEmpty(hoscode)){
            throw new YyghException(ResultCodeEnum.PARAM_ERROR);
        }
        //根据保存的hoscode查询数据库
        String signKeyInDB = hospitalSetService.getSignByHoscode(hoscode);
        String MD5signKey = MD5.encrypt(signKeyInDB);
        if(!MD5signKey.equals(signKey)){
            throw new YyghException(ResultCodeEnum.SIGN_ERROR);
        }
        departmentService.remove(hoscode,depcode);
        return Result.ok();
    }

    //上传排班接口开发
    @ApiOperation(value = "上传排班")
    @PostMapping("saveSchedule")
    public Result saveSchedule(HttpServletRequest request){
        Map<String, String[]> requestParameterMap = request.getParameterMap();
        Map<String, Object> parameterMap = HttpRequestHelper.switchMap(requestParameterMap);
        //进行签名的校验
        String signKey = (String)parameterMap.get("sign");
        String hoscode = (String)parameterMap.get("hoscode");
        //根据保存的hoscode查询数据库
        String signKeyInDB = hospitalSetService.getSignByHoscode(hoscode);
        String MD5signKey = MD5.encrypt(signKeyInDB);
        if(!MD5signKey.equals(signKey)){
            throw new YyghException(ResultCodeEnum.SIGN_ERROR);
        }
        scheduleService.save(parameterMap);
        return Result.ok();
    }

    //查询排班接口
    @ApiOperation(value = "获取排班分页列表")
    @PostMapping("schedule/list")
    public Result schedule(HttpServletRequest request){
        Map<String, String[]> requestParameterMap = request.getParameterMap();
        Map<String, Object> parameterMap = HttpRequestHelper.switchMap(requestParameterMap);
        //进行签名的校验
        String signKey = (String)parameterMap.get("sign");
        String hoscode = (String)parameterMap.get("hoscode");
        String depcode = (String)parameterMap.get("depcode");

        //根据保存的hoscode查询数据库
        String signKeyInDB = hospitalSetService.getSignByHoscode(hoscode);
        String MD5signKey = MD5.encrypt(signKeyInDB);
        if(!MD5signKey.equals(signKey)){
            throw new YyghException(ResultCodeEnum.SIGN_ERROR);
        }
        ScheduleQueryVo scheduleQueryVo = new ScheduleQueryVo();
        scheduleQueryVo.setHoscode(hoscode);
        scheduleQueryVo.setDepcode(depcode);

        int page = StringUtils.isEmpty((String)parameterMap.get("page")) ? 1 :
                Integer.parseInt((String)parameterMap.get("page"));
        int limit = StringUtils.isEmpty((String)parameterMap.get("limit")) ? 10 :
                Integer.parseInt((String)parameterMap.get("limit"));
        Page<Schedule> schedulePage = scheduleService.selectPage(page,limit,scheduleQueryVo);
        return Result.ok(schedulePage);
    }

    //删除医院排班接口
    @ApiOperation(value = "删除科室")
    @PostMapping("schedule/remove")
    public Result removeSchedule(HttpServletRequest request){
        Map<String, String[]> requestParameterMap = request.getParameterMap();
        Map<String, Object> parameterMap = HttpRequestHelper.switchMap(requestParameterMap);
        //进行签名的校验
        String signKey = (String)parameterMap.get("sign");
        String hoscode = (String)parameterMap.get("hoscode");
        String hosScheduleId = (String)parameterMap.get("hosScheduleId");

        //根据保存的hoscode查询数据库
        String signKeyInDB = hospitalSetService.getSignByHoscode(hoscode);
        String MD5signKey = MD5.encrypt(signKeyInDB);
        if(!MD5signKey.equals(signKey)){
            throw new YyghException(ResultCodeEnum.SIGN_ERROR);
        }
        scheduleService.remove(hoscode,hosScheduleId);
        return Result.ok();
    }

}
