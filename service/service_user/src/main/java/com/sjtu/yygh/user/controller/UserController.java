package com.sjtu.yygh.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sjtu.yygh.common.result.Result;

import com.sjtu.yygh.model.user.UserInfo;
import com.sjtu.yygh.user.service.UserInfoService;
import com.sjtu.yygh.vo.user.UserInfoQueryVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/user")
public class UserController {
    @Autowired
    private UserInfoService userInfoService;

    //添加查询带分页
    @GetMapping("{page}/{limit}")
    public Result list(@PathVariable Long page,
                       @PathVariable Long limit,
                       UserInfoQueryVo queryVo){
        Page<UserInfo> pageParam = new Page<>(page,limit);
        IPage<UserInfo> pageModel = userInfoService.selectPage(pageParam,queryVo);
        return Result.ok(pageParam);
    }

    @ApiOperation(value = "锁定")
    @GetMapping("lock/{userId}/{status}")
    public Result lock(@PathVariable Long userId,
                       @PathVariable Integer status){
        userInfoService.lock(userId,status);
        return Result.ok();
    }

    //用户详情
    @GetMapping("show/{userId}")
    public Result show(@PathVariable Long userId){
        Map<String,Object> map = userInfoService.show(userId);
        return Result.ok(map);
    }

    //认证审批
    @GetMapping("approval/{userId}/{authStatus}")
    public Result approval(@PathVariable Long userId,@PathVariable Integer authStatus){
        userInfoService.approval(userId,authStatus);
        return Result.ok();
    }


}
