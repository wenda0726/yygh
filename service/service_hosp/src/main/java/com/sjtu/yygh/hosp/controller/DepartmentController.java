package com.sjtu.yygh.hosp.controller;

import com.sjtu.yygh.common.result.Result;
import com.sjtu.yygh.hosp.service.DepartmentService;
import com.sjtu.yygh.vo.hosp.DepartmentVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
//@CrossOrigin
@RequestMapping("/admin/hosp/department")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    //根据医院编号，查询医院所有科室列表
    @ApiOperation(value = "查询医院所有科室列表")
    @GetMapping("getDeptList/{hoscode}")
    public Result getDeptList(@PathVariable("hoscode")String hoscode){
        List<DepartmentVo> result = departmentService.findDeptTree(hoscode);
        return Result.ok(result);

    }

}
