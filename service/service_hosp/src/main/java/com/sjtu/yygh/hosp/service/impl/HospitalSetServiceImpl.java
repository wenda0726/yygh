package com.sjtu.yygh.hosp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sjtu.yygh.common.exception.YyghException;
import com.sjtu.yygh.common.result.ResultCodeEnum;
import com.sjtu.yygh.hosp.mapper.HospitalSetMapper;
import com.sjtu.yygh.hosp.service.HospitalSetService;
import com.sjtu.yygh.model.hosp.HospitalSet;
import com.sjtu.yygh.vo.order.SignInfoVo;
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

    @Override
    public SignInfoVo getSignInfoVo(String hoscode) {
        SignInfoVo signInfoVo = new SignInfoVo();
        QueryWrapper<HospitalSet> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("hoscode",hoscode);
        HospitalSet hospitalSet = baseMapper.selectOne(queryWrapper);
        if(null == hospitalSet){
            throw new YyghException(ResultCodeEnum.PARAM_ERROR);
        }
        signInfoVo.setApiUrl(hospitalSet.getApiUrl());
        signInfoVo.setSignKey(hospitalSet.getSignKey());

        return signInfoVo;
    }
}
