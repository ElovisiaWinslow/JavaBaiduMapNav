// markers.js - 景点数据文件（修复版）

// 南京市著名景点标记数据
// 已移除失效的 icon 属性，将使用百度地图默认标记
const NANJING_LANDMARKS = [
    {
        id: 1,
        name: '中山陵',
        description: '孙中山先生的陵寝，中国近代建筑史上第一陵',
        position: { lng: 118.838749, lat: 32.060442 },
        category: '历史遗迹'
    },
    {
        id: 2,
        name: '夫子庙',
        description: '南京历史文化荟萃之地，秦淮风光带核心景区',
        position: { lng: 118.786001, lat: 32.022747 },
        category: '历史文化'
    },
    {
        id: 3,
        name: '南京博物院',
        description: '中国三大博物馆之一，馆藏文物丰富',
        position: { lng: 118.820456, lat: 32.042515 },
        category: '博物馆'
    },
    {
        id: 4,
        name: '玄武湖',
        description: '江南地区最大的城内公园，被誉为"金陵明珠"',
        position: { lng: 118.795289, lat: 32.075833 },
        category: '自然风光'
    },
    {
        id: 5,
        name: '南京长江大桥',
        description: '中国自行设计和建造的双层式铁路、公路两用桥梁',
        position: { lng: 118.736828, lat: 32.115284 },
        category: '现代建筑'
    },
    {
        id: 6,
        name: '总统府',
        description: '中国近代建筑遗存中规模最大、保存最完整的建筑群',
        position: { lng: 118.794128, lat: 32.045402 },
        category: '历史遗迹'
    },
    {
        id: 7,
        name: '南京大屠杀纪念馆',
        description: '为铭记侵华日军南京大屠杀暴行而筹建',
        position: { lng: 118.741441, lat: 32.037962 },
        category: '纪念馆'
    },
    {
        id: 8,
        name: '老门东',
        description: '南京传统民居聚集地，体现老城南风貌',
        position: { lng: 118.788841, lat: 32.019951 },
        category: '历史文化'
    }
];

// 南京市行政区划数据
const NANJING_DISTRICTS = [
    { name: '玄武区', center: { lng: 118.797779, lat: 32.048671 } },
    { name: '秦淮区', center: { lng: 118.79815, lat: 32.011036 } },
    { name: '建邺区', center: { lng: 118.732688, lat: 32.004456 } },
    { name: '鼓楼区', center: { lng: 118.76972, lat: 32.06678 } },
    { name: '浦口区', center: { lng: 118.628005, lat: 32.058797 } },
    { name: '栖霞区', center: { lng: 118.909939, lat: 32.103986 } },
    { name: '雨花台区', center: { lng: 118.779777, lat: 31.992257 } },
    { name: '江宁区', center: { lng: 118.839884, lat: 31.944933 } },
    { name: '六合区', center: { lng: 118.841324, lat: 32.342202 } },
    { name: '溧水区', center: { lng: 119.028722, lat: 31.653066 } },
    { name: '高淳区', center: { lng: 118.875887, lat: 31.327137 } }
];

export { NANJING_LANDMARKS, NANJING_DISTRICTS };