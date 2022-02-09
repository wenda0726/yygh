package com.sjtu.yygh.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sjtu.yygh.common.exception.YyghException;
import com.sjtu.yygh.common.helper.JwtHelper;
import com.sjtu.yygh.common.result.ResultCodeEnum;

import com.sjtu.yygh.common.utils.CookieUtil;
import com.sjtu.yygh.enums.AuthStatusEnum;
import com.sjtu.yygh.model.user.Patient;
import com.sjtu.yygh.model.user.UserInfo;
import com.sjtu.yygh.user.mapper.UserInfoMapper;
import com.sjtu.yygh.user.service.PatientService;
import com.sjtu.yygh.user.service.UserInfoService;
import com.sjtu.yygh.vo.user.LoginVo;
import com.sjtu.yygh.vo.user.UserAuthVo;
import com.sjtu.yygh.vo.user.UserInfoQueryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo>
        implements UserInfoService {

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @Autowired
    private PatientService patientService;

    //用户登录接口
    @Override
    public Map<String, Object> login(LoginVo loginVo) {
        //首先判断一下手机号和验证码是否为空
        String phone = loginVo.getPhone();
        String code = loginVo.getCode();
        if(StringUtils.isEmpty(phone) || StringUtils.isEmpty(code)){
            throw new YyghException(ResultCodeEnum.PARAM_ERROR);
        }
        //验证码核对
        String redisCode = redisTemplate.opsForValue().get(phone);
        if(!code.equals(redisCode)){
            throw new YyghException(ResultCodeEnum.CODE_ERROR);
        }
        //微信扫码登录
        UserInfo userInfo = null;
        //openid为空需要绑定手机号，不为空是第二次登录
        if(!StringUtils.isEmpty(loginVo.getOpenid())) {
            //绑定的手机号以前有没有进行过登录注册
            UserInfo exits = this.selectByPhone(phone);
            if(exits != null){
                //之间进行过登录注册
                //逻辑删除之前的信息
                baseMapper.deleteById(exits.getId());
            }
            //数据库中取出微信登录的信息
            userInfo = this.selectByOpenid(loginVo.getOpenid());
            if(null != userInfo) {
                userInfo.setPhone(loginVo.getPhone());
                this.updateById(userInfo);
            } else {
                throw new YyghException(ResultCodeEnum.DATA_ERROR);
            }
        }
        //直接第一次使用手机进行登录
        if (userInfo == null) {
            //该手机号没有进行过注册
            //根据手机号进行查询
            userInfo = this.selectByPhone(phone);
            if(userInfo == null){
                userInfo = new UserInfo();
                userInfo.setName("");
                userInfo.setPhone(phone);
                userInfo.setStatus(1);
                baseMapper.insert(userInfo);
            }
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

    @Override
    public UserInfo selectByOpenid(String openid) {
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("openid",openid);
        UserInfo userInfo = baseMapper.selectOne(queryWrapper);
        return userInfo;
    }

    //用户认证接口
    @Override
    public void userAuth(Long userId, UserAuthVo userAuthVo) {
        UserInfo userInfo = baseMapper.selectById(userId);
        userInfo.setName(userAuthVo.getName());
        //用户信息更新
        userInfo.setCertificatesType(userAuthVo.getCertificatesType());
        userInfo.setCertificatesNo(userAuthVo.getCertificatesNo());
        userInfo.setCertificatesUrl(userAuthVo.getCertificatesUrl());
        userInfo.setAuthStatus(AuthStatusEnum.AUTH_RUN.getStatus());
        //信息更新
        baseMapper.updateById(userInfo);
    }

    //用户列表
    @Override
    public IPage<UserInfo> selectPage(Page<UserInfo> pageParam, UserInfoQueryVo queryVo) {
        Integer authStatus = queryVo.getAuthStatus();
        String createTimeBegin = queryVo.getCreateTimeBegin();
        String createTimeEnd = queryVo.getCreateTimeEnd();
        String keyword = queryVo.getKeyword();
        Integer status = queryVo.getStatus();
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        //对条件值进行非空判断
        if(!StringUtils.isEmpty(keyword)){
            queryWrapper.like("name",keyword);
        }
        if(status != null){
            queryWrapper.eq("status",status);
        }
        if(authStatus != null){
            queryWrapper.eq("auth_status",authStatus);
        }
        if(!StringUtils.isEmpty(createTimeBegin)){
            queryWrapper.ge("create_time",createTimeBegin);
        }
        if(!StringUtils.isEmpty(createTimeEnd)){
            queryWrapper.le("create_time",createTimeEnd);
        }
        Page<UserInfo> userInfoPage = baseMapper.selectPage(pageParam, queryWrapper);
        userInfoPage.getRecords().stream().forEach(userInfo -> {
            this.packageUserInfo(userInfo);
        });
        return userInfoPage;
    }

    //用户状态锁定
    @Override
    public void lock(Long userId, Integer status) {
        if(status == 0 || status == 1){
            UserInfo userInfo = baseMapper.selectById(userId);
            userInfo.setStatus(status);
            baseMapper.updateById(userInfo);
        }
    }

    //用户详情信息，包含就诊人信息
    @Override
    public Map<String, Object> show(Long userId) {
        Map<String, Object> map = new HashMap<>();
        UserInfo userInfo = baseMapper.selectById(userId);
        this.packageUserInfo(userInfo);
        List<Patient> patientList = patientService.findAllByUserId(userId);
        map.put("userInfo",userInfo);
        map.put("patientList",patientList);

        return map;
    }

    //用户认证
    @Override
    public void approval(Long userId, Integer authStatus) {
        if(authStatus == 2 || authStatus == -1){
            UserInfo userInfo = baseMapper.selectById(userId);
            userInfo.setAuthStatus(authStatus);
            baseMapper.updateById(userInfo);
        }
    }

    private void packageUserInfo(UserInfo userInfo){
        Map<String, Object> param = userInfo.getParam();
        param.put("authStatusString",AuthStatusEnum.getStatusNameByStatus(userInfo.getAuthStatus()));
        String statusString = userInfo.getStatus() == 0 ? "锁定" :"正常";
        param.put("statusString",statusString);
    }

    private UserInfo selectByPhone(String phone){
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone",phone);
        return baseMapper.selectOne(queryWrapper);
    }


}
