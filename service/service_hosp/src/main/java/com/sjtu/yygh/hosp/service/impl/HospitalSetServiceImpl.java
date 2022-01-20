package com.sjtu.yygh.hosp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sjtu.yygh.hosp.mapper.HospitalSetMapper;
import com.sjtu.yygh.hosp.service.HospitalSetService;
import com.sjtu.yygh.model.hosp.HospitalSet;
import org.springframework.stereotype.Service;

@Service
public class HospitalSetServiceImpl extends ServiceImpl<HospitalSetMapper, HospitalSet>
        implements HospitalSetService {


    @Override
    public String getSignByHoscode(String hoscode) {
        QueryWrapper<HospitalSet> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("hoscode",hoscode);
        HospitalSet hospitalSet = baseMapper.selectOne(queryWrapper);
        if(hospitalSet == null){
            return "";
        }
        return hospitalSet.getSignKey();
    }
}
