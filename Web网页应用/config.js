// 百度地图API配置
const BAIDU_MAP_CONFIG = {
    // 请确保您的AK已经正确配置并且白名单为*
    AK: '',   // 输入你的AK
    API_URL: '//api.map.baidu.com/api',
    VERSION: '1.0',
    TYPE: 'webgl'
};

// 南京市地图中心点配置
const NANJING_CONFIG = {
    // 南京市中心坐标（新街口附近）
    CENTER: {
        lng: 118.778074,
        lat: 32.057236
    },
    DEFAULT_ZOOM: 12,
    MIN_ZOOM: 10,
    MAX_ZOOM: 20,
    CITY_NAME: '南京市'
};

// 地图样式配置
const MAP_STYLES = {
    // 默认样式
    DEFAULT: {
        backgroundColor: '#f0f5ff',
        displayOptions: {
            poi: true,
            poiText: true,
            poiIcon: true,
            overlay: true,
            building: true,
            layer: true
        }
    },
    
    // 简洁样式
    SIMPLE: {
        displayOptions: {
            poi: false,
            poiText: false,
            poiIcon: false,
            building: false
        }
    }
};

export { BAIDU_MAP_CONFIG, NANJING_CONFIG, MAP_STYLES };