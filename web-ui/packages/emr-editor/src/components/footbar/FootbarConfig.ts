// 缩放配置
export const SCALE_CONFIG = {
  MIN_SCALE: 50,
  MAX_SCALE: 300,
  DEFAULT_SCALE: 100,
  SCALE_STEP: 10,
  PRESETS: [50, 75, 100, 125, 150, 200, 250, 300]
} as const

// 纸张大小配置
export const PAPER_SIZE_CONFIG = {
  options: [
    { value: 'A4', label: 'A4', width: 210, height: 297 },
    { value: 'A3', label: 'A3', width: 297, height: 420 },
    { value: 'Letter', label: 'Letter', width: 216, height: 279 },
    { value: 'Legal', label: 'Legal', width: 216, height: 356 }
  ]
} as const

// 纸张方向配置
export const PAPER_DIRECTION_CONFIG = {
  options: [
    { value: 'portrait', label: '纵向' },
    { value: 'landscape', label: '横向' }
  ]
} as const

// 分页模式配置
export const PAGE_MODE_CONFIG = {
  options: [
    { value: 'paging', label: '分页模式' },
    { value: 'continuity', label: '连续模式' }
  ]
} as const

// 页边距配置
export const PAPER_MARGIN_CONFIG = {
  options: [
    { value: 'normal', label: '普通', margins: { top: 25.4, right: 25.4, bottom: 25.4, left: 25.4 } },
    { value: 'narrow', label: '窄边距', margins: { top: 12.7, right: 12.7, bottom: 12.7, left: 12.7 } },
    { value: 'wide', label: '宽边距', margins: { top: 50.8, right: 50.8, bottom: 50.8, left: 50.8 } }
  ]
} as const

// 工具函数
export function formatVisiblePages(pages: number[]): string {
  if (!pages || pages.length === 0) return '-'
  if (pages.length === 1) return pages[0].toString()

  const sorted = [...pages].sort((a, b) => a - b)
  const ranges: string[] = []
  let start = sorted[0]
  let prev = sorted[0]

  for (let i = 1; i < sorted.length; i++) {
    if (sorted[i] === prev + 1) {
      prev = sorted[i]
    } else {
      ranges.push(start === prev ? start.toString() : `${start}-${prev}`)
      start = sorted[i]
      prev = sorted[i]
    }
  }
  ranges.push(start === prev ? start.toString() : `${start}-${prev}`)

  return ranges.join(', ')
}

export function clampScale(scale: number): number {
  return Math.max(SCALE_CONFIG.MIN_SCALE, Math.min(SCALE_CONFIG.MAX_SCALE, scale))
}

export function getNearestScalePreset(scale: number): number {
  return SCALE_CONFIG.PRESETS.reduce((prev, curr) => {
    return Math.abs(curr - scale) < Math.abs(prev - scale) ? curr : prev
  })
}

export function cyclePaperSize(currentSize: string): string {
  const currentIndex = PAPER_SIZE_CONFIG.options.findIndex(opt => opt.value === currentSize)
  const nextIndex = (currentIndex + 1) % PAPER_SIZE_CONFIG.options.length
  return PAPER_SIZE_CONFIG.options[nextIndex].value
}

export function togglePaperDirection(currentDirection: string): string {
  return currentDirection === 'portrait' ? 'landscape' : 'portrait'
}

// 类型定义
export type PaperSize = typeof PAPER_SIZE_CONFIG.options[number]['value']
export type PaperDirection = typeof PAPER_DIRECTION_CONFIG.options[number]['value']
export type PageMode = typeof PAGE_MODE_CONFIG.options[number]['value']
export type PaperMargin = typeof PAPER_MARGIN_CONFIG.options[number]['value']
