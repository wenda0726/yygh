package com.sjtu.yygh.hosp.service;


import com.sjtu.yygh.model.hosp.Department;
import com.sjtu.yygh.vo.hosp.DepartmentQueryVo;
import com.sjtu.yygh.vo.hosp.DepartmentVo;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface DepartmentService {
    void save(Map<String, Object> parameterMap);

    Page<Department> selectPage(int page, int limit, DepartmentQueryVo queryVo);

    void remove(String hoscode, String depcode);

    List<DepartmentVo> findDeptTree(String hoscode);

    String getDepname(String hoscode, String depcode);

    Department getDepartment(String hoscode, String depcode);
}
