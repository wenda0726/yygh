package com.sjtu.yygh.hosp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sjtu.yygh.model.hosp.HospitalSet;
import com.sjtu.yygh.vo.order.SignInfoVo;

public interface HospitalSetService extends IService<HospitalSet> {
    String getSignByHoscode(String hoscode);

    //获取医院签名信息
    SignInfoVo getSignInfoVo(String hoscode);
}
