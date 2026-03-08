package cn.gemrun.base.module.declare.service.filing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;

import java.util.*;

import cn.gemrun.base.module.declare.controller.admin.filing.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.filing.FilingDO;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorValueDO;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.module.declare.dal.mysql.filing.FilingMapper;
import cn.gemrun.base.module.declare.dal.mysql.indicator.DeclareIndicatorValueMapper;
import cn.gemrun.base.module.declare.service.indicator.DeclareIndicatorValueService;
import cn.gemrun.base.framework.web.core.util.WebFrameworkUtils;
import cn.gemrun.base.module.system.api.user.AdminUserApi;
import cn.gemrun.base.module.system.api.user.dto.AdminUserRespDTO;
import cn.gemrun.base.framework.security.core.util.SecurityFrameworkUtils;
import cn.gemrun.base.module.bpm.framework.process.aspect.BpmDataPermissionContext;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import static cn.gemrun.base.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.gemrun.base.module.declare.enums.ErrorCodeConstants.*;

/**
 * 项目备案核心信息 Service 实现类
 *
 * 数据权限规则（简化版）：
 * - 可见 = 发起人(creator=当前用户) OR 参与人(initiator_ids包含当前用户)
 * - 发起人：创建/提交的人 → 始终能看到自己提交的
 * - 参与人：审批过的人 → 审批过就能看到
 *
 * @author 杜春渔
 */
@Service
@Slf4j
public class FilingServiceImpl implements FilingService {

    @Resource
    private FilingMapper filingMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeclareIndicatorValueService indicatorValueService;
    @Resource
    private DeclareIndicatorValueMapper indicatorValueMapper;

    @Override
    public Long createFiling(FilingSaveReqVO createReqVO) {
        // 获取当前登录用户的部门ID，用于数据权限控制
        Long deptId = null;
        Long userId = null;
        try {
            userId = WebFrameworkUtils.getLoginUserId();
            if (userId != null) {
                AdminUserRespDTO user = adminUserApi.getUser(userId);
                if (user != null) {
                    deptId = user.getDeptId();
                    log.info("[createFiling] 用户 {} 部门ID: {}", userId, deptId);
                }
            }
        } catch (Exception e) {
            log.warn("[createFiling] 获取用户部门ID失败: {}", e.getMessage());
        }

        // 插入基本信息
        FilingDO filing = BeanUtils.toBean(createReqVO, FilingDO.class);
        filing.setDeptId(deptId);  // 设置部门ID，用于数据权限控制
        filingMapper.insert(filing);

        // 保存指标值
        saveIndicatorValues(createReqVO, filing.getId(), userId);

        // 返回
        return filing.getId();
    }

    @Override
    public void updateFiling(FilingSaveReqVO updateReqVO) {
        // 校验存在
        validateFilingExists(updateReqVO.getId());

        // 打印日志调试
        log.info("更新备案信息 ID: {}, validStartTime: {}, validEndTime: {}",
                updateReqVO.getId(), updateReqVO.getValidStartTime(), updateReqVO.getValidEndTime());

        // 使用 BeanUtils 转换
        FilingDO updateObj = BeanUtils.toBean(updateReqVO, FilingDO.class);
        filingMapper.updateById(updateObj);

        // 保存指标值
        Long userId = null;
        try {
            userId = SecurityFrameworkUtils.getLoginUserId();
        } catch (Exception e) {
            log.warn("[updateFiling] 获取用户ID失败: {}", e.getMessage());
        }
        saveIndicatorValues(updateReqVO, updateReqVO.getId(), userId);
    }

    /**
     * 保存指标值
     */
    private void saveIndicatorValues(FilingSaveReqVO reqVO, Long filingId, Long userId) {
        if (reqVO.getIndicatorValues() == null || reqVO.getIndicatorValues().isEmpty()) {
            return;
        }

        // 转换为 DO 对象
        List<DeclareIndicatorValueDO> indicatorValues = BeanUtils.toBean(
                reqVO.getIndicatorValues(), DeclareIndicatorValueDO.class);

        // 批量保存指标值（业务类型为1，代表备案）
        indicatorValueService.batchSaveIndicatorValues(1, filingId, indicatorValues, userId);
        log.info("保存指标值完成，备案ID: {}, 指标数量: {}", filingId, indicatorValues.size());
    }

    @Override
    public void deleteFiling(Long id) {
        // 校验存在
        validateFilingExists(id);
        // 删除备案基本信息
        filingMapper.deleteById(id);
        // 逻辑删除关联的指标值（业务类型1=备案）
        DeclareIndicatorValueDO indicatorValue = new DeclareIndicatorValueDO();
        indicatorValue.setDeleted(true);
        indicatorValueMapper.update(indicatorValue, new QueryWrapper<DeclareIndicatorValueDO>()
                .eq("business_type", 1)
                .eq("business_id", id));
    }

    @Override
    public void deleteFilingListByIds(List<Long> ids) {
        // 批量逻辑删除关联的指标值（业务类型1=备案）
        for (Long id : ids) {
            DeclareIndicatorValueDO indicatorValue = new DeclareIndicatorValueDO();
            indicatorValue.setDeleted(true);
            indicatorValueMapper.update(indicatorValue, new QueryWrapper<DeclareIndicatorValueDO>()
                    .eq("business_type", 1)
                    .eq("business_id", id));
        }
        // 批量删除备案基本信息
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
        // 获取当前登录用户ID
        Long currentUserId = getCurrentUserId();

        // 如果是管理员（能看全部数据），则不进行数据权限过滤
        if (isAdmin(currentUserId)) {
            log.info("[getFilingPage] 用户 {} 是管理员，不过滤数据权限", currentUserId);
            return filingMapper.selectPage(pageReqVO);
        }

        // 从 AOP 上下文获取有权限的业务ID列表
        List<Long> authorizedBusinessIds = BpmDataPermissionContext.getAuthorizedBusinessIds();

        // 如果有权限数据（不为空），则过滤查询
        if (BpmDataPermissionContext.hasDataPermission()) {
            log.info("[getFilingPage] 用户 {} 有权限的业务ID数量: {}", currentUserId, authorizedBusinessIds.size());
            // 调用 Mapper 查询：有权限的业务ID
            return filingMapper.selectPageWithDataPermission(pageReqVO, String.valueOf(currentUserId), new HashSet<>(authorizedBusinessIds));
        }

        // 没有权限数据，返回空结果
        log.info("[getFilingPage] 用户 {} 无权限数据，返回空结果", currentUserId);
        return new PageResult<>(new ArrayList<>(), 0L);
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        try {
            return SecurityFrameworkUtils.getLoginUserId();
        } catch (Exception e) {
            log.warn("[getCurrentUserId] 获取用户ID失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 判断是否是管理员（可以查看全部数据）
     * 简单判断：用户ID为1或获取不到部门数据权限时视为管理员
     */
    private boolean isAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        // 用户ID为1通常是超级管理员
        return userId == 1L;
    }

    // ========== 流程相关方法已移至 BpmProcess AOP 自动处理 ==========

    @Override
    public void updateFilingStatus(Long id, Integer status) {
        FilingDO filing = filingMapper.selectById(id);
        if (filing == null) {
            log.warn("[updateFilingStatus] 备案不存在: id={}", id);
            return;
        }
        filing.setFilingStatus(status);
        filingMapper.updateById(filing);
        log.info("[updateFilingStatus] 更新备案状态: id={}, filingStatus={}", id, status);
    }

}