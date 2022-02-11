package com.sjtu.yygh.msm.service;

import com.sjtu.yygh.vo.msm.MsmVo;

public interface MsmService {
    boolean send(String phone, String code);

    //使用mq进行短信发送的接口
    boolean send(MsmVo msmVo);

}
