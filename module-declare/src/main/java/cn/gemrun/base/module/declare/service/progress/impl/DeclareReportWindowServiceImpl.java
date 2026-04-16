package cn.gemrun.base.module.declare.service.progress.impl;

import cn.gemrun.base.module.declare.dal.dataobject.progress.DeclareProgressReportDO;
import cn.gemrun.base.module.declare.dal.dataobject.progress.DeclareReportWindowDO;
import cn.gemrun.base.module.declare.dal.mysql.DeclareProgressReportMapper;
import cn.gemrun.base.module.declare.dal.mysql.DeclareReportWindowMapper;
import cn.gemrun.base.module.declare.service.progress.DeclareReportWindowService;
import cn.gemrun.base.module.declare.vo.progress.ReportWindowCreateReqVO;
import cn.gemrun.base.module.declare.vo.progress.ReportWindowVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeclareReportWindowServiceImpl implements DeclareReportWindowService {

    private final DeclareReportWindowMapper windowMapper;
    private final DeclareProgressReportMapper progressReportMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWindow(ReportWindowCreateReqVO reqVO) {
        DeclareReportWindowDO window = DeclareReportWindowDO.builder()
                .reportYear(reqVO.getReportYear())
                .reportBatch(reqVO.getReportBatch())
                .windowStart(reqVO.getWindowStart())
                .windowEnd(reqVO.getWindowEnd())
                .remark(reqVO.getRemark())
                .status(reqVO.getStatus() != null ? reqVO.getStatus() : 1)
                .build();
        windowMapper.insert(window);
        return window.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWindow(Long id, ReportWindowCreateReqVO reqVO) {
        DeclareReportWindowDO window = DeclareReportWindowDO.builder()
                .id(id)
                .reportYear(reqVO.getReportYear())
                .reportBatch(reqVO.getReportBatch())
                .windowStart(reqVO.getWindowStart())
                .windowEnd(reqVO.getWindowEnd())
                .remark(reqVO.getRemark())
                .status(reqVO.getStatus())
                .build();
        windowMapper.updateById(window);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWindow(Long id) {
        windowMapper.deleteById(id);
    }

    @Override
    public List<ReportWindowVO> getWindowList(Integer reportYear, Integer status) {
        LambdaQueryWrapper<DeclareReportWindowDO> wrapper = new LambdaQueryWrapper<DeclareReportWindowDO>()
                .eq(reportYear != null, DeclareReportWindowDO::getReportYear, reportYear)
                .eq(status != null, DeclareReportWindowDO::getStatus, status)
                .orderByAsc(DeclareReportWindowDO::getReportBatch);
        return windowMapper.selectList(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isAnyWindowOpen() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<DeclareReportWindowDO> wrapper = new LambdaQueryWrapper<DeclareReportWindowDO>()
                .eq(DeclareReportWindowDO::getStatus, 1)
                .le(DeclareReportWindowDO::getWindowStart, now)
                .ge(DeclareReportWindowDO::getWindowEnd, now);
        return windowMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean isInOpenWindow(Long hospitalId) {
        return isAnyWindowOpen();
    }

    @Override
    public List<ReportWindowVO> getAvailableWindowsForHospital(Long hospitalId) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<DeclareReportWindowDO> wrapper = new LambdaQueryWrapper<DeclareReportWindowDO>()
                .eq(DeclareReportWindowDO::getStatus, 1)
                .le(DeclareReportWindowDO::getWindowStart, now)
                .ge(DeclareReportWindowDO::getWindowEnd, now)
                .orderByAsc(DeclareReportWindowDO::getReportBatch);
        return windowMapper.selectList(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public ReportWindowVO getLatestOpenWindow() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<DeclareReportWindowDO> wrapper = new LambdaQueryWrapper<DeclareReportWindowDO>()
                .eq(DeclareReportWindowDO::getStatus, 1)
                .le(DeclareReportWindowDO::getWindowStart, now)
                .ge(DeclareReportWindowDO::getWindowEnd, now)
                .orderByDesc(DeclareReportWindowDO::getWindowStart)
                .last("LIMIT 1");
        DeclareReportWindowDO window = windowMapper.selectOne(wrapper);
        return window != null ? convertToVO(window) : null;
    }

    private ReportWindowVO convertToVO(DeclareReportWindowDO window) {
        return ReportWindowVO.builder()
                .id(window.getId())
                .reportYear(window.getReportYear())
                .reportBatch(window.getReportBatch())
                .windowStart(window.getWindowStart())
                .windowEnd(window.getWindowEnd())
                .remark(window.getRemark())
                .status(window.getStatus())
                .build();
    }
}
