package com.sjtu.yygh.hosp.service.impl;
import java.util.ArrayList;
import java.util.Date;

import com.google.common.collect.Maps;

import com.alibaba.fastjson.JSONObject;
import com.sjtu.yygh.common.exception.YyghException;
import com.sjtu.yygh.common.result.ResultCodeEnum;
import com.sjtu.yygh.hosp.repository.DepartmentRepository;
import com.sjtu.yygh.hosp.service.DepartmentService;
import com.sjtu.yygh.model.hosp.Department;
import com.sjtu.yygh.vo.hosp.DepartmentQueryVo;
import com.sjtu.yygh.vo.hosp.DepartmentVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Override
    public void remove(String hoscode, String depcode) {
        if(StringUtils.isEmpty(hoscode) || StringUtils.isEmpty(depcode)){
            throw new YyghException(ResultCodeEnum.CODE_ERROR);
        }
        Department department = departmentRepository.getDepartmentByHoscodeAndDepcode(hoscode, depcode);
        if(null != department){
            departmentRepository.deleteById(department.getId());
        }
    }

    @Override
    public List<DepartmentVo> findDeptTree(String hoscode) {
        List<DepartmentVo> result = new ArrayList<>();

        //首先根据医院编号查询到所有科室的信息
        Department query = new Department();
        query.setHoscode(hoscode);
        Example<Department> example = Example.of(query);
        List<Department> all = departmentRepository.findAll(example);
        //利用IO流操作进行分类 (根据department的bigcode进行分类)
        //key 为bigcode value 为分类完成的同一个bigcode下的department对象
        Map<String, List<Department>> collect = all.stream().collect(Collectors.groupingBy(Department::getBigcode));
        //遍历map集合进行封装
        for(Map.Entry<String,List<Department>> entry : collect.entrySet()){
            //父
            DepartmentVo parent = new DepartmentVo();
            String bigcode = entry.getKey();
            List<Department> value = entry.getValue();
            parent.setDepcode(bigcode);
            parent.setDepname(value.get(0).getBigname());
            //子
            List<DepartmentVo> children = new ArrayList<>();
            for(Department item : value){
                DepartmentVo child = new DepartmentVo();
                child.setDepname(item.getDepname());
                child.setDepcode(item.getDepcode());
                children.add(child);
            }
            parent.setChildren(children);
            result.add(parent);
        }
        return result;
    }

    @Override
    public String getDepname(String hoscode, String depcode) {
        Department department = departmentRepository.getDepartmentByHoscodeAndDepcode(hoscode, depcode);
        return department != null ? department.getDepname() : "";
    }

    @Override
    public void save(Map<String, Object> parameterMap) {
        String jsonString = JSONObject.toJSONString(parameterMap);
        Department department = JSONObject.parseObject(jsonString, Department.class);
        //首先根据医院id和科室id进行查询
        Department exits = departmentRepository.
                getDepartmentByHoscodeAndDepcode(department.getHoscode(),department.getDepcode());

        if(exits != null){
            exits.setUpdateTime(new Date());
            exits.setIsDeleted(0);
            departmentRepository.save(exits);
        }else{
            department.setCreateTime(new Date());
            department.setUpdateTime(new Date());
            department.setIsDeleted(0);
            departmentRepository.save(department);
        }

    }

    @Override
    public Page<Department> selectPage(int page, int limit, DepartmentQueryVo queryVo) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Department department = new Department();
        BeanUtils.copyProperties(queryVo,department);
        department.setIsDeleted(0);
        ExampleMatcher matcher = ExampleMatcher.matching().
                withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
                .withIgnoreCase(true);
        Example<Department> example = Example.of(department,matcher);
        return departmentRepository.findAll(example,pageable);
    }


}
