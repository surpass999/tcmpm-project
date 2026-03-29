package cn.gemrun.base.module.declare.api;

import cn.gemrun.base.module.declare.controller.admin.hospital.vo.DeclareHospitalRespVO;

import java.util.List;

/**
 * 医院信息 Api 接口
 *
 * 用于其他模块获取医院信息
 *
 * @author Gemini
 */
public interface DeclareHospitalApi {

    /**
     * 获取医院详情
     *
     * @param id 医院ID
     * @return 医院信息，如果不存在返回 null
     */
    DeclareHospitalRespVO getHospital(Long id);

    /**
     * 根据编码获取医院详情
     *
     * @param hospitalCode 医院编码
     * @return 医院信息，如果不存在返回 null
     */
    DeclareHospitalRespVO getHospitalByCode(String hospitalCode);

    /**
     * 获取医院列表（简表）
     *
     * @return 医院列表
     */
    List<DeclareHospitalRespVO> getSimpleHospitalList();

    /**
     * 获取医院列表（按省份）
     *
     * @param provinceCode 省份编码
     * @return 医院列表
     */
    List<DeclareHospitalRespVO> getHospitalListByProvince(String provinceCode);

}
