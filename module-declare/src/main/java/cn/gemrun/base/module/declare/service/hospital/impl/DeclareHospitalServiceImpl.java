package cn.gemrun.base.module.declare.service.hospital.impl;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.enums.CommonStatusEnum;
import cn.gemrun.base.framework.ip.core.utils.AreaUtils;
import cn.gemrun.base.module.declare.dal.dataobject.hospital.DeclareHospitalDO;
import cn.gemrun.base.module.declare.dal.mysql.DeclareHospitalMapper;
import cn.gemrun.base.module.declare.dal.mysql.HospitalTagMapper;
import cn.gemrun.base.module.declare.dal.mysql.HospitalTagRelationMapper;
import cn.gemrun.base.module.declare.service.hospital.DeclareHospitalService;
import cn.gemrun.base.module.declare.service.project.ProjectTypeService;
import cn.gemrun.base.module.declare.controller.admin.hospital.vo.DeclareHospitalCreateReqVO;
import cn.gemrun.base.module.declare.controller.admin.hospital.vo.DeclareHospitalRespVO;
import cn.gemrun.base.module.declare.controller.admin.hospital.vo.DeclareHospitalPageReqVO;
import cn.gemrun.base.module.declare.controller.admin.hospital.vo.DeclareHospitalUpdateReqVO;
import cn.gemrun.base.module.declare.controller.admin.hospital.vo.HospitalImportExcelVO;
import cn.gemrun.base.module.declare.controller.admin.hospital.vo.HospitalImportRespVO;
import cn.gemrun.base.module.declare.enums.ErrorCodeConstants;
import cn.gemrun.base.module.system.api.dept.DeptApi;
import cn.gemrun.base.module.system.api.dept.dto.DeptRespDTO;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.gemrun.base.framework.common.exception.util.ServiceExceptionUtil.exception;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 医院信息Service实现
 *
 * @author Gemini
 */
@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class DeclareHospitalServiceImpl implements DeclareHospitalService {

    private final DeclareHospitalMapper hospitalMapper;
    private final DeptApi deptApi;
    private final ProjectTypeService projectTypeService;
    private final HospitalTagMapper hospitalTagMapper;
    private final HospitalTagRelationMapper hospitalTagRelationMapper;

    /**
     * 根据区域ID获取区域名称
     */
    private String getAreaName(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        try {
            return AreaUtils.getArea(Integer.valueOf(code)) != null
                    ? AreaUtils.getArea(Integer.valueOf(code)).getName() : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHospital(DeclareHospitalCreateReqVO reqVO) {
        DeclareHospitalDO hospital = DeclareHospitalDO.builder()
                .deptId(reqVO.getDeptId())
                .hospitalCode(reqVO.getHospitalCode())
                .hospitalName(reqVO.getHospitalName())
                .shortName(reqVO.getShortName())
                .unifiedSocialCreditCode(reqVO.getUnifiedSocialCreditCode())
                .medicalLicenseNo(reqVO.getMedicalLicenseNo())
                .medicalLicenseExpire(reqVO.getMedicalLicenseExpire())
                .projectType(reqVO.getProjectType())
                .hospitalLevel(reqVO.getHospitalLevel())
                .hospitalCategory(reqVO.getHospitalCategory())
                .provinceCode(reqVO.getProvinceCode())
                .provinceName(getAreaName(reqVO.getProvinceCode()))
                .cityCode(reqVO.getCityCode())
                .cityName(getAreaName(reqVO.getCityCode()))
                .districtCode(reqVO.getDistrictCode())
                .districtName(getAreaName(reqVO.getDistrictCode()))
                .address(reqVO.getAddress())
                .contactPerson(reqVO.getContactPerson())
                .contactPhone(reqVO.getContactPhone())
                .contactEmail(reqVO.getContactEmail())
                .website(reqVO.getWebsite())
                .bedCount(reqVO.getBedCount())
                .employeeCount(reqVO.getEmployeeCount())
                .status(reqVO.getStatus())
                .build();
        hospitalMapper.insert(hospital);
        return hospital.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHospital(DeclareHospitalUpdateReqVO reqVO) {
        DeclareHospitalDO hospital = DeclareHospitalDO.builder()
                .id(reqVO.getId())
                .deptId(reqVO.getDeptId())
                .hospitalCode(reqVO.getHospitalCode())
                .hospitalName(reqVO.getHospitalName())
                .shortName(reqVO.getShortName())
                .unifiedSocialCreditCode(reqVO.getUnifiedSocialCreditCode())
                .medicalLicenseNo(reqVO.getMedicalLicenseNo())
                .medicalLicenseExpire(reqVO.getMedicalLicenseExpire())
                .projectType(reqVO.getProjectType())
                .hospitalLevel(reqVO.getHospitalLevel())
                .hospitalCategory(reqVO.getHospitalCategory())
                .provinceCode(reqVO.getProvinceCode())
                .provinceName(getAreaName(reqVO.getProvinceCode()))
                .cityCode(reqVO.getCityCode())
                .cityName(getAreaName(reqVO.getCityCode()))
                .districtCode(reqVO.getDistrictCode())
                .districtName(getAreaName(reqVO.getDistrictCode()))
                .address(reqVO.getAddress())
                .contactPerson(reqVO.getContactPerson())
                .contactPhone(reqVO.getContactPhone())
                .contactEmail(reqVO.getContactEmail())
                .website(reqVO.getWebsite())
                .bedCount(reqVO.getBedCount())
                .employeeCount(reqVO.getEmployeeCount())
                .status(reqVO.getStatus())
                .build();
        hospitalMapper.updateById(hospital);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHospital(Long id) {
        hospitalMapper.deleteById(id);
    }

    @Override
    public DeclareHospitalRespVO getHospital(Long id) {
        DeclareHospitalDO hospital = hospitalMapper.selectById(id);
        if (hospital == null) {
            return null;
        }
        Map<Long, DeptRespDTO> deptMap = hospital.getDeptId() != null
                ? deptApi.getDeptMap(Collections.singletonList(hospital.getDeptId())) : Collections.emptyMap();
        Map<String, String> tagNamesMap = hospital.getHospitalCode() != null
                ? buildTagNamesMapByHospitalCodes(Collections.singletonList(hospital.getHospitalCode())) : Collections.emptyMap();
        return convertToRespVO(hospital, deptMap, tagNamesMap);
    }

    @Override
    public DeclareHospitalRespVO getHospitalByDeptId(Long deptId) {
        DeclareHospitalDO hospital = hospitalMapper.selectByDeptId(deptId);
        if (hospital == null) {
            return null;
        }
        Map<Long, DeptRespDTO> deptMap = hospital.getDeptId() != null
                ? deptApi.getDeptMap(Collections.singletonList(hospital.getDeptId())) : Collections.emptyMap();
        Map<String, String> tagNamesMap = hospital.getHospitalCode() != null
                ? buildTagNamesMapByHospitalCodes(Collections.singletonList(hospital.getHospitalCode())) : Collections.emptyMap();
        return convertToRespVO(hospital, deptMap, tagNamesMap);
    }

    @Override
    public DeclareHospitalRespVO getHospitalByCode(String hospitalCode) {
        LambdaQueryWrapper<DeclareHospitalDO> wrapper = new LambdaQueryWrapper<DeclareHospitalDO>()
                .eq(DeclareHospitalDO::getHospitalCode, hospitalCode);
        DeclareHospitalDO hospital = hospitalMapper.selectOne(wrapper);
        if (hospital == null) {
            return null;
        }
        Map<Long, DeptRespDTO> deptMap = hospital.getDeptId() != null
                ? deptApi.getDeptMap(Collections.singletonList(hospital.getDeptId())) : Collections.emptyMap();
        Map<String, String> tagNamesMap = hospital.getHospitalCode() != null
                ? buildTagNamesMapByHospitalCodes(Collections.singletonList(hospital.getHospitalCode())) : Collections.emptyMap();
        return convertToRespVO(hospital, deptMap, tagNamesMap);
    }

    @Override
    public PageResult<DeclareHospitalRespVO> getHospitalPage(DeclareHospitalPageReqVO reqVO) {
        LambdaQueryWrapper<DeclareHospitalDO> wrapper = new LambdaQueryWrapper<>();
        if (reqVO.getHospitalName() != null && !reqVO.getHospitalName().isEmpty()) {
            wrapper.like(DeclareHospitalDO::getHospitalName, reqVO.getHospitalName());
        }
        if (reqVO.getShortName() != null && !reqVO.getShortName().isEmpty()) {
            wrapper.like(DeclareHospitalDO::getShortName, reqVO.getShortName());
        }
        if (reqVO.getDeptId() != null) {
            wrapper.eq(DeclareHospitalDO::getDeptId, reqVO.getDeptId());
        }
        if (reqVO.getProvinceCode() != null && !reqVO.getProvinceCode().isEmpty()) {
            wrapper.eq(DeclareHospitalDO::getProvinceCode, reqVO.getProvinceCode());
        }
        if (reqVO.getCityCode() != null && !reqVO.getCityCode().isEmpty()) {
            wrapper.eq(DeclareHospitalDO::getCityCode, reqVO.getCityCode());
        }
        if (reqVO.getHospitalLevel() != null && !reqVO.getHospitalLevel().isEmpty()) {
            wrapper.eq(DeclareHospitalDO::getHospitalLevel, reqVO.getHospitalLevel());
        }
        if (reqVO.getHospitalCategory() != null && !reqVO.getHospitalCategory().isEmpty()) {
            wrapper.eq(DeclareHospitalDO::getHospitalCategory, reqVO.getHospitalCategory());
        }
        if (reqVO.getProjectType() != null) {
            wrapper.eq(DeclareHospitalDO::getProjectType, reqVO.getProjectType());
        }
        if (reqVO.getStatus() != null) {
            wrapper.eq(DeclareHospitalDO::getStatus, reqVO.getStatus());
        }
        if (reqVO.getUnifiedSocialCreditCode() != null && !reqVO.getUnifiedSocialCreditCode().isEmpty()) {
            wrapper.eq(DeclareHospitalDO::getUnifiedSocialCreditCode, reqVO.getUnifiedSocialCreditCode());
        }
        wrapper.orderByDesc(DeclareHospitalDO::getId);

        // 使用分页查询
        IPage<DeclareHospitalDO> page = new Page<>(reqVO.getPageNo(), reqVO.getPageSize());
        IPage<DeclareHospitalDO> resultPage = hospitalMapper.selectPage(page, wrapper);

        // 批量获取部门信息
        List<Long> deptIds = resultPage.getRecords().stream()
                .map(DeclareHospitalDO::getDeptId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, DeptRespDTO> deptMap = deptIds.isEmpty() ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);

        // 批量获取标签名称
        List<String> hospitalCodes = resultPage.getRecords().stream()
                .map(DeclareHospitalDO::getHospitalCode)
                .filter(code -> code != null)
                .distinct()
                .collect(Collectors.toList());
        Map<String, String> tagNamesMap = buildTagNamesMapByHospitalCodes(hospitalCodes);

        List<DeclareHospitalRespVO> voList = resultPage.getRecords().stream()
                .map(hospital -> convertToRespVO(hospital, deptMap, tagNamesMap))
                .collect(Collectors.toList());

        return new PageResult<>(voList, resultPage.getTotal());
    }

    @Override
    public List<DeclareHospitalRespVO> getSimpleHospitalList() {
        LambdaQueryWrapper<DeclareHospitalDO> wrapper = new LambdaQueryWrapper<DeclareHospitalDO>()
                .eq(DeclareHospitalDO::getStatus, 1)
                .orderByAsc(DeclareHospitalDO::getHospitalName);
        List<DeclareHospitalDO> list = hospitalMapper.selectList(wrapper);
        if (CollUtil.isEmpty(list)) {
            return new java.util.ArrayList<>();
        }
        // 批量获取部门信息
        List<Long> deptIds = list.stream()
                .map(DeclareHospitalDO::getDeptId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, DeptRespDTO> deptMap = deptIds.isEmpty() ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
        // 批量获取标签名称
        List<String> hospitalCodes = list.stream()
                .map(DeclareHospitalDO::getHospitalCode)
                .filter(code -> code != null)
                .distinct()
                .collect(Collectors.toList());
        Map<String, String> tagNamesMap = buildTagNamesMapByHospitalCodes(hospitalCodes);
        return list.stream().map(hospital -> convertToRespVO(hospital, deptMap, tagNamesMap)).collect(Collectors.toList());
    }

    @Override
    public List<DeclareHospitalRespVO> getHospitalListByProvince(String provinceCode) {
        LambdaQueryWrapper<DeclareHospitalDO> wrapper = new LambdaQueryWrapper<DeclareHospitalDO>()
                .eq(DeclareHospitalDO::getProvinceCode, provinceCode)
                .eq(DeclareHospitalDO::getStatus, 1)
                .orderByAsc(DeclareHospitalDO::getHospitalName);
        List<DeclareHospitalDO> list = hospitalMapper.selectList(wrapper);
        if (CollUtil.isEmpty(list)) {
            return new java.util.ArrayList<>();
        }
        // 批量获取部门信息
        List<Long> deptIds = list.stream()
                .map(DeclareHospitalDO::getDeptId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, DeptRespDTO> deptMap = deptIds.isEmpty() ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
        // 批量获取标签名称
        List<String> hospitalCodes = list.stream()
                .map(DeclareHospitalDO::getHospitalCode)
                .filter(code -> code != null)
                .distinct()
                .collect(Collectors.toList());
        Map<String, String> tagNamesMap = buildTagNamesMapByHospitalCodes(hospitalCodes);
        return list.stream().map(hospital -> convertToRespVO(hospital, deptMap, tagNamesMap)).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HospitalImportRespVO importHospitalList(List<HospitalImportExcelVO> list, boolean updateSupport) {
        if (CollUtil.isEmpty(list)) {
            throw exception(ErrorCodeConstants.HOSPITAL_IMPORT_LIST_EMPTY);
        }

        HospitalImportRespVO respVO = HospitalImportRespVO.builder()
                .createHospitalNames(new ArrayList<>())
                .updateHospitalNames(new ArrayList<>())
                .failureHospitals(new LinkedHashMap<>())
                .build();

        int index = 1;
        for (HospitalImportExcelVO importVO : list) {
            index++;
            String hospitalName = StrUtil.blankToDefault(importVO.getHospitalName(), "第 " + index + " 行");

            // 校验必填字段
            if (StrUtil.isBlank(importVO.getHospitalName())) {
                respVO.getFailureHospitals().put(hospitalName, "医院全称不能为空");
                continue;
            }

            // 查询是否已存在（按 hospitalCode 优先，否则按 hospitalName）
            DeclareHospitalDO existHospital = null;
            if (StrUtil.isNotBlank(importVO.getHospitalCode())) {
                LambdaQueryWrapper<DeclareHospitalDO> codeWrapper = new LambdaQueryWrapper<DeclareHospitalDO>()
                        .eq(DeclareHospitalDO::getHospitalCode, importVO.getHospitalCode());
                existHospital = hospitalMapper.selectOne(codeWrapper);
            }
            if (existHospital == null) {
                LambdaQueryWrapper<DeclareHospitalDO> nameWrapper = new LambdaQueryWrapper<DeclareHospitalDO>()
                        .eq(DeclareHospitalDO::getHospitalName, importVO.getHospitalName());
                existHospital = hospitalMapper.selectOne(nameWrapper);
            }

            try {
                if (existHospital == null) {
                    // 新增
                    DeclareHospitalCreateReqVO createReqVO = convertToCreateReqVO(importVO);
                    createHospital(createReqVO);
                    respVO.getCreateHospitalNames().add(importVO.getHospitalName());
                } else if (updateSupport) {
                    // 更新
                    DeclareHospitalUpdateReqVO updateReqVO = convertToUpdateReqVO(importVO, existHospital.getId());
                    updateHospital(updateReqVO);
                    respVO.getUpdateHospitalNames().add(importVO.getHospitalName());
                } else {
                    // 已存在但不允许覆盖
                    respVO.getFailureHospitals().put(hospitalName, "医院已存在，如需更新请开启覆盖开关");
                }
            } catch (Exception e) {
                log.error("导入医院失败: {}", hospitalName, e);
                respVO.getFailureHospitals().put(hospitalName, e.getMessage());
            }
        }
        return respVO;
    }

    /**
     * 将导入VO转换为创建请求VO
     */
    private DeclareHospitalCreateReqVO convertToCreateReqVO(HospitalImportExcelVO importVO) {
        DeclareHospitalCreateReqVO reqVO = new DeclareHospitalCreateReqVO();
        reqVO.setHospitalCode(importVO.getHospitalCode());
        reqVO.setHospitalName(importVO.getHospitalName());
        reqVO.setShortName(importVO.getShortName());
        reqVO.setDeptId(importVO.getDeptId());
        reqVO.setHospitalLevel(importVO.getHospitalLevel());
        reqVO.setHospitalCategory(importVO.getHospitalCategory());
        reqVO.setProjectType(importVO.getProjectType());
        reqVO.setProvinceCode(importVO.getProvinceCode());
        reqVO.setCityCode(importVO.getCityCode());
        reqVO.setDistrictCode(importVO.getDistrictCode());
        reqVO.setAddress(importVO.getAddress());
        reqVO.setContactPerson(importVO.getContactPerson());
        reqVO.setContactPhone(importVO.getContactPhone());
        reqVO.setContactEmail(importVO.getContactEmail());
        reqVO.setWebsite(importVO.getWebsite());
        reqVO.setBedCount(importVO.getBedCount());
        reqVO.setEmployeeCount(importVO.getEmployeeCount());
        reqVO.setUnifiedSocialCreditCode(importVO.getUnifiedSocialCreditCode());
        reqVO.setMedicalLicenseNo(importVO.getMedicalLicenseNo());
        if (StrUtil.isNotBlank(importVO.getMedicalLicenseExpire())) {
            reqVO.setMedicalLicenseExpire(LocalDate.parse(importVO.getMedicalLicenseExpire()));
        }
        reqVO.setStatus(importVO.getStatus() != null ? importVO.getStatus() : CommonStatusEnum.ENABLE.getStatus());
        return reqVO;
    }

    /**
     * 将导入VO转换为更新请求VO
     */
    private DeclareHospitalUpdateReqVO convertToUpdateReqVO(HospitalImportExcelVO importVO, Long id) {
        DeclareHospitalUpdateReqVO reqVO = new DeclareHospitalUpdateReqVO();
        reqVO.setId(id);
        reqVO.setHospitalCode(importVO.getHospitalCode());
        reqVO.setHospitalName(importVO.getHospitalName());
        reqVO.setShortName(importVO.getShortName());
        reqVO.setDeptId(importVO.getDeptId());
        reqVO.setHospitalLevel(importVO.getHospitalLevel());
        reqVO.setHospitalCategory(importVO.getHospitalCategory());
        reqVO.setProjectType(importVO.getProjectType());
        reqVO.setProvinceCode(importVO.getProvinceCode());
        reqVO.setCityCode(importVO.getCityCode());
        reqVO.setDistrictCode(importVO.getDistrictCode());
        reqVO.setAddress(importVO.getAddress());
        reqVO.setContactPerson(importVO.getContactPerson());
        reqVO.setContactPhone(importVO.getContactPhone());
        reqVO.setContactEmail(importVO.getContactEmail());
        reqVO.setWebsite(importVO.getWebsite());
        reqVO.setBedCount(importVO.getBedCount());
        reqVO.setEmployeeCount(importVO.getEmployeeCount());
        reqVO.setUnifiedSocialCreditCode(importVO.getUnifiedSocialCreditCode());
        reqVO.setMedicalLicenseNo(importVO.getMedicalLicenseNo());
        if (StrUtil.isNotBlank(importVO.getMedicalLicenseExpire())) {
            reqVO.setMedicalLicenseExpire(LocalDate.parse(importVO.getMedicalLicenseExpire()));
        }
        reqVO.setStatus(importVO.getStatus() != null ? importVO.getStatus() : CommonStatusEnum.ENABLE.getStatus());
        return reqVO;
    }

    /**
     * 转换为响应VO
     */
    private DeclareHospitalRespVO convertToRespVO(DeclareHospitalDO hospital, Map<Long, DeptRespDTO> deptMap, Map<String, String> tagNamesMap) {
        DeclareHospitalRespVO respVO = DeclareHospitalRespVO.builder()
                .id(hospital.getId())
                .deptId(hospital.getDeptId())
                .hospitalCode(hospital.getHospitalCode())
                .hospitalName(hospital.getHospitalName())
                .shortName(hospital.getShortName())
                .unifiedSocialCreditCode(hospital.getUnifiedSocialCreditCode())
                .medicalLicenseNo(hospital.getMedicalLicenseNo())
                .medicalLicenseExpire(hospital.getMedicalLicenseExpire())
                .projectType(hospital.getProjectType())
                .hospitalLevel(hospital.getHospitalLevel())
                .hospitalCategory(hospital.getHospitalCategory())
                .provinceCode(hospital.getProvinceCode())
                .provinceName(hospital.getProvinceName())
                .cityCode(hospital.getCityCode())
                .cityName(hospital.getCityName())
                .districtCode(hospital.getDistrictCode())
                .districtName(hospital.getDistrictName())
                .address(hospital.getAddress())
                .contactPerson(hospital.getContactPerson())
                .contactPhone(hospital.getContactPhone())
                .contactEmail(hospital.getContactEmail())
                .website(hospital.getWebsite())
                .bedCount(hospital.getBedCount())
                .employeeCount(hospital.getEmployeeCount())
                .status(hospital.getStatus())
                .createTime(hospital.getCreateTime())
                .build();
        // 设置项目类型简称（从 declare_project_type 表的 name 字段读取，如"综合型"）
        respVO.setProjectTypeName(projectTypeService.getProjectTypeName(hospital.getProjectType()));
        // 设置项目类型全称（从 declare_project_type 表的 title 字段读取，如"综合型医院"）
        respVO.setProjectTypeTitle(projectTypeService.getProjectTypeTitle(hospital.getProjectType()));
        // 设置部门名称
        if (hospital.getDeptId() != null) {
            DeptRespDTO dept = deptMap.get(hospital.getDeptId());
            if (dept != null) {
                respVO.setDeptName(dept.getName());
            }
            respVO.setTagNames(tagNamesMap.get(hospital.getHospitalCode()));
        }
        return respVO;
    }

    /**
     * 批量构建医院标签名称映射（hospitalCode -> 逗号分隔的标签名称）
     */
    private Map<String, String> buildTagNamesMapByHospitalCodes(List<String> hospitalCodes) {
        if (CollUtil.isEmpty(hospitalCodes)) {
            return Collections.emptyMap();
        }
        List<cn.gemrun.base.module.declare.dal.dataobject.hospital.HospitalTagRelationDO> relations =
                hospitalTagRelationMapper.selectListByHospitalCodes(hospitalCodes);
        if (CollUtil.isEmpty(relations)) {
            return Collections.emptyMap();
        }
        List<Long> tagIds = relations.stream()
                .map(cn.gemrun.base.module.declare.dal.dataobject.hospital.HospitalTagRelationDO::getTagId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, cn.gemrun.base.module.declare.dal.dataobject.hospital.HospitalTagDO> tagMap =
                hospitalTagMapper.selectBatchIds(tagIds).stream()
                        .collect(Collectors.toMap(
                                cn.gemrun.base.module.declare.dal.dataobject.hospital.HospitalTagDO::getId,
                                tag -> tag));
        Map<String, List<String>> hospitalTagNamesMap = relations.stream()
                .collect(Collectors.groupingBy(
                        cn.gemrun.base.module.declare.dal.dataobject.hospital.HospitalTagRelationDO::getHospitalCode,
                        Collectors.mapping(
                                r -> {
                                    cn.gemrun.base.module.declare.dal.dataobject.hospital.HospitalTagDO tag = tagMap.get(r.getTagId());
                                    return tag != null ? tag.getTagName() : "";
                                },
                                Collectors.toList()
                        )));
        return hospitalTagNamesMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream().filter(name -> !name.isEmpty()).collect(Collectors.joining("，"))
                ));
    }

}
