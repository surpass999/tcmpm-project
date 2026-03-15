package cn.gemrun.base.module.declare.service.project.impl;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.module.declare.controller.admin.project.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.project.RectificationDO;
import cn.gemrun.base.module.declare.dal.mysql.project.RectificationMapper;
import cn.gemrun.base.module.declare.service.project.RectificationService;

import static cn.gemrun.base.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.gemrun.base.module.declare.enums.ErrorCodeConstants.*;

/**
 * 整改记录 Service 实现类
 *
 * @author Gemini
 */
@Service
@Slf4j
public class RectificationServiceImpl implements RectificationService {

    @Resource
    private RectificationMapper rectificationMapper;

    @Override
    public Long createRectification(RectificationSaveReqVO createReqVO) {
        RectificationDO rectification = BeanUtils.toBean(createReqVO, RectificationDO.class);
        rectificationMapper.insert(rectification);
        return rectification.getId();
    }

    @Override
    public void updateRectification(RectificationSaveReqVO updateReqVO) {
        validateRectificationExists(updateReqVO.getId());
        RectificationDO updateObj = BeanUtils.toBean(updateReqVO, RectificationDO.class);
        rectificationMapper.updateById(updateObj);
    }

    @Override
    public void deleteRectification(Long id) {
        validateRectificationExists(id);
        rectificationMapper.deleteById(id);
    }

    @Override
    public void deleteRectificationListByIds(Set<Long> ids) {
        rectificationMapper.deleteByIds(ids);
    }

    private void validateRectificationExists(Long id) {
        if (rectificationMapper.selectById(id) == null) {
            throw exception(RECTIFICATION_NOT_EXISTS);
        }
    }

    @Override
    public RectificationRespVO getRectification(Long id) {
        RectificationDO rectification = rectificationMapper.selectById(id);
        return convertToRespVO(rectification);
    }

    @Override
    public PageResult<RectificationRespVO> getRectificationPage(RectificationPageReqVO pageReqVO) {
        PageResult<RectificationDO> pageResult = rectificationMapper.selectPage(pageReqVO);
        return convertToPageResult(pageResult);
    }

    @Override
    public List<RectificationRespVO> getRectificationListByProjectId(Long projectId) {
        List<RectificationDO> list = rectificationMapper.selectListByProjectId(projectId);
        return BeanUtils.toBean(list, RectificationRespVO.class);
    }

    @Override
    public List<RectificationRespVO> getRectificationListByProcessId(Long processId) {
        List<RectificationDO> list = rectificationMapper.selectListByProcessId(processId);
        return BeanUtils.toBean(list, RectificationRespVO.class);
    }

    private RectificationRespVO convertToRespVO(RectificationDO rectification) {
        if (rectification == null) {
            return null;
        }
        return BeanUtils.toBean(rectification, RectificationRespVO.class);
    }

    private PageResult<RectificationRespVO> convertToPageResult(PageResult<RectificationDO> pageResult) {
        if (pageResult == null) {
            return null;
        }
        List<RectificationRespVO> list = BeanUtils.toBean(pageResult.getList(), RectificationRespVO.class);
        return new PageResult<>(list, pageResult.getTotal());
    }

}
