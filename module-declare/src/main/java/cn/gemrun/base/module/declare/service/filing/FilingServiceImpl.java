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


    private FilingDO validateFilingExists(Long id) {
        FilingDO filing = filingMapper.selectById(id);
        if (filing == null) {
            throw exception(FILING_NOT_EXISTS);
        }
        return filing;
    }

    @Override
    public FilingDO getFiling(Long id) {
        return filingMapper.selectById(id);
    }

    @Override
    public PageResult<FilingDO> getFilingPage(FilingPageReqVO pageReqVO) {
        return filingMapper.selectPage(pageReqVO);
    }

    // ========== 流程相关方法 ==========

    @Override
    @Transactional
    public void submitFiling(Long id) {
        // 1. 校验存在
        FilingDO filing = validateFilingExists(id);

        // 2. 校验状态（草稿才能提交）
        if (filing.getFilingStatus() != 0) {
            throw exception(FILING_SUBMIT_STATUS_ERROR);
        }

        // 3. 更新状态为"已提交"
        filing.setFilingStatus(1);  // 1 = 已提交
        filingMapper.updateById(filing);

        // 4. 注意：流程由 @DeclareProcess AOP 自动启动
        // 无需手动调用 processService.startProcess()
    }

    @Override
    @Transactional
    public void withdrawFiling(Long id) {
        // 1. 校验存在
        FilingDO filing = validateFilingExists(id);

        // 2. 校验状态（已提交才能撤回）
        if (filing.getFilingStatus() != 1) {
            throw exception(FILING_WITHDRAW_STATUS_ERROR);
        }

        // 3. 更新状态为"草稿"
        filing.setFilingStatus(0);  // 0 = 草稿
        filingMapper.updateById(filing);

        // 4. 注意：流程由 @DeclareProcess AOP 自动启动（如果配置了）
    }

    @Override
    @Transactional
    public void resubmitFiling(Long id) {
        // 1. 校验存在
        FilingDO filing = validateFilingExists(id);

        // 2. 校验状态（退回或草稿才能重新提交）
        if (filing.getFilingStatus() != 0 && filing.getFilingStatus() != 4) {
            throw exception(FILING_RESUBMIT_STATUS_ERROR);
        }

        // 3. 更新状态为"已提交"
        filing.setFilingStatus(1);  // 1 = 已提交
        filingMapper.updateById(filing);

        // 4. 注意：流程由 @DeclareProcess AOP 自动启动
    }

}