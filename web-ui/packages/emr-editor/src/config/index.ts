// Avoid importing SFC types directly in build-time checks - declare minimal type locally
type EmrEditorProps = {
  config?: any
}

// 医生端配置（完整功能）
export const DOCTOR_CONFIG: EmrEditorProps['config'] = {
  toolbar: {
    enabled: true,
    groups: {
      basic: true,
      text: true,
      structure: true,
      insert: true,
      controls: true,
      view: true,
      advanced: true
    }
  },
  features: {
    undo: true,
    redo: true,
    painter: true,
    format: true,
    font: true,
    size: true,
    color: true,
    highlight: true,
    bold: true,
    italic: true,
    underline: true,
    strikeout: true,
    superscript: true,
    subscript: true,
    align: true,
    title: true,
    list: true,
    indent: true,
    table: true,
    image: true,
    link: true,
    formula: true,
    separator: true,
    pageBreak: true,
    date: true,
    search: true,
    comment: true,
    print: true,
    controls: {
      text: true,
      select: true,
      checkbox: true,
      radio: true,
      date: true,
      number: true
    }
  }
}

// 护士端配置（简化功能）
export const NURSE_CONFIG: EmrEditorProps['config'] = {
  toolbar: {
    enabled: true,
    groups: {
      basic: true,
      text: false,      // 不显示复杂文本格式
      structure: false, // 不显示文档结构
      insert: false,    // 不显示插入功能
      controls: true,   // 只显示医疗控件
      view: true,
      advanced: false   // 不显示高级功能
    }
  },
  features: {
    undo: true,
    redo: true,
    controls: {
      text: true,
      select: true,
      checkbox: true,
      radio: true,
      date: true,
      number: true
    },
    print: true
  }
}

// 管理员配置（只读模式）
export const ADMIN_CONFIG: EmrEditorProps['config'] = {
  toolbar: {
    enabled: false  // 不显示任何工具栏
  },
  features: {
    // 所有功能关闭
  },
  options: {
    mode: 'readonly'  // 只读模式（需要canvas-editor支持）
  }
}

// 默认配置
export const DEFAULT_CONFIG: EmrEditorProps['config'] = {
  toolbar: {
    enabled: true,
    groups: {
      basic: true,
      text: true,
      structure: true,
      insert: true,
      controls: true,
      view: true,
      advanced: true
    }
  },
  features: {
    undo: true,
    redo: true,
    painter: true,
    format: true,
    font: true,
    size: true,
    color: true,
    highlight: true,
    bold: true,
    italic: true,
    underline: true,
    strikeout: true,
    superscript: true,
    subscript: true,
    align: true,
    title: true,
    list: true,
    indent: true,
    table: true,
    image: true,
    link: true,
    formula: true,
    separator: true,
    pageBreak: true,
    date: true,
    search: true,
    comment: true,
    print: true,
    controls: {
      text: true,
      select: true,
      checkbox: true,
      radio: true,
      date: true,
      number: true
    }
  }
}

// 快速配置生成器
export const createConfig = (options: {
  role?: 'doctor' | 'nurse' | 'admin' | 'custom'
  customFeatures?: Partial<EmrEditorProps['config']['features']>
  customGroups?: Partial<EmrEditorProps['config']['toolbar']['groups']>
}): EmrEditorProps['config'] => {
  let baseConfig: EmrEditorProps['config']

  switch (options.role) {
    case 'doctor':
      baseConfig = { ...DOCTOR_CONFIG }
      break
    case 'nurse':
      baseConfig = { ...NURSE_CONFIG }
      break
    case 'admin':
      baseConfig = { ...ADMIN_CONFIG }
      break
    default:
      baseConfig = { ...DEFAULT_CONFIG }
  }

  // 深度合并自定义配置
  if (options.customFeatures) {
    baseConfig.features = { ...baseConfig.features, ...options.customFeatures }
  }

  if (options.customGroups) {
    baseConfig.toolbar = {
      ...baseConfig.toolbar,
      groups: { ...baseConfig.toolbar?.groups, ...options.customGroups }
    }
  }

  return baseConfig
}

// 工具栏组名称映射
export const TOOLBAR_GROUP_LABELS = {
  basic: '基础操作',
  text: '文本格式',
  structure: '文档结构',
  insert: '插入元素',
  controls: '医疗控件',
  view: '视图控制',
  advanced: '高级功能'
}

// 功能权限标签映射
export const FEATURE_LABELS = {
  undo: '撤销',
  redo: '重做',
  painter: '格式刷',
  format: '清除格式',
  font: '字体',
  size: '字号',
  color: '字体颜色',
  highlight: '背景色',
  bold: '粗体',
  italic: '斜体',
  underline: '下划线',
  strikeout: '删除线',
  superscript: '上标',
  subscript: '下标',
  align: '对齐',
  title: '标题',
  list: '列表',
  indent: '缩进',
  table: '表格',
  image: '图片',
  link: '链接',
  formula: '公式',
  separator: '分隔符',
  pageBreak: '分页符',
  date: '日期',
  search: '搜索',
  comment: '批注',
  print: '打印'
}

// 医疗控件标签映射
export const CONTROL_LABELS = {
  text: '文本控件',
  select: '选择控件',
  checkbox: '复选框',
  radio: '单选框',
  date: '日期控件',
  number: '数字控件'
}
