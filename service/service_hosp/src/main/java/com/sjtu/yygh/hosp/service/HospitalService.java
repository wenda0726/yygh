package com.sjtu.yygh.hosp.service;


import com.sjtu.yygh.model.hosp.Hospital;
import com.sjtu.yygh.vo.hosp.HospitalQueryVo;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface HospitalService {
    void save(Map<String, Object> parameterMap);

    Hospital getByHoscode(String hoscode);

    Page<Hospital> selectPage(Integer page, Integer limit, HospitalQueryVo hospitalQueryVo);

    void updateStatus(String id, Integer status);


    Map<String, Object> showById(String id);

    String getHosname(String hoscode);

    List<Hospital> findByHosname(String hosname);

}
