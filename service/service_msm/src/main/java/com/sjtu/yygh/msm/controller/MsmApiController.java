package com.sjtu.yygh.msm.controller;


import com.sjtu.yygh.common.result.Result;
import com.sjtu.yygh.msm.service.MsmService;
import com.sjtu.yygh.msm.utils.RandomUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/msm")
public class MsmApiController {

    @Autowired
    private MsmService msmService;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @GetMapping("send/{phone}")
    public Result sendCode(@PathVariable String phone){
        //首先在redis中通过手机号进行获取验证码
        String code = redisTemplate.opsForValue().get(phone);
        if(!StringUtils.isEmpty(code)){
            return Result.ok();
        }
        //如果redis缓存中没有验证码，则需要进行发送
        //首先由工具类生成6位随机验证码
        code = RandomUtil.getSixBitRandom();
        boolean isSend = msmService.send(phone,code);
        if(isSend){
            //发送成功，将验证码和手机号存入redis缓存
            redisTemplate.opsForValue().set(phone,code,5, TimeUnit.MINUTES);
            return Result.ok();
        }else{
            return Result.fail().message("验证短信发送失败");
        }
    }
}
