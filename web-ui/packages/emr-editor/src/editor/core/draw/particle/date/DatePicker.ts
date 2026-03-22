import {
  EDITOR_COMPONENT,
  EDITOR_PREFIX
} from '../../../../dataset/constant/Editor'
import { EditorComponent } from '../../../../dataset/enum/Editor'
import { IElementPosition } from '../../../../interface/Element'
import { scrollIntoView, findScrollContainer } from '../../../../utils'
import { Draw } from '../../Draw'

export interface IDatePickerLang {
  now: string
  confirm: string
  return: string
  timeSelect: string
  weeks: {
    sun: string
    mon: string
    tue: string
    wed: string
    thu: string
    fri: string
    sat: string
  }
  year: string
  month: string
  hour: string
  minute: string
  second: string
}

export interface IDatePickerOption {
  onSubmit?: (date: string) => void
}

interface IDatePickerDom {
  container: HTMLDivElement
  dateWrap: HTMLDivElement
  datePickerWeek: HTMLDivElement
  timeWrap: HTMLUListElement
  title: {
    preYear: HTMLSpanElement
    preMonth: HTMLSpanElement
    now: HTMLSpanElement
    nextMonth: HTMLSpanElement
    nextYear: HTMLSpanElement
  }
  day: HTMLDivElement
  time: {
    hour: HTMLOListElement
    minute: HTMLOListElement
    second: HTMLOListElement
  }
  menu: {
    time: HTMLButtonElement
    now: HTMLButtonElement
    submit: HTMLButtonElement
  }
}

interface IRenderOption {
  value: string
  position: IElementPosition
  dateFormat?: string
}

export class DatePicker {
  private draw: Draw
  private options: IDatePickerOption
  private now: Date
  private dom: IDatePickerDom
  private renderOptions: IRenderOption | null
  private isDatePicker: boolean
  private pickDate: Date | null
  private lang: IDatePickerLang
  private windowHandler: (() => void) | null = null
  private isWindowListenerAdded: boolean = false
  private scrollContainer: any = null
  private _lastTransform: { x: number; y: number } = { x: 0, y: 0 }
  private _lastPopupSize: { w: number; h: number } = { w: 0, h: 0 }
  private _pendingRAF: boolean = false
  private _resizeObserver: ResizeObserver | null = null

  constructor(draw: Draw, options: IDatePickerOption = {}) {
    this.draw = draw
    this.options = options
    this.lang = this._getLang()
    this.now = new Date()
    this.dom = this._createDom()
    this.renderOptions = null
    this.isDatePicker = true
    this.pickDate = null
    this.windowHandler = () => {
      this._requestReposition()
    }
    this.isWindowListenerAdded = false
    // setup resize observer to update popup size cache
    try {
      this._resizeObserver = new ResizeObserver(entries => {
        const e = entries[0]
        if (e) {
          const cr = e.contentRect
          this._lastPopupSize.w = cr.width
          this._lastPopupSize.h = cr.height
          this._requestReposition()
        }
      })
      this._resizeObserver.observe(this.dom.container)
    } catch (e) {
      this._resizeObserver = null
    }
    this._bindEvent()
  }

  private _createDom(): IDatePickerDom {
    const datePickerContainer = document.createElement('div')
    datePickerContainer.classList.add(`${EDITOR_PREFIX}-date-container`)
    datePickerContainer.setAttribute(EDITOR_COMPONENT, EditorComponent.POPUP)
    // title-切换年月、年月显示
    const dateWrap = document.createElement('div')
    dateWrap.classList.add(`${EDITOR_PREFIX}-date-wrap`)
    const datePickerTitle = document.createElement('div')
    datePickerTitle.classList.add(`${EDITOR_PREFIX}-date-title`)
    const preYearTitle = document.createElement('span')
    preYearTitle.classList.add(`${EDITOR_PREFIX}-date-title__pre-year`)
    preYearTitle.innerText = `<<`
    const preMonthTitle = document.createElement('span')
    preMonthTitle.classList.add(`${EDITOR_PREFIX}-date-title__pre-month`)
    preMonthTitle.innerText = `<`
    const nowTitle = document.createElement('span')
    nowTitle.classList.add(`${EDITOR_PREFIX}-date-title__now`)
    const nextMonthTitle = document.createElement('span')
    nextMonthTitle.classList.add(`${EDITOR_PREFIX}-date-title__next-month`)
    nextMonthTitle.innerText = `>`
    const nextYearTitle = document.createElement('span')
    nextYearTitle.classList.add(`${EDITOR_PREFIX}-date-title__next-year`)
    nextYearTitle.innerText = `>>`
    datePickerTitle.append(preYearTitle)
    datePickerTitle.append(preMonthTitle)
    datePickerTitle.append(nowTitle)
    datePickerTitle.append(nextMonthTitle)
    datePickerTitle.append(nextYearTitle)
    // week-星期显示
    const datePickerWeek = document.createElement('div')
    datePickerWeek.classList.add(`${EDITOR_PREFIX}-date-week`)
    const {
      weeks: { sun, mon, tue, wed, thu, fri, sat }
    } = this.lang
    const weekList = [sun, mon, tue, wed, thu, fri, sat]
    weekList.forEach(week => {
      const weekDom = document.createElement('span')
      weekDom.innerText = `${week}`
      datePickerWeek.append(weekDom)
    })
    // day-天数显示
    const datePickerDay = document.createElement('div')
    datePickerDay.classList.add(`${EDITOR_PREFIX}-date-day`)
    // 日期内容构建
    dateWrap.append(datePickerTitle)
    dateWrap.append(datePickerWeek)
    dateWrap.append(datePickerDay)
    // time-时间选择
    const timeWrap = document.createElement('ul')
    timeWrap.classList.add(`${EDITOR_PREFIX}-time-wrap`)
    let hourTime: HTMLOListElement
    let minuteTime: HTMLOListElement
    let secondTime: HTMLOListElement
    const timeList = [this.lang.hour, this.lang.minute, this.lang.second]
    timeList.forEach((t, i) => {
      const li = document.createElement('li')
      const timeText = document.createElement('span')
      timeText.innerText = t
      li.append(timeText)
      const ol = document.createElement('ol')
      const isHour = i === 0
      const isMinute = i === 1
      const endIndex = isHour ? 24 : 60
      for (let i = 0; i < endIndex; i++) {
        const time = document.createElement('li')
        time.innerText = `${String(i).padStart(2, '0')}`
        time.setAttribute('data-id', `${i}`)
        ol.append(time)
      }
      if (isHour) {
        hourTime = ol
      } else if (isMinute) {
        minuteTime = ol
      } else {
        secondTime = ol
      }
      li.append(ol)
      timeWrap.append(li)
    })
    // menu-选择时间、现在、确定
    const datePickerMenu = document.createElement('div')
    datePickerMenu.classList.add(`${EDITOR_PREFIX}-date-menu`)
    const timeMenu = document.createElement('button')
    timeMenu.classList.add(`${EDITOR_PREFIX}-date-menu__time`)
    timeMenu.innerText = this.lang.timeSelect
    const nowMenu = document.createElement('button')
    nowMenu.classList.add(`${EDITOR_PREFIX}-date-menu__now`)
    nowMenu.innerText = this.lang.now
    const submitMenu = document.createElement('button')
    submitMenu.classList.add(`${EDITOR_PREFIX}-date-menu__submit`)
    submitMenu.innerText = this.lang.confirm
    datePickerMenu.append(timeMenu)
    datePickerMenu.append(nowMenu)
    datePickerMenu.append(submitMenu)
    // 构建
    datePickerContainer.append(dateWrap)
    datePickerContainer.append(timeWrap)
    datePickerContainer.append(datePickerMenu)
    // append to body so popup is out of editor flow and won't affect layout height
    document.body.append(datePickerContainer)
    // ensure fixed positioning and high z-index to overlay above editor
    datePickerContainer.style.position = 'fixed'
    datePickerContainer.style.zIndex = '1200'
    return {
      container: datePickerContainer,
      dateWrap,
      datePickerWeek,
      timeWrap,
      title: {
        preYear: preYearTitle,
        preMonth: preMonthTitle,
        now: nowTitle,
        nextMonth: nextMonthTitle,
        nextYear: nextYearTitle
      },
      day: datePickerDay,
      time: {
        hour: hourTime!,
        minute: minuteTime!,
        second: secondTime!
      },
      menu: {
        time: timeMenu,
        now: nowMenu,
        submit: submitMenu
      }
    }
  }

  private _bindEvent() {
    this.dom.title.preYear.onclick = () => {
      this._preYear()
    }
    this.dom.title.preMonth.onclick = () => {
      this._preMonth()
    }
    this.dom.title.nextMonth.onclick = () => {
      this._nextMonth()
    }
    this.dom.title.nextYear.onclick = () => {
      this._nextYear()
    }
    this.dom.menu.time.onclick = () => {
      this.isDatePicker = !this.isDatePicker
      this._toggleDateTimePicker()
    }
    this.dom.menu.now.onclick = () => {
      this._now()
      this._submit()
    }
    this.dom.menu.submit.onclick = () => {
      this.dispose()
      this._submit()
    }
    this.dom.time.hour.onclick = evt => {
      if (!this.pickDate) return
      const li = <HTMLLIElement>evt.target
      const id = li.dataset.id
      if (!id) return
      this.pickDate.setHours(Number(id))
      this._setTimePick(false)
    }
    this.dom.time.minute.onclick = evt => {
      if (!this.pickDate) return
      const li = <HTMLLIElement>evt.target
      const id = li.dataset.id
      if (!id) return
      this.pickDate.setMinutes(Number(id))
      this._setTimePick(false)
    }
    this.dom.time.second.onclick = evt => {
      if (!this.pickDate) return
      const li = <HTMLLIElement>evt.target
      const id = li.dataset.id
      if (!id) return
      this.pickDate.setSeconds(Number(id))
      this._setTimePick(false)
    }
  }

  private _setPosition() {
    if (!this.renderOptions) return
    const {
      position: {
      coordinate: {
          leftTop: [left = 0, top = 0]
        },
        lineHeight,
        pageNo
      }
    } = this.renderOptions
    const height = this.draw.getHeight()
    const pageGap = this.draw.getPageGap()
    const currentPageNo = pageNo ?? this.draw.getPageNo()
    const preY = currentPageNo * (height + pageGap)
    // 位置 - when using fixed positioning (appended to body), compute viewport coordinates and perform flip/constraints
    try {
      const cStyle = getComputedStyle(this.dom.container)
      // base viewport anchor coordinate (top-left of anchor relative to viewport)
      const drawRect = this.draw.getContainer().getBoundingClientRect()
      const anchorViewportLeft = drawRect.left + left
      const anchorViewportTop = drawRect.top + preY + top

      if (cStyle.position === 'fixed') {
        // ensure popup is visible to measure its size
        const prevDisplay = this.dom.container.style.display
        this.dom.container.style.display = 'block'
        const popupRect = this.dom.container.getBoundingClientRect()
        const popupWidth = popupRect.width || 300
        const popupHeight = popupRect.height || 300
        const viewportWidth = window.innerWidth
        const viewportHeight = window.innerHeight
        const footbarRect = document.querySelector('.emr-footbar-container')?.getBoundingClientRect()
        const footbarTop = footbarRect ? footbarRect.top : viewportHeight
        const margin = 8

        // prefer placing below anchor
        const spaceBelow = viewportHeight - (anchorViewportTop + lineHeight) - margin
        const spaceAbove = anchorViewportTop - margin

        let finalTop: number
        if (spaceBelow >= popupHeight && (anchorViewportTop + lineHeight + popupHeight) <= (footbarTop - margin)) {
          // enough space below and won't overlap footer
          finalTop = anchorViewportTop + lineHeight
        } else if (spaceAbove >= popupHeight) {
          // enough space above
          finalTop = anchorViewportTop - popupHeight
        } else {
          // limited space: fit within the larger side by limiting height
          if (spaceBelow >= spaceAbove) {
            finalTop = anchorViewportTop + lineHeight
            const maxAvail = Math.max(50, Math.min(spaceBelow, footbarTop - anchorViewportTop - lineHeight - margin))
            this.dom.container.style.maxHeight = `${maxAvail}px`
            this.dom.container.style.overflow = 'auto'
          } else {
            finalTop = Math.max(margin, anchorViewportTop - Math.max(50, spaceAbove))
            const maxAvail = Math.max(50, spaceAbove)
            this.dom.container.style.maxHeight = `${maxAvail}px`
            this.dom.container.style.overflow = 'auto'
          }
        }

        // horizontal clamp
        let finalLeft = anchorViewportLeft
        if (finalLeft + popupWidth + margin > viewportWidth) {
          finalLeft = Math.max(margin, viewportWidth - popupWidth - margin)
        }

        // use transform to avoid layout thrashing on scroll
        this.dom.container.style.left = `0px`
        this.dom.container.style.top = `0px`
        this.dom.container.style.willChange = 'transform'
        const dx = Math.round(finalLeft * 100) / 100
        const dy = Math.round(finalTop * 100) / 100
        const threshold = 0.5
        if (
          Math.abs(this._lastTransform.x - dx) > threshold ||
          Math.abs(this._lastTransform.y - dy) > threshold
        ) {
          this.dom.container.style.transform = `translate3d(${dx}px, ${dy}px, 0)`
          this._lastTransform.x = dx
          this._lastTransform.y = dy
        }
        // restore inline display if necessary
        this.dom.container.style.display = prevDisplay
      } else {
        // fallback: original relative positioning
        this.dom.container.style.left = `${left}px`
        this.dom.container.style.top = `${top + preY + lineHeight}px`
      }
    } catch (e) {
      this.dom.container.style.left = `${left}px`
      this.dom.container.style.top = `${top + preY + lineHeight}px`
    }
  }

  private _requestReposition() {
    if (this._pendingRAF) return
    this._pendingRAF = true
    requestAnimationFrame(() => {
      try {
        this._setPosition()
      } finally {
        this._pendingRAF = false
      }
    })
  }

  public isInvalidDate(value: Date): boolean {
    return value.toDateString() === 'Invalid Date'
  }

  private _setValue() {
    const value = this.renderOptions?.value!
    if (value) {
      const setDate = new Date(value)
      this.now = this.isInvalidDate(setDate) ? new Date() : setDate
    } else {
      this.now = new Date()
    }
    this.pickDate = new Date(this.now)
  }

  private _getLang() {
    const i18n = this.draw.getI18n()
    const t = i18n.t.bind(i18n)
    return {
      now: t('datePicker.now'),
      confirm: t('datePicker.confirm'),
      return: t('datePicker.return'),
      timeSelect: t('datePicker.timeSelect'),
      weeks: {
        sun: t('datePicker.weeks.sun'),
        mon: t('datePicker.weeks.mon'),
        tue: t('datePicker.weeks.tue'),
        wed: t('datePicker.weeks.wed'),
        thu: t('datePicker.weeks.thu'),
        fri: t('datePicker.weeks.fri'),
        sat: t('datePicker.weeks.sat')
      },
      year: t('datePicker.year'),
      month: t('datePicker.month'),
      hour: t('datePicker.hour'),
      minute: t('datePicker.minute'),
      second: t('datePicker.second')
    }
  }

  private _setLangChange() {
    this.dom.menu.time.innerText = this.lang.timeSelect
    this.dom.menu.now.innerText = this.lang.now
    this.dom.menu.submit.innerText = this.lang.confirm
    const {
      weeks: { sun, mon, tue, wed, thu, fri, sat }
    } = this.lang
    const weekList = [sun, mon, tue, wed, thu, fri, sat]
    this.dom.datePickerWeek.childNodes.forEach((child, i) => {
      const childElement = <HTMLSpanElement>child
      childElement.innerText = weekList[i]!
    })
    const hourTitle = <HTMLSpanElement>this.dom.time.hour.previousElementSibling
    hourTitle.innerText = this.lang.hour
    const minuteTitle = <HTMLSpanElement>(
      this.dom.time.minute.previousElementSibling
    )
    minuteTitle.innerText = this.lang.minute
    const secondTitle = <HTMLSpanElement>(
      this.dom.time.second.previousElementSibling
    )
    secondTitle.innerText = this.lang.second
  }

  private _update() {
    // 本地年月日
    const localDate = new Date()
    const localYear = localDate.getFullYear()
    const localMonth = localDate.getMonth() + 1
    const localDay = localDate.getDate()
    // 选择年月日
    let pickYear: number | null = null
    let pickMonth: number | null = null
    let pickDay: number | null = null
    if (this.pickDate) {
      pickYear = this.pickDate.getFullYear()
      pickMonth = this.pickDate.getMonth() + 1
      pickDay = this.pickDate.getDate()
    }
    // 当前年月日
    const year = this.now.getFullYear()
    const month = this.now.getMonth() + 1
    this.dom.title.now.innerText = `${year}${this.lang.year} ${String(
      month
    ).padStart(2, '0')}${this.lang.month}`
    // 日期补差
    const curDate = new Date(year, month, 0) // 当月日期
    const curDay = curDate.getDate() // 当月总天数
    let curWeek = new Date(year, month - 1, 1).getDay() // 当月第一天星期几
    if (curWeek === 0) {
      curWeek = 7
    }
    const preDay = new Date(year, month - 1, 0).getDate() // 上个月天数
    this.dom.day.innerHTML = ''
    // 渲染上个月日期
    const preStartDay = preDay - curWeek + 1
    for (let i = preStartDay; i <= preDay; i++) {
      const dayDom = document.createElement('div')
      dayDom.classList.add('disable')
      dayDom.innerText = `${i}`
      dayDom.onclick = () => {
        const newMonth = month - 2
        this.now = new Date(year, newMonth, i)
        this._setDatePick(year, newMonth, i)
      }
      this.dom.day.append(dayDom)
    }
    // 渲染当月日期
    for (let i = 1; i <= curDay; i++) {
      const dayDom = document.createElement('div')
      if (localYear === year && localMonth === month && localDay === i) {
        dayDom.classList.add('active')
      }
      if (
        this.pickDate &&
        pickYear === year &&
        pickMonth === month &&
        pickDay === i
      ) {
        dayDom.classList.add('select')
      }
      dayDom.innerText = `${i}`
      dayDom.onclick = evt => {
        evt.stopPropagation()
        const newMonth = month - 1
        this.now = new Date(year, newMonth, i)
        this._setDatePick(year, newMonth, i)
        // single click selects immediately
        this._submit()
        this.dispose()
      }
      this.dom.day.append(dayDom)
    }
    // 渲染下月日期
    const nextEndDay = 6 * 7 - curWeek - curDay
    for (let i = 1; i <= nextEndDay; i++) {
      const dayDom = document.createElement('div')
      dayDom.classList.add('disable')
      dayDom.innerText = `${i}`
      dayDom.onclick = () => {
        this.now = new Date(year, month, i)
        this._setDatePick(year, month, i)
      }
      this.dom.day.append(dayDom)
    }
  }

  private _toggleDateTimePicker() {
    if (this.isDatePicker) {
      this.dom.dateWrap.classList.add('active')
      this.dom.timeWrap.classList.remove('active')
      this.dom.menu.time.innerText = this.lang.timeSelect
    } else {
      this.dom.dateWrap.classList.remove('active')
      this.dom.timeWrap.classList.add('active')
      this.dom.menu.time.innerText = this.lang.return
      // 设置时分秒选择
      this._setTimePick()
    }
  }

  private _setDatePick(year: number, month: number, day: number) {
    this.now = new Date(year, month, day)
    this.pickDate?.setFullYear(year)
    this.pickDate?.setMonth(month)
    this.pickDate?.setDate(day)
    this._update()
  }

  private _setTimePick(isIntoView = true) {
    const hour = this.pickDate?.getHours() || 0
    const minute = this.pickDate?.getMinutes() || 0
    const second = this.pickDate?.getSeconds() || 0
    const {
      hour: hourDom,
      minute: minuteDom,
      second: secondDom
    } = this.dom.time
    const timeDomList = [hourDom, minuteDom, secondDom]
    // 清空
    timeDomList.forEach(timeDom => {
      timeDom
        .querySelectorAll('li')
        .forEach(li => li.classList.remove('active'))
    })
    const pickList: [HTMLOListElement, number][] = [
      [hourDom, hour],
      [minuteDom, minute],
      [secondDom, second]
    ]
    pickList.forEach(([dom, time]) => {
      const pickDom = dom.querySelector<HTMLLIElement>(`[data-id='${time}']`)!
      pickDom.classList.add('active')
      if (isIntoView) {
        scrollIntoView(dom, pickDom)
      }
    })
  }

  private _preMonth() {
    this.now.setMonth(this.now.getMonth() - 1)
    this._update()
  }

  private _nextMonth() {
    this.now.setMonth(this.now.getMonth() + 1)
    this._update()
  }

  private _preYear() {
    this.now.setFullYear(this.now.getFullYear() - 1)
    this._update()
  }

  private _nextYear() {
    this.now.setFullYear(this.now.getFullYear() + 1)
    this._update()
  }

  private _now() {
    this.pickDate = new Date()
    this.dispose()
  }

  private _toggleVisible(isVisible: boolean) {
    if (isVisible) {
      this.dom.container.classList.add('active')
    } else {
      this.dom.container.classList.remove('active')
    }
  }

  private _submit() {
    if (this.options.onSubmit && this.pickDate) {
      const format = this.renderOptions?.dateFormat
      const pickDateString = this.formatDate(this.pickDate, format)
      this.options.onSubmit(pickDateString)
    }
  }

  public formatDate(date: Date, format = 'YYYY-MM-DD HH:mm:ss'): string {
    let dateString = format
    const year = date.getFullYear().toString()
    const month = (date.getMonth() + 1).toString()
    const day = date.getDate().toString()
    const hours24 = date.getHours()
    const hours12 = hours24 % 12 === 0 ? 12 : hours24 % 12
    const minute = date.getMinutes().toString()
    const second = date.getSeconds().toString()
    const millisecond = date.getMilliseconds().toString()
    const dateOption = {
      'y+': year,
      'Y+': year,
      'M+': month,
      'd+': day,
      'D+': day,
      'h+': hours12.toString(),
      'H+': hours24.toString(),
      'm+': minute,
      's+': second,
      'S+': millisecond
    }
    for (const k in dateOption) {
      const reg = new RegExp('(' + k + ')').exec(format)
      const key = <keyof typeof dateOption>k
      if (reg) {
        dateString = dateString.replace(
          reg[1]!,
          reg[1]!.length === 1
            ? dateOption[key]!
            : dateOption[key]!.padStart(reg[1]!.length, '0')
        )
      }
    }
    return dateString
  }

  public render(option: IRenderOption) {
    this.renderOptions = option
    this.lang = this._getLang()
    this._setLangChange()
    this._setValue()
    this._update()
    this._setPosition()
    this.isDatePicker = true
    this._toggleDateTimePicker()
    this._toggleVisible(true)
    // add window listeners to reposition on scroll/resize while visible
    if (!this.isWindowListenerAdded && this.windowHandler) {
      // attach to nearest scrollable ancestor to reduce event volume
      const sc = findScrollContainer(this.draw.getContainer())
      this.scrollContainer = sc || window
      // If scroll container is the documentElement, also listen on window for compat
      if (this.scrollContainer === window || this.scrollContainer === document.documentElement) {
        window.addEventListener('scroll', this.windowHandler, { passive: true })
      } else if (this.scrollContainer instanceof HTMLElement) {
        this.scrollContainer.addEventListener('scroll', this.windowHandler, { passive: true })
      }
      window.addEventListener('resize', this.windowHandler)
      this.isWindowListenerAdded = true
    }
    // ensure position is recalculated after the popup becomes visible and layout settles
    requestAnimationFrame(() => {
      this._setPosition()
    })
  }

  public dispose() {
    this._toggleVisible(false)
    // remove listeners when hiding
    if (this.isWindowListenerAdded && this.windowHandler) {
      if (this.scrollContainer === window || this.scrollContainer === document.documentElement) {
        window.removeEventListener('scroll', this.windowHandler)
      } else if (this.scrollContainer instanceof HTMLElement) {
        this.scrollContainer.removeEventListener('scroll', this.windowHandler)
      }
      window.removeEventListener('resize', this.windowHandler)
      this.isWindowListenerAdded = false
      this.scrollContainer = null
    }
    if (this._resizeObserver) {
      try {
        this._resizeObserver.disconnect()
      } catch {}
      this._resizeObserver = null
    }
  }

  public destroy() {
    // ensure listeners removed
    this.dispose()
    this.dom.container.remove()
  }
}
