package cn.gemrun.base.module.declare.service.hospital;

import cn.gemrun.base.module.declare.controller.admin.hospital.vo.DeclareHospitalCreateReqVO;
import cn.gemrun.base.module.declare.controller.admin.hospital.vo.DeclareHospitalRespVO;
import cn.gemrun.base.module.declare.controller.admin.hospital.vo.DeclareHospitalPageReqVO;
import cn.gemrun.base.module.declare.controller.admin.hospital.vo.DeclareHospitalUpdateReqVO;
import cn.gemrun.base.module.declare.controller.admin.hospital.vo.HospitalImportExcelVO;
import cn.gemrun.base.module.declare.controller.admin.hospital.vo.HospitalImportRespVO;
import cn.gemrun.base.framework.common.pojo.PageResult;
import javax.validation.Valid;

import java.util.List;

/**
 * 医院信息Service
 *
 * @author Gemini
 */
public interface DeclareHospitalService {

    /**
     * 创建医院
     */
    Long createHospital(@Valid DeclareHospitalCreateReqVO reqVO);

    /**
     * 更新医院
     */
    void updateHospital(@Valid DeclareHospitalUpdateReqVO reqVO);

    /**
     * 删除医院
     */
    void deleteHospital(Long id);

    /**
     * 获取医院详情
     */
    DeclareHospitalRespVO getHospital(Long id);

    /**
     * 获取医院详情（根据编码）
     */
    DeclareHospitalRespVO getHospitalByCode(String hospitalCode);

    /**
     * 获取医院详情（根据部门ID）
     */
    DeclareHospitalRespVO getHospitalByDeptId(Long deptId);

    /**
     * 获取医院分页列表
     */
    PageResult<DeclareHospitalRespVO> getHospitalPage(DeclareHospitalPageReqVO reqVO);

    /**
     * 获取医院列表（简表）
     */
    List<DeclareHospitalRespVO> getSimpleHospitalList();

    /**
     * 获取医院列表（按省份）
     */
    List<DeclareHospitalRespVO> getHospitalListByProvince(String provinceCode);

    /**
     * 批量导入医院
     */
    HospitalImportRespVO importHospitalList(List<HospitalImportExcelVO> list, boolean updateSupport);

}
