package com.sjtu.yygh.hosp.repository;

import com.sjtu.yygh.model.hosp.Hospital;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HospitalRepository extends MongoRepository<Hospital,String> {
    Hospital getHospitalByHosname(String hosname);

    Hospital getHospitalByHoscode(String hoscode);

    Hospital getHospitalById(String id);

    List<Hospital> findHospitalByHosnameLike(String hosname);
}
