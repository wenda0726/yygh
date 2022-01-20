package com.sjtu.yygh.hosp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sjtu.yygh.model.hosp.HospitalSet;

public interface HospitalSetService extends IService<HospitalSet> {
    String getSignByHoscode(String hoscode);
}
