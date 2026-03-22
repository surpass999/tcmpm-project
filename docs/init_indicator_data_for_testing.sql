-- =====================================================
-- 智慧中医医院试点项目管理平台 - 测试数据初始化
-- 用于综合型项目流程测试
-- 作者：系统初始化
-- =====================================================

SET NAMES utf8mb4;

-- =====================================================
-- 第一部分：指标基础数据（declare_indicator）
-- =====================================================

-- 1. 基本情况类指标（101-199）
INSERT INTO `declare_indicator` (`indicator_code`, `indicator_name`, `unit`, `category`, `value_type`, `is_required`, `sort`, `project_type`, `business_type`, `logic_rule`, `calculation_rule`) VALUES
('101', '医院名称', '个', 1, 2, 1, 1, 0, 'project', '', ''),
('102', '医院等级', '级', 1, 10, 1, 2, 0, 'project', '', '["三级甲等","三级乙等","二级甲等","二级乙等"]'),
('103', '编制床位数', '张', 1, 1, 1, 3, 0, 'project', '', ''),
('104', '项目负责人姓名', '人', 1, 2, 1, 4, 0, 'project', '', ''),
('105', '项目负责人联系电话', '部', 1, 2, 1, 5, 0, 'project', '', ''),
('106', '项目技术负责人', '人', 1, 2, 1, 6, 0, 'project', '', '');

-- 2. 项目管理类指标（201-299）
INSERT INTO `declare_indicator` (`indicator_code`, `indicator_name`, `unit`, `category`, `value_type`, `is_required`, `sort`, `project_type`, `business_type`, `logic_rule`, `calculation_rule`) VALUES
('201', '项目总投资金额', '万元', 2, 1, 1, 10, 0, 'project', '201 >= 20101', ''),
('20101', '中央转移支付到账金额', '万元', 2, 1, 1, 11, 0, 'project', '20101 <= 201', ''),
('202', '累计完成投资金额', '万元', 2, 1, 1, 12, 0, 'project', '202 <= 201', ''),
('20201', '中央转移支付累计使用金额', '万元', 2, 1, 1, 13, 0, 'project', '20201 <= 20101', ''),
('203', '项目总体进度', '%', 2, 1, 1, 14, 0, 'project', '', '进度 = 已完成任务数 / 总任务数 × 100%'),
('20301', '当前建设阶段', '阶段', 2, 10, 1, 15, 0, 'project', '', '["需求分析","系统设计","开发实施","测试验收","试运行"]'),
('204', '项目开工时间', '日期', 2, 4, 0, 16, 0, 'project', '', ''),
('205', '计划完工时间', '日期', 2, 4, 0, 17, 0, 'project', '', ''),
('206', '实际完工时间', '日期', 2, 4, 0, 18, 0, 'project', '', ''),
('207', '资金执行率', '%', 2, 1, 1, 19, 0, 'project', '207 = 202 / 201 × 100', ''),
('208', '中央资金执行率', '%', 2, 1, 1, 20, 0, 'project', '208 = 20201 / 20101 × 100', '');

-- 3. 系统功能类指标（301-399）
INSERT INTO `declare_indicator` (`indicator_code`, `indicator_name`, `unit`, `category`, `value_type`, `is_required`, `sort`, `project_type`, `business_type`, `logic_rule`, `calculation_rule`) VALUES
('301', '计划建设的业务系统数量', '个', 3, 1, 1, 30, 0, 'project', '', ''),
('30101', '已完成的业务系统数量', '个', 3, 1, 1, 31, 0, 'project', '30101 <= 301', ''),
('30102', '正在开发的业务系统数量', '个', 3, 1, 1, 32, 0, 'project', '', ''),
('302', '系统功能模块总数', '个', 3, 1, 1, 33, 0, 'project', '', ''),
('30201', '已完成功能模块数', '个', 3, 1, 1, 34, 0, 'project', '30201 <= 302', ''),
('303', '系统功能覆盖率', '%', 3, 1, 1, 35, 0, 'project', '303 = 30201 / 302 × 100', ''),
('304', '与上级平台对接的系统数量', '个', 3, 1, 0, 36, 0, 'project', '', ''),
('305', '已完成的数据接口数量', '个', 3, 1, 0, 37, 0, 'project', '', ''),
('306', '系统运行稳定性（ uptime ）', '%', 3, 1, 0, 38, 0, 'project', '', '');

-- 4. 建设成效类指标（401-499）
INSERT INTO `declare_indicator` (`indicator_code`, `indicator_name`, `unit`, `category`, `value_type`, `is_required`, `sort`, `project_type`, `business_type`, `logic_rule`, `calculation_rule`) VALUES
('401', '服务患者人次', '人次', 4, 1, 0, 40, 0, 'project', '', ''),
('402', '业务效率提升比例', '%', 4, 1, 0, 41, 0, 'project', '', ''),
('403', '电子病历系统应用覆盖率', '%', 4, 1, 0, 42, 0, 'project', '', ''),
('404', '医院信息互联互通等级', '级', 4, 10, 0, 43, 0, 'project', '', '["四级甲等","四级乙等","三级","二级"]'),
('405', '形成的技术规范/标准数量', '个', 4, 1, 0, 44, 0, 'project', '', ''),
('406', '发表的相关论文数量', '篇', 4, 1, 0, 45, 0, 'project', '', ''),
('407', '获得的相关专利数量', '项', 4, 1, 0, 46, 0, 'project', '', ''),
('408', '开展培训次数', '次', 4, 1, 0, 47, 0, 'project', '', ''),
('409', '培训人员累计人次', '人次', 4, 1, 0, 48, 0, 'project', '', '');

-- 5. 数据集建设类指标（501-599）
INSERT INTO `declare_indicator` (`indicator_code`, `indicator_name`, `unit`, `category`, `value_type`, `is_required`, `sort`, `project_type`, `business_type`, `logic_rule`, `calculation_rule`) VALUES
('501', '计划建设的数据集数量', '个', 5, 1, 0, 50, 0, 'project', '', ''),
('502', '已完成建设的数据集数量', '个', 5, 1, 0, 51, 0, 'project', '502 <= 501', ''),
('503', '数据集总容量', 'GB', 5, 1, 0, 52, 0, 'project', '', ''),
('504', '数据质量合格率', '%', 5, 1, 0, 53, 0, 'project', '', ''),
('505', '已完成治理的数据总量', 'GB', 5, 1, 0, 54, 0, 'project', '', '');

-- 6. 数据交易/流通类指标（601-699）
INSERT INTO `declare_indicator` (`indicator_code`, `indicator_name`, `unit`, `category`, `value_type`, `is_required`, `sort`, `project_type`, `business_type`, `logic_rule`, `calculation_rule`) VALUES
('601', '数据产品证书数量', '个', 6, 1, 0, 60, 0, 'achievement', '', ''),
('602', '数据交易金额', '万元', 6, 1, 0, 61, 0, 'achievement', '', ''),
('603', '数据共享交换记录数', '条', 6, 1, 0, 62, 0, 'achievement', '', ''),
('604', '是否通过成果认定', '是/否', 6, 3, 0, 63, 0, 'achievement', '', '');

-- 7. 信息安全类指标（701-799）
INSERT INTO `declare_indicator` (`indicator_code`, `indicator_name`, `unit`, `category`, `value_type`, `is_required`, `sort`, `project_type`, `business_type`, `logic_rule`, `calculation_rule`) VALUES
('801', '等级保护备案系统数量', '个', 7, 1, 0, 70, 0, 'project', '', ''),
('80101', '三级等保备案系统数', '个', 7, 1, 0, 71, 0, 'project', '80101 + 80102 <= 801', ''),
('80102', '二级等保备案系统数', '个', 7, 1, 0, 72, 0, 'project', '80101 + 80102 <= 801', ''),
('802', '等级保护测评完成数量', '个', 7, 1, 0, 73, 0, 'project', '', ''),
('803', '密码应用测评结果', '结果', 7, 10, 0, 74, 0, 'project', '', '["合格","基本合格","不合格"]'),
('804', '数据安全分类分级完成情况', '是/否', 7, 3, 0, 75, 0, 'project', '', ''),
('805', '重要数据备份策略', '策略', 7, 10, 0, 76, 0, 'project', '', '["完全备份","增量备份","差异备份"]'),
('806', '网络安全事件发生次数', '次', 7, 1, 0, 77, 0, 'project', '', '');

-- 8. 阶段总结类指标（901-999）
INSERT INTO `declare_indicator` (`indicator_code`, `indicator_name`, `unit`, `category`, `value_type`, `is_required`, `sort`, `project_type`, `business_type`, `logic_rule`, `calculation_rule`) VALUES
('901', '本阶段工作总结', '文本', 1, 5, 1, 90, 0, 'process', '', ''),
('902', '本阶段存在的主要问题', '文本', 1, 5, 0, 91, 0, 'process', '', ''),
('903', '下阶段工作计划', '文本', 1, 5, 0, 92, 0, 'process', '', ''),
('904', '需要上级协调解决的问题', '文本', 1, 5, 0, 93, 0, 'process', '', ''),
('905', '支撑材料清单', '份', 1, 1, 0, 94, 0, 'process', '', '');

-- =====================================================
-- 第二部分：指标口径定义（declare_indicator_caliber）
-- =====================================================

INSERT INTO `declare_indicator_caliber` (`indicator_id`, `definition`, `statistic_scope`, `data_source`, `fill_require`, `calculation_example`) VALUES
(1, '指项目实施医院的全称', '项目实施医院', '项目申报材料', '必填', ''),
(2, '指医院当前评审确定的等级', '项目实施医院', '卫生健康行政部门', '必填', ''),
(3, '指医院编制床位数', '项目实施医院', '医院统计数据', '必填', ''),
(4, '指项目的主要负责人姓名', '项目组', '项目申报材料', '必填', ''),
(5, '指项目负责人的有效联系方式', '项目组', '项目申报材料', '必填', ''),
(6, '指项目技术负责人姓名', '项目组', '项目申报材料', '必填', '');

-- 资金类指标口径
INSERT INTO `declare_indicator_caliber` (`indicator_id`, `definition`, `statistic_scope`, `data_source`, `fill_require`, `calculation_example`) VALUES
(7, '指项目总投资预算金额', '整个项目周期', '项目批复文件、投资计划', '必填', '中央投资+地方配套+自筹'),
(8, '指中央转移支付资金实际到账金额', '统计截止日期', '财政拨款文件、银行到账凭证', '必填', '累计到账金额'),
(9, '指累计已完成的投资金额', '统计截止日期', '财务支出凭证、项目进度报告', '必填', '实际支出的总和'),
(10, '指中央转移支付资金累计使用金额', '统计截止日期', '财务支出凭证', '必填', '≤ 到账金额'),
(11, '指项目整体完成进度百分比', '整个项目', '项目进度跟踪', '必填', '已完成工作量 / 计划工作量 × 100%');

-- 系统功能类指标口径
INSERT INTO `declare_indicator_caliber` (`indicator_id`, `definition`, `statistic_scope`, `data_source`, `fill_require`, `calculation_example`) VALUES
(12, '指计划建设的业务系统总数', '整个项目', '项目批复方案', '必填', '如：HIS系统、PACS系统、LIS系统等'),
(13, '指已完成开发并投入使用的系统数', '统计截止日期', '项目验收报告', '必填', ''),
(14, '指正在开发中的系统数量', '统计截止日期', '项目进度报告', '必填', ''),
(15, '指计划建设的功能模块总数', '整个项目', '项目设计方案', '必填', '各系统功能模块之和'),
(16, '指已完成的功能模块数', '统计截止日期', '系统测试报告', '必填', ''),
(17, '指已完成功能模块占计划的比例', '统计截止日期', '系统统计', '必填', '已完成模块数 / 总模块数 × 100%');

-- 建设成效类指标口径
INSERT INTO `declare_indicator_caliber` (`indicator_id`, `definition`, `statistic_scope`, `data_source`, `fill_require`, `calculation_example`) VALUES
(18, '指系统上线后累计服务的患者人次', '统计周期内', '业务系统统计数据', '选填', ''),
(19, '指相比建设前业务效率提升的比例', '统计周期内', '对比分析报告', '选填', '（原来时间-现在时间）/原来时间×100%'),
(20, '指电子病历系统覆盖的临床科室比例', '统计截止日期', '系统应用统计', '选填', '使用科室数/总科室数×100%');

-- 信息安全类指标口径
INSERT INTO `declare_indicator_caliber` (`indicator_id`, `definition`, `statistic_scope`, `data_source`, `fill_require`, `calculation_example`) VALUES
(28, '指已进行等级保护备案的系统总数', '统计截止日期', '公安部门备案证明', '选填', ''),
(29, '指进行三级等保备案的系统数量', '统计截止日期', '公安部门备案证明', '选填', ''),
(30, '指进行二级等保备案的系统数量', '统计截止日期', '公安部门备案证明', '选填', ''),
(31, '指已完成等级保护测评的系统数量', '统计截止日期', '测评报告', '选填', ''),
(32, '指密码应用测评的结果', '统计截止日期', '密码测评报告', '选填', '合格/基本合格/不合格');

-- 阶段总结类指标口径
INSERT INTO `declare_indicator_caliber` (`indicator_id`, `definition`, `statistic_scope`, `data_source`, `fill_require`, `calculation_example`) VALUES
(36, '对本阶段工作情况的全面总结', '本阶段', '项目单位填写', '必填', '包括主要工作、进展、成果等'),
(37, '本阶段存在的主要问题和困难', '本阶段', '项目单位填写', '选填', '问题描述及原因分析'),
(38, '下一阶段的工作计划安排', '下阶段', '项目单位填写', '选填', '工作目标、任务安排、时间计划'),
(39, '需要上级部门协调解决的问题', '本阶段', '项目单位填写', '选填', '问题描述及建议解决方案');

-- =====================================================
-- 第三部分：指标联合验证规则（declare_indicator_joint_rule）
-- =====================================================

INSERT INTO `declare_indicator_joint_rule` (`rule_name`, `project_type`, `trigger_timing`, `rule_config`, `rule_level`, `rule_type`, `status`, `sort`) VALUES
('资金到账金额校验', 1, 'PROCESS_SUBMIT', '{"type":"COMPARE","indicators":["20101","201"],"expression":"${20101} <= ${201}","errorMsg":"中央转移支付到账金额不能超过项目总投资金额"}', 1, 2, 1, 1),
('资金使用不超过到账金额', 1, 'PROCESS_SUBMIT', '{"type":"COMPARE","indicators":["20201","20101"],"expression":"${20201} <= ${20101}","errorMsg":"中央转移支付累计使用金额不能超过到账金额"}', 1, 2, 1, 2),
('累计投资不超过总投资', 1, 'PROCESS_SUBMIT', '{"type":"COMPARE","indicators":["202","201"],"expression":"${202} <= ${201}","errorMsg":"累计完成投资不能超过项目总投资"}', 1, 2, 1, 3),
('已完成系统数不超过计划数', 1, 'PROCESS_SUBMIT', '{"type":"COMPARE","indicators":["30101","301"],"expression":"${30101} <= ${301}","errorMsg":"已完成系统数不能超过计划建设数"}', 1, 2, 1, 4),
('已完成模块数不超过计划数', 1, 'PROCESS_SUBMIT', '{"type":"COMPARE","indicators":["30201","302"],"expression":"${30201} <= ${302}","errorMsg":"已完成功能模块数不能超过计划数"}', 1, 2, 1, 5),
('等保系统数求和校验', 1, 'PROCESS_SUBMIT', '{"type":"SUM","indicators":["80101","80102"],"targetIndicator":"801","errorMsg":"三级+二级等保系统数必须等于等保备案总数"}', 1, 1, 1, 6),
('数据集完成数不超过计划数', 1, 'PROCESS_SUBMIT', '{"type":"COMPARE","indicators":["502","501"],"expression":"${502} <= ${501}","errorMsg":"已完成数据集数不能超过计划数"}', 1, 2, 1, 7),
('资金执行率自动计算', 1, 'FILL', '{"type":"CALCULATE","sourceIndicators":["202","201"],"targetIndicator":"207","formula":"${202}/${201}*100","errorMsg":""}', 2, 1, 1, 8),
('中央资金执行率自动计算', 1, 'FILL', '{"type":"CALCULATE","sourceIndicators":["20201","20101"],"targetIndicator":"208","formula":"${20201}/${20101}*100","errorMsg":""}', 2, 1, 1, 9),
('功能覆盖率自动计算', 1, 'FILL', '{"type":"CALCULATE","sourceIndicators":["30201","302"],"targetIndicator":"303","formula":"${30201}/${302}*100","errorMsg":""}', 2, 1, 1, 10);

SELECT '指标数据初始化完成！' AS result;
