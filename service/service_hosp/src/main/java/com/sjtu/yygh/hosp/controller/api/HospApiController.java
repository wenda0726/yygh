package com.sjtu.yygh.hosp.controller.api;

import com.baomidou.mybatisplus.extension.api.R;
import com.sjtu.yygh.common.result.Result;
import com.sjtu.yygh.hosp.service.DepartmentService;
import com.sjtu.yygh.hosp.service.HospitalService;
import com.sjtu.yygh.model.hosp.Hospital;
import com.sjtu.yygh.vo.hosp.DepartmentVo;
import com.sjtu.yygh.vo.hosp.HospitalQueryVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Api(tags = "医院管理接口")
@RestController
@RequestMapping("/api/hosp/hospital")
public class HospApiController {

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private DepartmentService departmentService;

    @ApiOperation(value = "查询医院列表")
    @GetMapping("findHospList/{page}/{limit}")
    public Result findHospList(@PathVariable Integer page, @PathVariable Integer limit, HospitalQueryVo hospitalQueryVo){
        Page<Hospital> hospitals = hospitalService.selectPage(page, limit, hospitalQueryVo);
        return Result.ok(hospitals);
    }

    @ApiOperation(value = "根据医院名称模糊查询医院")
    @GetMapping("findByHosname/{hosname}")
    public Result findByHosname(@PathVariable String hosname){
        List<Hospital> lists = hospitalService.findByHosname(hosname);
        return Result.ok(lists);
    }

    @ApiOperation(value = "根据医院编号查询医院详情信息")
    @GetMapping("department/{hoscode}")
    public Result index(@PathVariable("hoscode")String hoscode){
        List<DepartmentVo> deptTree = departmentService.findDeptTree(hoscode);
        return Result.ok(deptTree);
    }

    @ApiOperation(value = "根据医院编号查询医院挂号详情规则")
    @GetMapping("findHospDetail/{hoscode}")
    public Result findHospDetail(@PathVariable("hoscode") String hoscode){
        Map<String,Object> map = hospitalService.getBookingRule(hoscode);
        return Result.ok(map);
    }


}
