package com.sjtu.yygh.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sjtu.yygh.model.user.UserInfo;
import com.sjtu.yygh.vo.user.LoginVo;
import com.sjtu.yygh.vo.user.UserAuthVo;
import com.sjtu.yygh.vo.user.UserInfoQueryVo;

import java.util.Map;

public interface UserInfoService extends IService<UserInfo> {
    Map<String, Object> login(LoginVo loginVo);

    UserInfo selectByOpenid(String openid);

    //用户认证接口
    void userAuth(Long userId, UserAuthVo userAuthVo);

    IPage<UserInfo> selectPage(Page<UserInfo> pageParam, UserInfoQueryVo queryVo);

    void lock(Long userId, Integer status);

    Map<String, Object> show(Long userId);

    void approval(Long userId, Integer authStatus);

}
