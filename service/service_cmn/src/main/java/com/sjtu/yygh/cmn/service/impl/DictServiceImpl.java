package com.sjtu.yygh.cmn.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sjtu.yygh.cmn.listener.DictListener;
import com.sjtu.yygh.cmn.mapper.DictMapper;
import com.sjtu.yygh.cmn.service.DictService;
import com.sjtu.yygh.model.cmn.Dict;
import com.sjtu.yygh.vo.cmn.DictEeVo;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

@Service
public class DictServiceImpl extends ServiceImpl<DictMapper, Dict> implements DictService {
    @Override
    @Cacheable(value = "dict", keyGenerator = "keyGenerator")
    public List<Dict> findChildData(Long id) {
        QueryWrapper<Dict> wrapper = new QueryWrapper<>();
        wrapper.eq("parent_id", id);
        List<Dict> dicts = baseMapper.selectList(wrapper);
        for (Dict dict : dicts) {
            Long dictId = dict.getId();
            dict.setHasChildren(this.hasChildren(dictId));
        }
        return dicts;
    }

    //判读当前id下面是否还有子节点
    private boolean hasChildren(Long id) {
        QueryWrapper<Dict> wrapper = new QueryWrapper<>();
        wrapper.eq("parent_id", id);
        Integer count = baseMapper.selectCount(wrapper);
        return count > 0;
    }

    @Override
    public void exportData(HttpServletResponse response) {

        //设置下载信息
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        // 这里URLEncoder.encode可以防止中文乱码 当然和easyexcel没有关系
        String fileName = "dict";
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");

        //查询数据库中的数据
        List<Dict> dicts = baseMapper.selectList(null);
        List<DictEeVo> vos = new ArrayList<>();
        //对象转换
        for (Dict dict : dicts) {
            DictEeVo vo = new DictEeVo();
            BeanUtils.copyProperties(dict, vo);
            vos.add(vo);
        }
        try {
            EasyExcel.write(response.getOutputStream(), DictEeVo.class).sheet("dict").doWrite(vos);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //导入数据字典
    @Override
    @CacheEvict(value = "dict", allEntries = true)
    public void importData(MultipartFile multipartFile) {
        try {
            EasyExcel.read(multipartFile.getInputStream(), DictEeVo.class, new DictListener(baseMapper))
                    .sheet().doRead();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //根据parentDictCode 和 value查询
    @Override
    public String getName(String parentDictCode, String value) {
        if(StringUtils.isEmpty(parentDictCode)){
           QueryWrapper<Dict> queryWrapper = new QueryWrapper<>();
           queryWrapper.eq("value",value);
           Dict dict = baseMapper.selectOne(queryWrapper);
           return dict == null ? "" : dict.getName();
        }else{
            Dict parentDict = this.getByDictCode(parentDictCode);
            Long parentId = parentDict != null ? parentDict.getId() : -1;
            QueryWrapper<Dict> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("parent_id",parentId);
            queryWrapper.eq("value",value);
            Dict dict = baseMapper.selectOne(queryWrapper);
            return dict == null ? "" : dict.getName();
        }
    }

    //根据dictCode查询下层节点
    @Override
    public List<Dict> findByDictCode(String dictCode) {
        Dict dict = this.getByDictCode(dictCode);

        return this.findChildData(dict.getId());
    }

    //根据dictCode查询Dict
    private Dict getByDictCode(String dictCode){
        QueryWrapper<Dict> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("dict_code",dictCode);
        return baseMapper.selectOne(queryWrapper);
    }



}
