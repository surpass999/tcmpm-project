/**
 * useErrorKeys - 错误 Key 生成和统一错误存储
 *
 * 核心设计：ruleId 错误追踪体系
 * - 彻底移除 dirty 机制
 * - 每个验证失败时生成唯一 ruleId，设置到所有涉及字段
 * - 验证通过时精确清除该 ruleId 的所有错误
 *
 * ruleId 格式：
 *   - 基础验证:  "basic:{indicatorCode}[:{fieldCode}]"
 *   - 逻辑规则:  "logic:{indicatorCode}[:{entryNum}]"
 *   - 上期值:    "positive:{indicatorCode}"
 */

import { reactive } from 'vue';
import type { FieldError } from '../types';

// ==================== 统一错误存储 ====================

/** 统一错误存储（替代 7 个分散错误状态 + 2 个脏追踪） */
export const fieldErrors = reactive<Record<string, FieldError>>({});

// ==================== RuleErrorRegistry ====================

/** 规则错误条目 */
interface RuleErrorEntry {
  id: string
  message: string
  errorType: FieldError['errorType']
  /** 设置了此错误的字段 key 列表 */
  involvedKeys: string[]
}

/**
 * 规则错误注册表：ruleId → 错误条目
 * 作用：同一 ruleId 可关联多个字段，验证通过时精确清除
 */
export class RuleErrorRegistry {
  private map = new Map<string, RuleErrorEntry>()

  /** 记录一条规则验证失败：在所有涉及字段上注册 */
  set(ruleId: string, message: string, errorType: FieldError['errorType'], involvedKeys: string[]): void {
    const existing = this.map.get(ruleId)
    if (existing) {
      // 同一 ruleId 追加新的字段
      for (const key of involvedKeys) {
        if (!existing.involvedKeys.includes(key)) {
          existing.involvedKeys.push(key)
        }
      }
      existing.message = message
    } else {
      this.map.set(ruleId, { id: ruleId, message, errorType, involvedKeys: [...involvedKeys] })
    }
  }

  /** 精确清除一条规则的所有错误（验证通过时调用） */
  clear(ruleId: string): void {
    this.map.delete(ruleId)
  }

  /** 某规则是否仍有错误 */
  has(ruleId: string): boolean {
    return this.map.has(ruleId)
  }

  /** 清除所有规则错误 */
  clearAll(): void {
    this.map.clear()
  }

  /** 内部访问（供 clearAllErrorsForKey 使用） */
  get entries(): Map<string, RuleErrorEntry> {
    return this.map
  }
}

export const ruleErrorRegistry = new RuleErrorRegistry()

// ==================== Key 生成函数 ====================

/** 生成顶层指标错误 key */
export function toTopLevelKey(indicatorId: number): string {
  return `t:${indicatorId}`;
}

/** 动态容器的字段错误 key：${rowKey}${fieldCode}（如 7020101field001）
 * rowKey 本身由 generateContainerRowKey 生成，已包含 indicatorCode 前缀
 * 条件容器的 rowKey = indicatorCode（如 "603"），同样适用此格式 */
export function toContainerKey(rowKey: string, fieldCode: string): string {
  return `${rowKey}${fieldCode}`;
}

// ==================== 错误操作函数 ====================

/** 设置错误（ruleId 记录错误来源，验证通过时精确清除） */
export function setFieldError(
  key: string,
  message: string,
  errorType: FieldError['errorType'],
  ruleId?: string
): void {
  fieldErrors[key] = { message, errorType, ruleId }
  if (ruleId) {
    ruleErrorRegistry.set(ruleId, message, errorType, [key])
  }
}

/** 清除某字段上所有规则设置的所有错误 */
function clearAllErrorsForKey(key: string): void {
  // 清除 fieldErrors
  delete fieldErrors[key]
  // 同步清除注册表中所有涉及此 key 的条目
  for (const [id, entry] of ruleErrorRegistry.entries) {
    entry.involvedKeys = entry.involvedKeys.filter(k => k !== key)
    if (entry.involvedKeys.length === 0) {
      ruleErrorRegistry.clear(id)
    }
  }
}

/** 清除错误（force 可强制清除必填错误，用于联动隐藏时） */
export function clearFieldError(key: string, force = false): void {
  if (fieldErrors[key]) {
    if (fieldErrors[key].errorType === 'required' && !force) return
    clearAllErrorsForKey(key)
  }
}

/** 强制清除（包括必填错误），返回是否真的清掉了 */
export function forceClearFieldError(key: string): boolean {
  const existed = !!fieldErrors[key]
  clearAllErrorsForKey(key)
  return existed
}

/** 仅清除指定类型的错误（保留其他类型的错误） */
export function clearFieldErrorByType(key: string, targetType: FieldError['errorType']): void {
  if (fieldErrors[key]?.errorType === targetType) {
    const ruleId = fieldErrors[key].ruleId
    delete fieldErrors[key]
    if (ruleId) ruleErrorRegistry.clear(ruleId)
  }
}

/** 清除所有错误 */
export function clearAllErrors(): void {
  for (const key of Object.keys(fieldErrors)) {
    delete fieldErrors[key]
  }
  ruleErrorRegistry.clearAll()
}

/**
 * 统一错误读取入口
 * 所有类型的错误都直接显示（无需 dirty 追踪）
 */
export function getFieldError(key: string): string | undefined {
  return fieldErrors[key]?.message
}
