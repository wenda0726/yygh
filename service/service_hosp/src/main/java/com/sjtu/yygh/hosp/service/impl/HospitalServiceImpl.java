package com.sjtu.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.sjtu.yygh.cmn.client.DictFeignClient;
import com.sjtu.yygh.common.result.Result;
import com.sjtu.yygh.hosp.repository.HospitalRepository;
import com.sjtu.yygh.hosp.service.HospitalService;
import com.sjtu.yygh.model.hosp.Hospital;
import com.sjtu.yygh.vo.hosp.HospitalQueryVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class HospitalServiceImpl implements HospitalService {

    @Autowired
    private HospitalRepository hospitalRepository;
    @Autowired
    private DictFeignClient dictFeignClient;

    @Override
    public void save(Map<String, Object> parameterMap) {
        //将传过来的数据提取成对象
        String jsonString = JSONObject.toJSONString(parameterMap);
        Hospital hospital = JSONObject.parseObject(jsonString, Hospital.class);
        String hosname = hospital.getHosname();
        //首先查询数据库有无数据
        Hospital targetHosp =  hospitalRepository.getHospitalByHosname(hosname);

        //如果有则更新
        if(null != targetHosp){
            hospital.setStatus(targetHosp.getStatus());
            hospital.setCreateTime(targetHosp.getCreateTime());
        }else{
            //没有则添加
            //0表示还没有上线
            hospital.setStatus(0);
            hospital.setCreateTime(new Date());
        }
        hospital.setUpdateTime(new Date());
        hospital.setIsDeleted(0);
        hospitalRepository.save(hospital);


    }

    @Override
    public Hospital getByHoscode(String hoscode) {
        return hospitalRepository.getHospitalByHoscode(hoscode);
    }

    @Override
    public Page<Hospital> selectPage(Integer page, Integer limit, HospitalQueryVo hospitalQueryVo) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Hospital hospital = new Hospital();
        BeanUtils.copyProperties(hospitalQueryVo,hospital);
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
                .withIgnoreCase(true);
        Example<Hospital> example = Example.of(hospital,matcher);
        Page<Hospital> hospitalPage = hospitalRepository.findAll(example, pageable);
        List<Hospital> content = hospitalPage.getContent();
        for (Hospital item : content) {
            this.setHospitalHosType(item);
        }
        return hospitalPage;
    }

    @Override
    public void updateStatus(String id, Integer status) {
        Hospital hospital = hospitalRepository.getHospitalById(id);
        if(hospital != null){
            hospital.setStatus(status);
            hospital.setUpdateTime(new Date());
            hospitalRepository.save(hospital);

        }
    }

    @Override
    public Map<String, Object> showById(String id) {
        Map<String,Object> map = new HashMap<>();
        Hospital hospital = hospitalRepository.getHospitalById(id);
        this.setHospitalHosType(hospital);
        //单独处理更直观
        map.put("hospital",hospital);
        map.put("bookingRule", hospital.getBookingRule());
        //不需要重复返回
        hospital.setBookingRule(null);
        return map;
    }

    @Override
    public String getHosname(String hoscode) {
        Hospital hospital = this.getByHoscode(hoscode);
        return hospital != null ? hospital.getHosname() : "";
    }

    @Override
    public List<Hospital> findByHosname(String hosname) {
        List<Hospital> list = hospitalRepository.findHospitalByHosnameLike(hosname);
        return list;
    }


    //获取查询List集合，遍历进行医院等级的封装
    private void setHospitalHosType(Hospital hospital) {
        //根据diccode 和 hostype 获取到医院等级信息
        String hostypeString =  dictFeignClient.getName("Hostype",hospital.getHostype());
        //查询省 、市 、区
        String provinceString = dictFeignClient.getName(hospital.getProvinceCode());
        String cityString = dictFeignClient.getName(hospital.getCityCode());
        String districtString = dictFeignClient.getName(hospital.getDistrictCode());
        hospital.getParam().put("fullAddress",provinceString+cityString+districtString);
        hospital.getParam().put("hostypeString",hostypeString);
        return ;
    }
}
