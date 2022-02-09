package com.sjtu.yygh.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sjtu.yygh.cmn.client.DictFeignClient;
import com.sjtu.yygh.enums.DictEnum;
import com.sjtu.yygh.model.user.Patient;
import com.sjtu.yygh.user.mapper.PatientMapper;
import com.sjtu.yygh.user.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientServiceImpl extends ServiceImpl<PatientMapper, Patient> implements PatientService {

    @Autowired
    private DictFeignClient dictFeignClient;
    @Override
    public List<Patient> findAllByUserId(Long userId) {
        QueryWrapper<Patient> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id",userId);
        List<Patient> patientList = baseMapper.selectList(queryWrapper);
        patientList.stream().forEach(patient -> {
            //远程调用，对省份信息、证件信息等参数进行封装
            this.packPatient(patient);
        });
        return patientList;
    }

    @Override
    public Patient getPatientById(Long id) {
        Patient patient = baseMapper.selectById(id);
        this.packPatient(patient);
        return patient;
    }

    private void packPatient(Patient patient) {
        //根据证件类型编码，获取证件类型
        String certificatesTypeString  =
                dictFeignClient.getName(DictEnum.CERTIFICATES_TYPE.getDictCode(), patient.getCertificatesType());
        //联系人证件类型
        String contactsCertificatesTypeString =
                dictFeignClient.getName(DictEnum.CERTIFICATES_TYPE.getDictCode(),patient.getContactsCertificatesType());
        //省
        String provinceString = dictFeignClient.getName(patient.getProvinceCode());
        //市
        String cityString = dictFeignClient.getName(patient.getCityCode());
        //区
        String districtString = dictFeignClient.getName(patient.getDistrictCode());
        patient.getParam().put("certificatesTypeString", certificatesTypeString);
        patient.getParam().put("contactsCertificatesTypeString", contactsCertificatesTypeString);
        patient.getParam().put("provinceString", provinceString);
        patient.getParam().put("cityString", cityString);
        patient.getParam().put("districtString", districtString);
        patient.getParam().put("fullAddress", provinceString + cityString + districtString + patient.getAddress());

    }

}
