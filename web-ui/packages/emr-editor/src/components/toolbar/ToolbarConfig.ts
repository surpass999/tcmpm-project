import type { ToolbarGroupProps } from './ToolbarGroup.vue'
import type { ToolbarItemProps } from './ToolbarItem.vue'
import type { DropdownOption } from './ToolbarDropdown.vue'

// 工具栏配置接口
export interface ToolbarConfig {
  groups: Record<string, ToolbarGroupProps>
}

// 基础操作组配置
export const BASIC_GROUP: ToolbarGroupProps = {
  key: 'basic',
  title: '基础操作',
  showDivider: true,
  items: [
    {
      type: 'button',
      command: 'undo',
      title: '撤销 (Ctrl+Z)',
      icon: 'undo'
    },
    {
      type: 'button',
      command: 'redo',
      title: '重做 (Ctrl+Y)',
      icon: 'redo'
    },
    {
      type: 'button',
      command: 'painter',
      title: '格式刷',
      icon: 'paint-brush'
    },
    {
      type: 'button',
      command: 'format',
      title: '清除格式',
      icon: 'eraser'
    }
  ]
}

// 文本格式组配置
export const TEXT_GROUP: ToolbarGroupProps = {
  key: 'text',
  title: '文本格式',
  showDivider: true,
  items: [
    {
      type: 'dropdown',
      command: 'font',
      title: '字体',
      displayText: '微软雅黑',
      dropdownOptions: [
        { value: "'Microsoft YaHei', '微软雅黑', sans-serif", label: '微软雅黑' },
        { value: "'华文宋体', 'STSong', 'Microsoft YaHei', sans-serif", label: '华文宋体' },
        { value: "'华文黑体', 'STHeiti', 'Microsoft YaHei', sans-serif", label: '华文黑体' },
        { value: "'华文仿宋', 'FangSong', 'Microsoft YaHei', sans-serif", label: '华文仿宋' },
        { value: "'华文楷体', 'STKaiti', 'Microsoft YaHei', sans-serif", label: '华文楷体' },
        { value: "'华文琥珀', 'HuaWenHuPo', 'Microsoft YaHei', sans-serif", label: '华文琥珀' },
        { value: "'华文隶书', 'STLiti', 'Microsoft YaHei', sans-serif", label: '华文隶书' },
        { value: "'华文新魏', 'STXinwei', 'Microsoft YaHei', sans-serif", label: '华文新魏' },
        { value: "'华文行楷', 'STXingkai', 'Microsoft YaHei', sans-serif", label: '华文行楷' },
        { value: "'华文中宋', 'STZhongsong', 'Microsoft YaHei', sans-serif", label: '华文中宋' },
        { value: "'华文彩云', 'STCaiyun', 'Microsoft YaHei', sans-serif", label: '华文彩云' },
        { value: "Arial, 'Helvetica Neue', Helvetica, sans-serif", label: 'Arial' },
        { value: "'Segoe UI', Tahoma, Geneva, sans-serif", label: 'Segoe UI' },
        { value: "'Ink Free', cursive", label: 'Ink Free' },
        { value: "Fantasy, 'Impact', Charcoal, sans-serif", label: 'Fantasy' }
      ]
    },
    {
      type: 'dropdown',
      command: 'size',
      title: '字号',
      displayText: '小四',
      dropdownOptions: [
        { value: 56, label: '初号' },
        { value: 48, label: '小初' },
        { value: 34, label: '一号' },
        { value: 32, label: '小一' },
        { value: 29, label: '二号' },
        { value: 24, label: '小二' },
        { value: 21, label: '三号' },
        { value: 20, label: '小三' },
        { value: 18, label: '四号' },
        { value: 16, label: '小四' },
        { value: 14, label: '五号' },
        { value: 12, label: '小五' },
        { value: 10, label: '六号' },
        { value: 8, label: '小六' },
        { value: 7, label: '七号' },
        { value: 6, label: '八号' }
      ]
    },
    {
      type: 'button',
      command: 'bold',
      title: '粗体',
      icon: 'bold'
    },
    {
      type: 'button',
      command: 'italic',
      title: '斜体',
      icon: 'italic'
    },
    {
      type: 'dropdown',
      command: 'underline',
      title: '下划线',
      dropdownOptions: [
        { value: 'solid', label: '', icon: 'line-single' },
        { value: 'double', label: '', icon: 'line-double' },
        { value: 'dashed', label: '', icon: 'line-dash' },
        { value: 'dotted', label: '', icon: 'line-dot' },
        { value: 'wavy', label: '', icon: 'line-wavy' }
      ],
      icon: 'underline'
    },
    {
      type: 'button',
      command: 'color',
      title: '字体颜色',
      icon: 'palette'
    },
    {
      type: 'button',
      command: 'highlight',
      title: '背景色',
      icon: 'highlighter'
    },
    {
      type: 'button',
      command: 'size-add',
      title: '增大字号',
      icon: 'size-add'
    },
    {
      type: 'button',
      command: 'size-minus',
      title: '减小字号',
      icon: 'size-minus'
    },
    {
      type: 'button',
      command: 'strikeout',
      title: '删除线',
      icon: 'strikeout'
    },
    {
      type: 'button',
      command: 'superscript',
      title: '上标',
      icon: 'superscript'
    },
    {
      type: 'button',
      command: 'subscript',
      title: '下标',
      icon: 'subscript'
    }
  ]
}

// 文档结构与对齐（标题、对齐、列表、行距）
export const STRUCTURE_GROUP: ToolbarGroupProps = {
  key: 'structure',
  title: '文档结构',
  showDivider: true,
  items: [
    {
      type: 'dropdown',
      command: 'title',
      title: '切换标题',
      displayText: '正文',
      dropdownOptions: [
        { value: 'normal', label: '正文', style: { fontSize: '14px' } },
        { value: 'first', label: '标题1', style: { fontSize: '24px' } },
        { value: 'second', label: '标题2', style: { fontSize: '22px' } },
        { value: 'third', label: '标题3', style: { fontSize: '20px' } },
        { value: 'fourth', label: '标题4', style: { fontSize: '18px' } },
        { value: 'fifth', label: '标题5', style: { fontSize: '16px' } },
        { value: 'sixth', label: '标题6', style: { fontSize: '14px' } }
      ]
    },
    { type: 'button', command: 'align-left', title: '左对齐', icon: 'align-left' },
    { type: 'button', command: 'align-center', title: '居中', icon: 'align-center' },
    { type: 'button', command: 'align-right', title: '右对齐', icon: 'align-right' },
    { type: 'button', command: 'alignment', title: '两端对齐', icon: 'alignment' },
    { type: 'button', command: 'justify', title: '分散对齐', icon: 'justify' },
    {
      type: 'dropdown',
      command: 'row-margin',
      title: '行间距',
      displayText: '1',
      dropdownOptions: [
        { value: 1, label: '1' },
        { value: 1.25, label: '1.25' },
        { value: 1.5, label: '1.5' },
        { value: 1.75, label: '1.75' },
        { value: 2, label: '2' },
        { value: 2.5, label: '2.5' },
        { value: 3, label: '3' }
      ],
      icon: 'row-margin'
    },
    {
      type: 'dropdown',
      command: 'list',
      title: '列表',
      displayText: '',
      dropdownOptions: [
        { value: 'none', label: '取消列表' },
        {
          value: 'ol',
          label: '有序列表',
          html: '<label>有序列表：</label><ol><li>________</li></ol>'
        },
        {
          value: 'ul-checkbox',
          label: '复选框列表',
          html: "<label>复选框列表：</label><ul style=\"list-style-type:'☑️ ';\"><li>________</li></ul>"
        },
        {
          value: 'ul-disc',
          label: '实心圆点列表',
          html: '<label>实心圆点列表：</label><ul style="list-style-type: disc;"><li>________</li></ul>'
        },
        {
          value: 'ul-circle',
          label: '空心圆点列表',
          html: '<label>空心圆点列表：</label><ul style="list-style-type: circle;"><li>________</li></ul>'
        },
        {
          value: 'ul-square',
          label: '空心方块列表',
          html: "<label>空心方块列表：</label><ul style=\"list-style-type:'☐ ';\"><li>________</li></ul>"
        }
      ],
      icon: 'list'
    }
  ]
}

// （医疗控件已并入插入元素组）

// 插入元素组配置
export const INSERT_GROUP: ToolbarGroupProps = {
  key: 'insert',
  title: '插入元素',
  showDivider: true,
  items: [
    {
      type: 'button',
      command: 'table',
      title: '插入表格',
      icon: 'table'
    },
    {
      type: 'button',
      command: 'image',
      title: '插入图片',
      icon: 'image'
    },
    {
      type: 'button',
      command: 'link',
      title: '插入链接',
      icon: 'link'
    },
    {
      type: 'button',
      command: 'formula',
      title: '数学公式ww',
      icon: 'formula'
    },
    {
      type: 'button',
      command: 'codeblock',
      title: '代码块',
      icon: 'codeblock'
    },
    {
      type: 'button',
      command: 'page-break',
      title: '分页符',
      icon: 'page-break'
    },
    {
      type: 'dropdown',
      command: 'separator',
      title: '分隔符',
      icon: 'separator',
      dropdownOptions: [
        { value: '0,0', label: '单实线', html: '<i></i>' },
        { value: '1,1', label: '点线', html: '<i></i>' },
        { value: '3,1', label: '短划线', html: '<i></i>' },
        { value: '4,4', label: '长划线', html: '<i></i>' },
        { value: '7,3,3,3', label: '划点划', html: '<i></i>' },
        { value: '6,2,2,2,2,2', label: '划点点', html: '<i></i>' }
      ]
    }
    ,
    {
      type: 'button',
      command: 'watermark',
      title: '水印',
      icon: 'watermark'
    },
    {
      type: 'dropdown',
      command: 'control',
      title: '控件',
      icon: 'control',
      dropdownOptions: [
        { value: 'text', label: '文本控件', icon: 'control-text' },
        { value: 'number', label: '数值控件', icon: 'control-number' },
        { value: 'select', label: '选择控件', icon: 'control-select' },
        { value: 'date', label: '日期控件', icon: 'control-date' },
        { value: 'checkbox', label: '复选框', icon: 'control-checkbox' },
        { value: 'radio', label: '单选框', icon: 'control-radio' }
      ]
    },
    {
      type: 'button',
      command: 'control-checkbox',
      title: '复选框',
      icon: 'checkbox'
    },
    {
      type: 'button',
      command: 'control-radio',
      title: '单选框',
      icon: 'radio'
    },
    {
      type: 'button',
      command: 'latex',
      title: 'LateX',
      icon: 'latex'
    },
    {
      type: 'dropdown',
      command: 'date',
      title: '日期',
      icon: 'date',
      dropdownOptions: [
        { value: 'yyyy-MM-dd', label: '日期' },
        { value: 'yyyy-MM-dd hh:mm:ss', label: '日期和时间' }
      ]
    },
    {
      type: 'button',
      command: 'block',
      title: '内容块',
      icon: 'block'
    }
  ]
}
// （视图控制在当前 HTML 菜单中未出现，已移除）

// 高级 / 其他功能组（搜索、打印等）
export const ADVANCED_GROUP: ToolbarGroupProps = {
  key: 'advanced',
  title: '高级',
  showDivider: false,
  items: [
    {
      type: 'button',
      command: 'search',
      title: '搜索与替换',
      icon: 'search'
    },
    {
      type: 'button',
      command: 'print',
      title: '打印',
      icon: 'print'
    }
  ]
}

// 默认工具栏配置
export const DEFAULT_TOOLBAR_CONFIG: ToolbarConfig = {
  groups: {
    basic: BASIC_GROUP,
    text: TEXT_GROUP,
    structure: STRUCTURE_GROUP,
    insert: INSERT_GROUP,
    advanced: ADVANCED_GROUP
  }
}

// 根据配置生成可见的工具栏组
export function getVisibleGroups(
  config: ToolbarConfig,
  enabledGroups: Record<string, boolean>
): ToolbarGroupProps[] {
  return Object.keys(config.groups)
    .filter(key => enabledGroups[key])
    .map(key => config.groups[key]!)
}

// 工具栏命令类型
export type ToolbarCommand =
  | 'undo' | 'redo' | 'painter' | 'format'
  | 'font' | 'size' | 'bold' | 'italic' | 'underline'
  | 'color' | 'highlight' | 'align-left' | 'align-center' | 'align-right'
  | 'control-text' | 'control-select' | 'control-checkbox' | 'control-radio' | 'control-date' | 'control-number'
  | 'table' | 'image' | 'link' | 'formula' | 'separator'
  | 'zoom-out' | 'zoom-in' | 'page-mode'

// 命令处理器接口
export interface ToolbarCommandHandler {
  (command: ToolbarCommand, value?: string | number): void
}
