package cn.gemrun.base.module.declare.service.filing;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import cn.gemrun.base.module.declare.controller.admin.filing.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.filing.FilingDO;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.pojo.PageParam;
import cn.gemrun.base.framework.common.util.object.BeanUtils;

import cn.gemrun.base.module.declare.dal.mysql.filing.FilingMapper;

import static cn.gemrun.base.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.gemrun.base.framework.common.util.collection.CollectionUtils.convertList;
import static cn.gemrun.base.framework.common.util.collection.CollectionUtils.diffList;
import static cn.gemrun.base.module.declare.enums.ErrorCodeConstants.*;

/**
 * 项目备案核心信息 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class FilingServiceImpl implements FilingService {

    @Resource
    private FilingMapper filingMapper;

    @Override
    public Long createFiling(FilingSaveReqVO createReqVO) {
        // 插入
        FilingDO filing = BeanUtils.toBean(createReqVO, FilingDO.class);
        filingMapper.insert(filing);

        // 返回
        return filing.getId();
    }

    @Override
    public void updateFiling(FilingSaveReqVO updateReqVO) {
        // 校验存在
        validateFilingExists(updateReqVO.getId());
        // 更新
        FilingDO updateObj = BeanUtils.toBean(updateReqVO, FilingDO.class);
        filingMapper.updateById(updateObj);
    }

    @Override
    public void deleteFiling(Long id) {
        // 校验存在
        validateFilingExists(id);
        // 删除
        filingMapper.deleteById(id);
    }

    @Override
        public void deleteFilingListByIds(List<Long> ids) {
        // 删除
        filingMapper.deleteByIds(ids);
        }


    private void validateFilingExists(Long id) {
        if (filingMapper.selectById(id) == null) {
            throw exception(FILING_NOT_EXISTS);
        }
    }

    @Override
    public FilingDO getFiling(Long id) {
        return filingMapper.selectById(id);
    }

    @Override
    public PageResult<FilingDO> getFilingPage(FilingPageReqVO pageReqVO) {
        return filingMapper.selectPage(pageReqVO);
    }

}