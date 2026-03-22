-- =====================================================
-- 智慧中医医院试点项目管理平台 - 过程指标配置
-- 综合型项目（project_type=1）的过程指标配置
-- 用于测试项目审批流程
-- 作者：系统初始化
-- =====================================================

SET NAMES utf8mb4;

-- =====================================================
-- 过程指标配置说明：
-- process_type: 1=建设过程, 2=半年报, 3=年度总结, 4=中期评估, 5=整改记录, 6=验收申请, 7=成果与流通
-- project_type: 0=全部适用, 1=综合型, 2=中医电子病历型, 3=智慧中药房型, 4=名老中医传承型, 5=中医临床科研型, 6=中医智慧医共体型
-- is_required: 是否必填
-- sort: 排序
-- =====================================================

-- =====================================================
-- 1. 建设过程（CONSTRUCTION）指标配置
-- =====================================================
-- 目的：记录项目启动后的日常建设进展
-- 核心：资金到账、投资使用、系统建设进度

INSERT INTO `declare_process_indicator_config` (`process_type`, `project_type`, `indicator_id`, `is_required`, `sort`) VALUES
-- 资金类（核心）
(1, 1, (SELECT id FROM declare_indicator WHERE indicator_code='201'), 1, 1),    -- 项目总投资金额-必填
(1, 1, (SELECT id FROM declare_indicator WHERE indicator_code='20101'), 1, 2),  -- 中央转移支付到账金额-必填
(1, 1, (SELECT id FROM declare_indicator WHERE indicator_code='202'), 1, 3),    -- 累计完成投资金额-必填
(1, 1, (SELECT id FROM declare_indicator WHERE indicator_code='20201'), 1, 4),  -- 中央转移支付累计使用金额-必填
(1, 1, (SELECT id FROM declare_indicator WHERE indicator_code='207'), 0, 5),    -- 资金执行率
(1, 1, (SELECT id FROM declare_indicator WHERE indicator_code='208'), 0, 6),    -- 中央资金执行率
-- 进度类（核心）
(1, 1, (SELECT id FROM declare_indicator WHERE indicator_code='203'), 1, 10),   -- 项目总体进度-必填
(1, 1, (SELECT id FROM declare_indicator WHERE indicator_code='20301'), 1, 11), -- 当前建设阶段-必填
-- 系统功能类
(1, 1, (SELECT id FROM declare_indicator WHERE indicator_code='301'), 1, 20),   -- 计划建设的业务系统数量-必填
(1, 1, (SELECT id FROM declare_indicator WHERE indicator_code='30101'), 0, 21),  -- 已完成的业务系统数量
(1, 1, (SELECT id FROM declare_indicator WHERE indicator_code='30102'), 0, 22), -- 正在开发的业务系统数量
(1, 1, (SELECT id FROM declare_indicator WHERE indicator_code='302'), 0, 23),    -- 系统功能模块总数
(1, 1, (SELECT id FROM declare_indicator WHERE indicator_code='30201'), 0, 24),  -- 已完成功能模块数
(1, 1, (SELECT id FROM declare_indicator WHERE indicator_code='303'), 0, 25),   -- 系统功能覆盖率
-- 信息安全类
(1, 1, (SELECT id FROM declare_indicator WHERE indicator_code='801'), 0, 30),    -- 等级保护备案系统数量
(1, 1, (SELECT id FROM declare_indicator WHERE indicator_code='80101'), 0, 31), -- 三级等保备案系统数
(1, 1, (SELECT id FROM declare_indicator WHERE indicator_code='80102'), 0, 32), -- 二级等保备案系统数
-- 阶段总结（必填）
(1, 1, (SELECT id FROM declare_indicator WHERE indicator_code='901'), 1, 90),   -- 本阶段工作总结-必填
(1, 1, (SELECT id FROM declare_indicator WHERE indicator_code='902'), 0, 91),    -- 本阶段存在的主要问题
(1, 1, (SELECT id FROM declare_indicator WHERE indicator_code='903'), 0, 92);    -- 下阶段工作计划

-- =====================================================
-- 2. 半年报（HALF_YEAR）指标配置
-- =====================================================
-- 目的：半年期阶段性总结，填报周期要求
-- 核心：半年度资金使用、功能模块完成率

INSERT INTO `declare_process_indicator_config` (`process_type`, `project_type`, `indicator_id`, `is_required`, `sort`) VALUES
-- 资金类（核心）
(2, 1, (SELECT id FROM declare_indicator WHERE indicator_code='201'), 1, 1),    -- 项目总投资金额-必填
(2, 1, (SELECT id FROM declare_indicator WHERE indicator_code='20101'), 1, 2),  -- 中央转移支付到账金额-必填
(2, 1, (SELECT id FROM declare_indicator WHERE indicator_code='202'), 1, 3),    -- 累计完成投资金额-必填
(2, 1, (SELECT id FROM declare_indicator WHERE indicator_code='20201'), 1, 4),  -- 中央转移支付累计使用金额-必填
(2, 1, (SELECT id FROM declare_indicator WHERE indicator_code='207'), 1, 5),    -- 资金执行率-必填
(2, 1, (SELECT id FROM declare_indicator WHERE indicator_code='208'), 1, 6),    -- 中央资金执行率-必填
-- 进度类
(2, 1, (SELECT id FROM declare_indicator WHERE indicator_code='203'), 1, 10),   -- 项目总体进度-必填
(2, 1, (SELECT id FROM declare_indicator WHERE indicator_code='20301'), 1, 11), -- 当前建设阶段-必填
-- 系统功能类（重点）
(2, 1, (SELECT id FROM declare_indicator WHERE indicator_code='301'), 1, 20),   -- 计划建设的业务系统数量-必填
(2, 1, (SELECT id FROM declare_indicator WHERE indicator_code='30101'), 1, 21), -- 已完成的业务系统数量-必填
(2, 1, (SELECT id FROM declare_indicator WHERE indicator_code='30102'), 0, 22), -- 正在开发的业务系统数量
(2, 1, (SELECT id FROM declare_indicator WHERE indicator_code='302'), 1, 23),  -- 系统功能模块总数-必填
(2, 1, (SELECT id FROM declare_indicator WHERE indicator_code='30201'), 1, 24), -- 已完成功能模块数-必填
(2, 1, (SELECT id FROM declare_indicator WHERE indicator_code='303'), 1, 25),  -- 系统功能覆盖率-必填
(2, 1, (SELECT id FROM declare_indicator WHERE indicator_code='304'), 0, 26),   -- 与上级平台对接的系统数量
(2, 1, (SELECT id FROM declare_indicator WHERE indicator_code='305'), 0, 27),   -- 已完成的数据接口数量
-- 建设成效类
(2, 1, (SELECT id FROM declare_indicator WHERE indicator_code='401'), 0, 30),  -- 服务患者人次
(2, 1, (SELECT id FROM declare_indicator WHERE indicator_code='402'), 0, 31),  -- 业务效率提升比例
(2, 1, (SELECT id FROM declare_indicator WHERE indicator_code='403'), 0, 32),  -- 电子病历系统应用覆盖率
-- 数据集建设类
(2, 1, (SELECT id FROM declare_indicator WHERE indicator_code='501'), 0, 40),   -- 计划建设的数据集数量
(2, 1, (SELECT id FROM declare_indicator WHERE indicator_code='502'), 0, 41),   -- 已完成建设的数据集数量
(2, 1, (SELECT id FROM declare_indicator WHERE indicator_code='503'), 0, 42),   -- 数据集总容量
-- 信息安全类
(2, 1, (SELECT id FROM declare_indicator WHERE indicator_code='801'), 0, 50),  -- 等级保护备案系统数量
(2, 1, (SELECT id FROM declare_indicator WHERE indicator_code='802'), 0, 51),   -- 等级保护测评完成数量
-- 阶段总结（必填）
(2, 1, (SELECT id FROM declare_indicator WHERE indicator_code='901'), 1, 90),  -- 本阶段工作总结-必填
(2, 1, (SELECT id FROM declare_indicator WHERE indicator_code='902'), 1, 91),  -- 本阶段存在的主要问题-必填
(2, 1, (SELECT id FROM declare_indicator WHERE indicator_code='903'), 1, 92),  -- 下阶段工作计划-必填
(2, 1, (SELECT id FROM declare_indicator WHERE indicator_code='904'), 0, 93);   -- 需要上级协调解决的问题

-- =====================================================
-- 3. 年度总结（ANNUAL）指标配置
-- =====================================================
-- 目的：年度建设情况总结，成果沉淀
-- 核心：年度投资完成、系统功能、数据集、阶段成果

INSERT INTO `declare_process_indicator_config` (`process_type`, `project_type`, `indicator_id`, `is_required`, `sort`) VALUES
-- 资金类（核心）
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='201'), 1, 1),    -- 项目总投资金额-必填
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='20101'), 1, 2),  -- 中央转移支付到账金额-必填
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='202'), 1, 3),    -- 累计完成投资金额-必填
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='20201'), 1, 4),  -- 中央转移支付累计使用金额-必填
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='207'), 1, 5),    -- 资金执行率-必填
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='208'), 1, 6),    -- 中央资金执行率-必填
-- 进度类
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='203'), 1, 10),   -- 项目总体进度-必填
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='20301'), 1, 11), -- 当前建设阶段-必填
-- 系统功能类（全面）
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='301'), 1, 20),   -- 计划建设的业务系统数量-必填
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='30101'), 1, 21), -- 已完成的业务系统数量-必填
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='30102'), 0, 22), -- 正在开发的业务系统数量
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='302'), 1, 23),  -- 系统功能模块总数-必填
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='30201'), 1, 24), -- 已完成功能模块数-必填
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='303'), 1, 25),  -- 系统功能覆盖率-必填
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='304'), 1, 26),  -- 与上级平台对接的系统数量-必填
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='305'), 1, 27),  -- 已完成的数据接口数量-必填
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='306'), 0, 28),   -- 系统运行稳定性
-- 建设成效类（重点）
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='401'), 1, 30),  -- 服务患者人次-必填
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='402'), 1, 31),  -- 业务效率提升比例-必填
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='403'), 0, 32),  -- 电子病历系统应用覆盖率
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='404'), 0, 33),   -- 医院信息互联互通等级
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='405'), 1, 34),  -- 形成的技术规范/标准数量-必填
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='406'), 0, 35),   -- 发表的相关论文数量
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='407'), 0, 36),   -- 获得的相关专利数量
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='408'), 1, 37),  -- 开展培训次数-必填
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='409'), 1, 38),  -- 培训人员累计人次-必填
-- 数据集建设类
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='501'), 1, 40),  -- 计划建设的数据集数量-必填
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='502'), 1, 41),  -- 已完成建设的数据集数量-必填
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='503'), 1, 42),  -- 数据集总容量-必填
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='504'), 0, 43),   -- 数据质量合格率
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='505'), 0, 44),   -- 已完成治理的数据总量
-- 信息安全类
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='801'), 1, 50),  -- 等级保护备案系统数量-必填
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='80101'), 0, 51), -- 三级等保备案系统数
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='80102'), 0, 52), -- 二级等保备案系统数
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='802'), 1, 53),   -- 等级保护测评完成数量-必填
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='803'), 0, 54),   -- 密码应用测评结果
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='804'), 0, 55),   -- 数据安全分类分级完成情况
-- 阶段总结（必填）
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='901'), 1, 90),  -- 本阶段工作总结-必填
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='902'), 1, 91),  -- 本阶段存在的主要问题-必填
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='903'), 1, 92),  -- 下阶段工作计划-必填
(3, 1, (SELECT id FROM declare_indicator WHERE indicator_code='904'), 0, 93);   -- 需要上级协调解决的问题

-- =====================================================
-- 4. 中期评估（MIDTERM）指标配置
-- =====================================================
-- 目的：评估项目阶段性成效，发现问题
-- 核心：资金执行率、进度偏差、功能实现率、问题清单

INSERT INTO `declare_process_indicator_config` (`process_type`, `project_type`, `indicator_id`, `is_required`, `sort`) VALUES
-- 资金类（核心-评估重点）
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='201'), 1, 1),    -- 项目总投资金额-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='20101'), 1, 2),  -- 中央转移支付到账金额-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='202'), 1, 3),    -- 累计完成投资金额-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='20201'), 1, 4),  -- 中央转移支付累计使用金额-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='207'), 1, 5),    -- 资金执行率-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='208'), 1, 6),    -- 中央资金执行率-必填
-- 进度类（核心-评估重点）
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='203'), 1, 10),   -- 项目总体进度-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='20301'), 1, 11), -- 当前建设阶段-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='204'), 1, 12),   -- 项目开工时间-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='205'), 1, 13),   -- 计划完工时间-必填
-- 系统功能类（评估重点）
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='301'), 1, 20),   -- 计划建设的业务系统数量-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='30101'), 1, 21), -- 已完成的业务系统数量-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='302'), 1, 22),  -- 系统功能模块总数-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='30201'), 1, 23), -- 已完成功能模块数-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='303'), 1, 24),  -- 系统功能覆盖率-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='304'), 1, 25),  -- 与上级平台对接的系统数量-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='305'), 1, 26),  -- 已完成的数据接口数量-必填
-- 建设成效类
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='401'), 1, 30),  -- 服务患者人次-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='402'), 1, 31),  -- 业务效率提升比例-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='403'), 1, 32),  -- 电子病历系统应用覆盖率-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='404'), 1, 33),   -- 医院信息互联互通等级-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='405'), 1, 34),  -- 形成的技术规范/标准数量-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='406'), 0, 35),   -- 发表的相关论文数量
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='407'), 0, 36),   -- 获得的相关专利数量
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='408'), 1, 37),  -- 开展培训次数-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='409'), 1, 38),  -- 培训人员累计人次-必填
-- 数据集建设类
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='501'), 1, 40),  -- 计划建设的数据集数量-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='502'), 1, 41),  -- 已完成建设的数据集数量-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='503'), 1, 42),  -- 数据集总容量-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='504'), 1, 43),   -- 数据质量合格率-必填
-- 信息安全类（评估重点）
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='801'), 1, 50),  -- 等级保护备案系统数量-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='80101'), 1, 51), -- 三级等保备案系统数-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='80102'), 1, 52), -- 二级等保备案系统数-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='802'), 1, 53),  -- 等级保护测评完成数量-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='803'), 1, 54),  -- 密码应用测评结果-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='804'), 1, 55),  -- 数据安全分类分级完成情况-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='805'), 0, 56),   -- 重要数据备份策略
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='806'), 0, 57),   -- 网络安全事件发生次数
-- 阶段总结（必填）
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='901'), 1, 90),  -- 本阶段工作总结-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='902'), 1, 91),  -- 本阶段存在的主要问题-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='903'), 1, 92),  -- 下阶段工作计划-必填
(4, 1, (SELECT id FROM declare_indicator WHERE indicator_code='904'), 1, 93);   -- 需要上级协调解决的问题-必填

-- =====================================================
-- 5. 整改记录（RECTIFICATION）指标配置
-- =====================================================
-- 目的：记录评估/审核中发现问题的整改情况
-- 核心：问题描述、整改措施、完成情况

INSERT INTO `declare_process_indicator_config` (`process_type`, `project_type`, `indicator_id`, `is_required`, `sort`) VALUES
-- 资金类
(5, 1, (SELECT id FROM declare_indicator WHERE indicator_code='207'), 1, 1),    -- 资金执行率-必填
(5, 1, (SELECT id FROM declare_indicator WHERE indicator_code='208'), 1, 2),    -- 中央资金执行率-必填
-- 进度类
(5, 1, (SELECT id FROM declare_indicator WHERE indicator_code='203'), 1, 10),   -- 项目总体进度-必填
-- 建设成效类
(5, 1, (SELECT id FROM declare_indicator WHERE indicator_code='401'), 0, 20),   -- 服务患者人次
(5, 1, (SELECT id FROM declare_indicator WHERE indicator_code='402'), 0, 21),   -- 业务效率提升比例
-- 阶段总结（核心）
(5, 1, (SELECT id FROM declare_indicator WHERE indicator_code='901'), 1, 90),  -- 本阶段工作总结-必填（整改情况说明）
(5, 1, (SELECT id FROM declare_indicator WHERE indicator_code='902'), 1, 91),  -- 本阶段存在的主要问题-必填（整改问题清单）
(5, 1, (SELECT id FROM declare_indicator WHERE indicator_code='903'), 1, 92);  -- 下阶段工作计划-必填（整改措施计划）

-- =====================================================
-- 6. 验收申请（ACCEPTANCE）指标配置
-- =====================================================
-- 目的：项目完工后的全面验收
-- 核心：资金使用总结、系统功能完整度、数据集、信息安全测评

INSERT INTO `declare_process_indicator_config` (`process_type`, `project_type`, `indicator_id`, `is_required`, `sort`) VALUES
-- 资金类（核心-验收重点）
(6, 1, (SELECT id FROM declare_indicator WHERE indicator_code='201'), 1, 1),    -- 项目总投资金额-必填
(6, 1, (SELECT id FROM declare_indicator WHERE indicator_code='20101'), 1, 2),  -- 中央转移支付到账金额-必填
(6, 1, (SELECT id FROM declare_indicator WHERE indicator_code='202'), 1, 3),    -- 累计完成投资金额-必填
(6, 1, (SELECT id FROM declare_indicator WHERE indicator_code='20201'), 1, 4),  -- 中央转移支付累计使用金额-必填
(6, 1, (SELECT id FROM declare_indicator WHERE indicator_code='207'), 1, 5),    -- 资金执行率-必填
(6, 1, (SELECT id FROM declare_indicator WHERE indicator_code='208'), 1, 6),    -- 中央资金执行率-必填
-- 进度类
(6, 1, (SELECT id FROM declare_indicator WHERE indicator_code='203'), 1, 10),   -- 项目总体进度-必填
(6, 1, (SELECT id FROM declare_indicator WHERE indicator_code='20301'), 1, 11), -- 当前建设阶段-必填
(6, 1, (SELECT id FROM declare_indicator WHERE indicator_code='204'), 1, 12),   -- 项目开工时间-必填
(6, 1, (SELECT id FROM declare_indicator WHERE indicator_code='205'), 1, 13),   -- 计划完工时间-必填
(6, 1, (SELECT id FROM declare_indicator WHERE indicator_code='206'), 1, 14);   -- 实际完工时间-必填

-- 继续添加更多指标...

-- =====================================================
-- 过程指标配置统计查询
-- =====================================================
SELECT 
    CASE process_type
        WHEN 1 THEN '建设过程'
        WHEN 2 THEN '半年报'
        WHEN 3 THEN '年度总结'
        WHEN 4 THEN '中期评估'
        WHEN 5 THEN '整改记录'
        WHEN 6 THEN '验收申请'
        WHEN 7 THEN '成果与流通'
    END AS '过程类型',
    COUNT(*) AS '指标数量',
    SUM(is_required) AS '必填指标数'
FROM declare_process_indicator_config 
WHERE project_type = 1 OR project_type = 0
GROUP BY process_type
ORDER BY process_type;

SELECT '过程指标配置完成！' AS result;
