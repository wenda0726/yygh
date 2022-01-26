package com.sjtu.yygh.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sjtu.yygh.common.exception.YyghException;
import com.sjtu.yygh.common.helper.JwtHelper;
import com.sjtu.yygh.common.result.ResultCodeEnum;

import com.sjtu.yygh.model.user.UserInfo;
import com.sjtu.yygh.user.mapper.UserInfoMapper;
import com.sjtu.yygh.user.service.UserInfoService;
import com.sjtu.yygh.vo.user.LoginVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo>
        implements UserInfoService {

    //用户登录接口
    @Override
    public Map<String, Object> login(LoginVo loginVo) {
        //首先判断一下手机号和验证码是否为空
        String phone = loginVo.getPhone();
        String code = loginVo.getCode();
        if(StringUtils.isEmpty(phone) || StringUtils.isEmpty(code)){
            throw new YyghException(ResultCodeEnum.PARAM_ERROR);
        }
        //根据手机号进行查询
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone",phone);
        UserInfo userInfo = baseMapper.selectOne(queryWrapper);
        if(userInfo == null){
            //该手机号没有进行过注册
            userInfo = new UserInfo();
            userInfo.setName("");
            userInfo.setPhone(phone);
            userInfo.setStatus(1);
            baseMapper.insert(userInfo);
        }
        //校验用户是否被禁用
        if(userInfo.getStatus() == 0){
            throw new YyghException(ResultCodeEnum.LOGIN_DISABLED_ERROR);
        }
        Map<String,Object> resultMap = new HashMap<>();
        String name = userInfo.getName();
        if(StringUtils.isEmpty(name)){
            name = userInfo.getNickName();
        }
        if(StringUtils.isEmpty(name)){
            name = userInfo.getPhone();
        }
        resultMap.put("name",name);
        // token生成以及用户登录校验
        String token = JwtHelper.createToken(userInfo.getId(), name);
        resultMap.put("token",token);

        return resultMap;
    }


}
