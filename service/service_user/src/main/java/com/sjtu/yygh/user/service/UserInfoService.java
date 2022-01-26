package com.sjtu.yygh.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sjtu.yygh.model.user.UserInfo;
import com.sjtu.yygh.vo.user.LoginVo;

import java.util.Map;

public interface UserInfoService extends IService<UserInfo> {
    Map<String, Object> login(LoginVo loginVo);
}
