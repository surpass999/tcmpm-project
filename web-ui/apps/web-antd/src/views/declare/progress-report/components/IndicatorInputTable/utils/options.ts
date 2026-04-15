/**
 * 选项解析工具函数
 * 提供选项 JSON 解析的辅助函数
 */

/** 解析选项 JSON */
export function parseOptions(valueOptions: string): Array<{ value: string; label: string; exclusive?: boolean; inputType?: boolean }> {
  if (!valueOptions) return [];
  try {
    const parsed = JSON.parse(valueOptions);
    return Array.isArray(parsed)
      ? parsed.map((item: any) => ({
          value: String(item.value),
          label: item.label ?? item.value,
          exclusive: item.exclusive == true,
          inputType: Number(item.value) >= 1000,
        }))
      : [];
  } catch {
    return [];
  }
}
