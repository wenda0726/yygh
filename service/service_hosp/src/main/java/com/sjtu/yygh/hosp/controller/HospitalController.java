package com.sjtu.yygh.hosp.controller;



import com.sjtu.yygh.common.result.Result;
import com.sjtu.yygh.hosp.service.HospitalService;
import com.sjtu.yygh.model.hosp.Hospital;
import com.sjtu.yygh.vo.hosp.HospitalQueryVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(tags = "医院管理接口")
@RestController
@RequestMapping("/admin/hosp/hospital")
//@CrossOrigin
public class HospitalController {

    @Autowired
    private HospitalService hospitalService;


    @ApiOperation(value = "获取分页列表")
    @GetMapping("/list/{page}/{limit}")
    public Result index(@PathVariable("page")Integer page,
                        @PathVariable("limit")Integer limit,
                        HospitalQueryVo hospitalQueryVo){
        Page<Hospital> pageModel = hospitalService.selectPage(page,limit,hospitalQueryVo);
        return Result.ok(pageModel);
    }

    @ApiOperation(value = "更新上线状态")
    @GetMapping("updateStatus/{id}/{status}")
    public Result updateStatus(@PathVariable("id")String id, @PathVariable("status") Integer status){
        hospitalService.updateStatus(id,status);
        return Result.ok();
    }

    @ApiOperation(value = "获取医院详情")
    @GetMapping("showHospDetail/{id}")
    public Result show(@PathVariable("id")String id){
        Map<String,Object> result = hospitalService.showById(id);
        return Result.ok(result);
    }

}
