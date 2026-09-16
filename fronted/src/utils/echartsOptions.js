/**
 * 看板 ECharts 统一配置工厂（标准后台风格）
 * 各图只传数据，禁止在页面里重复拼 option。
 */

// 标准后台调色板（Element Plus 色系）
export const PALETTE = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#9254DE', '#36CFC9', '#FF85C0']

const TEXT = '#303133'
const SUB_TEXT = '#606266'
const SPLIT_LINE = '#EBEEF5'
const AXIS_LINE = '#DCDFE6'
const TOOLTIP_BORDER = '#DCDFE6'

function baseGrid(extra = {}) {
  return { left: 12, right: 20, top: 42, bottom: 12, containLabel: true, ...extra }
}

const axisLabelStyle = { color: SUB_TEXT, fontSize: 11 }
const splitLineStyle = { lineStyle: { color: SPLIT_LINE, type: 'dashed' } }

function valueAxis(name) {
  return {
    type: 'value',
    name: name || '',
    nameTextStyle: { color: SUB_TEXT, fontSize: 11 },
    axisLabel: axisLabelStyle,
    splitLine: splitLineStyle
  }
}

function categoryAxis(categories, extra = {}) {
  return {
    type: 'category',
    boundaryGap: extra.boundaryGap ?? true,
    data: categories,
    axisLine: { lineStyle: { color: AXIS_LINE } },
    axisTick: { show: false },
    axisLabel: axisLabelStyle,
    ...extra
  }
}

/**
 * 折线图（支持双 Y 轴）
 * @param {string[]} categories 类目轴
 * @param {Array<{name,data,color?,area?,yIndex?}>} series 系列
 * @param {{rotate?:number,legend?:boolean,smooth?:boolean}} opts
 */
export function lineChart(categories, series, opts = {}) {
  const dual = series.some(s => s.yIndex === 1)
  return {
    color: PALETTE,
    tooltip: { trigger: 'axis', borderColor: TOOLTIP_BORDER, textStyle: { color: TEXT } },
    legend: {
      show: opts.legend !== false,
      top: 4,
      textStyle: { color: SUB_TEXT, fontSize: 12 }
    },
    grid: baseGrid({ top: opts.legend === false ? 16 : 42, bottom: opts.rotate ? 56 : 12 }),
    xAxis: categoryAxis(categories, {
      boundaryGap: false,
      axisLabel: { ...axisLabelStyle, rotate: opts.rotate || 0, hideOverlap: true }
    }),
    yAxis: dual ? [valueAxis(), { ...valueAxis(), splitLine: { show: false } }] : valueAxis(),
    series: series.map(s => ({
      name: s.name,
      type: 'line',
      smooth: opts.smooth !== false,
      yAxisIndex: s.yIndex || 0,
      data: s.data,
      symbolSize: 6,
      lineStyle: { width: 2.5, color: s.color },
      itemStyle: { color: s.color },
      areaStyle: s.area ? { color: hexArea(s.color || PALETTE[0]) } : undefined,
      ...(s.dashed ? { lineStyle: { width: 2.5, color: s.color, type: 'dashed' } } : {})
    }))
  }
}

/**
 * 纵向柱状图（支持逐柱 itemStyle 颜色）
 * @param {string[]} categories
 * @param {Array<{name,data,color?}>} series
 */
export function barChart(categories, series, opts = {}) {
  return {
    color: PALETTE,
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, borderColor: TOOLTIP_BORDER, textStyle: { color: TEXT } },
    legend: { show: opts.legend === true, top: 4, textStyle: { color: SUB_TEXT } },
    grid: baseGrid({ top: opts.legend === true ? 42 : 18, bottom: opts.rotate ? 56 : 12 }),
    xAxis: categoryAxis(categories, {
      axisLabel: { ...axisLabelStyle, rotate: opts.rotate || 0, interval: opts.rotate ? 0 : 'auto' }
    }),
    yAxis: valueAxis(),
    series: series.map(s => ({
      name: s.name,
      type: 'bar',
      data: s.data,
      barMaxWidth: 34,
      itemStyle: { borderRadius: [4, 4, 0, 0], color: s.color }
    }))
  }
}

/**
 * 饼图 / 环形图
 * @param {Array<{name,value}>} data
 * @param {{ring?:boolean}} opts
 */
export function pieChart(data, opts = {}) {
  return {
    color: PALETTE,
    tooltip: { trigger: 'item', borderColor: TOOLTIP_BORDER, textStyle: { color: TEXT } },
    legend: { bottom: 0, type: 'scroll', textStyle: { color: SUB_TEXT, fontSize: 11 } },
    series: [{
      type: 'pie',
      radius: opts.ring ? ['45%', '70%'] : '68%',
      center: ['50%', '46%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: { color: TEXT, fontSize: 11 },
      labelLine: { lineStyle: { color: AXIS_LINE } },
      data
    }]
  }
}

/**
 * 热力图（日期 × 情绪）
 * @param {{dates:string[],emotions:string[],data:number[][]}} heatmap
 */
export function heatmapChart(heatmap) {
  const max = Math.max(1, ...(heatmap.data || []).map(d => d[2] || 0))
  return {
    tooltip: {
      position: 'top',
      borderColor: TOOLTIP_BORDER,
      textStyle: { color: TEXT },
      formatter: p => `${heatmap.dates[p.value[0]]} · ${heatmap.emotions[p.value[1]]}<br/>记录数：<b>${p.value[2]}</b>`
    },
    grid: { left: 12, right: 16, top: 16, bottom: 76, containLabel: true },
    xAxis: categoryAxis(heatmap.dates, {
      splitArea: { show: false },
      axisLabel: { ...axisLabelStyle, rotate: 45, interval: 2, fontSize: 10 }
    }),
    yAxis: {
      type: 'category',
      data: heatmap.emotions,
      axisLine: { lineStyle: { color: AXIS_LINE } },
      axisTick: { show: false },
      axisLabel: axisLabelStyle
    },
    visualMap: {
      min: 0,
      max,
      calculable: false,
      orient: 'horizontal',
      left: 'center',
      bottom: 4,
      itemWidth: 12,
      itemHeight: 90,
      textStyle: { color: SUB_TEXT, fontSize: 10 },
      inRange: { color: ['#F0F7FF', '#A0CFFF', '#409EFF', '#F56C6C'] }
    },
    series: [{
      type: 'heatmap',
      data: heatmap.data,
      itemStyle: { borderColor: '#fff', borderWidth: 1 },
      emphasis: { itemStyle: { shadowBlur: 6, shadowColor: 'rgba(0,0,0,0.15)' } }
    }]
  }
}

/**
 * 横向条形排行图（TOP10）
 * @param {Array<{name,value}>} items
 * @param {string} color
 */
export function horizBarChart(items, color = '#409EFF') {
  const names = items.map(i => i.name).reverse()
  const values = items.map(i => i.value).reverse()
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      borderColor: TOOLTIP_BORDER,
      textStyle: { color: TEXT }
    },
    grid: { left: 8, right: 42, top: 12, bottom: 12, containLabel: true },
    xAxis: {
      type: 'value',
      axisLabel: axisLabelStyle,
      splitLine: splitLineStyle,
      minInterval: 1
    },
    yAxis: {
      type: 'category',
      data: names,
      axisLine: { lineStyle: { color: AXIS_LINE } },
      axisTick: { show: false },
      axisLabel: { ...axisLabelStyle, width: 110, overflow: 'truncate', ellipsis: '…' }
    },
    series: [{
      type: 'bar',
      data: values,
      barMaxWidth: 18,
      itemStyle: { borderRadius: [0, 4, 4, 0], color },
      label: { show: true, position: 'right', color: TEXT, fontSize: 11 }
    }]
  }
}

/**
 * 仪表盘式完成率环形图；percent 为 null 时中心显示「—」
 * @param {number|null} percent 0-100
 */
export function gaugeChart(percent) {
  const hasValue = percent !== null && percent !== undefined
  const value = hasValue ? Number(percent) : 0
  return {
    series: [{
      type: 'gauge',
      startAngle: 220,
      endAngle: -40,
      min: 0,
      max: 100,
      radius: '92%',
      center: ['50%', '58%'],
      progress: {
        show: true,
        width: 16,
        roundCap: true,
        itemStyle: { color: rateColor(value) }
      },
      axisLine: {
        roundCap: true,
        lineStyle: { width: 16, color: [[1, '#E4E7ED']] }
      },
      pointer: { show: false },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { show: false },
      anchor: { show: false },
      title: { offsetCenter: [0, '32%'], color: SUB_TEXT, fontSize: 13 },
      detail: {
        valueAnimation: true,
        offsetCenter: [0, 0],
        fontSize: 30,
        fontWeight: 'bold',
        color: TEXT,
        formatter: () => (hasValue ? `${value.toFixed(1)}%` : '—')
      },
      data: [{ value, name: '预约完成率' }]
    }]
  }
}

function rateColor(v) {
  if (v >= 80) return '#67C23A'
  if (v >= 50) return '#409EFF'
  return '#E6A23C'
}

// 折线区域填充
function hexArea(hex) {
  return {
    type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
    colorStops: [
      { offset: 0, color: hex + '40' },
      { offset: 1, color: hex + '05' }
    ]
  }
}
