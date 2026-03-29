package cn.gemrun.base.module.declare.api.impl;

import cn.gemrun.base.module.declare.api.DeclareHospitalApi;
import cn.gemrun.base.module.declare.service.hospital.DeclareHospitalService;
import cn.gemrun.base.module.declare.controller.admin.hospital.vo.DeclareHospitalRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 医院信息 Api 实现
 *
 * @author Gemini
 */
@Service
@RequiredArgsConstructor
public class DeclareHospitalApiImpl implements DeclareHospitalApi {

    @Lazy
    private final DeclareHospitalService hospitalService;

    @Override
    public DeclareHospitalRespVO getHospital(Long id) {
        return hospitalService.getHospital(id);
    }

    @Override
    public DeclareHospitalRespVO getHospitalByCode(String hospitalCode) {
        return hospitalService.getHospitalByCode(hospitalCode);
    }

    @Override
    public List<DeclareHospitalRespVO> getSimpleHospitalList() {
        return hospitalService.getSimpleHospitalList();
    }

    @Override
    public List<DeclareHospitalRespVO> getHospitalListByProvince(String provinceCode) {
        return hospitalService.getHospitalListByProvince(provinceCode);
    }

}
