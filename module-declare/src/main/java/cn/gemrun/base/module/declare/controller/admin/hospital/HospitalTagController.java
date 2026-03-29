package cn.gemrun.base.module.declare.controller.admin.hospital;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.module.declare.service.hospital.HospitalTagService;
import cn.gemrun.base.module.declare.controller.admin.hospital.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import javax.annotation.Resource;
import javax.validation.Valid;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.gemrun.base.framework.common.pojo.CommonResult.success;

/**
 * 医院标签Controller
 *
 * @author Gemini
 */
@Slf4j
@RestController
@RequestMapping("/declare/hospital-tag")
@Validated
public class HospitalTagController {

    @Resource
    private HospitalTagService hospitalTagService;

    /**
     * 创建标签
     */
    @PostMapping("/create")
    public CommonResult<Long> createTag(@Valid @RequestBody HospitalTagCreateReqVO reqVO) {
        return success(hospitalTagService.createTag(reqVO));
    }

    /**
     * 更新标签
     */
    @PutMapping("/update")
    public CommonResult<Boolean> updateTag(@Valid @RequestBody HospitalTagUpdateReqVO reqVO) {
        hospitalTagService.updateTag(reqVO);
        return success(true);
    }

    /**
     * 删除标签
     */
    @DeleteMapping("/delete")
    public CommonResult<Boolean> deleteTag(@RequestParam("id") Long id) {
        hospitalTagService.deleteTag(id);
        return success(true);
    }

    /**
     * 获取标签详情
     */
    @GetMapping("/get")
    public CommonResult<HospitalTagVO> getTag(@RequestParam("id") Long id) {
        return success(hospitalTagService.getTag(id));
    }

    /**
     * 获取标签列表
     */
    @GetMapping("/list")
    public CommonResult<List<HospitalTagVO>> getTagList() {
        return success(hospitalTagService.getTagList());
    }

    /**
     * 获取标签列表（按分类）
     */
    @GetMapping("/list-by-category")
    public CommonResult<List<HospitalTagVO>> getTagListByCategory(@RequestParam("category") String category) {
        return success(hospitalTagService.getTagListByCategory(category));
    }

    /**
     * 获取标签统计
     */
    @GetMapping("/statistics")
    public CommonResult<List<HospitalTagVO>> getTagStatistics() {
        return success(hospitalTagService.getTagStatistics());
    }

    /**
     * 分配医院标签
     */
    @PostMapping("/assign")
    @PreAuthorize("@ss.hasPermission('declare:hospital-tag:assign')")
    public CommonResult<Boolean> assignTags(@Valid @RequestBody HospitalTagAssignReqVO reqVO) {
        log.info("[assignTags] hospitalCode={}, tagIds={}", reqVO.getHospitalCode(), reqVO.getTagIds());
        hospitalTagService.assignTags(reqVO);
        return success(true);
    }

    /**
     * 获取医院的标签列表
     */
    @GetMapping("/hospital-tags")
    public CommonResult<List<HospitalTagVO>> getHospitalTags(@RequestParam("hospitalCode") String hospitalCode) {
        return success(hospitalTagService.getTagsByHospitalCode(hospitalCode));
    }
}
