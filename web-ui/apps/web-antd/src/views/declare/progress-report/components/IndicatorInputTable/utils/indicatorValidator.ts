/**
 * 指标联合验证引擎（兼容导出层）
 *
 * 警告：本文件中的大部分实现已迁移到 indicator-validator/ 模块。
 *       新代码请直接从 indicator-validator/ 导入，不再使用本文件。
 *
 * 迁移进度：
 * ✅ types.ts      — 操作符枚举 + 类型定义（新建）
 * ✅ parser.ts     — 规则字符串解析器（新建）
 * ✅ engine.ts     — 验证执行引擎（新建）
 * ✅ 本文件        — 兼容导出层（简化版）
 */

export * from './indicator-validator';
