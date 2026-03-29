package cn.gemrun.base.module.declare.service.hospital;

import cn.gemrun.base.module.declare.controller.admin.hospital.vo.HospitalTagCreateReqVO;
import cn.gemrun.base.module.declare.controller.admin.hospital.vo.HospitalTagVO;
import cn.gemrun.base.module.declare.controller.admin.hospital.vo.HospitalTagAssignReqVO;
import cn.gemrun.base.module.declare.controller.admin.hospital.vo.HospitalTagUpdateReqVO;
import javax.validation.Valid;

import java.util.List;

public interface HospitalTagService {

    /**
     * 创建标签
     */
    Long createTag(@Valid HospitalTagCreateReqVO reqVO);

    /**
     * 更新标签
     */
    void updateTag(@Valid HospitalTagUpdateReqVO reqVO);

    /**
     * 删除标签
     */
    void deleteTag(Long id);

    /**
     * 获取标签详情
     */
    HospitalTagVO getTag(Long id);

    /**
     * 获取标签列表
     */
    List<HospitalTagVO> getTagList();

    /**
     * 获取标签列表（按分类）
     */
    List<HospitalTagVO> getTagListByCategory(String category);

    /**
     * 获取标签统计
     */
    List<HospitalTagVO> getTagStatistics();

    /**
     * 分配医院标签
     */
    void assignTags(HospitalTagAssignReqVO reqVO);

    /**
     * 获取医院的标签列表
     */
    List<HospitalTagVO> getTagsByHospitalCode(String hospitalCode);
}
