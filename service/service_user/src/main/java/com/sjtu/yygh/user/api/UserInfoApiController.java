package com.sjtu.yygh.user.api;

import com.sjtu.yygh.common.result.Result;
import com.sjtu.yygh.common.utils.AuthContextHolder;
import com.sjtu.yygh.model.user.UserInfo;
import com.sjtu.yygh.user.service.UserInfoService;
import com.sjtu.yygh.vo.user.LoginVo;
import com.sjtu.yygh.vo.user.UserAuthVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@Api(tags = "用户管理接口")
public class UserInfoApiController {

    @Autowired
    private UserInfoService userInfoService;

    @ApiOperation(value = "登录接口")
    @PostMapping("login")
    public Result login(@RequestBody LoginVo loginVo){
        Map<String,Object> result = userInfoService.login(loginVo);

        return Result.ok(result);
    }

    //用户认证接口
    @PostMapping("auth/userAuth")
    public Result userAuth(@RequestBody UserAuthVo userAuthVo, HttpServletRequest request){
        Long userId = AuthContextHolder.getUserId(request);
        userInfoService.userAuth(userId,userAuthVo);
        return Result.ok();
    }

    //根据用户id获取用户信息接口
    @GetMapping("auth/getUserInfo")
    public Result getUserInfo(HttpServletRequest request){
        Long userId = AuthContextHolder.getUserId(request);
        UserInfo userInfo = userInfoService.getById(userId);
        return Result.ok(userInfo);
    }
}
