package cn.gemrun.base.module.declare.service.progress;

import cn.gemrun.base.module.declare.vo.progress.ReportWindowCreateReqVO;
import cn.gemrun.base.module.declare.vo.progress.ReportWindowVO;

import java.util.List;

public interface DeclareReportWindowService {

    /**
     * 创建时间窗口
     */
    Long createWindow(ReportWindowCreateReqVO reqVO);

    /**
     * 更新时间窗口
     */
    void updateWindow(Long id, ReportWindowCreateReqVO reqVO);

    /**
     * 删除时间窗口
     */
    void deleteWindow(Long id);

    /**
     * 获取时间窗口列表
     */
    List<ReportWindowVO> getWindowList(Integer reportYear);

    /**
     * 检查是否有开放的时间窗口
     */
    boolean isAnyWindowOpen();

    /**
     * 检查特定医院是否在开放时间窗口内
     */
    boolean isInOpenWindow(Long hospitalId);

    /**
     * 获取医院当前可用的填报窗口（当前时间在窗口期内）
     */
    List<ReportWindowVO> getAvailableWindowsForHospital(Long hospitalId);

    /**
     * 获取最新的开放填报窗口
     */
    ReportWindowVO getLatestOpenWindow();
}
