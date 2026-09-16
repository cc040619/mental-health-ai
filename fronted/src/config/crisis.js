// 心理危机干预热线与引导文案常量
// 全站唯一口径，一处修改全站生效；后端零参与，展示零网络请求
// 注意：热线号码以官方最新公布为准，答辩前人工核对一遍最新官方口径

// 全国心理援助热线（2025 年上线，统一短号）
// 北京心理危机研究与干预中心：010-82951332
// 希望 24 热线：400-161-9995
export const crisisHotlines = [
    {
        name: '全国心理援助热线',
        phone: '12356',
        note: '2025 年上线的全国统一心理援助短号，24 小时接听'
    },
    {
        name: '北京心理危机研究与干预中心',
        phone: '010-82951332',
        note: '国内最早的自杀研究与危机干预专业机构之一'
    },
    {
        name: '希望24热线',
        phone: '400-161-9995',
        note: '24 小时心理危机干预热线，全国接入'
    }
]

// 温和共情引导文案（卡片标题、底部陪伴语、安全计划入口）
// 伦理准则：不使用诊断性结论，统一用「值得被关心 / 可以寻求帮助」
export const crisisCopy = {
    title: '你现在不是一个人，我们在这里',
    subtitle: '此刻的感受很难熬，但你愿意留下来看看这段话，就已经很勇敢了。',
    hotlineHint: '如果你正在伤害自己，或有这样的想法，请先拨打下面任一条热线，有人正在等你。',
    callBtn: '拨打',
    copyBtn: '复制',
    planEntry: '填写我的安全计划',
    planHint: '提前记下可以信赖的人和应对方式，难熬时翻一翻',
    footer: '痛苦不会永远停留。哪怕只是一通电话、一个人，都能成为此刻的支撑。值得被关心，也可以寻求帮助。'
}

// 一键拨打：移动端 tel: 链接（PC 端点击会唤起系统拨号程序或无反应，前端兜底提示）
export const telLink = (phone) => `tel:${phone}`

// 复制号码到剪贴板，Clipboard API 不可用时降级为选中文本
// 返回 true=成功复制（PC 端可提示"已复制，请拨打"），false=降级选中
export const copyPhone = async (phone) => {
    if (navigator.clipboard && navigator.clipboard.writeText) {
        try {
            await navigator.clipboard.writeText(phone)
            return true
        } catch (e) {
            // Clipboard API 失败（如非 HTTPS / 权限被拒），降级选中文本
        }
    }
    // 降级：选中文本，让用户手动 Ctrl+C
    const ta = document.createElement('textarea')
    ta.value = phone
    ta.style.position = 'fixed'
    ta.style.opacity = '0'
    document.body.appendChild(ta)
    ta.select()
    const ok = document.execCommand('copy')
    document.body.removeChild(ta)
    return ok
}
